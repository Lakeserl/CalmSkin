package com.lakeserl.inventory_service.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String orderId;
    private List<Long> reservationIds;
    private Instant expiresAt;
    private Boolean released;
    private Boolean confirmed;
    private Boolean returned;
}
