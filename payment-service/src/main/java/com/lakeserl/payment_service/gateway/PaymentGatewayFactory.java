package com.lakeserl.payment_service.gateway;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.lakeserl.payment_service.exception.UnsupportedPaymentMethodException;
import com.lakeserl.payment_service.models.enums.PaymentMethod;

/**
 * Resolves the correct {@link PaymentGateway} for a given {@link PaymentMethod}.
 * Spring auto-injects every bean implementing {@link PaymentGateway}.
 */
@Component
public class PaymentGatewayFactory {

    private final Map<PaymentMethod, PaymentGateway> gateways;

    public PaymentGatewayFactory(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(toMap(PaymentGateway::getMethod, identity()));
    }

    /**
     * @throws UnsupportedPaymentMethodException if no gateway is registered for the method
     */
    public PaymentGateway getGateway(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new UnsupportedPaymentMethodException(method);
        }
        return gateway;
    }
}
