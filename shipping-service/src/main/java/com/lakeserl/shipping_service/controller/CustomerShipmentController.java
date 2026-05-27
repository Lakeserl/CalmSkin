package com.lakeserl.shipping_service.controller;

import com.lakeserl.shipping_service.dto.response.ApiResponse;
import com.lakeserl.shipping_service.dto.response.ApiResponse.PageInfo;
import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Customer-facing shipment endpoints.
 *
 * <p>All responses are ownership-scoped to the caller's UUID (extracted from
 * the {@code X-User-Id} header injected by the API Gateway via
 * {@link com.lakeserl.shipping_service.config.RoleHeaderAuthenticationFilter}).
 * This prevents IDOR: a customer can only see their own shipments.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class CustomerShipmentController {

    private final ShipmentService shipmentService;

    /**
     * GET /api/v1/shipments/mine — paginated list of the caller's shipments,
     * newest first.
     */
    @GetMapping("/mine")
    public ApiResponse<List<ShipmentDTO>> listMine(Authentication auth, Pageable pageable) {
        UUID userId = parseUserId(auth);
        Page<ShipmentDTO> page = shipmentService.listByUserId(userId, pageable);
        return ApiResponse.ok("OK", page.getContent(), PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build());
    }

    /**
     * GET /api/v1/shipments/by-order/{orderNumber} — full tracking timeline
     * for a specific order belonging to the caller.
     *
     * <p>Returns 404 (not 403) when the order exists but belongs to a
     * different user, to avoid leaking order existence (IDOR mitigation).
     */
    @GetMapping("/by-order/{orderNumber}")
    public ApiResponse<ShipmentDTO> getByOrder(
            @PathVariable String orderNumber,
            Authentication auth) {
        UUID userId = parseUserId(auth);
        return ApiResponse.ok(shipmentService.getByOrderNumberForUser(orderNumber, userId));
    }

    private UUID parseUserId(Authentication auth) {
        // RoleHeaderAuthenticationFilter sets the principal name to the
        // value of the X-User-Id header forwarded by the API Gateway.
        return UUID.fromString((String) auth.getPrincipal());
    }
}
