

ALTER TABLE payments
    ADD COLUMN order_number    VARCHAR(40),
    ADD COLUMN transaction_ref VARCHAR(100),
    ADD COLUMN refunded_amount BIGINT NOT NULL DEFAULT 0;

UPDATE payments SET order_number = payment_number WHERE order_number IS NULL;

ALTER TABLE payments ALTER COLUMN order_number SET NOT NULL;

CREATE INDEX idx_payments_order_number ON payments(order_number);
CREATE INDEX idx_payments_transaction_ref ON payments(transaction_ref);

ALTER TABLE payments DROP CONSTRAINT chk_payment_amount_positive;
ALTER TABLE payments ADD CONSTRAINT chk_payment_amount_non_negative CHECK (amount >= 0);
ALTER TABLE payments ADD CONSTRAINT chk_refunded_amount_non_negative CHECK (refunded_amount >= 0);
ALTER TABLE payments ADD CONSTRAINT chk_refunded_within_amount CHECK (refunded_amount <= amount);


ALTER TABLE refunds
    ADD COLUMN refund_number VARCHAR(40),
    ADD COLUMN refund_method VARCHAR(20);

UPDATE refunds SET refund_number = 'REF-' || id WHERE refund_number IS NULL;

ALTER TABLE refunds ALTER COLUMN refund_number SET NOT NULL;
ALTER TABLE refunds ADD CONSTRAINT uq_refund_number UNIQUE (refund_number);
ALTER TABLE refunds ALTER COLUMN refund_method SET NOT NULL;
