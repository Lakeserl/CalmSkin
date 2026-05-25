CREATE TABLE shipments (
    id                    BIGSERIAL       PRIMARY KEY,
    order_id              BIGINT          NOT NULL UNIQUE,
    order_number          VARCHAR(30)     NOT NULL UNIQUE,
    user_id               UUID            NOT NULL,

    provider              VARCHAR(20)     NOT NULL,
    provider_order_id     VARCHAR(100),
    tracking_number       VARCHAR(100)    UNIQUE,

    status                VARCHAR(30)     NOT NULL DEFAULT 'PENDING',

    recipient_name        VARCHAR(100)    NOT NULL,
    recipient_phone       VARCHAR(20)     NOT NULL,
    address_street        VARCHAR(255)    NOT NULL,
    address_ward          VARCHAR(100)    NOT NULL,
    address_district      VARCHAR(100)    NOT NULL,
    address_province      VARCHAR(100)    NOT NULL,
    address_country       VARCHAR(2)      NOT NULL DEFAULT 'VN',

    weight_g              INTEGER,
    shipping_fee          DECIMAL(15,2)   NOT NULL DEFAULT 0,
    cod_amount            DECIMAL(15,2)   NOT NULL DEFAULT 0,

    estimated_pickup_at   TIMESTAMP,
    estimated_delivery_at TIMESTAMP,
    picked_up_at          TIMESTAMP,
    delivered_at          TIMESTAMP,
    cancelled_at          TIMESTAMP,
    cancel_reason         TEXT,

    created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipments_order_number ON shipments(order_number);
CREATE INDEX idx_shipments_user_id      ON shipments(user_id);
CREATE INDEX idx_shipments_status       ON shipments(status);
CREATE INDEX idx_shipments_provider     ON shipments(provider, status);
CREATE INDEX idx_shipments_tracking     ON shipments(tracking_number);

CREATE TABLE shipment_tracking_events (
    id              BIGSERIAL       PRIMARY KEY,
    shipment_id     BIGINT          NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    status          VARCHAR(30)     NOT NULL,
    description     TEXT,
    location        VARCHAR(255),
    source          VARCHAR(20)     NOT NULL,
    -- PROVIDER_WEBHOOK / ADMIN_MANUAL / SCHEDULER / EVENT
    raw_payload     TEXT,
    occurred_at     TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tracking_events_shipment_id ON shipment_tracking_events(shipment_id, occurred_at DESC);
