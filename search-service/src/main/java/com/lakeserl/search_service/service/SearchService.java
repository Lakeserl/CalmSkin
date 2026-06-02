package com.lakeserl.search_service.service;

import com.lakeserl.search_service.dto.SearchResponse;
import com.lakeserl.search_service.dto.ProductIndexDTO;

import java.util.List;

public interface SearchService {

    SearchResponse search(String query, String category, String brand,
                          Long minPrice, Long maxPrice, String skinType,
                          int page, int size);

    List<String> suggest(String query, int limit);

    List<String> trending(int limit);

    void indexProduct(ProductIndexDTO dto);

    void updateProduct(ProductIndexDTO dto);

    void deleteProduct(String productId);

    int reindexAll();
}
