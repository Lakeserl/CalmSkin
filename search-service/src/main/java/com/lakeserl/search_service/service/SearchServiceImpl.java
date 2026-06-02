package com.lakeserl.search_service.service;

import com.lakeserl.search_service.client.ProductServiceClient;
import com.lakeserl.search_service.document.ProductDocument;
import com.lakeserl.search_service.dto.ApiResponse;
import com.lakeserl.search_service.dto.ProductIndexDTO;
import com.lakeserl.search_service.dto.SearchResponse;
import com.lakeserl.search_service.dto.SearchResultDTO;
import com.lakeserl.search_service.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final String TRENDING_KEY = "search:trending";

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductServiceClient productServiceClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public SearchResponse search(String query, String category, String brand,
                                 Long minPrice, Long maxPrice, String skinType,
                                 int page, int size) {
        Criteria criteria = buildCriteria(query, category, brand, minPrice, maxPrice, skinType);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria, PageRequest.of(page, size));

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(criteriaQuery, ProductDocument.class);

        List<SearchResultDTO> results = hits.getSearchHits().stream()
                .map(this::toResult)
                .collect(Collectors.toList());

        if (query != null && !query.isBlank()) {
            // Increment trending score for this search term (async-safe — Redis ZINCRBY is atomic)
            redisTemplate.opsForZSet().incrementScore(TRENDING_KEY, query.toLowerCase().trim(), 1.0);
        }

        long total = hits.getTotalHits();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        return SearchResponse.builder()
                .results(results)
                .totalHits(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public List<String> suggest(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        // Use the stored suggestions from productName field — query-time prefix match via ES completion suggester
        // For simplicity, do a prefix criteria search on the name field and return names
        Criteria criteria = new Criteria("name").startsWith(query.toLowerCase());
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria, PageRequest.of(0, limit));
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(criteriaQuery, ProductDocument.class);
        return hits.getSearchHits().stream()
                .map(h -> h.getContent().getName())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> trending(int limit) {
        Set<String> terms = redisTemplate.opsForZSet().reverseRange(TRENDING_KEY, 0, limit - 1);
        return terms != null ? new ArrayList<>(terms) : List.of();
    }

    @Override
    public void indexProduct(ProductIndexDTO dto) {
        searchRepository.save(toDocument(dto));
        log.debug("Indexed product id={}", dto.getId());
    }

    @Override
    public void updateProduct(ProductIndexDTO dto) {
        searchRepository.save(toDocument(dto));
        log.debug("Updated product id={} in index", dto.getId());
    }

    @Override
    public void deleteProduct(String productId) {
        searchRepository.deleteById(productId);
        log.debug("Deleted product id={} from index", productId);
    }

    @Override
    public int reindexAll() {
        int page = 0;
        int total = 0;
        List<ProductIndexDTO> batch;
        do {
            ApiResponse<List<ProductIndexDTO>> response =
                    productServiceClient.getAllForIndex(internalSecret, page, 500);
            batch = response != null && response.getData() != null ? response.getData() : List.of();
            if (!batch.isEmpty()) {
                List<ProductDocument> docs = batch.stream().map(this::toDocument).collect(Collectors.toList());
                searchRepository.saveAll(docs);
                total += docs.size();
                log.info("Reindex: saved page={}, count={}, cumulative={}", page, docs.size(), total);
            }
            page++;
        } while (batch.size() == 500);

        log.info("Reindex complete — {} products indexed", total);
        return total;
    }

    private Criteria buildCriteria(String query, String category, String brand,
                                   Long minPrice, Long maxPrice, String skinType) {
        Criteria criteria = new Criteria("status").is("ACTIVE");

        if (query != null && !query.isBlank()) {
            Criteria textSearch = new Criteria("name").matches(query)
                    .or(new Criteria("description").matches(query))
                    .or(new Criteria("brandName").matches(query))
                    .or(new Criteria("ingredients").matches(query));
            criteria = criteria.and(textSearch);
        }
        if (category != null && !category.isBlank()) {
            criteria = criteria.and(new Criteria("categoryName").is(category));
        }
        if (brand != null && !brand.isBlank()) {
            criteria = criteria.and(new Criteria("brandName").is(brand));
        }
        if (minPrice != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(minPrice));
        }
        if (maxPrice != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(maxPrice));
        }
        if (skinType != null && !skinType.isBlank()) {
            criteria = criteria.and(new Criteria("skinTypes").is(skinType.toUpperCase()));
        }
        return criteria;
    }

    private SearchResultDTO toResult(SearchHit<ProductDocument> hit) {
        ProductDocument doc = hit.getContent();
        return SearchResultDTO.builder()
                .id(doc.getId())
                .name(doc.getName())
                .brandName(doc.getBrandName())
                .categoryName(doc.getCategoryName())
                .price(doc.getPrice())
                .status(doc.getStatus())
                .primaryImageUrl(doc.getPrimaryImageUrl())
                .soldCount(doc.getSoldCount())
                .build();
    }

    private ProductDocument toDocument(ProductIndexDTO dto) {
        List<String> suggestionInputs = new ArrayList<>();
        if (dto.getName() != null) suggestionInputs.add(dto.getName());
        if (dto.getBrandName() != null) suggestionInputs.add(dto.getBrandName());

        return ProductDocument.builder()
                .id(String.valueOf(dto.getId()))
                .name(dto.getName())
                .sku(dto.getSku())
                .description(dto.getDescription())
                .brandName(dto.getBrandName())
                .categoryName(dto.getCategoryName())
                .ingredients(dto.getIngredients())
                .skinTypes(dto.getSkinTypes())
                .skinConcerns(dto.getSkinConcerns())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .soldCount(dto.getSoldCount())
                .createdAt(dto.getCreatedAt())
                .primaryImageUrl(dto.getPrimaryImageUrl())
                .suggest(new Completion(suggestionInputs.toArray(new String[0])))
                .build();
    }
}
