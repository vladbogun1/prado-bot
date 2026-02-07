CREATE TABLE voice_user
(
    guild_id    BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    username    VARCHAR(100) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    points      BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (guild_id, user_id),
    muted       BIT(1)       NOT NULL DEFAULT 0,
    deafened    BIT(1)       NOT NULL DEFAULT 0,
    suppressed  BIT(1)       NOT NULL DEFAULT 0
);

CREATE TABLE voice_session
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id           BIGINT       NOT NULL,
    user_id            BIGINT       NOT NULL,

    started_at         TIMESTAMP    NOT NULL,
    ended_at           TIMESTAMP NULL,

    active_seconds     BIGINT       NOT NULL DEFAULT 0,
    paused             BOOLEAN      NOT NULL DEFAULT FALSE,
    last_state_at      TIMESTAMP    NOT NULL,

    voice_channel_id   BIGINT       NOT NULL,
    voice_channel_name VARCHAR(100) NOT NULL
);

create table if not exists voice_boards (
    id bigint primary key auto_increment,
    guild_id bigint not null,
    channel_id bigint not null,
    message_id bigint not null,
    message_url varchar(255) not null,
    view_key varchar(64) not null,
    refresh_seconds int not null,
    line_limit int not null,
    created_at timestamp(3) not null,
    updated_at timestamp(3) not null,
    unique key uq_voice_boards (guild_id, channel_id, view_key)
);

CREATE INDEX ix_voice_session_guild_started ON voice_session (guild_id, started_at);
CREATE INDEX ix_voice_session_guild_user_ended ON voice_session (guild_id, user_id, ended_at);
