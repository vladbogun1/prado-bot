CREATE TABLE game_mission_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mission_key VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    objective VARCHAR(200) NOT NULL
);

CREATE TABLE game_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location_key VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE game_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_key VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    effects_json TEXT NOT NULL,
    consumable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE game_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_key VARCHAR(40) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    mission_type_key VARCHAR(32) NULL,
    min_progress INT NOT NULL,
    max_progress INT NOT NULL,
    min_heat INT NOT NULL,
    max_heat INT NOT NULL,
    base_success DOUBLE NOT NULL,
    stat_key VARCHAR(20) NOT NULL,
    stat_scale DOUBLE NOT NULL,
    risk INT NOT NULL,
    requirements_json TEXT NOT NULL,
    success_effects_json TEXT NOT NULL,
    fail_effects_json TEXT NOT NULL
);

CREATE TABLE game_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_key VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    weight INT NOT NULL,
    base_chance DOUBLE NOT NULL,
    mission_type_key VARCHAR(32) NULL,
    min_progress INT NOT NULL,
    max_progress INT NOT NULL,
    min_heat INT NOT NULL,
    max_heat INT NOT NULL,
    requirements_json TEXT NOT NULL,
    effects_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE game_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mission_type_key VARCHAR(32) NOT NULL,
    location_key VARCHAR(32) NOT NULL,
    min_progress INT NOT NULL,
    max_progress INT NOT NULL,
    min_heat INT NOT NULL,
    max_heat INT NOT NULL,
    scene_text TEXT NOT NULL
);

CREATE TABLE game_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    mission_type_key VARCHAR(32) NOT NULL,
    location_key VARCHAR(32) NOT NULL,
    progress INT NOT NULL,
    heat INT NOT NULL,
    step INT NOT NULL,
    rng_seed BIGINT NOT NULL,
    stats_json TEXT NOT NULL,
    inventory_json TEXT NOT NULL,
    last_scene_text TEXT NOT NULL,
    last_outcome_text TEXT NOT NULL,
    last_delta_coins INT NOT NULL,
    available_actions_json TEXT NOT NULL,
    version INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_action_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE game_cooldown (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_game_finished_at TIMESTAMP NULL,
    daily_earned INT NOT NULL,
    daily_date DATE NOT NULL,
    UNIQUE KEY uq_game_cooldown (guild_id, user_id)
);

CREATE TABLE game_event_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    step INT NOT NULL,
    action_key VARCHAR(40) NOT NULL,
    success BOOLEAN NOT NULL,
    delta_coins INT NOT NULL,
    delta_heat INT NOT NULL,
    delta_progress INT NOT NULL,
    event_keys_json TEXT NOT NULL,
    outcome_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_game_session_active ON game_session (guild_id, user_id, status);
CREATE INDEX ix_game_session_expires ON game_session (status, expires_at);
CREATE INDEX ix_game_event_log_session ON game_event_log (session_id, step);

INSERT INTO game_mission_type (mission_key, name, objective) VALUES
('DELIVERY', 'Грязная доставка', 'Доставить груз без шума'),
('REPO', 'Возврат долга', 'Вернуть чужую тачку в целости'),
('HEIST', 'Тихое ограбление', 'Вынести кэш и не спалиться'),
('CHASE', 'Жёсткая погоня', 'Уйти от хвоста по улицам LS');

INSERT INTO game_location (location_key, name, description) VALUES
('VINEWOOD', 'Вайнвуд', 'Блеск неона и скрытые камеры'),
('GROVE', 'Гроув-стрит', 'Своя территория, но чужие смотрят'),
('DEL_PERRO', 'Дель-Перро', 'Пляж, туристы и копы рядом'),
('SANDY', 'Сэнди-Шорс', 'Пыль, ранчо и пустые трассы'),
('DOWNTOWN', 'Даунтаун', 'Плотный трафик и высокие ставки');

INSERT INTO game_item (item_key, name, description, effects_json, consumable) VALUES
('LOCKPICK', 'Отмычка', 'Открывает быстрые пути.', '{"successBonus":0.05}', TRUE),
('MASK', 'Маска', 'Скрывает лицо на камерах.', '{"successBonus":0.03}', FALSE),
('ENERGY', 'Энергетик', 'Резкий буст на один ход.', '{"successBonus":0.04}', TRUE),
('ARMOR', 'Бронежилет', 'Снижает риск ошибок.', '{"successBonus":0.02}', FALSE),
('RADIO', 'Рация', 'Связь с напарником.', '{"successBonus":0.03}', FALSE),
('SMOKE', 'Дымовуха', 'Даёт шанс уйти незамеченным.', '{"successBonus":0.02}', TRUE),
('BRIBE', 'Пачка налички', 'Убеждает проблемных людей.', '{"successBonus":0.05}', TRUE),
('HACK', 'Хак-кит', 'Открывает цифровые двери.', '{"successBonus":0.04}', TRUE),
('SPIKES', 'Шипы', 'Срывает погоню за спиной.', '{"successBonus":0.03}', TRUE),
('MEDKIT', 'Аптечка', 'Держит в тонусе.', '{"successBonus":0.02}', TRUE);

INSERT INTO game_action (action_key, label, action_type, mission_type_key, min_progress, max_progress, min_heat, max_heat, base_success, stat_key, stat_scale, risk, requirements_json, success_effects_json, fail_effects_json) VALUES
('DRIVE_FAST', 'Газ в пол', 'drive', NULL, 0, 100, 0, 100, 0.55, 'drive', 0.4, 8, '{}', '{"deltaProgress":12,"deltaCoins":{"min":2,"max":4}}', '{"deltaProgress":-5,"deltaHeat":8}'),
('CUT_THROUGH', 'Срезать дворами', 'drive', 'DELIVERY', 0, 80, 0, 70, 0.6, 'drive', 0.35, 6, '{}', '{"deltaProgress":10,"deltaHeat":-2}', '{"deltaHeat":10}'),
('QUIET_ROUTE', 'Тихий маршрут', 'drive', NULL, 0, 100, 0, 60, 0.62, 'drive', 0.3, 3, '{}', '{"deltaProgress":8,"deltaHeat":-3}', '{"deltaProgress":-2,"deltaHeat":4}'),
('DRIFT_ESCAPE', 'Дрифт на развороте', 'drive', 'CHASE', 30, 100, 30, 100, 0.5, 'drive', 0.45, 10, '{}', '{"deltaProgress":14,"deltaHeat":-4}', '{"deltaHeat":12}'),
('HIDE_CAR', 'Спрятать тачку', 'stealth', 'REPO', 20, 100, 0, 80, 0.6, 'stealth', 0.4, 4, '{}', '{"deltaProgress":10,"deltaHeat":-5}', '{"deltaHeat":6}'),
('SNEAK_IN', 'Пробраться внутрь', 'stealth', 'HEIST', 0, 60, 0, 60, 0.6, 'stealth', 0.45, 5, '{}', '{"deltaProgress":9,"deltaCoins":{"min":2,"max":3}}', '{"deltaHeat":7}'),
('LOCKPICK_ENTRY', 'Взломать дверь', 'stealth', NULL, 0, 100, 0, 70, 0.58, 'stealth', 0.35, 5, '{"requiredItems":["LOCKPICK"]}', '{"deltaProgress":10,"deltaCoins":{"min":1,"max":2}}', '{"deltaHeat":8}'),
('MASK_UP', 'Замаскироваться', 'stealth', NULL, 0, 100, 0, 80, 0.65, 'stealth', 0.25, 2, '{"requiredItems":["MASK"]}', '{"deltaHeat":-6}', '{"deltaHeat":4}'),
('TALK_BOUNCER', 'Уговорить охрану', 'talk', 'HEIST', 0, 80, 0, 70, 0.55, 'talk', 0.4, 6, '{}', '{"deltaProgress":8,"deltaCoins":{"min":1,"max":2}}', '{"deltaHeat":10}'),
('BRIBE_COP', 'Подмазать копа', 'talk', NULL, 0, 100, 20, 100, 0.5, 'talk', 0.45, 4, '{"requiredItems":["BRIBE"]}', '{"deltaHeat":-10,"deltaCoins":-2}', '{"deltaHeat":8}'),
('SMOOTH_TALK', 'Заговорить свидетеля', 'talk', NULL, 0, 100, 0, 70, 0.6, 'talk', 0.35, 3, '{}', '{"deltaHeat":-5,"deltaProgress":5}', '{"deltaHeat":6}'),
('INTIMIDATE', 'Прижать должника', 'talk', 'REPO', 0, 60, 0, 70, 0.52, 'talk', 0.35, 7, '{}', '{"deltaProgress":8,"deltaCoins":{"min":2,"max":4}}', '{"deltaHeat":9}'),
('HACK_PANEL', 'Вскрыть панель', 'stealth', 'HEIST', 10, 90, 0, 80, 0.55, 'stealth', 0.4, 5, '{"requiredItems":["HACK"]}', '{"deltaProgress":12,"deltaCoins":{"min":2,"max":3}}', '{"deltaHeat":9}'),
('CALL_BACKUP', 'Вызвать подмогу', 'talk', NULL, 0, 100, 0, 100, 0.6, 'talk', 0.3, 5, '{"requiredItems":["RADIO"]}', '{"deltaProgress":6,"deltaHeat":-3}', '{"deltaHeat":5}'),
('SMOKE_SCREEN', 'Дымовая завеса', 'stealth', NULL, 0, 100, 20, 100, 0.6, 'stealth', 0.3, 2, '{"requiredItems":["SMOKE"]}', '{"deltaHeat":-8}', '{"deltaHeat":4}'),
('SPIKES_DROP', 'Рассыпать шипы', 'drive', 'CHASE', 40, 100, 20, 100, 0.55, 'drive', 0.35, 5, '{"requiredItems":["SPIKES"]}', '{"deltaHeat":-10,"deltaProgress":6}', '{"deltaHeat":6}'),
('ENERGY_PUSH', 'Рывок на кофеине', 'drive', NULL, 0, 100, 0, 100, 0.57, 'drive', 0.3, 4, '{"requiredItems":["ENERGY"]}', '{"deltaProgress":10}', '{"deltaHeat":6}'),
('MEDKIT_STEADY', 'Прийти в себя', 'stealth', NULL, 0, 100, 0, 100, 0.62, 'stealth', 0.25, 1, '{"requiredItems":["MEDKIT"]}', '{"deltaHeat":-4}', '{"deltaHeat":2}'),
('ARMOR_UP', 'Встать в броню', 'stealth', NULL, 0, 100, 0, 100, 0.6, 'stealth', 0.2, 1, '{"requiredItems":["ARMOR"]}', '{"deltaHeat":-3}', '{"deltaHeat":2}'),
('FAST_HANDOFF', 'Быстрая передача', 'talk', 'DELIVERY', 40, 100, 0, 70, 0.6, 'talk', 0.35, 4, '{}', '{"deltaProgress":12,"deltaCoins":{"min":2,"max":3}}', '{"deltaHeat":7}'),
('PARKOUR_EXIT', 'Паркур через крыши', 'stealth', NULL, 20, 100, 0, 90, 0.5, 'stealth', 0.45, 8, '{}', '{"deltaProgress":11}', '{"deltaHeat":10}'),
('CUT_CAMERA', 'Вырубить камеру', 'stealth', 'HEIST', 0, 80, 0, 60, 0.62, 'stealth', 0.3, 3, '{}', '{"deltaHeat":-6,"deltaProgress":6}', '{"deltaHeat":5}'),
('SWITCH_PLATES', 'Сменить номера', 'stealth', 'CHASE', 30, 100, 0, 70, 0.58, 'stealth', 0.35, 4, '{}', '{"deltaHeat":-7}', '{"deltaHeat":6}'),
('SIDE_DEAL', 'Сделка на стороне', 'talk', NULL, 0, 100, 0, 80, 0.52, 'talk', 0.3, 6, '{}', '{"deltaCoins":{"min":2,"max":5},"deltaHeat":4}', '{"deltaHeat":8}'),
('LAST_PUSH', 'Финишный рывок', 'drive', NULL, 70, 100, 0, 100, 0.5, 'drive', 0.45, 9, '{}', '{"deltaProgress":15,"deltaCoins":{"min":3,"max":5}}', '{"deltaHeat":12}');

INSERT INTO game_event (event_key, title, weight, base_chance, mission_type_key, min_progress, max_progress, min_heat, max_heat, requirements_json, effects_json, status) VALUES
('FIND_STASH', 'Нашёл тайник с кэшем', 8, 0.25, NULL, 0, 100, 0, 100, '{}', '{"deltaCoins":{"min":3,"max":6}}', 'continue'),
('MUGGING', 'Пацаны пытались ограбить', 6, 0.2, NULL, 0, 100, 0, 100, '{}', '{"deltaCoins":{"min":-4,"max":-2},"deltaHeat":4}', 'continue'),
('POLICE_STOP', 'Копы устроили проверку', 5, 0.18, NULL, 0, 100, 30, 100, '{}', '{"deltaHeat":10,"deltaProgress":-4}', 'continue'),
('RANDOM_HELPER', 'Случайный помощник дал предмет', 4, 0.2, NULL, 0, 100, 0, 100, '{}', '{"addItem":"ENERGY"}', 'continue'),
('BAD_ROAD', 'Плохая дорога', 5, 0.18, NULL, 0, 100, 0, 100, '{}', '{"deltaProgress":-6}', 'continue'),
('TRAFFIC_JAM', 'Пробка отнимает время', 4, 0.2, 'DELIVERY', 0, 100, 0, 100, '{}', '{"deltaProgress":-5,"deltaHeat":3}', 'continue'),
('SECURITY_ALERT', 'Сработала тревога', 5, 0.2, 'HEIST', 0, 100, 0, 100, '{}', '{"deltaHeat":12}', 'continue'),
('STREET_RACE', 'Шальной гонщик помог уйти', 3, 0.15, 'CHASE', 20, 100, 0, 100, '{}', '{"deltaHeat":-8,"deltaProgress":6}', 'continue'),
('DIRTY_COP', 'Грязный коп попросил долю', 4, 0.16, NULL, 0, 100, 0, 100, '{}', '{"deltaCoins":{"min":-3,"max":-1},"deltaHeat":-2}', 'continue'),
('CASH_DROP', 'Кейс с деньгами в кустах', 3, 0.12, NULL, 0, 100, 0, 100, '{}', '{"deltaCoins":{"min":2,"max":4}}', 'continue'),
('LOCAL_BACKUP', 'Местные прикрыли отход', 3, 0.14, 'REPO', 20, 100, 0, 100, '{}', '{"deltaHeat":-6,"deltaProgress":5}', 'continue'),
('RADIO_TIP', 'По рации подсказали маршрут', 4, 0.18, NULL, 0, 100, 0, 100, '{"requiredItems":["RADIO"]}', '{"deltaProgress":6}', 'continue');

INSERT INTO game_scene (mission_type_key, location_key, min_progress, max_progress, min_heat, max_heat, scene_text) VALUES
('DELIVERY', 'VINEWOOD', 0, 30, 0, 40, 'Вайнвуд светится, а груз в багажнике тяжёлый. Нужно проскочить тихо.'),
('DELIVERY', 'VINEWOOD', 30, 70, 0, 70, 'Ты на холмах Вайнвуда, камеры на каждом повороте. Любая ошибка — и копы рядом.'),
('DELIVERY', 'DEL_PERRO', 0, 40, 0, 60, 'Дель-Перро шумит туристами. Курьер ждёт у пирса.'),
('DELIVERY', 'DOWNTOWN', 40, 90, 20, 100, 'Даунтаун зажат в трафике. Время идёт, а заказчик нервничает.'),
('DELIVERY', 'SANDY', 60, 100, 0, 80, 'Сэнди-Шорс встречает пылью и пустыми трассами. Тут можно прибавить ход.'),
('REPO', 'GROVE', 0, 30, 0, 50, 'Гроув-стрит молчит, но окна светятся. Должник где-то рядом.'),
('REPO', 'GROVE', 30, 80, 0, 80, 'Соседи уже заметили возню вокруг тачки. Время уходит.'),
('REPO', 'DOWNTOWN', 20, 70, 20, 90, 'Парковка у офисов полна камер. Нужно забрать машину чисто.'),
('REPO', 'SANDY', 0, 60, 0, 60, 'На ранчо шумят двигатели. Добыча готова, но сторож на чеку.'),
('REPO', 'DEL_PERRO', 50, 100, 0, 70, 'Пляжные улицы узкие, а владелец близко. Осталось увести тачку.'),
('HEIST', 'DOWNTOWN', 0, 40, 0, 60, 'Даунтаун хранит кэш за стеклом. Нужно пройти охрану.'),
('HEIST', 'DOWNTOWN', 40, 80, 20, 80, 'Сейф уже рядом, но тревога может вспыхнуть в любую секунду.'),
('HEIST', 'VINEWOOD', 0, 50, 0, 60, 'Вайнвудский особняк богатый, но охрана без улыбок.'),
('HEIST', 'DEL_PERRO', 30, 90, 0, 80, 'Клуб у пляжа шумит. Твоя цель — касса в подвале.'),
('HEIST', 'GROVE', 60, 100, 0, 70, 'Свой район, но сегодня чужая добыча. Осталось вынести сумку.'),
('CHASE', 'DOWNTOWN', 0, 40, 20, 100, 'В центре города копы уже на хвосте. Манёвр решает всё.'),
('CHASE', 'VINEWOOD', 30, 70, 20, 100, 'Вайнвудские серпантины дают шанс оторваться.'),
('CHASE', 'DEL_PERRO', 0, 50, 10, 90, 'Пляжные дороги забиты, но здесь есть короткие тропы.'),
('CHASE', 'SANDY', 40, 90, 0, 80, 'Сэнди-Шорс даёт простор, но пыль выдаёт след.'),
('CHASE', 'GROVE', 60, 100, 30, 100, 'Гроув-стрит уводит в узкие проезды. Погоня всё ближе.');
