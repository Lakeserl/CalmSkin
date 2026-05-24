package com.lakeserl.user_service.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RecentlyViewedId implements Serializable {

    private UUID userId;
    private Long productId;

    public RecentlyViewedId() {}

    public RecentlyViewedId(UUID userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecentlyViewedId other)) return false;
        return Objects.equals(userId, other.userId) && Objects.equals(productId, other.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId);
    }
}
