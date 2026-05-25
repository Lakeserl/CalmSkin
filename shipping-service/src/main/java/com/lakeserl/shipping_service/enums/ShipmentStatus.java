package com.lakeserl.shipping_service.enums;

public enum ShipmentStatus {
    PENDING,
    PICKING,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED,
    RETURNED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED;
    }

    public boolean isCancellable() {
        return this == PENDING || this == PICKING;
    }
}
