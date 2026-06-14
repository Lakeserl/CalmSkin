package com.lakeserl.shipping_service.controller;

import com.lakeserl.shipping_service.dto.request.WebhookEventRequest;
import com.lakeserl.shipping_service.dto.response.ApiResponse;
import com.lakeserl.shipping_service.dto.response.ShipmentDTO;
import com.lakeserl.shipping_service.entity.Shipment;
import com.lakeserl.shipping_service.enums.ShippingProvider;
import com.lakeserl.shipping_service.exception.WebhookAuthException;
import com.lakeserl.shipping_service.service.ShipmentMapper;
import com.lakeserl.shipping_service.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Carriers POST status updates here. Each callback must carry the configured
// shared secret in X-Webhook-Secret. Mock carrier uses /webhooks/mock for
// local development.
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final ShipmentService shipmentService;
    private final ShipmentMapper mapper;

    @Value("${app.shipping.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/{provider}")
    public ApiResponse<ShipmentDTO> handleWebhook(
            @PathVariable ShippingProvider provider,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @Valid @RequestBody WebhookEventRequest payload) {
        if (secret == null || !java.security.MessageDigest.isEqual(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new WebhookAuthException("Invalid or missing webhook secret");
        }
        Shipment shipment = shipmentService.applyWebhook(
                payload.trackingNumber(),
                payload.status(),
                payload.description(),
                payload.location(),
                "provider=" + provider.name());
        return ApiResponse.ok("Webhook applied", mapper.toDto(shipment, List.of()));
    }
}
