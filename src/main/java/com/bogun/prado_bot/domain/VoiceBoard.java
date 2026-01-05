package com.bogun.prado_bot.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "voice_boards")
public class VoiceBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="guild_id", nullable=false)
    private long guildId;

    @Column(name="channel_id", nullable=false)
    private long channelId;

    @Column(name="message_id", nullable=false)
    private long messageId;

    @Column(name="message_url", nullable=false, length = 255)
    private String messageUrl;

    @Column(name="view_key", nullable=false, length = 64)
    private String viewKey;

    @Column(name="refresh_seconds", nullable=false)
    private int refreshSeconds;

    @Column(name="line_limit", nullable=false)
    private int lineLimit;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
