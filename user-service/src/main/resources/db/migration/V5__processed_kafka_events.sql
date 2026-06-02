CREATE TABLE IF NOT EXISTS processed_kafka_events (
    event_id     VARCHAR(255) NOT NULL PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now()
);
