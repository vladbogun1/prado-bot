package com.bogun.prado_bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "voice_session")
public class VoiceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "active_seconds", nullable = false)
    private long activeSeconds;

    @Column(name = "paused", nullable = false)
    private boolean paused;

    @Column(name = "last_state_at", nullable = false)
    private Instant lastStateAt;

    @Column(name = "voice_channel_id", nullable = false)
    private Long voiceChannelId;

    @Column(name = "voice_channel_name", nullable = false)
    private String voiceChannelName;

    @Column(name="muted", nullable = false)
    private boolean muted;

    @Column(name="deafened", nullable = false)
    private boolean deafened;

    @Column(name="suppressed", nullable = false)
    private boolean suppressed;

}
