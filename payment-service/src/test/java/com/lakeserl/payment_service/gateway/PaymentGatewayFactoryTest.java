package com.lakeserl.payment_service.gateway;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.lakeserl.payment_service.exception.UnsupportedPaymentMethodException;
import com.lakeserl.payment_service.gateway.dto.PaymentInitRequest;
import com.lakeserl.payment_service.gateway.dto.PaymentInitResult;
import com.lakeserl.payment_service.gateway.dto.RefundGatewayRequest;
import com.lakeserl.payment_service.gateway.dto.RefundResult;
import com.lakeserl.payment_service.gateway.dto.WebhookVerifyResult;
import com.lakeserl.payment_service.models.enums.PaymentMethod;


class PaymentGatewayFactoryTest {

    private PaymentGatewayFactory factory;

    private static class StubGateway implements PaymentGateway {
        private final PaymentMethod method;

        StubGateway(PaymentMethod method) {
            this.method = method;
        }

        @Override
        public PaymentMethod getMethod() {
            return method;
        }

        @Override
        public PaymentInitResult initiate(PaymentInitRequest request) {
            return PaymentInitResult.builder().success(true).build();
        }

        @Override
        public WebhookVerifyResult verifyWebhook(Map<String, String> params) {
            return WebhookVerifyResult.builder().signatureValid(true).build();
        }

        @Override
        public RefundResult refund(RefundGatewayRequest request) {
            return RefundResult.builder().success(true).build();
        }
    }

    @BeforeEach
    void setUp() {
        factory = new PaymentGatewayFactory(List.of(
                new StubGateway(PaymentMethod.VNPAY),
                new StubGateway(PaymentMethod.MOMO),
                new StubGateway(PaymentMethod.ZALOPAY)));
    }

    @Test
    @DisplayName("getGateway returns correct gateway for VNPAY")
    void getGateway_vnpay() {
        PaymentGateway gw = factory.getGateway(PaymentMethod.VNPAY);
        assertNotNull(gw);
        assertEquals(PaymentMethod.VNPAY, gw.getMethod());
    }

    @Test
    @DisplayName("getGateway returns correct gateway for MOMO")
    void getGateway_momo() {
        PaymentGateway gw = factory.getGateway(PaymentMethod.MOMO);
        assertNotNull(gw);
        assertEquals(PaymentMethod.MOMO, gw.getMethod());
    }

    @Test
    @DisplayName("getGateway returns correct gateway for ZALOPAY")
    void getGateway_zalopay() {
        PaymentGateway gw = factory.getGateway(PaymentMethod.ZALOPAY);
        assertNotNull(gw);
        assertEquals(PaymentMethod.ZALOPAY, gw.getMethod());
    }

    @Test
    @DisplayName("getGateway throws UnsupportedPaymentMethodException for COD")
    void getGateway_cod_throws() {
        assertThrows(UnsupportedPaymentMethodException.class,
                () -> factory.getGateway(PaymentMethod.COD));
    }

    @Test
    @DisplayName("getGateway throws UnsupportedPaymentMethodException for POINTS")
    void getGateway_points_throws() {
        assertThrows(UnsupportedPaymentMethodException.class,
                () -> factory.getGateway(PaymentMethod.POINTS));
    }

    @Test
    @DisplayName("factory handles empty gateway list gracefully")
    void emptyGatewayList() {
        PaymentGatewayFactory emptyFactory = new PaymentGatewayFactory(List.of());
        assertThrows(UnsupportedPaymentMethodException.class,
                () -> emptyFactory.getGateway(PaymentMethod.VNPAY));
    }
}
