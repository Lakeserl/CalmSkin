-- Composite index for the common admin filter: orders by user, filtered by status, sorted by date.
-- Without this, the query planner uses separate single-column indexes and merges them, which is
-- significantly slower as the orders table grows.
CREATE INDEX IF NOT EXISTS idx_orders_user_status_created
    ON orders(user_id, status, created_at DESC);
