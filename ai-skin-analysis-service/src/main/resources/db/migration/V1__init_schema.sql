CREATE TABLE skin_analysis_sessions (
    id                      BIGSERIAL    PRIMARY KEY,
    session_id              VARCHAR(50)  NOT NULL UNIQUE,
    user_id                 BIGINT       NOT NULL,

    age                     INT,
    self_skin_type          VARCHAR(30),
    self_concerns           TEXT,
    allergies               TEXT,

    original_image_url      VARCHAR(500),
    processed_image_url     VARCHAR(500),
    image_hash              VARCHAR(64),

    status                  VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    failure_reason          TEXT,

    cv_features             TEXT,

    detected_skin_type      VARCHAR(30),
    detected_concerns       TEXT,
    skin_condition_report   TEXT,
    recommended_product_ids TEXT,
    morning_routine         TEXT,
    evening_routine         TEXT,

    tokens_used             INT,
    processing_time_ms      INT,

    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP
);

CREATE INDEX idx_sessions_user_id    ON skin_analysis_sessions(user_id);
CREATE INDEX idx_sessions_image_hash ON skin_analysis_sessions(image_hash);
CREATE INDEX idx_sessions_status     ON skin_analysis_sessions(status);
CREATE INDEX idx_sessions_created_at ON skin_analysis_sessions(created_at DESC);

CREATE TABLE ai_usage_logs (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT,
    service_name     VARCHAR(50)  NOT NULL,
    model_name       VARCHAR(50)  NOT NULL,
    tokens_input     INT          NOT NULL,
    tokens_output    INT          NOT NULL,
    cost_usd         NUMERIC(10,6),
    response_time_ms INT,
    success          BOOLEAN      NOT NULL,
    error_message    TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usage_user_date ON ai_usage_logs(user_id, created_at);
CREATE INDEX idx_usage_service   ON ai_usage_logs(service_name, created_at);
