package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByNameIn(List<String> names);
    
    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.inciName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Ingredient> searchIngredients(@Param("query") String query, Pageable pageable);
}
