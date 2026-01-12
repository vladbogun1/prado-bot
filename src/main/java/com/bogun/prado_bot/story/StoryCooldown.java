package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryCooldown {
    private Long id;
    private long guildId;
    private long userId;
    private String campaignKey;
    private Instant lastFinishedAt;
    private int dailyEarned;
    private LocalDate dailyDate;
}
