package com.lakeserl.ai_skin_analysis_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "tokens_input", nullable = false)
    private Integer tokensInput;

    @Column(name = "tokens_output", nullable = false)
    private Integer tokensOutput;

    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
