-- user_addresses.user_id has no index; all address lookups for a user do a full table scan.
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id);

-- user_providers.user_id FK has no index; provider lookups by user scan the full table.
CREATE INDEX IF NOT EXISTS idx_user_providers_user_id ON user_providers(user_id);
