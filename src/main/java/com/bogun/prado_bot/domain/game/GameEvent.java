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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "game_event")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 40, unique = true)
    private String key;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "base_chance", nullable = false)
    private double baseChance;

    @Column(name = "mission_type_key", length = 32)
    private String missionTypeKey;

    @Column(name = "min_progress", nullable = false)
    private int minProgress;

    @Column(name = "max_progress", nullable = false)
    private int maxProgress;

    @Column(name = "min_heat", nullable = false)
    private int minHeat;

    @Column(name = "max_heat", nullable = false)
    private int maxHeat;

    @Lob
    @Column(name = "requirements_json", nullable = false)
    private String requirementsJson;

    @Lob
    @Column(name = "effects_json", nullable = false)
    private String effectsJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
