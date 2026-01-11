ALTER TABLE game_session
    ADD COLUMN node_key VARCHAR(40) NOT NULL DEFAULT '',
    ADD COLUMN tail_level INT NOT NULL DEFAULT 0,
    ADD COLUMN flags_json TEXT NULL,
    ADD COLUMN checkpoints_json TEXT NULL,
    ADD COLUMN earned_temp INT NOT NULL DEFAULT 0;

UPDATE game_session
SET flags_json = '[]'
WHERE flags_json IS NULL;

UPDATE game_session
SET checkpoints_json = '[]'
WHERE checkpoints_json IS NULL;

ALTER TABLE game_session
    MODIFY flags_json TEXT NOT NULL,
    MODIFY checkpoints_json TEXT NOT NULL;

ALTER TABLE game_scene
    ADD COLUMN node_key VARCHAR(40) NULL;

ALTER TABLE game_action
    ADD COLUMN node_key VARCHAR(40) NULL,
    ADD COLUMN trigger_events_json TEXT NOT NULL DEFAULT '[]';

ALTER TABLE game_event
    ADD COLUMN node_key VARCHAR(40) NULL,
    ADD COLUMN event_kind VARCHAR(20) NOT NULL DEFAULT 'AMBIENT',
    ADD COLUMN trigger_action_types_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN trigger_action_keys_json TEXT NOT NULL DEFAULT '[]';

ALTER TABLE game_event_log
    ADD COLUMN node_from_key VARCHAR(40) NULL,
    ADD COLUMN node_to_key VARCHAR(40) NULL;

CREATE TABLE game_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_key VARCHAR(40) NOT NULL UNIQUE,
    mission_type_key VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    is_start BOOLEAN NOT NULL DEFAULT FALSE,
    tags_json TEXT NOT NULL
);

CREATE TABLE game_node_transition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_node_key VARCHAR(40) NOT NULL,
    to_node_key VARCHAR(40) NOT NULL,
    weight INT NOT NULL,
    condition_json TEXT NOT NULL
);

CREATE INDEX ix_game_node_mission ON game_node (mission_type_key, is_start);
CREATE INDEX ix_game_node_transition_from ON game_node_transition (from_node_key);

INSERT INTO game_node (node_key, mission_type_key, title, description, is_start, tags_json) VALUES
('DELIVERY_PICKUP', 'DELIVERY', 'Забрать груз', 'Точка забора, груз готов. Нужно стартовать без шума.', TRUE, '["start"]'),
('DELIVERY_TRANSIT', 'DELIVERY', 'В пути', 'Город шумит, копы рядом. Главное — не привлекать внимание.', FALSE, '["mid"]'),
('DELIVERY_HANDOFF', 'DELIVERY', 'Передача', 'Покупатель на связи, пора передать груз.', FALSE, '["handoff"]'),
('DELIVERY_ESCAPE', 'DELIVERY', 'Уход', 'Внимание копов выросло, нужно уйти чисто.', FALSE, '["escape"]'),

('REPO_SPOT', 'REPO', 'Найти тачку', 'Нужно найти цель и оценить обстановку.', TRUE, '["start"]'),
('REPO_ACCESS', 'REPO', 'Доступ', 'Машина рядом, но хозяин на чеку.', FALSE, '["mid"]'),
('REPO_TAKEOFF', 'REPO', 'Угон', 'Забираешь машину. Главное — не спалиться.', FALSE, '["takeoff"]'),
('REPO_CHASE', 'REPO', 'Погоня', 'За тобой хвост, нужно оторваться.', FALSE, '["chase"]'),

('HEIST_APPROACH', 'HEIST', 'Подход', 'Объект рядом, охрана и камеры активны.', TRUE, '["start"]'),
('HEIST_ENTRY', 'HEIST', 'Проникновение', 'Ты внутри. Время ограничено.', FALSE, '["mid"]'),
('HEIST_VAULT', 'HEIST', 'Сейф', 'Кэш перед тобой, тревога возможна.', FALSE, '["vault"]'),
('HEIST_EXIT', 'HEIST', 'Выход', 'Нужно уйти с добычей.', FALSE, '["exit"]'),

('CHASE_START', 'CHASE', 'На хвосте', 'Погоня началась, каждая секунда важна.', TRUE, '["start"]'),
('CHASE_PRESSURE', 'CHASE', 'Давление', 'Копы усиливают давление.', FALSE, '["pressure"]'),
('CHASE_BREAK', 'CHASE', 'Сбросить хвост', 'Есть шанс оторваться.', FALSE, '["break"]'),
('CHASE_SAFE', 'CHASE', 'Укрытие', 'Безопасная зона близко.', FALSE, '["safe"]');

INSERT INTO game_node_transition (from_node_key, to_node_key, weight, condition_json) VALUES
('DELIVERY_PICKUP', 'DELIVERY_TRANSIT', 80, '{"minProgress":0,"maxProgress":60}'),
('DELIVERY_PICKUP', 'DELIVERY_ESCAPE', 20, '{"minHeat":50}'),
('DELIVERY_TRANSIT', 'DELIVERY_HANDOFF', 70, '{"minProgress":40}'),
('DELIVERY_TRANSIT', 'DELIVERY_ESCAPE', 30, '{"minHeat":60}'),
('DELIVERY_HANDOFF', 'DELIVERY_ESCAPE', 50, '{"minHeat":40}'),

('REPO_SPOT', 'REPO_ACCESS', 80, '{"minProgress":0,"maxProgress":60}'),
('REPO_ACCESS', 'REPO_TAKEOFF', 70, '{"minProgress":30}'),
('REPO_ACCESS', 'REPO_CHASE', 30, '{"minTail":2}'),
('REPO_TAKEOFF', 'REPO_CHASE', 60, '{"minTail":2}'),

('HEIST_APPROACH', 'HEIST_ENTRY', 80, '{"minProgress":0,"maxProgress":60}'),
('HEIST_ENTRY', 'HEIST_VAULT', 70, '{"minProgress":30}'),
('HEIST_ENTRY', 'HEIST_EXIT', 30, '{"minHeat":60}'),
('HEIST_VAULT', 'HEIST_EXIT', 80, '{"minProgress":60}'),

('CHASE_START', 'CHASE_PRESSURE', 80, '{"minProgress":0}'),
('CHASE_PRESSURE', 'CHASE_BREAK', 70, '{"minProgress":40}'),
('CHASE_BREAK', 'CHASE_SAFE', 80, '{"minHeat":0,"maxHeat":80}');

UPDATE game_scene SET node_key = 'DELIVERY_PICKUP' WHERE mission_type_key = 'DELIVERY' AND max_progress <= 40;
UPDATE game_scene SET node_key = 'DELIVERY_TRANSIT' WHERE mission_type_key = 'DELIVERY' AND min_progress > 40 AND max_progress < 90;
UPDATE game_scene SET node_key = 'DELIVERY_HANDOFF' WHERE mission_type_key = 'DELIVERY' AND min_progress >= 90;

UPDATE game_scene SET node_key = 'REPO_SPOT' WHERE mission_type_key = 'REPO' AND max_progress <= 40;
UPDATE game_scene SET node_key = 'REPO_ACCESS' WHERE mission_type_key = 'REPO' AND min_progress > 40 AND max_progress < 90;
UPDATE game_scene SET node_key = 'REPO_TAKEOFF' WHERE mission_type_key = 'REPO' AND min_progress >= 90;

UPDATE game_scene SET node_key = 'HEIST_APPROACH' WHERE mission_type_key = 'HEIST' AND max_progress <= 40;
UPDATE game_scene SET node_key = 'HEIST_ENTRY' WHERE mission_type_key = 'HEIST' AND min_progress > 40 AND max_progress < 90;
UPDATE game_scene SET node_key = 'HEIST_VAULT' WHERE mission_type_key = 'HEIST' AND min_progress >= 90;

UPDATE game_scene SET node_key = 'CHASE_START' WHERE mission_type_key = 'CHASE' AND max_progress <= 40;
UPDATE game_scene SET node_key = 'CHASE_PRESSURE' WHERE mission_type_key = 'CHASE' AND min_progress > 40 AND max_progress < 90;
UPDATE game_scene SET node_key = 'CHASE_BREAK' WHERE mission_type_key = 'CHASE' AND min_progress >= 90;

UPDATE game_event SET event_kind = 'REACTION', trigger_action_types_json = '["drive"]' WHERE event_key IN ('POLICE_STOP', 'TRAFFIC_JAM', 'STREET_RACE');
UPDATE game_event SET event_kind = 'REACTION', trigger_action_types_json = '["stealth"]' WHERE event_key IN ('SECURITY_ALERT');
UPDATE game_event SET event_kind = 'REACTION', trigger_action_types_json = '["talk"]' WHERE event_key IN ('DIRTY_COP');

UPDATE game_action SET trigger_events_json = '["POLICE_STOP","TRAFFIC_JAM"]' WHERE action_key IN ('DRIVE_FAST', 'CUT_THROUGH');
UPDATE game_action SET trigger_events_json = '["SECURITY_ALERT"]' WHERE action_key IN ('SNEAK_IN', 'HACK_PANEL');
UPDATE game_action SET trigger_events_json = '["DIRTY_COP"]' WHERE action_key IN ('BRIBE_COP', 'SMOOTH_TALK');

UPDATE game_action SET requirements_json = '{"requiredItems":["LOCKPICK"]}' WHERE action_key = 'LOCKPICK_ENTRY';
UPDATE game_action SET requirements_json = '{"requiredItems":["RADIO"]}' WHERE action_key = 'CALL_BACKUP';
UPDATE game_action SET requirements_json = '{"requiredItems":["SMOKE"]}' WHERE action_key = 'SMOKE_SCREEN';

UPDATE game_action SET success_effects_json = '{"deltaProgress":12,"deltaCoins":{"min":2,"max":4},"deltaTail":1}' WHERE action_key = 'DRIVE_FAST';
UPDATE game_action SET fail_effects_json = '{"deltaProgress":-5,"deltaHeat":8,"setFlag":"cargo_damaged"}' WHERE action_key = 'DRIVE_FAST';
UPDATE game_action SET success_effects_json = '{"deltaProgress":10,"deltaHeat":-2,"clearFlag":"tail_detected"}' WHERE action_key = 'CUT_THROUGH';
UPDATE game_action SET success_effects_json = '{"deltaProgress":8,"deltaHeat":-3,"clearFlag":"spotted_by_cameras"}' WHERE action_key = 'QUIET_ROUTE';
UPDATE game_action SET fail_effects_json = '{"deltaProgress":-2,"deltaHeat":4,"deltaTail":1}' WHERE action_key = 'QUIET_ROUTE';

UPDATE game_event SET effects_json = '{"deltaCoins":{"min":-4,"max":-2},"deltaHeat":4,"deltaTail":1}' WHERE event_key = 'MUGGING';
UPDATE game_event SET effects_json = '{"deltaHeat":10,"deltaProgress":-4,"deltaTail":1}' WHERE event_key = 'POLICE_STOP';
