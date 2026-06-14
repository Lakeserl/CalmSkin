-- AI usage logging for cost monitoring (uniform with ai-skin-analysis ai_usage_logs).
-- user_id is UUID here to match chatbot's user identity.
CREATE TABLE ai_usage_logs (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          UUID,
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

CREATE INDEX idx_chat_usage_user_date ON ai_usage_logs(user_id, created_at);
CREATE INDEX idx_chat_usage_service   ON ai_usage_logs(service_name, created_at);
