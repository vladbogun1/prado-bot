package com.bogun.prado_bot.story;

import java.time.Instant;
import java.time.LocalDate;

public class StoryCooldown {
    private Long id;
    private long guildId;
    private long userId;
    private String campaignKey;
    private Instant lastFinishedAt;
    private int dailyEarned;
    private LocalDate dailyDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getGuildId() {
        return guildId;
    }

    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getCampaignKey() {
        return campaignKey;
    }

    public void setCampaignKey(String campaignKey) {
        this.campaignKey = campaignKey;
    }

    public Instant getLastFinishedAt() {
        return lastFinishedAt;
    }

    public void setLastFinishedAt(Instant lastFinishedAt) {
        this.lastFinishedAt = lastFinishedAt;
    }

    public int getDailyEarned() {
        return dailyEarned;
    }

    public void setDailyEarned(int dailyEarned) {
        this.dailyEarned = dailyEarned;
    }

    public LocalDate getDailyDate() {
        return dailyDate;
    }

    public void setDailyDate(LocalDate dailyDate) {
        this.dailyDate = dailyDate;
    }
}
