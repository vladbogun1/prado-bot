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
@Table(name = "game_item")
public class GameItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, length = 32, unique = true)
    private String key;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Lob
    @Column(name = "effects_json", nullable = false)
    private String effectsJson;

    @Column(name = "consumable", nullable = false)
    private boolean consumable;
}
