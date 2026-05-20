package com.lakeserl.order_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private Map<String, Long> byStatus;
    private BigDecimal averageOrderValue;
}
