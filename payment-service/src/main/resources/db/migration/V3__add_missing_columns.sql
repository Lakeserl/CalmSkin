-- Idempotent: V1 may already define these columns/constraints/indexes
-- (the schema was consolidated into V1), so every statement guards itself
-- and this migration is safe whether or not V1 already applied each item.

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS order_number    VARCHAR(40),
    ADD COLUMN IF NOT EXISTS transaction_ref VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refunded_amount BIGINT NOT NULL DEFAULT 0;

UPDATE payments SET order_number = payment_number WHERE order_number IS NULL;

ALTER TABLE payments ALTER COLUMN order_number SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_order_number ON payments(order_number);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_ref ON payments(transaction_ref);

-- Postgres has no ADD CONSTRAINT IF NOT EXISTS; drop-then-add makes it idempotent.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_amount_positive;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_payment_amount_non_negative;
ALTER TABLE payments ADD CONSTRAINT chk_payment_amount_non_negative CHECK (amount >= 0);

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_refunded_amount_non_negative;
ALTER TABLE payments ADD CONSTRAINT chk_refunded_amount_non_negative CHECK (refunded_amount >= 0);

ALTER TABLE payments DROP CONSTRAINT IF EXISTS chk_refunded_within_amount;
ALTER TABLE payments ADD CONSTRAINT chk_refunded_within_amount CHECK (refunded_amount <= amount);


ALTER TABLE refunds
    ADD COLUMN IF NOT EXISTS refund_number VARCHAR(40),
    ADD COLUMN IF NOT EXISTS refund_method VARCHAR(20);

UPDATE refunds SET refund_number = 'REF-' || id WHERE refund_number IS NULL;

ALTER TABLE refunds ALTER COLUMN refund_number SET NOT NULL;

ALTER TABLE refunds DROP CONSTRAINT IF EXISTS uq_refund_number;
ALTER TABLE refunds ADD CONSTRAINT uq_refund_number UNIQUE (refund_number);

ALTER TABLE refunds ALTER COLUMN refund_method SET NOT NULL;
