package com.lakeserl.payment_service.gateway.zalopay;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lakeserl.payment_service.config.properties.ZaloPayProperties;
import com.lakeserl.payment_service.gateway.PaymentGateway;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest;
import com.lakeserl.payment_service.gateway.dto.RefundResult;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.enums.PaymentMethod;

import lombok.RequiredArgsConstructor;

/**
 * ZaloPay sandbox gateway integration. Uses HMAC-SHA256 with key1 for
 * creating payments (pipe-delimited MAC) and key2 for verifying callbacks.
 */
@Component
@RequiredArgsConstructor
public class ZaloPayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(ZaloPayGateway.class);

    private final ZaloPayProperties props;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.ZALOPAY;
    }

    @Override
    public PaymentInitResult initiate(PaymentInitRequest request) {
        try {
            String appTransId = request.orderNumber();
            long appTime = System.currentTimeMillis();
            String appUser = "calmskin_user";
            String embedData = "{}";
            String item = "[]";

            String macData = ZaloPaySignatureUtil.buildCreateMacData(
                    props.appId(), appTransId, appUser,
                    request.amount(), appTime, embedData, item);

            String mac = ZaloPaySignatureUtil.hmacSHA256(props.key1(), macData);

            // In sandbox, the actual POST to ZaloPay API returns a payment URL
            log.info("ZaloPay payment MAC computed for appTransId={}, endpoint={}",
                    appTransId, props.endpoint());

            return PaymentInitResult.builder()
                    .success(true)
                    .paymentUrl(props.endpoint() + "/v2/create?mac=" + mac)
                    .transactionRef(appTransId)
                    .build();
        } catch (Exception e) {
            log.error("ZaloPay initiate failed", e);
            return PaymentInitResult.fail("ZaloPay initiate failed: " + e.getMessage());
        }
    }

    @Override
    public WebhookVerifyResult verifyWebhook(Map<String, String> params) {
        // ZaloPay callback sends: data (JSON string) + mac (HMAC of data using key2)
        String callbackData = params.get("data");
        String receivedMac = params.get("mac");

        boolean signatureValid = ZaloPaySignatureUtil.verify(receivedMac, props.key2(), callbackData);

        // Parse fields from the callback data — in real flow this is JSON
        String transactionId = params.get("zp_trans_id");
        String transactionRef = params.get("app_trans_id");
        String amountStr = params.get("amount");

        Long amount = null;
        if (amountStr != null) {
            try {
                amount = Long.parseLong(amountStr);
            } catch (NumberFormatException ignored) {
            }
        }

        return WebhookVerifyResult.builder()
                .signatureValid(signatureValid)
                .success(signatureValid) // ZaloPay: valid MAC = successful
                .transactionId(transactionId)
                .amount(amount)
                .transactionRef(transactionRef)
                .rawResponse(params.toString())
                .build();
    }

    @Override
    public RefundResult refund(RefundGatewayRequest request) {
        try {
            log.info("ZaloPay refund prepared for ref={}, endpoint={}",
                    request.refundRef(), props.endpoint());
            return RefundResult.builder()
                    .success(true)
                    .gatewayRefundId(request.refundRef())
                    .build();
        } catch (Exception e) {
            log.error("ZaloPay refund failed for ref={}", request.refundRef(), e);
            return RefundResult.fail("ZaloPay refund failed: " + e.getMessage());
        }
    }
}
