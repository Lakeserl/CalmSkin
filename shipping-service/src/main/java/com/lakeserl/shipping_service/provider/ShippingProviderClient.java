package com.lakeserl.shipping_service.provider;

import com.lakeserl.shipping_service.enums.ShippingProvider;

// One implementation per carrier. Each binds to a single ShippingProvider value
// and is selected at runtime by ShippingProviderRegistry. Implementations MUST
// be idempotent on the provider's order_number — callers may retry after a
// failed-but-actually-succeeded create.
public interface ShippingProviderClient {

    ShippingProvider provider();

    CreateShipmentResponse createShipment(CreateShipmentRequest request);

    // Cancels an already-created label at the carrier. No-op if the carrier
    // does not support cancellation; throws if the shipment is already picked up.
    void cancelShipment(String providerOrderId, String reason);
}
