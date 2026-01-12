package com.bogun.prado_bot.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StorySessionStore {

    private static final TypeReference<Map<String, Integer>> MAP_INT = new TypeReference<>() {};
    private static final TypeReference<Set<String>> SET_STRING = new TypeReference<>() {};

    private final StorySessionRepository sessionRepository;
    private final StoryCooldownRepository cooldownRepository;
    private final StoryLogRepository logRepository;
    private final ObjectMapper mapper;

    public StorySessionStore(StorySessionRepository sessionRepository,
                             StoryCooldownRepository cooldownRepository,
                             StoryLogRepository logRepository,
                             ObjectMapper mapper) {
        this.sessionRepository = sessionRepository;
        this.cooldownRepository = cooldownRepository;
        this.logRepository = logRepository;
        this.mapper = mapper;
    }

    @Transactional
    public Optional<StorySession> findActiveSession(long guildId, long userId, String campaignKey) {
        return sessionRepository.findFirstByGuildIdAndUserIdAndCampaignKeyAndStatusOrderByCreatedAtDesc(
                guildId,
                userId,
                campaignKey,
                "ACTIVE"
        ).map(this::toDomain);
    }

    @Transactional
    public Optional<StorySession> findSessionForUpdate(long sessionId) {
        return sessionRepository.findByIdForUpdate(sessionId).map(this::toDomain);
    }

    @Transactional
    public StorySession createSession(StorySession session) {
        StorySessionEntity entity = toEntity(session);
        StorySessionEntity saved = sessionRepository.save(entity);
        session.setId(saved.getId());
        return session;
    }

    @Transactional
    public void updateSession(StorySession session) {
        StorySessionEntity entity = toEntity(session);
        sessionRepository.save(entity);
    }

    @Transactional
    public void expireSession(long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(entity -> {
            entity.setStatus("EXPIRED");
            sessionRepository.save(entity);
        });
    }

    @Transactional
    public void insertLog(long sessionId, int step, String nodeKey, String choiceKey, boolean success,
                          String deltaJson, String outcomeText) {
        StoryLogEntity log = StoryLogEntity.builder()
                .sessionId(sessionId)
                .step(step)
                .nodeKey(nodeKey)
                .choiceKey(choiceKey)
                .success(success)
                .deltaJson(deltaJson)
                .outcomeText(outcomeText)
                .createdAt(Instant.now())
                .build();
        logRepository.save(log);
    }

    @Transactional
    public Optional<StoryCooldown> findCooldown(long guildId, long userId, String campaignKey) {
        return cooldownRepository.findByGuildIdAndUserIdAndCampaignKey(guildId, userId, campaignKey)
                .map(this::toDomainCooldown);
    }

    @Transactional
    public StoryCooldown upsertCooldown(StoryCooldown cooldown) {
        StoryCooldownEntity entity = toEntity(cooldown);
        StoryCooldownEntity saved = cooldownRepository.save(entity);
        cooldown.setId(saved.getId());
        return cooldown;
    }

    private StorySession toDomain(StorySessionEntity entity) {
        return StorySession.builder()
                .id(entity.getId())
                .guildId(entity.getGuildId())
                .userId(entity.getUserId())
                .channelId(entity.getChannelId())
                .campaignKey(entity.getCampaignKey())
                .status(entity.getStatus())
                .nodeKey(entity.getNodeKey())
                .step(entity.getStep())
                .rngSeed(entity.getRngSeed())
                .stats(readJson(entity.getStatsJson(), MAP_INT, Collections.emptyMap()))
                .flags(readJson(entity.getFlagsJson(), SET_STRING, Collections.emptySet()))
                .inventory(readJson(entity.getInventoryJson(), MAP_INT, Collections.emptyMap()))
                .earnedTemp(entity.getEarnedTemp())
                .lastOutcomeText(entity.getLastOutcomeText())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .lastActionAt(entity.getLastActionAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }

    private StorySessionEntity toEntity(StorySession session) {
        return StorySessionEntity.builder()
                .id(session.getId())
                .guildId(session.getGuildId())
                .userId(session.getUserId())
                .channelId(session.getChannelId())
                .campaignKey(session.getCampaignKey())
                .status(session.getStatus())
                .nodeKey(session.getNodeKey())
                .step(session.getStep())
                .rngSeed(session.getRngSeed())
                .statsJson(writeJson(session.getStats()))
                .flagsJson(writeJson(session.getFlags()))
                .inventoryJson(writeJson(session.getInventory()))
                .earnedTemp(session.getEarnedTemp())
                .lastOutcomeText(session.getLastOutcomeText())
                .version(session.getVersion())
                .createdAt(session.getCreatedAt())
                .lastActionAt(session.getLastActionAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    private StoryCooldown toDomainCooldown(StoryCooldownEntity entity) {
        return StoryCooldown.builder()
                .id(entity.getId())
                .guildId(entity.getGuildId())
                .userId(entity.getUserId())
                .campaignKey(entity.getCampaignKey())
                .lastFinishedAt(entity.getLastFinishedAt())
                .dailyEarned(entity.getDailyEarned())
                .dailyDate(entity.getDailyDate())
                .build();
    }

    private StoryCooldownEntity toEntity(StoryCooldown cooldown) {
        return StoryCooldownEntity.builder()
                .id(cooldown.getId())
                .guildId(cooldown.getGuildId())
                .userId(cooldown.getUserId())
                .campaignKey(cooldown.getCampaignKey())
                .lastFinishedAt(cooldown.getLastFinishedAt())
                .dailyEarned(cooldown.getDailyEarned())
                .dailyDate(cooldown.getDailyDate())
                .build();
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
