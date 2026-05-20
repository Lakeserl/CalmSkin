package com.lakeserl.payment_service.gateway;

import java.util.Map;

import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest;
import com.lakeserl.payment_service.gateway.dto.RefundResult;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.enums.PaymentMethod;

/**
 * Abstraction over a single payment gateway (VNPay, Momo, ZaloPay).
 * Each implementation is a Spring {@code @Component} collected by
 * {@link PaymentGatewayFactory}.
 */
public interface PaymentGateway {

    /** Which {@link PaymentMethod} this gateway serves. */
    PaymentMethod getMethod();

    /** Build a payment URL and return a transaction reference. */
    PaymentInitResult initiate(PaymentInitRequest request);

    /** Verify the webhook signature, parse the payload. */
    WebhookVerifyResult verifyWebhook(Map<String, String> params);

    /** Request a refund from the gateway. */
    RefundResult refund(RefundGatewayRequest request);
}
