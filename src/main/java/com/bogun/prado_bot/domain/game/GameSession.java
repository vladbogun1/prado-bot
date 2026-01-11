package com.bogun.prado_bot.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "game_session")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameSessionStatus status;

    @Column(name = "mission_type_key", nullable = false, length = 32)
    private String missionTypeKey;

    @Column(name = "location_key", nullable = false, length = 32)
    private String locationKey;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "heat", nullable = false)
    private int heat;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "rng_seed", nullable = false)
    private long rngSeed;

    @Lob
    @Column(name = "stats_json", nullable = false)
    private String statsJson;

    @Lob
    @Column(name = "inventory_json", nullable = false)
    private String inventoryJson;

    @Lob
    @Column(name = "last_scene_text", nullable = false)
    private String lastSceneText;

    @Lob
    @Column(name = "last_outcome_text", nullable = false)
    private String lastOutcomeText;

    @Column(name = "last_delta_coins", nullable = false)
    private int lastDeltaCoins;

    @Lob
    @Column(name = "available_actions_json", nullable = false)
    private String availableActionsJson;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_action_at", nullable = false)
    private Instant lastActionAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
