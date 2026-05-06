package com.lakeserl.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ingredient_conflicts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ingredient_conflict", columnNames = {"ingredient_a_id", "ingredient_b_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conflict_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_a_id", nullable = false)
    private Ingredient ingredientA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_b_id", nullable = false)
    private Ingredient ingredientB;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "severity", length = 20)
    private String severity;
}
