package com.lakeserl.subscription_service.enums;

public enum SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isModifiable() {
        return this == ACTIVE || this == PAUSED;
    }
}
