package com.lakeserl.user_service.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lakeserl.user_service.model.dto.UserDTO;
import com.lakeserl.user_service.model.dto.response.AddressResponse;
import com.lakeserl.user_service.model.dto.response.ApiResponse;
import com.lakeserl.user_service.model.entity.UserPoint;
import com.lakeserl.user_service.service.AddressService;
import com.lakeserl.user_service.service.AuthService;
import com.lakeserl.user_service.service.LoyaltyService;
import com.lakeserl.user_service.service.UserService;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

    private final UserService userService;
    private final AddressService addressService;
    private final LoyaltyService loyaltyService;
    private final AuthService authService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(id)));
    }

    @GetMapping("/{id}/addresses/default")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.getDefaultAddress(id)));
    }

    @GetMapping("/{id}/points")
    public ResponseEntity<ApiResponse<UserPoint>> getPoints(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getPoints(id)));
    }

    @PostMapping("/{id}/points")
    public ResponseEntity<ApiResponse<Void>> addPoints(
            @PathVariable UUID id,
            @RequestParam int points,
            @RequestParam String referenceId,
            @RequestParam(defaultValue = "ORDER") String referenceType,
            @RequestParam(defaultValue = "Points from order") String description) {
        loyaltyService.earnPoints(id, points, referenceId, referenceType, description);
        return ResponseEntity.ok(ApiResponse.ok("Points added"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> validateToken(@RequestBody Map<String, String> body) {
        boolean valid = authService.validateToken(body.get("token"));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("valid", valid)));
    }
}
