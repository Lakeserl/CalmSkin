package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
