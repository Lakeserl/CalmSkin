package com.lakeserl.ai_skin_analysis_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_kafka_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedKafkaEvent {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}
