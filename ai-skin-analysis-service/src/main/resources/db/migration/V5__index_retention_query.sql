-- Covers the nightly retention scheduler: WHERE processed_image_url IS NOT NULL AND created_at < cutoff
-- Partial index: only indexes rows that still have an image URL, which shrinks after each purge run.
-- CONCURRENTLY avoids table lock during migration on a live database.
CREATE INDEX CONCURRENTLY idx_skin_sessions_retention
    ON skin_analysis_sessions (created_at)
    WHERE processed_image_url IS NOT NULL;
