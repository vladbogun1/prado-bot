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
@Table(name = "game_node")
public class GameNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_key", nullable = false, length = 40, unique = true)
    private String key;

    @Column(name = "mission_type_key", nullable = false, length = 32)
    private String missionTypeKey;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "is_start", nullable = false)
    private boolean start;

    @Lob
    @Column(name = "tags_json", nullable = false)
    private String tagsJson;
}
