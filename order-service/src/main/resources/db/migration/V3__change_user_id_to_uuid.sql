-- Aligns order-service with user-service, whose user PK is UUID.
-- bigint -> uuid has no implicit cast; the orders table is recreated empty
-- in every environment, so the column is dropped and re-added.
ALTER TABLE orders DROP COLUMN user_id;
ALTER TABLE orders ADD COLUMN user_id UUID NOT NULL;
CREATE INDEX idx_orders_user_id ON orders(user_id);
