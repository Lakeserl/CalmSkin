package com.lakeserl.product_service.specification;

import com.lakeserl.product_service.entity.Brand;
import com.lakeserl.product_service.entity.Category;
import com.lakeserl.product_service.entity.Product;
import com.lakeserl.product_service.entity.ProductTag;
import com.lakeserl.product_service.enums.ProductStatus;
import com.lakeserl.product_service.enums.ProductUsageStep;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            Join<Product, Brand> brandJoin = root.join("brand", JoinType.LEFT);
            Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("description")), likePattern),
                    cb.like(cb.lower(brandJoin.get("name")), likePattern),
                    cb.like(cb.lower(categoryJoin.get("name")), likePattern)
            );
        };
    }

    public static Specification<Product> hasCategorySlug(String categorySlug) {
        return (root, query, cb) -> {
            if (categorySlug == null || categorySlug.trim().isEmpty()) {
                return null;
            }
            Join<Product, Category> categoryJoin = root.join("category", JoinType.INNER);
            return cb.equal(categoryJoin.get("slug"), categorySlug);
        };
    }

    public static Specification<Product> hasBrandSlug(String brandSlug) {
        return (root, query, cb) -> {
            if (brandSlug == null || brandSlug.trim().isEmpty()) {
                return null;
            }
            Join<Product, Brand> brandJoin = root.join("brand", JoinType.INNER);
            return cb.equal(brandJoin.get("slug"), brandSlug);
        };
    }

    public static Specification<Product> hasSkinType(String skinType) {
        return (root, query, cb) -> {
            if (skinType == null || skinType.trim().isEmpty()) {
                return null;
            }
            // Using PostgreSQL JSONB operator to check if array contains element
            return cb.isTrue(
                    cb.function("jsonb_contains", Boolean.class,
                            root.get("suitableSkinTypes"),
                            cb.literal("[\"" + skinType + "\"]"))
            );
        };
    }

    public static Specification<Product> hasSkinConcern(String skinConcern) {
        return (root, query, cb) -> {
            if (skinConcern == null || skinConcern.trim().isEmpty()) {
                return null;
            }
            // Using PostgreSQL JSONB operator
            return cb.isTrue(
                    cb.function("jsonb_contains", Boolean.class,
                            root.get("skinConcerns"),
                            cb.literal("[\"" + skinConcern + "\"]"))
            );
        };
    }

    public static Specification<Product> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return null;
            }
            // Use salePrice if not null, otherwise basePrice
            Expression<BigDecimal> actualPrice = cb.coalesce(root.get("salePrice"), root.get("basePrice"));
            return cb.greaterThanOrEqualTo(actualPrice, minPrice);
        };
    }

    public static Specification<Product> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return null;
            }
            Expression<BigDecimal> actualPrice = cb.coalesce(root.get("salePrice"), root.get("basePrice"));
            return cb.lessThanOrEqualTo(actualPrice, maxPrice);
        };
    }

    public static Specification<Product> hasTagSlug(String tagSlug) {
        return (root, query, cb) -> {
            if (tagSlug == null || tagSlug.trim().isEmpty()) {
                return null;
            }
            Join<Product, ProductTag> productTagJoin = root.join("productTags", JoinType.INNER);
            return cb.equal(productTagJoin.join("tag").get("slug"), tagSlug);
        };
    }

    public static Specification<Product> hasUsageStep(ProductUsageStep usageStep) {
        return (root, query, cb) -> usageStep == null ? null : cb.equal(root.get("usageStep"), usageStep);
    }

    public static Specification<Product> isFeatured(Boolean isFeatured) {
        return (root, query, cb) -> isFeatured == null ? null : cb.equal(root.get("isFeatured"), isFeatured);
    }

    public static Specification<Product> isNewArrival(Boolean isNewArrival) {
        return (root, query, cb) -> isNewArrival == null ? null : cb.equal(root.get("isNewArrival"), isNewArrival);
    }
}
