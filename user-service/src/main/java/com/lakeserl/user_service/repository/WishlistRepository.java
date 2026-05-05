package com.lakeserl.user_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakeserl.user_service.model.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    List<Wishlist> findByUserId(UUID userId);

    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}
