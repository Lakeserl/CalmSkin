-- Outbox table for reliable Kafka publishing via transactional outbox pattern.
-- Events are written in the same DB transaction as the business operation,
-- then forwarded to Kafka by OutboxEventPublisher scheduler.
CREATE TABLE IF NOT EXISTS outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON outbox_events (status, created_at)
    WHERE status = 'PENDING';

-- Idempotency table for Kafka consumer deduplication.
-- Prevents re-processing the same event on broker redelivery.
CREATE TABLE IF NOT EXISTS processed_kafka_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    topic        VARCHAR(100),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
