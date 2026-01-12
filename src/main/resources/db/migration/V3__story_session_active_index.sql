ALTER TABLE story_session
  DROP INDEX uq_active,
  ADD COLUMN active_key VARCHAR(20)
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN 'ACTIVE' ELSE NULL END) STORED,
  ADD UNIQUE INDEX uq_active (guild_id, user_id, campaign_key, active_key);
