-- V2: Outbox pattern + Kafka idempotency tables

-- ────────────────────────────────────────────────────────────────────────────
-- outbox_events  (Transactional Outbox Pattern)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    id             BIGSERIAL   PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','SENT','FAILED')),
    retry_count    INT          NOT NULL DEFAULT 0,
    error_message  TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events (status, created_at);

-- ────────────────────────────────────────────────────────────────────────────
-- processed_kafka_events  (Consumer idempotency guard)
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS processed_kafka_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
