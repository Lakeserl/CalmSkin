package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    Page<Product> findByIsFeaturedTrueAndStatus(ProductStatus status, Pageable pageable);
    
    Page<Product> findByIsNewArrivalTrueAndStatus(ProductStatus status, Pageable pageable);
    
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);
    
    Page<Product> findByBrandIdAndStatus(Long brandId, ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.soldCount DESC")
    Page<Product> findBestSellers(Pageable pageable);

    /** Atomically increases sold_count when an order completes (Kafka order.completed). */
    @Modifying
    @Query("UPDATE Product p SET p.soldCount = p.soldCount + :quantity WHERE p.id = :productId")
    int incrementSoldCount(@Param("productId") Long productId, @Param("quantity") long quantity);
    
    // For similar products, we use a basic query here, but complex logic might be in the service layer using specifications or custom queries
    @Query(value = "SELECT * FROM products p " +
                   "WHERE p.category_id = :categoryId " +
                   "AND p.status = 'ACTIVE' " +
                   "AND p.product_id != :excludeId " +
                   "ORDER BY CASE WHEN p.brand_id = :brandId THEN 1 ELSE 2 END, " +
                   "CASE WHEN p.usage_step = :usageStep THEN 1 ELSE 2 END " +
                   "LIMIT :limit", nativeQuery = true)
    List<Product> findSimilarProducts(@Param("categoryId") Long categoryId, 
                                      @Param("brandId") Long brandId, 
                                      @Param("usageStep") String usageStep, 
                                      @Param("excludeId") Long excludeId, 
                                      @Param("limit") int limit);

    /**
     * Recommendation query (v1, rule-based):
     * - Only ACTIVE products
     * - Exclude products already purchased by the user
     * - Boost products whose brand the user has purchased before (brand affinity)
     * - Filter by skinType match OR skinConcern match in JSON columns
     *
     * skinType: stored as JSON array e.g. ["OILY","COMBINATION"] — use native JSON containment.
     * skinConcern list: each concern is matched individually with LIKE against the JSON array string.
     *
     * This is a best-effort native query. For production at scale, migrate to Elasticsearch.
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.status = 'ACTIVE'
              AND (:excludeIds IS NULL OR p.product_id NOT IN (:excludeIds))
              AND (
                    (:skinType IS NULL OR p.suitable_skin_types::text LIKE CONCAT('%', :skinType, '%'))
                 OR (:concern1 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern1, '%'))
                 OR (:concern2 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern2, '%'))
                 OR (:concern3 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern3, '%'))
              )
            ORDER BY
              CASE WHEN :boostBrandId IS NOT NULL AND p.brand_id = :boostBrandId THEN 0 ELSE 1 END,
              (SELECT COALESCE(rs.average_rating, 0) FROM review_summaries rs WHERE rs.product_id = p.product_id) DESC,
              p.sold_count DESC
            LIMIT :limitCount
            """, nativeQuery = true)
    List<Product> findRecommendations(
            @Param("skinType") String skinType,
            @Param("concern1") String concern1,
            @Param("concern2") String concern2,
            @Param("concern3") String concern3,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("boostBrandId") Long boostBrandId,
            @Param("limitCount") int limitCount);

    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.status = 'ACTIVE'
              AND p.usage_step = :usageStep
              AND (
                    (:skinType IS NULL OR p.suitable_skin_types::text LIKE CONCAT('%', :skinType, '%'))
                 OR (:concern1 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern1, '%'))
                 OR (:concern2 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern2, '%'))
                 OR (:concern3 IS NULL OR p.skin_concerns::text LIKE CONCAT('%', :concern3, '%'))
              )
            ORDER BY
              (SELECT COALESCE(rs.average_rating, 0) FROM review_summaries rs WHERE rs.product_id = p.product_id) DESC,
              p.sold_count DESC
            LIMIT :limitCount
            """, nativeQuery = true)
    List<Product> findRoutineProducts(
            @Param("usageStep") String usageStep,
            @Param("skinType") String skinType,
            @Param("concern1") String concern1,
            @Param("concern2") String concern2,
            @Param("concern3") String concern3,
            @Param("limitCount") int limitCount);
}

