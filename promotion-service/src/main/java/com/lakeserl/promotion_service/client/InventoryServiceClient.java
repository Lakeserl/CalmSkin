package com.lakeserl.promotion_service.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Calls inventory-service internal APIs to hold stock for free-gift / sample
 * products. A free-gift hold uses a distinct reservation key
 * ({@code <orderId>:promo-gift}) so it never collides with the cart-item
 * reservation order-service makes under the bare orderId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final RestClient inventoryServiceClient;

    /** Reservation key suffix that isolates promotion holds from cart holds. */
    public static String giftReservationKey(String orderId) {
        return orderId + ":promo-gift";
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackReserve")
    public void reserve(String reservationKey, List<ReserveItem> items) {
        inventoryServiceClient.post()
                .uri("/internal/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReserveStockRequest(reservationKey, items))
                .retrieve()
                .toBodilessEntity();
        log.info("Reserved {} gift item(s) under key {}", items.size(), reservationKey);
    }

    void fallbackReserve(String reservationKey, List<ReserveItem> items, Throwable t) {
        log.error("inventory reserve failed for key {}: {}", reservationKey, t.getMessage());
        throw new IllegalStateException("Inventory service unavailable for gift reservation", t);
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackOrderRef")
    public void confirm(String reservationKey) {
        inventoryServiceClient.post()
                .uri("/internal/inventory/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OrderRef(reservationKey))
                .retrieve()
                .toBodilessEntity();
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "fallbackOrderRef")
    public void release(String reservationKey) {
        inventoryServiceClient.post()
                .uri("/internal/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OrderRef(reservationKey))
                .retrieve()
                .toBodilessEntity();
    }

    void fallbackOrderRef(String reservationKey, Throwable t) {
        // confirm/release are reconciled by inventory-service's own expiry sweep,
        // so a transient failure here is logged rather than fatal.
        log.error("inventory confirm/release failed for key {}: {}", reservationKey, t.getMessage());
    }

    public record ReserveItem(Long productId, Long variantId, Integer quantity) {
    }

    private record ReserveStockRequest(String orderId, List<ReserveItem> items) {
    }

    private record OrderRef(String orderId) {
    }
}
