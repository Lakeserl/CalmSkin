package com.lakeserl.product_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewedEvent {
    private Long productId;
    private String slug;
    private String userId; 
    private String sessionId;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
