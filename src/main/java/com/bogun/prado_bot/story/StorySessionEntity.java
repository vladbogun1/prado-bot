package com.bogun.prado_bot.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "story_session")
public class StorySessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "channel_id", nullable = false)
    private long channelId;

    @Column(name = "campaign_key", nullable = false, length = 40)
    private String campaignKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "node_key", nullable = false, length = 60)
    private String nodeKey;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "rng_seed", nullable = false)
    private long rngSeed;

    @Column(name = "stats_json", nullable = false, columnDefinition = "TEXT")
    private String statsJson;

    @Column(name = "flags_json", nullable = false, columnDefinition = "TEXT")
    private String flagsJson;

    @Column(name = "inventory_json", nullable = false, columnDefinition = "TEXT")
    private String inventoryJson;

    @Column(name = "earned_temp", nullable = false)
    private int earnedTemp;

    @Column(name = "last_outcome_text", nullable = false, columnDefinition = "TEXT")
    private String lastOutcomeText;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
