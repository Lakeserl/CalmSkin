-- Clean wishlists table data since UUID cannot be cast to BIGINT
TRUNCATE TABLE wishlists;

-- Drop constraints
ALTER TABLE wishlists DROP CONSTRAINT IF EXISTS wishlists_user_id_product_id_key;

-- Alter column data type
ALTER TABLE wishlists ALTER COLUMN product_id TYPE BIGINT;

-- Re-add unique constraint
ALTER TABLE wishlists ADD CONSTRAINT wishlists_user_id_product_id_key UNIQUE (user_id, product_id);
