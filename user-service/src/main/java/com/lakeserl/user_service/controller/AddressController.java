package com.lakeserl.user_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.request.AddressRequest;
import com.lakeserl.user_service.model.dto.response.AddressResponse;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.security.userDetails.CustomUserDetails;
import com.lakeserl.user_service.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "User address management APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get all addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getAddresses(principal.getId())));
    }

    @PostMapping
    @Operation(summary = "Create a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.createAddress(principal.getId(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.updateAddress(principal.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        addressService.deleteAddress(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted"));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.setDefaultAddress(principal.getId(), id)));
    }
}
