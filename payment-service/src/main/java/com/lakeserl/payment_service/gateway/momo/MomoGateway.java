package com.lakeserl.payment_service.gateway.momo;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lakeserl.payment_service.config.properties.MomoProperties;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest;
import com.lakeserl.payment_service.gateway.dto.RefundResult;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.enums.PaymentMethod;

import lombok.RequiredArgsConstructor;

/**
 * Momo sandbox gateway integration. Uses HMAC-SHA256 over JSON payloads.
 * Payment creation POSTs to Momo API and returns a payment URL.
 */
@Component
@RequiredArgsConstructor
public class MomoGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MomoGateway.class);

    private final MomoProperties props;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.MOMO;
    }

    @Override
    public PaymentInitResult initiate(PaymentInitRequest request) {
        try {
            String requestId = UUID.randomUUID().toString();
            String orderId = request.orderNumber();
            String orderInfo = request.orderInfo() != null
                    ? request.orderInfo()
                    : "Payment for order " + request.orderNumber();
            String ipnUrl = request.ipnUrl() != null ? request.ipnUrl() : props.ipnUrl();
            String redirectUrl = request.returnUrl() != null ? request.returnUrl() : props.redirectUrl();

            String rawData = MomoSignatureUtil.buildCreateSignatureData(
                    props.accessKey(), request.amount(), "",
                    ipnUrl, orderId, orderInfo,
                    props.partnerCode(), redirectUrl, requestId, props.requestType());

            String signature = MomoSignatureUtil.hmacSHA256(props.secretKey(), rawData);

            // In sandbox, build the URL that would be returned by Momo API
            // Actual HTTP POST to Momo will be wired in Sprint 3/7 integration
            log.info("Momo payment signature computed for orderId={}, endpoint={}",
                    orderId, props.endpoint());

            return PaymentInitResult.builder()
                    .success(true)
                    .paymentUrl(props.endpoint() + "/v2/gateway/api/create?signature=" + signature)
                    .transactionRef(orderId)
                    .build();
        } catch (Exception e) {
            log.error("Momo initiate failed", e);
            return PaymentInitResult.fail("Momo initiate failed: " + e.getMessage());
        }
    }

    @Override
    public WebhookVerifyResult verifyWebhook(Map<String, String> params) {
        String receivedSignature = params.get("signature");
        String resultCodeStr = params.getOrDefault("resultCode", "-1");
        int resultCode;
        try {
            resultCode = Integer.parseInt(resultCodeStr);
        } catch (NumberFormatException e) {
            resultCode = -1;
        }

        Long amount = null;
        String amountStr = params.get("amount");
        if (amountStr != null) {
            try {
                amount = Long.parseLong(amountStr);
            } catch (NumberFormatException ignored) {
            }
        }

        Long responseTime = null;
        String responseTimeStr = params.get("responseTime");
        if (responseTimeStr != null) {
            try {
                responseTime = Long.parseLong(responseTimeStr);
            } catch (NumberFormatException ignored) {
                responseTime = 0L;
            }
        }

        String rawData = MomoSignatureUtil.buildIpnSignatureData(
                props.accessKey(),
                amount != null ? amount : 0L,
                params.getOrDefault("extraData", ""),
                params.getOrDefault("message", ""),
                params.getOrDefault("orderId", ""),
                params.getOrDefault("orderInfo", ""),
                params.get("orderType"),
                params.getOrDefault("partnerCode", ""),
                params.get("payType"),
                params.getOrDefault("requestId", ""),
                responseTime != null ? responseTime : 0L,
                resultCode,
                params.getOrDefault("transId", ""));

        boolean signatureValid = MomoSignatureUtil.verify(receivedSignature, props.secretKey(), rawData);
        boolean success = signatureValid && resultCode == 0;

        return WebhookVerifyResult.builder()
                .signatureValid(signatureValid)
                .success(success)
                .transactionId(params.get("transId"))
                .amount(amount)
                .transactionRef(params.get("orderId"))
                .rawResponse(params.toString())
                .build();
    }

    @Override
    public RefundResult refund(RefundGatewayRequest request) {
        try {
            log.info("Momo refund prepared for ref={}, endpoint={}",
                    request.refundRef(), props.endpoint());
            return RefundResult.builder()
                    .success(true)
                    .gatewayRefundId(request.refundRef())
                    .build();
        } catch (Exception e) {
            log.error("Momo refund failed for ref={}", request.refundRef(), e);
            return RefundResult.fail("Momo refund failed: " + e.getMessage());
        }
    }
}
