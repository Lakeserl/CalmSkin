package com.lakeserl.user_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.WishlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist management APIs")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get wishlist")
    public ResponseEntity<ApiResponse<List<UUID>>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(principal.getId())));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID productId) {
        wishlistService.addToWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok("Added to wishlist"));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID productId) {
        wishlistService.removeFromWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.ok("Removed from wishlist"));
    }
}
