CREATE TABLE processed_kafka_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
