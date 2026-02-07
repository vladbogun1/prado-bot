ALTER TABLE voice_session
    ADD COLUMN muted BIT(1) NOT NULL DEFAULT 0;

ALTER TABLE voice_session
    ADD COLUMN deafened BIT(1) NOT NULL DEFAULT 0;

ALTER TABLE voice_session
    ADD COLUMN suppressed BIT(1) NOT NULL DEFAULT 0;
