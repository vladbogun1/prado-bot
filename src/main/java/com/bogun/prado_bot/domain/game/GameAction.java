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
@Table(name = "game_action")
public class GameAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_key", nullable = false, length = 40, unique = true)
    private String key;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Column(name = "action_type", nullable = false, length = 20)
    private String type;

    @Column(name = "mission_type_key", length = 32)
    private String missionTypeKey;

    @Column(name = "node_key", length = 40)
    private String nodeKey;

    @Column(name = "min_progress", nullable = false)
    private int minProgress;

    @Column(name = "max_progress", nullable = false)
    private int maxProgress;

    @Column(name = "min_heat", nullable = false)
    private int minHeat;

    @Column(name = "max_heat", nullable = false)
    private int maxHeat;

    @Column(name = "base_success", nullable = false)
    private double baseSuccess;

    @Column(name = "stat_key", nullable = false, length = 20)
    private String statKey;

    @Column(name = "stat_scale", nullable = false)
    private double statScale;

    @Column(name = "risk", nullable = false)
    private int risk;

    @Lob
    @Column(name = "requirements_json", nullable = false)
    private String requirementsJson;

    @Lob
    @Column(name = "trigger_events_json", nullable = false)
    private String triggerEventsJson;

    @Lob
    @Column(name = "success_effects_json", nullable = false)
    private String successEffectsJson;

    @Lob
    @Column(name = "fail_effects_json", nullable = false)
    private String failEffectsJson;
}
