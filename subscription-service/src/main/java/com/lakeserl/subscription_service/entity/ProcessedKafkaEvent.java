package com.lakeserl.subscription_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_kafka_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedKafkaEvent {

    /** Composite key: topic + ":" + message key. */
    @Id
    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "topic", length = 100)
    private String topic;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessedKafkaEvent(String eventId, String topic) {
        this.eventId = eventId;
        this.topic = topic;
        this.processedAt = LocalDateTime.now();
    }
}
