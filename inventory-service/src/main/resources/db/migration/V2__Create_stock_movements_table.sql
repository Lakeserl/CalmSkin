CREATE TABLE stock_movements (
    id              BIGSERIAL    PRIMARY KEY,
    inventory_id    BIGINT       NOT NULL REFERENCES inventories(id),
    movement_type   VARCHAR(20)  NOT NULL,
    quantity        INT          NOT NULL,
    reference_id    VARCHAR(100),
    reference_type  VARCHAR(50),
    note            TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_movements_inventory_id ON stock_movements(inventory_id);
CREATE INDEX idx_movements_reference    ON stock_movements(reference_id);
