-- V1: Initial review service schema
-- CalmSKIN Review Service — Flyway Migration V1

-- ────────────────────────────────────────────────────────────────────────────
-- reviews
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT        NOT NULL,
    user_id            UUID          NOT NULL,
    order_id           BIGINT        NOT NULL,
    order_item_id      BIGINT        NOT NULL,
    rating             SMALLINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title              VARCHAR(255),
    body               TEXT,
    skin_type          VARCHAR(30),
    age_range          VARCHAR(20),
    skin_effect_rating SMALLINT      CHECK (skin_effect_rating BETWEEN 1 AND 5),
    texture_rating     SMALLINT      CHECK (texture_rating    BETWEEN 1 AND 5),
    scent_rating       SMALLINT      CHECK (scent_rating      BETWEEN 1 AND 5),
    packaging_rating   SMALLINT      CHECK (packaging_rating  BETWEEN 1 AND 5),
    value_rating       SMALLINT      CHECK (value_rating      BETWEEN 1 AND 5),
    is_verified        BOOLEAN       NOT NULL DEFAULT TRUE,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED'
                           CHECK (status IN ('PUBLISHED','HIDDEN','PENDING_MODERATION','DELETED')),
    helpful_count      INT           NOT NULL DEFAULT 0,
    not_helpful_count  INT           NOT NULL DEFAULT 0,
    report_count       INT           NOT NULL DEFAULT 0,
    admin_note         TEXT,
    moderated_by       UUID,
    moderated_at       TIMESTAMP,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reviews_product_status ON reviews (product_id, status);
CREATE INDEX IF NOT EXISTS idx_reviews_user            ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order_item      ON reviews (order_item_id);

-- ────────────────────────────────────────────────────────────────────────────
-- review_media
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_media (
    id            BIGSERIAL PRIMARY KEY,
    review_id     BIGINT       NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    media_type    VARCHAR(10)  NOT NULL CHECK (media_type IN ('IMAGE','VIDEO')),
    url           VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512),
    sort_order    SMALLINT     NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_media_review ON review_media (review_id, sort_order);

-- ────────────────────────────────────────────────────────────────────────────
-- review_votes
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_votes (
    id         BIGSERIAL PRIMARY KEY,
    review_id  BIGINT    NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL,
    is_helpful BOOLEAN   NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vote_review_user UNIQUE (review_id, user_id)
);

-- ────────────────────────────────────────────────────────────────────────────
-- review_reports
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_reports (
    id          BIGSERIAL PRIMARY KEY,
    review_id   BIGINT      NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    reporter_id UUID        NOT NULL,
    reason      VARCHAR(50) NOT NULL
                    CHECK (reason IN ('SPAM','FAKE','OFFENSIVE','OFF_TOPIC','OTHER')),
    detail      TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','DISMISSED','ACTION_TAKEN')),
    resolved_by UUID,
    resolved_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_report_review_reporter UNIQUE (review_id, reporter_id)
);

CREATE INDEX IF NOT EXISTS idx_review_reports_status ON review_reports (status);

-- ────────────────────────────────────────────────────────────────────────────
-- review_replies
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_replies (
    id         BIGSERIAL PRIMARY KEY,
    review_id  BIGINT    NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL,
    is_seller  BOOLEAN   NOT NULL DEFAULT FALSE,
    body       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_replies_review ON review_replies (review_id);

-- ────────────────────────────────────────────────────────────────────────────
-- product_review_summary
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS product_review_summary (
    product_id        BIGINT         PRIMARY KEY,
    total_count       INT            NOT NULL DEFAULT 0,
    average_rating    NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    count_1star       INT            NOT NULL DEFAULT 0,
    count_2star       INT            NOT NULL DEFAULT 0,
    count_3star       INT            NOT NULL DEFAULT 0,
    count_4star       INT            NOT NULL DEFAULT 0,
    count_5star       INT            NOT NULL DEFAULT 0,
    count_oily        INT            NOT NULL DEFAULT 0,
    count_dry         INT            NOT NULL DEFAULT 0,
    count_combination INT            NOT NULL DEFAULT 0,
    count_sensitive   INT            NOT NULL DEFAULT 0,
    count_normal      INT            NOT NULL DEFAULT 0,
    avg_skin_effect   NUMERIC(3, 2),
    avg_texture       NUMERIC(3, 2),
    avg_scent         NUMERIC(3, 2),
    avg_packaging     NUMERIC(3, 2),
    avg_value         NUMERIC(3, 2),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────────────────────
-- review_eligibility
-- ────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_eligibility (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             UUID      NOT NULL,
    order_item_id       BIGINT    NOT NULL,
    product_id          BIGINT    NOT NULL,
    order_completed_at  TIMESTAMP NOT NULL,
    review_id           BIGINT    REFERENCES reviews (id),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_eligibility UNIQUE (user_id, order_item_id)
);

CREATE INDEX IF NOT EXISTS idx_eligibility_user    ON review_eligibility (user_id);
CREATE INDEX IF NOT EXISTS idx_eligibility_product ON review_eligibility (user_id, product_id);
