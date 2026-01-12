package com.bogun.prado_bot.story;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

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

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public String getCampaignKey() {
        return campaignKey;
    }

    public void setCampaignKey(String campaignKey) {
        this.campaignKey = campaignKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public long getRngSeed() {
        return rngSeed;
    }

    public void setRngSeed(long rngSeed) {
        this.rngSeed = rngSeed;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public void setStats(Map<String, Integer> stats) {
        this.stats = stats;
    }

    public Set<String> getFlags() {
        return flags;
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
    }

    public Map<String, Integer> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, Integer> inventory) {
        this.inventory = inventory;
    }

    public int getEarnedTemp() {
        return earnedTemp;
    }

    public void setEarnedTemp(int earnedTemp) {
        this.earnedTemp = earnedTemp;
    }

    public String getLastOutcomeText() {
        return lastOutcomeText;
    }

    public void setLastOutcomeText(String lastOutcomeText) {
        this.lastOutcomeText = lastOutcomeText;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActionAt() {
        return lastActionAt;
    }

    public void setLastActionAt(Instant lastActionAt) {
        this.lastActionAt = lastActionAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
