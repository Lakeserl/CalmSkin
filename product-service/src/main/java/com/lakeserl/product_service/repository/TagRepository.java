package com.lakeserl.product_service.repository;

import com.lakeserl.product_service.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
}
