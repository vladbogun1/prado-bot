package com.bogun.prado_bot.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "story_cooldown")
public class StoryCooldownEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "campaign_key", nullable = false, length = 40)
    private String campaignKey;

    @Column(name = "last_finished_at")
    private Instant lastFinishedAt;

    @Column(name = "daily_earned", nullable = false)
    private int dailyEarned;

    @Column(name = "daily_date", nullable = false)
    private LocalDate dailyDate;
}
