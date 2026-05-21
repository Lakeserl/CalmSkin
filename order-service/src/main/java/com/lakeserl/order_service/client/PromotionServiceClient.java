package com.lakeserl.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component("promotionServiceApiClient")
@RequiredArgsConstructor
public class PromotionServiceClient {

    private final RestClient promotionServiceClient;

    @CircuitBreaker(name = "promotion-service", fallbackMethod = "fallbackValidateVoucher")
    public VoucherValidationResponse validateVoucher(VoucherValidationRequest request) {
        log.info("Validating voucherCode={} for userId={} against promotion-service", request.getVoucherCode(), request.getUserId());
        return promotionServiceClient.post()
                .uri("/internal/promotions/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VoucherValidationResponse.class);
    }

    public VoucherValidationResponse fallbackValidateVoucher(VoucherValidationRequest request, Throwable t) {
        log.error("Promotion service fallback triggered for voucherCode={} due to: {}. Allowing order to proceed without discount.",
                request.getVoucherCode(), t.getMessage());
        return VoucherValidationResponse.builder()
                .valid(false)
                .discountAmount(BigDecimal.ZERO)
                .reason("Promotion validation is temporarily unavailable (" + t.getMessage() + ")")
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherValidationRequest {
        private String voucherCode;
        private BigDecimal subtotal;
        private UUID userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VoucherValidationResponse {
        private boolean valid;
        private BigDecimal discountAmount;
        private String reason;
    }
}
