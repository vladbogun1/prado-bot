package com.bogun.prado_bot.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "story_log")
public class StoryLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private long sessionId;

    @Column(name = "step", nullable = false)
    private int step;

    @Column(name = "node_key", nullable = false, length = 60)
    private String nodeKey;

    @Column(name = "choice_key", nullable = false, length = 80)
    private String choiceKey;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "delta_json", nullable = false, columnDefinition = "TEXT")
    private String deltaJson;

    @Column(name = "outcome_text", nullable = false, columnDefinition = "TEXT")
    private String outcomeText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
