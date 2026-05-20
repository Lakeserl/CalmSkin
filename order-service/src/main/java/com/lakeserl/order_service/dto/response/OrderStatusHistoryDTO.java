package com.lakeserl.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryDTO {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private String reason;
    private String metadata;
    private LocalDateTime createdAt;
}
