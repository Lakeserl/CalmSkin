-- Shared sequence used by Hibernate GenerationType.AUTO for Long PKs (roles, user_providers).
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS roles (
    id    BIGINT      NOT NULL DEFAULT nextval('hibernate_sequence') PRIMARY KEY,
    roles VARCHAR(60) UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email         VARCHAR(100) UNIQUE,
    phone_number  VARCHAR(255) UNIQUE,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    avatar_url    VARCHAR(255),
    date_of_birth DATE         NOT NULL,
    gender        VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS user_providers (
    id               BIGINT       NOT NULL DEFAULT nextval('hibernate_sequence') PRIMARY KEY,
    provider_user_id VARCHAR(255) NOT NULL,
    provider         VARCHAR(50)  NOT NULL,
    user_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE IF NOT EXISTS user_addresses (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_name VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    province       VARCHAR(100) NOT NULL,
    district       VARCHAR(100) NOT NULL,
    ward           VARCHAR(100) NOT NULL,
    street         VARCHAR(255) NOT NULL,
    postal_code    VARCHAR(10),
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    device_info VARCHAR(255),
    ip_address  VARCHAR(45),
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS otp_tokens (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code   VARCHAR(255) NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    is_used    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS skin_profiles (
    id            UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    skin_type     VARCHAR(30),
    skin_concerns JSONB,
    allergies     JSONB,
    note          TEXT,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wishlists (
    id         UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, product_id)
);

CREATE TABLE IF NOT EXISTS user_points (
    id           UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_points INT       NOT NULL DEFAULT 0,
    tier         VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    updated_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS point_transactions (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    points         INT          NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    reference_id   VARCHAR(255),
    reference_type VARCHAR(50),
    description    VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_point_tx_user ON point_transactions(user_id);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action      VARCHAR(100) NOT NULL,
    description TEXT,
    ip_address  VARCHAR(45),
    device_info VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
