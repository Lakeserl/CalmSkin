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
