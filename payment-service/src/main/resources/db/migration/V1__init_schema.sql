CREATE TABLE payments (
    id                      BIGSERIAL       PRIMARY KEY,
    payment_number          VARCHAR(30)     NOT NULL UNIQUE,
    order_id                BIGINT          NOT NULL,
    order_number            VARCHAR(40)     NOT NULL,
    user_id                 UUID            NOT NULL,
    amount                  BIGINT          NOT NULL,
    refunded_amount         BIGINT          NOT NULL DEFAULT 0,
    method                  VARCHAR(20)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    transaction_ref         VARCHAR(100),
    gateway_transaction_id  VARCHAR(100),
    gateway_response        TEXT,                             
    payment_url             TEXT,                              
    failure_reason          VARCHAR(255),
    expires_at              TIMESTAMP,                         
    paid_at                 TIMESTAMP,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payments_order_id             ON payments(order_id);
CREATE INDEX idx_payments_user_id              ON payments(user_id);
CREATE INDEX idx_payments_status               ON payments(status);
CREATE INDEX idx_payments_gateway_txn_id       ON payments(gateway_transaction_id);
CREATE INDEX idx_payments_status_expires       ON payments(status, expires_at);

CREATE TABLE payment_webhooks (
    id              BIGSERIAL       PRIMARY KEY,
    payment_id      BIGINT          REFERENCES payments(id),  
    gateway         VARCHAR(20)     NOT NULL,                 
    raw_params      TEXT            NOT NULL,                 
    signature_valid BOOLEAN         NOT NULL DEFAULT FALSE,
    processed       BOOLEAN         NOT NULL DEFAULT FALSE,
    error_message   TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhooks_payment_id  ON payment_webhooks(payment_id);
CREATE INDEX idx_webhooks_gateway     ON payment_webhooks(gateway, processed);
CREATE INDEX idx_webhooks_created_at  ON payment_webhooks(created_at);


CREATE TABLE refunds (
    id                  BIGSERIAL       PRIMARY KEY,
    refund_number       VARCHAR(40)     NOT NULL UNIQUE,
    payment_id          BIGINT          NOT NULL REFERENCES payments(id),
    order_id            BIGINT          NOT NULL,
    amount              BIGINT          NOT NULL,
    reason              VARCHAR(255),
    refund_method       VARCHAR(20)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    gateway_refund_id   VARCHAR(100),
    failure_reason      VARCHAR(255),
    retry_count         INT             NOT NULL DEFAULT 0,
    processed_at        TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refund_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_refunds_payment_id   ON refunds(payment_id);
CREATE INDEX idx_refunds_order_id     ON refunds(order_id);
CREATE INDEX idx_refunds_status       ON refunds(status);