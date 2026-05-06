package com.lakeserl.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "product_ingredients",
    indexes = {
        @Index(name = "idx_pi_product", columnList = "product_id"),
        @Index(name = "idx_pi_ingredient", columnList = "ingredient_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_ingredient", columnNames = {"product_id", "ingredient_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_ingredient_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "concentration_percent", precision = 5, scale = 2)
    private BigDecimal concentrationPercent;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_key_ingredient", nullable = false)
    @Builder.Default
    private Boolean isKeyIngredient = false;
}
