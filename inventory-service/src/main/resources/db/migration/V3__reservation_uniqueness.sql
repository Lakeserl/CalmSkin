-- Prevent duplicate PENDING reservations for the same order+inventory pair.
-- Without this, a redelivered Kafka event can double-reserve stock before the
-- idempotency check in the consumer saves the processed event record.
CREATE UNIQUE INDEX uq_reservation_pending
    ON stock_reservations(order_id, inventory_id)
    WHERE status = 'PENDING';
