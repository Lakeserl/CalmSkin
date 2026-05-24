CREATE TABLE IF NOT EXISTS recently_viewed (
    user_id    UUID        NOT NULL,
    product_id BIGINT      NOT NULL,
    viewed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_recently_viewed_user_time
    ON recently_viewed(user_id, viewed_at DESC);
