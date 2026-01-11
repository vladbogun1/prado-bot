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
@Table(name = "game_node_transition")
public class GameNodeTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_node_key", nullable = false, length = 40)
    private String fromNodeKey;

    @Column(name = "to_node_key", nullable = false, length = 40)
    private String toNodeKey;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Lob
    @Column(name = "condition_json", nullable = false)
    private String conditionJson;
}
