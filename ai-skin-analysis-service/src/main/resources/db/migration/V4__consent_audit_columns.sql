-- Decree 13/2023 audit requirements: record WHEN consent was given and WHICH version
-- of the consent text the user saw. Required for proving compliance on request.
ALTER TABLE skin_analysis_sessions
    ADD COLUMN consent_at TIMESTAMP,
    ADD COLUMN consent_version VARCHAR(20);

COMMENT ON COLUMN skin_analysis_sessions.consent_at IS
    'Timestamp when the user gave explicit consent — audit trail for Decree 13/2023';
COMMENT ON COLUMN skin_analysis_sessions.consent_version IS
    'Version of the consent disclosure text shown at the time of consent (e.g. "1.0")';
