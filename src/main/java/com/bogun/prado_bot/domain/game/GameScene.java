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
@Table(name = "game_scene")
public class GameScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_type_key", nullable = false, length = 32)
    private String missionTypeKey;

    @Column(name = "location_key", nullable = false, length = 32)
    private String locationKey;

    @Column(name = "min_progress", nullable = false)
    private int minProgress;

    @Column(name = "max_progress", nullable = false)
    private int maxProgress;

    @Column(name = "min_heat", nullable = false)
    private int minHeat;

    @Column(name = "max_heat", nullable = false)
    private int maxHeat;

    @Lob
    @Column(name = "scene_text", nullable = false)
    private String sceneText;
}
