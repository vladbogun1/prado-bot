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
        StoryLogEntity log = new StoryLogEntity();
        log.setSessionId(sessionId);
        log.setStep(step);
        log.setNodeKey(nodeKey);
        log.setChoiceKey(choiceKey);
        log.setSuccess(success);
        log.setDeltaJson(deltaJson);
        log.setOutcomeText(outcomeText);
        log.setCreatedAt(Instant.now());
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
        StorySession session = new StorySession();
        session.setId(entity.getId());
        session.setGuildId(entity.getGuildId());
        session.setUserId(entity.getUserId());
        session.setChannelId(entity.getChannelId());
        session.setCampaignKey(entity.getCampaignKey());
        session.setStatus(entity.getStatus());
        session.setNodeKey(entity.getNodeKey());
        session.setStep(entity.getStep());
        session.setRngSeed(entity.getRngSeed());
        session.setStats(readJson(entity.getStatsJson(), MAP_INT, Collections.emptyMap()));
        session.setFlags(readJson(entity.getFlagsJson(), SET_STRING, Collections.emptySet()));
        session.setInventory(readJson(entity.getInventoryJson(), MAP_INT, Collections.emptyMap()));
        session.setEarnedTemp(entity.getEarnedTemp());
        session.setLastOutcomeText(entity.getLastOutcomeText());
        session.setVersion(entity.getVersion());
        session.setCreatedAt(entity.getCreatedAt());
        session.setLastActionAt(entity.getLastActionAt());
        session.setExpiresAt(entity.getExpiresAt());
        return session;
    }

    private StorySessionEntity toEntity(StorySession session) {
        StorySessionEntity entity = new StorySessionEntity();
        entity.setId(session.getId());
        entity.setGuildId(session.getGuildId());
        entity.setUserId(session.getUserId());
        entity.setChannelId(session.getChannelId());
        entity.setCampaignKey(session.getCampaignKey());
        entity.setStatus(session.getStatus());
        entity.setNodeKey(session.getNodeKey());
        entity.setStep(session.getStep());
        entity.setRngSeed(session.getRngSeed());
        entity.setStatsJson(writeJson(session.getStats()));
        entity.setFlagsJson(writeJson(session.getFlags()));
        entity.setInventoryJson(writeJson(session.getInventory()));
        entity.setEarnedTemp(session.getEarnedTemp());
        entity.setLastOutcomeText(session.getLastOutcomeText());
        entity.setVersion(session.getVersion());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setLastActionAt(session.getLastActionAt());
        entity.setExpiresAt(session.getExpiresAt());
        return entity;
    }

    private StoryCooldown toDomainCooldown(StoryCooldownEntity entity) {
        StoryCooldown cooldown = new StoryCooldown();
        cooldown.setId(entity.getId());
        cooldown.setGuildId(entity.getGuildId());
        cooldown.setUserId(entity.getUserId());
        cooldown.setCampaignKey(entity.getCampaignKey());
        cooldown.setLastFinishedAt(entity.getLastFinishedAt());
        cooldown.setDailyEarned(entity.getDailyEarned());
        cooldown.setDailyDate(entity.getDailyDate());
        return cooldown;
    }

    private StoryCooldownEntity toEntity(StoryCooldown cooldown) {
        StoryCooldownEntity entity = new StoryCooldownEntity();
        entity.setId(cooldown.getId());
        entity.setGuildId(cooldown.getGuildId());
        entity.setUserId(cooldown.getUserId());
        entity.setCampaignKey(cooldown.getCampaignKey());
        entity.setLastFinishedAt(cooldown.getLastFinishedAt());
        entity.setDailyEarned(cooldown.getDailyEarned());
        entity.setDailyDate(cooldown.getDailyDate());
        return entity;
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
