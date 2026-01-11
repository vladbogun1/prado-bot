package com.bogun.prado_bot.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "game_event_log")
public class GameEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "action_key", nullable = false, length = 40)
    private String actionKey;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "delta_coins", nullable = false)
    private int deltaCoins;

    @Column(name = "delta_heat", nullable = false)
    private int deltaHeat;

    @Column(name = "delta_progress", nullable = false)
    private int deltaProgress;

    @Lob
    @Column(name = "event_keys_json", nullable = false)
    private String eventKeysJson;

    @Lob
    @Column(name = "outcome_text", nullable = false)
    private String outcomeText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
