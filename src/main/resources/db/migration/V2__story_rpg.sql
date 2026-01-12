-- CAMPAIGN
CREATE TABLE story_campaign (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campaign_key VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(255) NOT NULL,
  start_node_key VARCHAR(60) NOT NULL
);

-- NODE
CREATE TABLE story_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campaign_key VARCHAR(40) NOT NULL,
  node_key VARCHAR(60) NOT NULL,
  title VARCHAR(140) NOT NULL,
  variants_json TEXT NOT NULL,
  auto_effects_json TEXT NOT NULL,
  is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
  terminal_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
  reward_json TEXT NOT NULL,
  UNIQUE KEY uq_node (campaign_key, node_key)
);

-- CHOICE (action)
CREATE TABLE story_choice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campaign_key VARCHAR(40) NOT NULL,
  node_key VARCHAR(60) NOT NULL,
  choice_key VARCHAR(80) NOT NULL,
  label VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL,
  conditions_json TEXT NOT NULL,
  check_json TEXT NOT NULL,
  success_node_key VARCHAR(60) NOT NULL,
  fail_node_key VARCHAR(60) NOT NULL,
  success_text VARCHAR(220) NOT NULL,
  fail_text VARCHAR(220) NOT NULL,
  success_effects_json TEXT NOT NULL,
  fail_effects_json TEXT NOT NULL,
  UNIQUE KEY uq_choice (campaign_key, choice_key),
  INDEX ix_choice_node (campaign_key, node_key, sort_order)
);

-- SESSION
CREATE TABLE story_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guild_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  channel_id BIGINT NOT NULL,
  campaign_key VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  node_key VARCHAR(60) NOT NULL,
  step INT NOT NULL,
  rng_seed BIGINT NOT NULL,
  stats_json TEXT NOT NULL,
  flags_json TEXT NOT NULL,
  inventory_json TEXT NOT NULL,
  earned_temp INT NOT NULL,
  last_outcome_text TEXT NOT NULL,
  version INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_action_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  UNIQUE KEY uq_active (guild_id, user_id, campaign_key, status)
);

-- COOLDOWN / DAILY CAP
CREATE TABLE story_cooldown (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guild_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  campaign_key VARCHAR(40) NOT NULL,
  last_finished_at TIMESTAMP NULL,
  daily_earned INT NOT NULL,
  daily_date DATE NOT NULL,
  UNIQUE KEY uq_cd (guild_id, user_id, campaign_key)
);

-- LOG
CREATE TABLE story_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  step INT NOT NULL,
  node_key VARCHAR(60) NOT NULL,
  choice_key VARCHAR(80) NOT NULL,
  success BOOLEAN NOT NULL,
  delta_json TEXT NOT NULL,
  outcome_text TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX ix_log_session (session_id, step)
);

INSERT INTO story_campaign (campaign_key, name, description, start_node_key) VALUES
('BUM_TO_BOSS', 'Бомжара: от пирса до офиса', 'Нодовая RPG в стиле GTA5: выжить на дне, влезть в движ, либо легально подняться. Концовки: бизнес/норм/тюрьма/смерть.', 'N0_UNDER_PIER');

INSERT INTO story_node (campaign_key, node_key, title, variants_json, auto_effects_json, is_terminal, terminal_type, reward_json) VALUES

-- START
('BUM_TO_BOSS','N0_UNDER_PIER','Под пирсом Дель-Перро',
'[
 "Ты просыпаешься под пирсом Дель-Перро. Песок липнет к одежде, во рту привкус дешёвого кофе. В кармане пусто. Над головой — доски пирса и чужие шаги. {hp} HP, $ {cash}, Розыск {wanted}.",
 "Под пирсом пахнет морем и мусором. Ты жив, но это уже достижение. Вдалеке сирена. Ты считаешь мелочь: ничего. {hp} HP, $ {cash}, Розыск {wanted}."
]',
'{}', FALSE, 'NONE',
'{}'
),

-- FIRST BRANCH: легально / криминал / риск
('BUM_TO_BOSS','N1_MORNING_CHOICES','Утро на пляже',
'[
 "Солнце поднимается над пляжем. Туристы залипают в телефоны, продавец хот-догов курит у фургона. Вокруг — шанс и опасность.",
 "На променаде Дель-Перро толпа: кто-то пьёт смузи, кто-то держит сумку нараспашку. Тебе решать, что ты сегодня за человек."
]',
'{}', FALSE, 'NONE','{}'
),

-- LEGAL PATH
('BUM_TO_BOSS','N2_SHELTER','Ночлежка и шанс',
'[
 "В ночлежке пахнет хлоркой. Соцработник смотрит на тебя устало: “Хочешь выбраться — начни с дисциплины”.",
 "Тебе дают койку и душ. Это не дом, но сегодня ты можешь быть чистым и трезвым."
]',
'{"chance":0.25,"effects":[{"op":"stat.add","key":"hp","value":+5}]}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N3_TEMP_JOB','Подработка',
'[
 "Тебя берут на подработку: разгрузка у супермаркета. Спина ноет, но это честные деньги.",
 "Таскаешь коробки. Начальник ворчит, но платит наличкой. Впервые за долгое время у тебя есть план."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N4_SMALL_RENT','Комната на окраине',
'[
 "Ты снимаешь дешёвую комнату. Слышно, как соседи ругаются за стеной, но это твой угол. Ты перестаёшь быть призраком города.",
 "Ключ от комнаты холодит ладонь. Маленькая победа. Но удержать её сложнее, чем получить."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N5_LEGAL_BUSINESS','Легальный рост',
'[
 "Ты находишь идею: маленькая доставка/мойка/ларёк. Нужны связи и дисциплина.",
 "У тебя впервые появляется мысль: “Я могу стать кем-то, если не сорвусь”."
]',
'{}', FALSE,'NONE','{}'
),

-- CRIME PATH
('BUM_TO_BOSS','N2_PICKPOCKET','Лёгкая добыча',
'[
 "Телефон в заднем кармане туриста будто кричит: “Возьми меня”. Но камеры везде.",
 "Ты ловишь момент — и понимаешь, что после первого шага назад дороги почти нет."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N3_PAWNSHOP','Ломбард на бульваре',
'[
 "Ломбардщик крутит в руках добычу. Он не задаёт вопросов — он задаёт цену.",
 "Ты меняешь чужую вещь на грязные деньги. В кармане тяжелеет, а в голове холоднеет."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N4_GANG_OFFER','Предложение от движухи',
'[
 "У переулка тебя тормозит тип в кепке: “Слышал, ты быстрый. Есть работа — грязная, но оплачиваемая”.",
 "Тебе предлагают “простую доставку”. В таких предложениях слово “простая” всегда лживое."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N5_DIRTY_JOB','Грязная работа',
'[
 "Ночь. Машина пахнет бензином и чужим страхом. Ты везёшь пакет, о котором лучше не думать.",
 "Тебе говорят: “Едешь туда, отдаёшь тому, уходишь тихо”. В GTA это никогда не работает."
]',
'{}', FALSE,'NONE','{}'
),

('BUM_TO_BOSS','N6_POLICE_NET','Сетка копов',
'[
 "Сирены рядом. Ты чувствуешь, что город сжимается вокруг тебя.",
 "Патрульная машина идёт параллельно. Один неверный выбор — и ты в наручниках или хуже."
]',
'{}', FALSE,'NONE','{}'
),

-- MIX / TURNING POINT
('BUM_TO_BOSS','N6_CROSSROAD','Переломный момент',
'[
 "Ты на границе: либо сгоришь в грязи, либо сделаешь шаг в нормальную жизнь. Но прошлое уже за тобой следит.",
 "Ты понимаешь: ещё один риск — и всё. Ещё одна честная попытка — и, может, выберешься."
]',
'{}', FALSE,'NONE','{}'
),

-- ENDINGS
('BUM_TO_BOSS','E_SUCCESS_BOSS','Ты стал бизнесменом',
'[
 "Проходит время. Ты сидишь в маленьком офисе. Не небоскрёб, но твой. На столе договоры, а не бутылки. Ты улыбаешься: “Я выбрался”.",
 "Твой бизнес маленький, но честный. Ты уже не прячешься от города — город платит тебе."
]',
'{}', TRUE,'SUCCESS',
'{"payout":{"mode":"keep_all","terminal_bonus":{"min":8,"max":14},"cap_daily":50},"ending_tag":"BOSS"}'
),

('BUM_TO_BOSS','E_NEUTRAL_WORK','Нормальная жизнь',
'[
 "Ты не стал легендой. Но у тебя есть работа, крыша и тишина ночью. Для тебя это победа.",
 "Ты живёшь просто. Иногда тяжело. Но ты жив — и это главное."
]',
'{}', TRUE,'SUCCESS',
'{"payout":{"mode":"keep_all","terminal_bonus":{"min":3,"max":7},"cap_daily":50},"ending_tag":"WORK"}'
),

('BUM_TO_BOSS','E_FAIL_PRISON','Тюрьма',
'[
 "Судья не слушает оправдания. Ты видишь решётку раньше, чем видишь шанс.",
 "Наручники щёлкают. Город забирает своё. В следующий раз думай раньше."
]',
'{}', TRUE,'FAIL',
'{"payout":{"mode":"lose_percent","lose_percent":0.8,"terminal_bonus":{"min":0,"max":2},"cap_daily":50},"ending_tag":"PRISON"}'
),

('BUM_TO_BOSS','E_DEAD','Ты сдох',
'[
 "Выстрел звучит буднично. Город даже не замедляется. На асфальте остаётся только пятно и тишина.",
 "Ты падаешь. Сверху мигают огни. Дель-Перро живёт дальше, как будто тебя никогда не было."
]',
'{}', TRUE,'DEAD',
'{"payout":{"mode":"lose_all","terminal_bonus":{"min":0,"max":0},"cap_daily":50},"ending_tag":"DEAD"}'
);

INSERT INTO story_choice
(campaign_key, node_key, choice_key, label, sort_order,
 conditions_json, check_json,
 success_node_key, fail_node_key,
 success_text, fail_text,
 success_effects_json, fail_effects_json)
VALUES

-- N0 -> N1
('BUM_TO_BOSS','N0_UNDER_PIER','C0_STAND_UP','Встать и осмотреться',1,
'{}',
'{"type":"none"}',
'N1_MORNING_CHOICES','N1_MORNING_CHOICES',
'Ты поднимаешься, стряхиваешь песок и смотришь по сторонам.',
'Ты поднимаешься, стряхиваешь песок и смотришь по сторонам.',
'[]','[]'
),

-- N1: 3 направления
('BUM_TO_BOSS','N1_MORNING_CHOICES','C1_GO_SHELTER','Идти в ночлежку (легально)',1,
'{}',
'{"type":"roll","stat":"discipline","base_success":0.55,"stat_scale":0.03,"wanted_scale":0.002}',
'N2_SHELTER','N2_SHELTER',
'Ты выбираешь сложный путь: дисциплина вместо хаоса.',
'Ты всё равно идёшь туда — даже если стыдно.',
'[{"op":"stat.add","key":"discipline","value":+1},{"op":"stat.add","key":"wanted","value":-2}]',
'[{"op":"stat.add","key":"discipline","value":+0},{"op":"stat.add","key":"hp","value":-2}]'
),

('BUM_TO_BOSS','N1_MORNING_CHOICES','C2_PICKPOCKET','Стащить телефон у туриста',2,
'{}',
'{"type":"roll","stat":"street","base_success":0.52,"stat_scale":0.04,"wanted_scale":0.002,"item_bonuses":{"MASK":0.05}}',
'N2_PICKPOCKET','N6_POLICE_NET',
'Ловкое движение — и телефон уже у тебя.',
'Турист орёт. Кто-то кричит: “Эй!” Камера поймала момент.',
'[{"op":"stat.add","key":"cash","value":+120},{"op":"stat.add","key":"wanted","value":+12},{"op":"stat.add","key":"street","value":+1},{"op":"earned.add","value":+2}]',
'[{"op":"stat.add","key":"wanted","value":+18},{"op":"stat.add","key":"hp","value":-5},{"op":"earned.add","value":-1}]'
),

('BUM_TO_BOSS','N1_MORNING_CHOICES','C3_SCAVENGE','Собирать банки и мелочь',3,
'{}',
'{"type":"roll","stat":"discipline","base_success":0.70,"stat_scale":0.02,"wanted_scale":0.001}',
'N3_TEMP_JOB','N2_SHELTER',
'Ты находишь пару долларов и видишь объявление о подработке.',
'Ты ковыряешься в мусоре, руки дрожат от усталости. Нужен другой план.',
'[{"op":"stat.add","key":"cash","value":+25},{"op":"stat.add","key":"discipline","value":+1},{"op":"earned.add","value":+1}]',
'[{"op":"stat.add","key":"hp","value":-4}]'
),

-- LEGAL PATH chain
('BUM_TO_BOSS','N2_SHELTER','C4_TALK_SOCIAL','Поговорить с соцработником',1,
'{}',
'{"type":"roll","stat":"charisma","base_success":0.60,"stat_scale":0.03,"wanted_scale":0.001}',
'N3_TEMP_JOB','N3_TEMP_JOB',
'Он даёт тебе адрес подработки и совет: “Не срывайся”.',
'Он всё равно даёт адрес, но смотрит так, будто не верит.',
'[{"op":"stat.add","key":"charisma","value":+1},{"op":"flag.add","flag":"HAS_PLAN"}]',
'[]'
),

('BUM_TO_BOSS','N3_TEMP_JOB','C5_WORK_HARD','Пахать смену до конца',1,
'{}',
'{"type":"roll","stat":"discipline","base_success":0.62,"stat_scale":0.04,"wanted_scale":0.002}',
'N4_SMALL_RENT','N2_SHELTER',
'Ты выдержал. Получаешь деньги и ощущение контроля.',
'Тебя клинит. Ты срываешься и уходишь. Завтра будет тяжелее.',
'[{"op":"stat.add","key":"cash","value":+160},{"op":"stat.add","key":"discipline","value":+1},{"op":"earned.add","value":+3},{"op":"stat.add","key":"wanted","value":-3}]',
'[{"op":"stat.add","key":"hp","value":-6},{"op":"stat.add","key":"discipline","value":-1}]'
),

('BUM_TO_BOSS','N4_SMALL_RENT','C6_KEEP_CLEAN','Держаться чисто и копить',1,
'{"min_cash":100}',
'{"type":"roll","stat":"discipline","base_success":0.58,"stat_scale":0.05,"wanted_scale":0.001}',
'N5_LEGAL_BUSINESS','N6_CROSSROAD',
'Ты не сорвался. У тебя появляется шанс на своё дело.',
'Тяжело. Друзья с улицы тянут обратно. Ты на переломе.',
'[{"op":"stat.add","key":"cash","value":+200},{"op":"stat.add","key":"discipline","value":+1},{"op":"earned.add","value":+4},{"op":"flag.add","flag":"SAVED_MONEY"}]',
'[{"op":"stat.add","key":"hp","value":-8},{"op":"stat.add","key":"wanted","value":+5}]'
),

('BUM_TO_BOSS','N5_LEGAL_BUSINESS','C7_START_SMALL','Открыть микробизнес (доставка/мойка)',1,
'{"min_cash":250,"required_flags":["SAVED_MONEY"]}',
'{"type":"roll","stat":"discipline","base_success":0.55,"stat_scale":0.05,"wanted_scale":0.002}',
'E_SUCCESS_BOSS','E_NEUTRAL_WORK',
'Ты запускаешь дело. Маленькое, но твоё.',
'Дело не взлетело, но ты остаёшься на плаву — работа есть.',
'[{"op":"earned.add","value":+8}]',
'[{"op":"earned.add","value":+3}]'
),

-- CRIME PATH chain
('BUM_TO_BOSS','N2_PICKPOCKET','C8_RUN_PAWN','Скинуть добычу в ломбард',1,
'{}',
'{"type":"roll","stat":"street","base_success":0.62,"stat_scale":0.03,"wanted_scale":0.003}',
'N3_PAWNSHOP','N6_POLICE_NET',
'Ломбардщик берёт товар. Деньги у тебя.',
'Около ломбарда стоят копы. Слишком поздно разворачиваться.',
'[{"op":"stat.add","key":"cash","value":+90},{"op":"earned.add","value":+2}]',
'[{"op":"stat.add","key":"wanted","value":+12},{"op":"earned.add","value":-1}]'
),

('BUM_TO_BOSS','N3_PAWNSHOP','C9_TAKE_GANG_OFFER','Согласиться на “работу”',1,
'{}',
'{"type":"roll","stat":"street","base_success":0.56,"stat_scale":0.04,"wanted_scale":0.002}',
'N4_GANG_OFFER','N6_POLICE_NET',
'Ты киваешь. Дверь в грязь открыта.',
'Тебя кто-то палит. В переулке слишком много глаз.',
'[{"op":"stat.add","key":"street","value":+1},{"op":"stat.add","key":"wanted","value":+6},{"op":"earned.add","value":+2}]',
'[{"op":"stat.add","key":"wanted","value":+10},{"op":"stat.add","key":"hp","value":-6}]'
),

('BUM_TO_BOSS','N4_GANG_OFFER','C10_DIRTY_DELIVERY','Взять грязную доставку',1,
'{}',
'{"type":"roll","stat":"street","base_success":0.50,"stat_scale":0.04,"wanted_scale":0.003}',
'N5_DIRTY_JOB','N6_POLICE_NET',
'Ты берёшь пакет. Теперь ты в игре.',
'Пакет оказался меткой. Тебя уже ждут проблемы.',
'[{"op":"stat.add","key":"cash","value":+70},{"op":"stat.add","key":"wanted","value":+10},{"op":"earned.add","value":+4}]',
'[{"op":"stat.add","key":"wanted","value":+18},{"op":"earned.add","value":-2}]'
),

('BUM_TO_BOSS','N5_DIRTY_JOB','C11_HIDE_LOW','Проехать тихо, без внимания',1,
'{"max_wanted":70}',
'{"type":"roll","stat":"discipline","base_success":0.48,"stat_scale":0.04,"wanted_scale":0.004}',
'N6_CROSSROAD','N6_POLICE_NET',
'Ты почти вывозишь. Но страх остаётся.',
'Сирены. Ты понимаешь, что тебя повязали по цепочке.',
'[{"op":"stat.add","key":"wanted","value":-4},{"op":"earned.add","value":+2}]',
'[{"op":"stat.add","key":"wanted","value":+15},{"op":"earned.add","value":-2}]'
),

-- POLICE NET: может быть тюрьма/смерть/выскочить
('BUM_TO_BOSS','N6_POLICE_NET','C12_BRIBE','Попробовать откупиться',1,
'{"min_cash":50}',
'{"type":"roll","stat":"charisma","base_success":0.45,"stat_scale":0.04,"wanted_scale":0.003}',
'N6_CROSSROAD','E_FAIL_PRISON',
'Ты говоришь спокойно. Деньги решают вопрос — пока что.',
'Копы улыбаются: “Спасибо”. И надевают наручники.',
'[{"op":"stat.add","key":"cash","value":-50},{"op":"stat.add","key":"wanted","value":-10},{"op":"earned.add","value":+1}]',
'[]'
),

('BUM_TO_BOSS','N6_POLICE_NET','C13_RUN','Рвануть и уйти дворами',2,
'{}',
'{"type":"roll","stat":"street","base_success":0.42,"stat_scale":0.05,"wanted_scale":0.004}',
'N6_CROSSROAD','E_DEAD',
'Ты уходишь в лабиринт дворов. Сердце рвёт грудь, но ты жив.',
'Ты бежишь — и слышишь хлопок. Дальше только темнота.',
'[{"op":"stat.add","key":"hp","value":-12},{"op":"stat.add","key":"wanted","value":+8},{"op":"earned.add","value":+1}]',
'[]'
),

-- CROSSROAD: выход из криминала или добить себя
('BUM_TO_BOSS','N6_CROSSROAD','C14_GO_LEGAL','Сдаться и перейти в легал (начать заново)',1,
'{"max_wanted":60}',
'{"type":"roll","stat":"discipline","base_success":0.52,"stat_scale":0.05,"wanted_scale":0.002}',
'N2_SHELTER','E_FAIL_PRISON',
'Ты режешь связи и выбираешь вылезти. Это больно, но правильно.',
'Прошлое догоняет. Тебя закрывают по старым делам.',
'[{"op":"stat.add","key":"wanted","value":-12},{"op":"stat.add","key":"discipline","value":+1}]',
'[]'
),

('BUM_TO_BOSS','N6_CROSSROAD','C15_LAST_SCORE','Пойти ва-банк: последний грязный заработок',2,
'{"min_hp":30}',
'{"type":"roll","stat":"street","base_success":0.40,"stat_scale":0.06,"wanted_scale":0.004}',
'E_SUCCESS_BOSS','E_DEAD',
'Ты рискуешь — и выигрываешь. Теперь можно отмыться и открыть дело.',
'Ты ошибся. Здесь нет вторых попыток.',
'[{"op":"earned.add","value":+10},{"op":"stat.add","key":"wanted","value":+5}]',
'[]'
);
