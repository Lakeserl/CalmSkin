CREATE TABLE inventories (
    id                  BIGSERIAL    PRIMARY KEY,
    product_id          BIGINT       NOT NULL,
    variant_id          BIGINT,
    quantity_available  INT          NOT NULL DEFAULT 0,
    quantity_reserved   INT          NOT NULL DEFAULT 0,
    quantity_sold       INT          NOT NULL DEFAULT 0,
    low_stock_threshold INT          NOT NULL DEFAULT 10,
    warehouse_location  VARCHAR(50),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_product_variant UNIQUE (product_id, variant_id),
    CONSTRAINT chk_available_non_negative   CHECK (quantity_available >= 0),
    CONSTRAINT chk_reserved_non_negative    CHECK (quantity_reserved  >= 0)
);
CREATE INDEX idx_inventory_product_id ON inventories(product_id);

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
