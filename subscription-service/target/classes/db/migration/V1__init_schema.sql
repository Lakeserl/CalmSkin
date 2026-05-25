CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    frequency_days INT NOT NULL,
    address_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_ordered_at TIMESTAMP,
    next_order_due_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_due ON subscriptions(next_order_due_at) WHERE status = 'ACTIVE';
