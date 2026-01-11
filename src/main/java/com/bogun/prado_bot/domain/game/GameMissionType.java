package com.bogun.prado_bot.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "game_mission_type")
public class GameMissionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_key", nullable = false, length = 32, unique = true)
    private String key;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "objective", nullable = false, length = 200)
    private String objective;
}
