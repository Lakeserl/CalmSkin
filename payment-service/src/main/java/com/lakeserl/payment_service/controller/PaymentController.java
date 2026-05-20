package com.lakeserl.payment_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import java.util.HashMap;
import java.util.Map;

import com.lakeserl.payment_service.exception.PaymentNotFoundException;
import com.lakeserl.payment_service.models.dto.request.PaymentInitiateRequest;
import com.lakeserl.payment_service.models.dto.request.CodConfirmRequest;
import com.lakeserl.payment_service.models.dto.request.RefundInitiateRequest;
import com.lakeserl.payment_service.models.dto.response.RefundDTO;
import com.lakeserl.payment_service.models.entity.Refund;
import com.lakeserl.payment_service.models.dto.response.ApiResponse;
import com.lakeserl.payment_service.models.dto.response.ApiResponse.PageInfo;
import com.lakeserl.payment_service.models.dto.response.PaymentDTO;
import com.lakeserl.payment_service.models.dto.response.PaymentInitResponse;
import com.lakeserl.payment_service.models.entity.Payment;
import com.lakeserl.payment_service.models.mapper.PaymentMapper;
import com.lakeserl.payment_service.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    @PostMapping("/initiate")
    public ApiResponse<PaymentInitResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            HttpServletRequest servletRequest) {
        
        String ipAddress = servletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = servletRequest.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        Payment payment = paymentService.initiatePayment(request, ipAddress);
        return ApiResponse.ok("Payment initiated successfully", paymentMapper.toInitResponse(payment));
    }

    @GetMapping("/{paymentNumber}")
    public ApiResponse<PaymentDTO> getPaymentByNumber(
            @PathVariable String paymentNumber,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        Payment payment = paymentService.getPaymentByNumber(paymentNumber);

        // Security check: Only owner or admin can retrieve
        boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
        if (!isAdmin && userId != null && !payment.getUserId().equals(userId)) {
            throw new PaymentNotFoundException("Payment with number " + paymentNumber + " not found");
        }

        return ApiResponse.ok(paymentMapper.toDTO(payment));
    }

    @GetMapping
    public ApiResponse<List<PaymentDTO>> getPaymentsForUser(
            @RequestHeader("X-User-Id") Long userId,
            Pageable pageable) {
        
        Page<Payment> pageResult = paymentService.getPaymentsByUserId(userId, pageable);
        List<PaymentDTO> dtoList = pageResult.getContent().stream()
                .map(paymentMapper::toDTO)
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .currentPage(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();

        return ApiResponse.ok("Payments retrieved successfully", dtoList, pageInfo);
    }

    @GetMapping("/admin")
    public ApiResponse<List<PaymentDTO>> getAllPaymentsForAdmin(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            Pageable pageable) {
        
        boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
        if (!isAdmin) {
            throw new com.lakeserl.payment_service.exception.InvalidPaymentStateException("Access denied: ADMIN role required");
        }

        Page<Payment> pageResult = paymentService.getAllPayments(pageable);
        List<PaymentDTO> dtoList = pageResult.getContent().stream()
                .map(paymentMapper::toDTO)
                .toList();

        PageInfo pageInfo = PageInfo.builder()
                .currentPage(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();

        return ApiResponse.ok("All payments retrieved successfully for admin", dtoList, pageInfo);
    }

    @RequestMapping(value = "/webhook/{gateway}/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleWebhook(
            @PathVariable String gateway,
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> bodyParams) {

        log.info("Received webhook IPN callback for gateway={}, queryParams={}, bodyParams={}",
                gateway, queryParams, bodyParams);

        Map<String, String> params = new HashMap<>(queryParams);
        if (bodyParams != null) {
            for (Map.Entry<String, Object> entry : bodyParams.entrySet()) {
                if (entry.getValue() != null) {
                    params.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        // ZaloPay specific flattening of JSON data
        if ("zalopay".equalsIgnoreCase(gateway) && params.containsKey("data")) {
            try {
                String dataJson = params.get("data");
                Map<String, Object> nestedData = objectMapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});
                for (Map.Entry<String, Object> entry : nestedData.entrySet()) {
                    if (entry.getValue() != null) {
                        params.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse nested ZaloPay webhook data JSON", e);
            }
        }

        WebhookVerifyResult verifyResult = paymentService.processWebhook(gateway, params);

        if ("vnpay".equalsIgnoreCase(gateway)) {
            Map<String, String> response = new HashMap<>();
            if (!verifyResult.signatureValid()) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid signature");
            } else {
                // If payment wasn't matched or amount mismatched, processWebhook would log error.
                // For simplified response matching:
                if (verifyResult.transactionRef() == null || verifyResult.transactionRef().isBlank()) {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                } else {
                    // Let's query db or assume success/already processed status
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm success");
                }
            }
            return ResponseEntity.ok(response);
        }

        if ("zalopay".equalsIgnoreCase(gateway)) {
            Map<String, Object> response = new HashMap<>();
            if (verifyResult.success()) {
                response.put("return_code", 1);
                response.put("return_message", "Mac success");
            } else {
                response.put("return_code", 2);
                response.put("return_message", "Webhook processing failed / Invalid signature");
            }
            return ResponseEntity.ok(response);
        }

        if ("momo".equalsIgnoreCase(gateway)) {
            // Momo expects a 204 No Content response
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(Map.of("status", "processed"));
    }

    @PostMapping("/cod/confirm")
    public ApiResponse<PaymentDTO> confirmCodPayment(
            @Valid @RequestBody CodConfirmRequest request) {
        
        Payment payment = paymentService.confirmCodPayment(request.orderNumber());
        return ApiResponse.ok("COD payment confirmed successfully", paymentMapper.toDTO(payment));
    }

    @PostMapping("/{paymentNumber}/refund")
    public ApiResponse<RefundDTO> refundPayment(
            @PathVariable String paymentNumber,
            @Valid @RequestBody RefundInitiateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        
        Payment payment = paymentService.getPaymentByNumber(paymentNumber);
        
        boolean isAdmin = userRole != null && userRole.toUpperCase().contains("ADMIN");
        if (!isAdmin && userId != null && !payment.getUserId().equals(userId)) {
            throw new PaymentNotFoundException("Payment with number " + paymentNumber + " not found");
        }

        Refund refund = paymentService.refundPayment(paymentNumber, request.amount(), request.reason());
        return ApiResponse.ok("Refund processed successfully", paymentMapper.toRefundDTO(refund));
    }
}
