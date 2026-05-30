-- Decree 13/2023 (Vietnam Personal Data Protection): face photos are biometric data.
-- Processing requires explicit, purpose-specific, informed consent before collection.
-- This column records that consent was obtained at the time of each analysis request.
ALTER TABLE skin_analysis_sessions
    ADD COLUMN consent_given BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN skin_analysis_sessions.consent_given IS
    'Explicit user consent for biometric (face photo) data processing — required by Decree 13/2023';
