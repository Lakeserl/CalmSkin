-- Enable pgvector extension (requires postgres image: pgvector/pgvector:pg16)
CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI PgVectorStore schema — dimensions=768 for Gemini text-embedding-004
-- PgVectorStore auto-configuration is set to initialize-schema=false so we own it here.
CREATE TABLE IF NOT EXISTS vector_store (
    id       UUID    DEFAULT gen_random_uuid() PRIMARY KEY,
    content  TEXT,
    metadata JSON,
    embedding vector(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store
    USING hnsw (embedding vector_cosine_ops);
