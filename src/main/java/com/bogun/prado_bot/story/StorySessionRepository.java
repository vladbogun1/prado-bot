package com.bogun.prado_bot.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class StorySessionRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final SimpleJdbcInsert sessionInsert;
    private final SimpleJdbcInsert cooldownInsert;

    private static final TypeReference<Map<String, Integer>> MAP_INT = new TypeReference<>() {};
    private static final TypeReference<Set<String>> SET_STRING = new TypeReference<>() {};

    public StorySessionRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.sessionInsert = new SimpleJdbcInsert(jdbc.getJdbcTemplate())
                .withTableName("story_session")
                .usingGeneratedKeyColumns("id");
        this.cooldownInsert = new SimpleJdbcInsert(jdbc.getJdbcTemplate())
                .withTableName("story_cooldown")
                .usingGeneratedKeyColumns("id");
    }

    public Optional<StorySession> findActiveSession(long guildId, long userId, String campaignKey) {
        var sql = """
                SELECT * FROM story_session
                WHERE guild_id = :guildId AND user_id = :userId
                  AND campaign_key = :campaignKey AND status = 'ACTIVE'
                ORDER BY created_at DESC
                LIMIT 1
                """;
        var params = Map.of("guildId", guildId, "userId", userId, "campaignKey", campaignKey);
        var rows = jdbc.query(sql, params, (rs, rowNum) -> mapSession(rs.getLong("id"), rs));
        return rows.stream().findFirst();
    }

    public Optional<StorySession> findSessionForUpdate(long sessionId) {
        var sql = """
                SELECT * FROM story_session
                WHERE id = :id
                FOR UPDATE
                """;
        var params = Map.of("id", sessionId);
        var rows = jdbc.query(sql, params, (rs, rowNum) -> mapSession(rs.getLong("id"), rs));
        return rows.stream().findFirst();
    }

    public StorySession createSession(StorySession session) {
        var params = new MapSqlParameterSource()
                .addValue("guild_id", session.getGuildId())
                .addValue("user_id", session.getUserId())
                .addValue("channel_id", session.getChannelId())
                .addValue("campaign_key", session.getCampaignKey())
                .addValue("status", session.getStatus())
                .addValue("node_key", session.getNodeKey())
                .addValue("step", session.getStep())
                .addValue("rng_seed", session.getRngSeed())
                .addValue("stats_json", writeJson(session.getStats()))
                .addValue("flags_json", writeJson(session.getFlags()))
                .addValue("inventory_json", writeJson(session.getInventory()))
                .addValue("earned_temp", session.getEarnedTemp())
                .addValue("last_outcome_text", session.getLastOutcomeText())
                .addValue("version", session.getVersion())
                .addValue("created_at", Timestamp.from(session.getCreatedAt()))
                .addValue("last_action_at", Timestamp.from(session.getLastActionAt()))
                .addValue("expires_at", Timestamp.from(session.getExpiresAt()));
        Number id = sessionInsert.executeAndReturnKey(params);
        session.setId(id.longValue());
        return session;
    }

    public void updateSession(StorySession session) {
        var sql = """
                UPDATE story_session
                SET status = :status,
                    node_key = :nodeKey,
                    step = :step,
                    stats_json = :statsJson,
                    flags_json = :flagsJson,
                    inventory_json = :inventoryJson,
                    earned_temp = :earnedTemp,
                    last_outcome_text = :lastOutcomeText,
                    version = :version,
                    last_action_at = :lastActionAt,
                    expires_at = :expiresAt
                WHERE id = :id
                """;
        var params = Map.of(
                "id", session.getId(),
                "status", session.getStatus(),
                "nodeKey", session.getNodeKey(),
                "step", session.getStep(),
                "statsJson", writeJson(session.getStats()),
                "flagsJson", writeJson(session.getFlags()),
                "inventoryJson", writeJson(session.getInventory()),
                "earnedTemp", session.getEarnedTemp(),
                "lastOutcomeText", session.getLastOutcomeText(),
                "version", session.getVersion(),
                "lastActionAt", Timestamp.from(session.getLastActionAt()),
                "expiresAt", Timestamp.from(session.getExpiresAt())
        );
        jdbc.update(sql, params);
    }

    public void expireSession(long sessionId) {
        jdbc.update("UPDATE story_session SET status = 'EXPIRED' WHERE id = :id",
                Map.of("id", sessionId));
    }

    public void insertLog(long sessionId, int step, String nodeKey, String choiceKey, boolean success,
                          String deltaJson, String outcomeText) {
        var sql = """
                INSERT INTO story_log (session_id, step, node_key, choice_key, success, delta_json, outcome_text)
                VALUES (:sessionId, :step, :nodeKey, :choiceKey, :success, :deltaJson, :outcomeText)
                """;
        var params = Map.of(
                "sessionId", sessionId,
                "step", step,
                "nodeKey", nodeKey,
                "choiceKey", choiceKey,
                "success", success,
                "deltaJson", deltaJson,
                "outcomeText", outcomeText
        );
        jdbc.update(sql, params);
    }

    public Optional<StoryCooldown> findCooldown(long guildId, long userId, String campaignKey) {
        var sql = """
                SELECT * FROM story_cooldown
                WHERE guild_id = :guildId AND user_id = :userId AND campaign_key = :campaignKey
                """;
        var params = Map.of("guildId", guildId, "userId", userId, "campaignKey", campaignKey);
        var rows = jdbc.query(sql, params, (rs, rowNum) -> {
            StoryCooldown cd = new StoryCooldown();
            cd.setId(rs.getLong("id"));
            cd.setGuildId(rs.getLong("guild_id"));
            cd.setUserId(rs.getLong("user_id"));
            cd.setCampaignKey(rs.getString("campaign_key"));
            Timestamp ts = rs.getTimestamp("last_finished_at");
            cd.setLastFinishedAt(ts != null ? ts.toInstant() : null);
            cd.setDailyEarned(rs.getInt("daily_earned"));
            cd.setDailyDate(rs.getDate("daily_date").toLocalDate());
            return cd;
        });
        return rows.stream().findFirst();
    }

    public StoryCooldown upsertCooldown(StoryCooldown cooldown) {
        if (cooldown.getId() == null) {
            var params = new MapSqlParameterSource()
                    .addValue("guild_id", cooldown.getGuildId())
                    .addValue("user_id", cooldown.getUserId())
                    .addValue("campaign_key", cooldown.getCampaignKey())
                    .addValue("last_finished_at", cooldown.getLastFinishedAt() == null ? null : Timestamp.from(cooldown.getLastFinishedAt()))
                    .addValue("daily_earned", cooldown.getDailyEarned())
                    .addValue("daily_date", java.sql.Date.valueOf(cooldown.getDailyDate()));
            Number id = cooldownInsert.executeAndReturnKey(params);
            cooldown.setId(id.longValue());
            return cooldown;
        }
        var sql = """
                UPDATE story_cooldown
                SET last_finished_at = :lastFinishedAt,
                    daily_earned = :dailyEarned,
                    daily_date = :dailyDate
                WHERE id = :id
                """;
        var params = Map.of(
                "id", cooldown.getId(),
                "lastFinishedAt", cooldown.getLastFinishedAt() == null ? null : Timestamp.from(cooldown.getLastFinishedAt()),
                "dailyEarned", cooldown.getDailyEarned(),
                "dailyDate", java.sql.Date.valueOf(cooldown.getDailyDate())
        );
        jdbc.update(sql, params);
        return cooldown;
    }

    private StorySession mapSession(long id, java.sql.ResultSet rs) throws java.sql.SQLException {
        StorySession session = new StorySession();
        session.setId(id);
        session.setGuildId(rs.getLong("guild_id"));
        session.setUserId(rs.getLong("user_id"));
        session.setChannelId(rs.getLong("channel_id"));
        session.setCampaignKey(rs.getString("campaign_key"));
        session.setStatus(rs.getString("status"));
        session.setNodeKey(rs.getString("node_key"));
        session.setStep(rs.getInt("step"));
        session.setRngSeed(rs.getLong("rng_seed"));
        session.setStats(readJson(rs.getString("stats_json"), MAP_INT, Collections.emptyMap()));
        session.setFlags(readJson(rs.getString("flags_json"), SET_STRING, Collections.emptySet()));
        session.setInventory(readJson(rs.getString("inventory_json"), MAP_INT, Collections.emptyMap()));
        session.setEarnedTemp(rs.getInt("earned_temp"));
        session.setLastOutcomeText(rs.getString("last_outcome_text"));
        session.setVersion(rs.getInt("version"));
        session.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        session.setLastActionAt(rs.getTimestamp("last_action_at").toInstant());
        session.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        return session;
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
