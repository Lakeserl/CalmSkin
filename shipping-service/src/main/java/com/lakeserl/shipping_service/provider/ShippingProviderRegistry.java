package com.lakeserl.shipping_service.provider;

import com.lakeserl.shipping_service.enums.ShippingProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ShippingProviderRegistry {

    private final Map<ShippingProvider, ShippingProviderClient> clients = new EnumMap<>(ShippingProvider.class);

    // Spring injects every ShippingProviderClient bean. We index them by enum
    // so callers select by enum value instead of bean name — keeps call sites
    // free of provider-specific knowledge.
    public ShippingProviderRegistry(List<ShippingProviderClient> implementations) {
        for (ShippingProviderClient impl : implementations) {
            clients.put(impl.provider(), impl);
        }
    }

    public ShippingProviderClient get(ShippingProvider provider) {
        ShippingProviderClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("No ShippingProviderClient registered for " + provider);
        }
        return client;
    }
}
