CREATE TABLE stock_reservations (
    id              BIGSERIAL    PRIMARY KEY,
    inventory_id    BIGINT       NOT NULL REFERENCES inventories(id),
    order_id        VARCHAR(100) NOT NULL,
    quantity        INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reservations_order_id   ON stock_reservations(order_id);
CREATE INDEX idx_reservations_status_exp ON stock_reservations(status, expires_at);
