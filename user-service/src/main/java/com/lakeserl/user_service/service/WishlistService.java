package com.lakeserl.user_service.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.entity.Wishlist;
import com.lakeserl.user_service.repository.UserRepository;
import com.lakeserl.user_service.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

    public List<Long> getWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(Wishlist::getProductId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWishlist(UUID userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // Already exists
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        wishlistRepository.save(Wishlist.builder()
                .user(user).productId(productId).build());
    }

    @Transactional
    public void removeFromWishlist(UUID userId, Long productId) {
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistRepository::delete);
    }
}
