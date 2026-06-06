CREATE TABLE chat_conversations (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     UUID         NOT NULL,
    title       VARCHAR(200),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
    id              BIGSERIAL   PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    tokens_used     INT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Knowledge base entries table — admin management layer on top of vector_store
CREATE TABLE knowledge_base_entries (
    id              BIGSERIAL    PRIMARY KEY,
    vector_store_id VARCHAR(50)  NOT NULL UNIQUE,
    topic           VARCHAR(50)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    content         TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_user_id ON chat_conversations(user_id);
CREATE INDEX idx_messages_conversation ON chat_messages(conversation_id, created_at DESC);
CREATE INDEX idx_knowledge_topic       ON knowledge_base_entries(topic);
