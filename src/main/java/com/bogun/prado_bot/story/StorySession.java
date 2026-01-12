package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorySession {
    private Long id;
    private long guildId;
    private long userId;
    private long channelId;
    private String campaignKey;
    private String status;
    private String nodeKey;
    private int step;
    private long rngSeed;
    private Map<String, Integer> stats;
    private Set<String> flags;
    private Map<String, Integer> inventory;
    private int earnedTemp;
    private String lastOutcomeText;
    private int version;
    private Instant createdAt;
    private Instant lastActionAt;
    private Instant expiresAt;
}
