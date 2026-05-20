CREATE TABLE orders (
    id                  BIGSERIAL       PRIMARY KEY,
    order_number        VARCHAR(30)     NOT NULL UNIQUE,

    user_id             BIGINT          NOT NULL,

    shipping_name       VARCHAR(100)    NOT NULL,
    shipping_phone      VARCHAR(20)     NOT NULL,
    shipping_province   VARCHAR(100)    NOT NULL,
    shipping_district   VARCHAR(100)    NOT NULL,
    shipping_ward       VARCHAR(100)    NOT NULL,
    shipping_street     VARCHAR(255)    NOT NULL,

    subtotal            DECIMAL(15,2)   NOT NULL,
    discount_amount     DECIMAL(15,2)   NOT NULL DEFAULT 0,
    shipping_fee        DECIMAL(15,2)   NOT NULL DEFAULT 0,
    points_used         INT             NOT NULL DEFAULT 0,
    points_amount       DECIMAL(15,2)   NOT NULL DEFAULT 0,
    total_amount        DECIMAL(15,2)   NOT NULL,

    voucher_code        VARCHAR(50),
    voucher_discount    DECIMAL(15,2)   DEFAULT 0,

    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30)     NOT NULL,

    note                TEXT,
    cancel_reason       TEXT,

    confirmed_at        TIMESTAMP,
    paid_at             TIMESTAMP,
    preparing_at        TIMESTAMP,
    shipped_at          TIMESTAMP,
    delivered_at        TIMESTAMP,
    cancelled_at        TIMESTAMP,

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id      ON orders(user_id);
CREATE INDEX idx_orders_status       ON orders(status);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_created_at   ON orders(created_at DESC);

CREATE TABLE order_items (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL REFERENCES orders(id),

    product_id          BIGINT          NOT NULL,
    variant_id          BIGINT,
    product_name        VARCHAR(255)    NOT NULL,
    product_sku         VARCHAR(100)    NOT NULL,
    variant_name        VARCHAR(100),
    product_image_url   VARCHAR(500),
    brand_name          VARCHAR(100),

    unit_price          DECIMAL(15,2)   NOT NULL,
    quantity            INT             NOT NULL,
    subtotal            DECIMAL(15,2)   NOT NULL,

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

CREATE TABLE order_status_history (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        BIGINT      NOT NULL REFERENCES orders(id),
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      VARCHAR(100),
    reason          TEXT,
    metadata        TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_status_history_order_id ON order_status_history(order_id);

CREATE TABLE order_payment_info (
    id                  BIGSERIAL   PRIMARY KEY,
    order_id            BIGINT      NOT NULL UNIQUE REFERENCES orders(id),
    payment_method      VARCHAR(30) NOT NULL,
    payment_status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_id      VARCHAR(255),
    gateway_response    TEXT,
    amount              DECIMAL(15,2) NOT NULL,
    refund_amount       DECIMAL(15,2) DEFAULT 0,
    paid_at             TIMESTAMP,
    refunded_at         TIMESTAMP,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE order_shipping_info (
    id                  BIGSERIAL   PRIMARY KEY,
    order_id            BIGINT      NOT NULL UNIQUE REFERENCES orders(id),
    shipping_provider   VARCHAR(50),
    tracking_number     VARCHAR(100),
    shipping_status     VARCHAR(50),
    estimated_delivery  TIMESTAMP,
    actual_delivery     TIMESTAMP,
    shipping_fee        DECIMAL(15,2),
    provider_order_id   VARCHAR(100),
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);
