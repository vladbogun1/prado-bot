SET @add_muted := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voice_session'
              AND column_name = 'muted'
        ),
        'SELECT 1',
        'ALTER TABLE voice_session ADD COLUMN muted BIT(1) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @add_muted;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_deafened := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voice_session'
              AND column_name = 'deafened'
        ),
        'SELECT 1',
        'ALTER TABLE voice_session ADD COLUMN deafened BIT(1) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @add_deafened;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_suppressed := (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'voice_session'
              AND column_name = 'suppressed'
        ),
        'SELECT 1',
        'ALTER TABLE voice_session ADD COLUMN suppressed BIT(1) NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @add_suppressed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
