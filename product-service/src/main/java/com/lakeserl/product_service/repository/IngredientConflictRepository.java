package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.IngredientConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientConflictRepository extends JpaRepository<IngredientConflict, Long> {

    @Query("SELECT ic FROM IngredientConflict ic WHERE " +
           "(ic.ingredientA.id IN :ingredientIds AND ic.ingredientB.id IN :ingredientIds)")
    List<IngredientConflict> findConflictsBetween(@Param("ingredientIds") List<Long> ingredientIds);
}
