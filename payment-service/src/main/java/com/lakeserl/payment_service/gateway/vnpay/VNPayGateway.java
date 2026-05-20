package com.lakeserl.payment_service.gateway.vnpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lakeserl.payment_service.config.properties.VNPayProperties;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest;
import com.lakeserl.payment_service.gateway.dto.RefundResult;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.enums.PaymentMethod;

import lombok.RequiredArgsConstructor;

/**
 * VNPay sandbox gateway integration. Builds payment URLs using VNPay's
 * query-param / HMAC-SHA512 protocol and verifies IPN callbacks.
 * <p>
 * Reference: {@code vnpay_jsp/} sample in this repo.
 */
@Component
@RequiredArgsConstructor
public class VNPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(VNPayGateway.class);
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMddHHmmss");

    private final VNPayProperties props;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.VNPAY;
    }

    // ── initiate ──────────────────────────────────────────────────────────

    @Override
    public PaymentInitResult initiate(PaymentInitRequest request) {
        try {
            String txnRef = request.orderNumber();

            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", props.version());
            params.put("vnp_Command", props.command());
            params.put("vnp_TmnCode", props.tmnCode());
            params.put("vnp_Amount", String.valueOf(request.amount() * 100)); // VNPay uses VND*100
            params.put("vnp_CurrCode", props.currCode());
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", request.orderInfo() != null
                    ? request.orderInfo()
                    : "Payment for order " + request.orderNumber());
            params.put("vnp_OrderType", props.orderType());
            params.put("vnp_Locale", props.locale());
            params.put("vnp_ReturnUrl", request.returnUrl() != null
                    ? request.returnUrl()
                    : props.returnUrl());
            params.put("vnp_IpAddr", request.ipAddress() != null ? request.ipAddress() : "127.0.0.1");

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            String createDate;
            String expireDate;
            synchronized (DATE_FMT) {
                createDate = DATE_FMT.format(cal.getTime());
                cal.add(Calendar.MINUTE, 15);
                expireDate = DATE_FMT.format(cal.getTime());
            }
            params.put("vnp_CreateDate", createDate);
            params.put("vnp_ExpireDate", expireDate);

            // Sort, build hash data (URL-encoded values) and query string
            ArrayList<String> keys = new ArrayList<>(params.keySet());
            Collections.sort(keys);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String value = params.get(key);
                if (value != null && !value.isEmpty()) {
                    String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII);
                    String encodedKey = URLEncoder.encode(key, StandardCharsets.US_ASCII);
                    hashData.append(key).append('=').append(encodedValue);
                    query.append(encodedKey).append('=').append(encodedValue);
                    if (i < keys.size() - 1) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            String secureHash = VNPaySignatureUtil.hmacSHA512(props.hashSecret(), hashData.toString());
            String paymentUrl = props.paymentUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

            log.info("VNPay payment URL created for txnRef={}", txnRef);
            return PaymentInitResult.builder()
                    .success(true)
                    .paymentUrl(paymentUrl)
                    .transactionRef(txnRef)
                    .build();
        } catch (Exception e) {
            log.error("Failed to create VNPay payment URL", e);
            return PaymentInitResult.fail("VNPay initiate failed: " + e.getMessage());
        }
    }

    // ── verifyWebhook ─────────────────────────────────────────────────────

    @Override
    public WebhookVerifyResult verifyWebhook(Map<String, String> params) {
        boolean signatureValid = VNPaySignatureUtil.verify(params, props.hashSecret());

        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        boolean success = signatureValid && "00".equals(responseCode);
        String transactionId = params.get("vnp_TransactionNo");
        String transactionRef = params.get("vnp_TxnRef");
        String amountStr = params.get("vnp_Amount");

        // VNPay amount is VND * 100
        Long amount = null;
        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                amount = Long.parseLong(amountStr) / 100;
            } catch (NumberFormatException ignored) {
                // logged below via rawResponse
            }
        }

        return WebhookVerifyResult.builder()
                .signatureValid(signatureValid)
                .success(success)
                .transactionId(transactionId)
                .amount(amount)
                .transactionRef(transactionRef)
                .rawResponse(params.toString())
                .build();
    }

    // ── refund ─────────────────────────────────────────────────────────────

    @Override
    public RefundResult refund(RefundGatewayRequest request) {
        try {
            String requestId = request.refundRef();
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            String createDate;
            synchronized (DATE_FMT) {
                createDate = DATE_FMT.format(cal.getTime());
            }

            // VNPay refund hash: pipe-delimited fields
            String hashData = String.join("|",
                    requestId,
                    props.version(),
                    "refund",
                    props.tmnCode(),
                    "02", // full refund transaction type
                    request.refundRef(),
                    String.valueOf(request.amount() * 100),
                    "",   // vnp_TransactionNo — empty if unknown
                    request.transactionDate() != null ? request.transactionDate() : "",
                    "system",
                    createDate,
                    request.ipAddress() != null ? request.ipAddress() : "127.0.0.1",
                    request.reason() != null ? request.reason() : "Refund");

            String secureHash = VNPaySignatureUtil.hmacSHA512(props.hashSecret(), hashData);

            // In sandbox, just log — actual HTTP POST to VNPay API is deferred to Sprint 6
            log.info("VNPay refund prepared for ref={}, hash computed, apiUrl={}",
                    request.refundRef(), props.apiUrl());

            return RefundResult.builder()
                    .success(true)
                    .gatewayRefundId(requestId)
                    .build();
        } catch (Exception e) {
            log.error("VNPay refund failed for ref={}", request.refundRef(), e);
            return RefundResult.fail("VNPay refund failed: " + e.getMessage());
        }
    }
}
