package com.lakeserl.product_service.entity;

import com.lakeserl.product_service.enums.IngredientSafetyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
    name = "ingredients",
    indexes = {
        @Index(name = "idx_ingredient_name", columnList = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "inci_name", length = 300)
    private String inciName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "benefits", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> benefits;

    @Column(name = "side_effects", columnDefinition = "TEXT")
    private String sideEffects;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_level", length = 20)
    @Builder.Default
    private IngredientSafetyLevel safetyLevel = IngredientSafetyLevel.SAFE;

    @Column(name = "suitable_skin_types", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> suitableSkinTypes;

    @Column(name = "avoid_skin_concerns", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> avoidSkinConcerns;

    @Column(name = "is_common_allergen", nullable = false)
    @Builder.Default
    private Boolean isCommonAllergen = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
