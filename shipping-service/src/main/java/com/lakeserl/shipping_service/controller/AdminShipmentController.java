package com.lakeserl.shipping_service.controller;

import com.lakeserl.shipping_service.dto.request.CancelShipmentRequest;
import com.lakeserl.shipping_service.dto.request.UpdateShipmentStatusRequest;
import com.lakeserl.shipping_service.dto.response.ApiResponse;
import com.lakeserl.shipping_service.dto.response.ApiResponse.PageInfo;
import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShipmentStatus;
import com.lakeserl.shipping_service.enums.TrackingEventSource;
import com.lakeserl.shipping_service.service.ShipmentMapper;
import com.lakeserl.shipping_service.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/shipments")
@RequiredArgsConstructor
public class AdminShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentMapper mapper;

    @GetMapping
    public ApiResponse<List<ShipmentDTO>> list(
            @RequestParam(required = false) ShipmentStatus status,
            Pageable pageable) {
        Page<ShipmentDTO> page = shipmentService.listByStatus(status, pageable);
        return ApiResponse.ok("OK", page.getContent(), PageInfo.builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build());
    }

    @GetMapping("/{id}")
    public ApiResponse<ShipmentDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(shipmentService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ShipmentDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        Shipment updated = shipmentService.updateStatus(id, request.status(),
                request.description(), request.location(),
                TrackingEventSource.ADMIN_MANUAL, null);
        return ApiResponse.ok("Status updated", mapper.toDto(updated, List.of()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<ShipmentDTO> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelShipmentRequest request) {
        ShipmentDTO existing = shipmentService.getById(id);
        Shipment cancelled = shipmentService.cancelByOrderId(existing.getOrderId(), request.reason());
        return ApiResponse.ok("Cancelled", mapper.toDto(cancelled, List.of()));
    }
}
