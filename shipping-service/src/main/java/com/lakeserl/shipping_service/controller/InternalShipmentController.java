package com.lakeserl.shipping_service.controller;

import com.lakeserl.shipping_service.dto.response.ApiResponse;
import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/shipments")
@RequiredArgsConstructor
public class InternalShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/by-order/{orderNumber}")
    public ApiResponse<ShipmentDTO> getByOrderNumber(@PathVariable String orderNumber) {
        return ApiResponse.ok(shipmentService.getByOrderNumber(orderNumber));
    }

    @GetMapping("/{id}")
    public ApiResponse<ShipmentDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(shipmentService.getById(id));
    }
}
