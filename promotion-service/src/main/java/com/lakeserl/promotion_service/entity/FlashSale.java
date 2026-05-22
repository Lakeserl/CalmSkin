package com.lakeserl.promotion_service.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A flash-sale line: a product (optionally a variant) sold at {@code salePrice}
 * for up to {@code quantityLimit} units. {@code quantityReserved} counts units
 * held by in-flight order locks; {@code quantitySold} counts confirmed sales.
 */
@Entity
@Table(name = "flash_sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "original_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "sale_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "quantity_limit", nullable = false)
    private Integer quantityLimit;

    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;

    /** Units still available = limit - sold - reserved. */
    public int available() {
        return quantityLimit - quantitySold - quantityReserved;
    }
}
