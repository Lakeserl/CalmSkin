package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RecentlyViewedRequest {

    @NotNull
    @Positive
    private Long productId;

    public RecentlyViewedRequest() {}

    public RecentlyViewedRequest(Long productId) {
        this.productId = productId;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
