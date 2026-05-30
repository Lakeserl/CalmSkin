CREATE TABLE outbox_events (
    id           BIGSERIAL    PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(published, created_at)
    WHERE published = FALSE;

CREATE TABLE processed_kafka_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    processed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
