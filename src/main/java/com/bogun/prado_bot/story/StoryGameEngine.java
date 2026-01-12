package com.bogun.prado_bot.story;

import com.bogun.prado_bot.config.StoryGameProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

@Service
public class StoryGameEngine {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_DEAD = "DEAD";

    private static final TypeReference<List<Map<String, Object>>> EFFECT_LIST = new TypeReference<>() {};

    private final StoryRepository storyRepository;
    private final StorySessionStore sessionStore;
    private final EconomyAdapter economyAdapter;
    private final StoryGameProperties properties;
    private final ObjectMapper mapper;

    public StoryGameEngine(StoryRepository storyRepository,
                           StorySessionStore sessionStore,
                           EconomyAdapter economyAdapter,
                           StoryGameProperties properties,
                           ObjectMapper mapper) {
        this.storyRepository = storyRepository;
        this.sessionStore = sessionStore;
        this.economyAdapter = economyAdapter;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Transactional
    public StoryRender startOrResume(long guildId, long userId, long channelId, String username, String memberName) {
        String campaignKey = properties.getDefaultCampaignKey();
        storyRepository.validateCampaign(campaignKey);

        Optional<StorySession> existing = sessionStore.findActiveSession(guildId, userId, campaignKey);
        if (existing.isPresent()) {
            StorySession session = existing.get();
            if (Instant.now().isAfter(session.getExpiresAt())) {
                sessionStore.expireSession(session.getId());
            } else {
                return renderSession(session);
            }
        }

        StoryCooldown cooldown = sessionStore.findCooldown(guildId, userId, campaignKey)
                .orElse(null);
        if (cooldown != null && cooldown.getLastFinishedAt() != null) {
            Duration since = Duration.between(cooldown.getLastFinishedAt(), Instant.now());
            if (since.compareTo(Duration.ofMinutes(properties.getCooldownMinutes())) < 0) {
                long waitMinutes = Math.max(1, properties.getCooldownMinutes() - since.toMinutes());
                throw new StoryGameException("Подожди ещё " + waitMinutes + " мин. до следующего запуска.");
            }
        }

        StoryCampaign campaign = storyRepository.findCampaign(campaignKey)
                .orElseThrow(() -> new StoryGameException("Кампания не найдена."));
        StoryNode startNode = storyRepository.findNode(campaignKey, campaign.startNodeKey())
                .orElseThrow(() -> new StoryGameException("Стартовый узел не найден."));

        StorySession session = new StorySession();
        session.setGuildId(guildId);
        session.setUserId(userId);
        session.setChannelId(channelId);
        session.setCampaignKey(campaignKey);
        session.setStatus(STATUS_ACTIVE);
        session.setNodeKey(startNode.nodeKey());
        session.setStep(0);
        session.setRngSeed(new SplittableRandom().nextLong());
        session.setStats(defaultStats());
        session.setFlags(Collections.emptySet());
        session.setInventory(Collections.emptyMap());
        session.setEarnedTemp(0);
        session.setLastOutcomeText("История начинается.");
        session.setVersion(0);
        Instant now = Instant.now();
        session.setCreatedAt(now);
        session.setLastActionAt(now);
        session.setExpiresAt(now.plus(Duration.ofMinutes(properties.getSessionTtlMinutes())));

        applyAutoEffects(session, startNode, "start");
        sessionStore.createSession(session);

        return renderSession(session);
    }

    @Transactional
    public StoryRender applyChoice(long sessionId, String choiceKey, int version,
                                   long actorUserId, String username, String memberName) {
        StorySession session = sessionStore.findSessionForUpdate(sessionId)
                .orElseThrow(() -> new StoryGameException("Сессия не найдена."));
        if (session.getUserId() != actorUserId) {
            throw new StoryGameException("Эта сессия принадлежит другому игроку.");
        }
        if (!STATUS_ACTIVE.equals(session.getStatus())) {
            throw new StoryGameException("Сессия уже завершена.");
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            sessionStore.expireSession(session.getId());
            throw new StoryGameException("Сессия истекла. Запусти игру снова.");
        }
        if (session.getVersion() != version) {
            throw new StoryGameException("Эта кнопка уже обработана.");
        }

        StoryNode currentNode = storyRepository.findNode(session.getCampaignKey(), session.getNodeKey())
                .orElseThrow(() -> new StoryGameException("Текущий узел не найден."));
        List<StoryChoice> choices = storyRepository.findChoices(session.getCampaignKey(), session.getNodeKey());
        StoryChoice choice = choices.stream()
                .filter(c -> c.choiceKey().equals(choiceKey))
                .findFirst()
                .orElseThrow(() -> new StoryGameException("Выбор не найден."));

        if (!conditionsMet(choice.conditionsJson(), session)) {
            throw new StoryGameException("Условия для выбора не выполнены.");
        }

        boolean success = rollSuccess(choice.checkJson(), session, choice.choiceKey());
        List<Map<String, Object>> applied = new ArrayList<>();
        String outcomeText = success ? choice.successText() : choice.failText();
        String nextNodeKey = success ? choice.successNodeKey() : choice.failNodeKey();

        applied.addAll(applyEffects(session, success ? choice.successEffectsJson() : choice.failEffectsJson()));

        int nextStep = session.getStep() + 1;
        session.setStep(nextStep);
        session.setNodeKey(nextNodeKey);
        session.setVersion(session.getVersion() + 1);
        session.setLastOutcomeText(outcomeText);
        Instant now = Instant.now();
        session.setLastActionAt(now);
        session.setExpiresAt(now.plus(Duration.ofMinutes(properties.getSessionTtlMinutes())));

        StoryNode nextNode = storyRepository.findNode(session.getCampaignKey(), nextNodeKey)
                .orElseThrow(() -> new StoryGameException("Следующий узел не найден."));
        applied.addAll(applyAutoEffects(session, nextNode, "node"));

        int payout = 0;
        if (nextNode.terminal()) {
            payout = applyPayout(session, nextNode, username, memberName);
            session.setStatus(statusFromTerminal(nextNode.terminalType()));
            session.setExpiresAt(now);
            session.setLastOutcomeText(outcomeText + "\n\nВыплата: " + payout + " монет.");
        }

        String deltaJson = writeJson(buildDelta(applied, payout));
        sessionStore.insertLog(session.getId(), nextStep, currentNode.nodeKey(), choice.choiceKey(), success, deltaJson, outcomeText);
        sessionStore.updateSession(session);

        return renderSession(session);
    }

    private StoryRender renderSession(StorySession session) {
        StoryNode node = storyRepository.findNode(session.getCampaignKey(), session.getNodeKey())
                .orElseThrow(() -> new StoryGameException("Узел не найден."));
        List<StoryChoice> choices = storyRepository.findChoices(session.getCampaignKey(), session.getNodeKey());

        String variantText = selectVariant(node.variantsJson(), session, node.nodeKey());
        variantText = applyPlaceholders(variantText, session);

        StringBuilder description = new StringBuilder();
        if (session.getLastOutcomeText() != null && !session.getLastOutcomeText().isBlank()) {
            description.append(session.getLastOutcomeText()).append("\n\n");
        }
        description.append(variantText);

        MessageEmbed embed = new EmbedBuilder()
                .setTitle(node.title())
                .setDescription(description.toString())
                .addField("HUD", hudLine(session), false)
                .build();

        List<Button> buttons = new ArrayList<>();
        if (STATUS_ACTIVE.equals(session.getStatus()) && !node.terminal()) {
            for (StoryChoice choice : choices) {
                if (conditionsMet(choice.conditionsJson(), session)) {
                    buttons.add(Button.primary(buttonId(session.getId(), choice.choiceKey(), session.getVersion()), choice.label()));
                }
            }
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) {
            rows.add(ActionRow.of(buttons.subList(i, Math.min(i + 5, buttons.size()))));
        }

        return new StoryRender(embed, rows);
    }

    private String hudLine(StorySession session) {
        var stats = session.getStats();
        return "❤️ " + stats.getOrDefault("hp", 0)
                + " | 💵 $" + stats.getOrDefault("cash", 0)
                + " | 🚨 " + stats.getOrDefault("wanted", 0)
                + " | 🪙 " + session.getEarnedTemp();
    }

    private String applyPlaceholders(String text, StorySession session) {
        Map<String, Integer> stats = session.getStats();
        return text
                .replace("{hp}", String.valueOf(stats.getOrDefault("hp", 0)))
                .replace("{cash}", String.valueOf(stats.getOrDefault("cash", 0)))
                .replace("{wanted}", String.valueOf(stats.getOrDefault("wanted", 0)))
                .replace("{street}", String.valueOf(stats.getOrDefault("street", 0)))
                .replace("{charisma}", String.valueOf(stats.getOrDefault("charisma", 0)))
                .replace("{discipline}", String.valueOf(stats.getOrDefault("discipline", 0)));
    }

    private String selectVariant(String variantsJson, StorySession session, String nodeKey) {
        List<String> variants = readList(variantsJson);
        if (variants.isEmpty()) {
            return "";
        }
        int idx = Math.abs(Objects.hash(session.getRngSeed(), nodeKey)) % variants.size();
        return variants.get(idx);
    }

    private boolean conditionsMet(String conditionsJson, StorySession session) {
        JsonNode node = readJsonNode(conditionsJson);
        if (node == null || node.isEmpty()) {
            return true;
        }
        Map<String, Integer> stats = session.getStats();
        if (node.has("min_cash") && stats.getOrDefault("cash", 0) < node.get("min_cash").asInt()) {
            return false;
        }
        if (node.has("min_hp") && stats.getOrDefault("hp", 0) < node.get("min_hp").asInt()) {
            return false;
        }
        if (node.has("max_wanted") && stats.getOrDefault("wanted", 0) > node.get("max_wanted").asInt()) {
            return false;
        }
        if (node.has("required_items")) {
            for (JsonNode itemNode : node.get("required_items")) {
                String item = itemNode.asText();
                if (session.getInventory().getOrDefault(item, 0) <= 0) {
                    return false;
                }
            }
        }
        if (node.has("required_flags")) {
            for (JsonNode flagNode : node.get("required_flags")) {
                if (!session.getFlags().contains(flagNode.asText())) {
                    return false;
                }
            }
        }
        if (node.has("forbidden_flags")) {
            for (JsonNode flagNode : node.get("forbidden_flags")) {
                if (session.getFlags().contains(flagNode.asText())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean rollSuccess(String checkJson, StorySession session, String choiceKey) {
        JsonNode node = readJsonNode(checkJson);
        if (node == null || node.isEmpty()) {
            return true;
        }
        if (node.has("type") && "none".equals(node.get("type").asText())) {
            return true;
        }
        String stat = node.path("stat").asText(null);
        double base = node.path("base_success").asDouble(0.5);
        double statScale = node.path("stat_scale").asDouble(0.03);
        double wantedScale = node.path("wanted_scale").asDouble(0.0);
        Map<String, Integer> stats = session.getStats();
        int statValue = stats.getOrDefault(stat, 0);
        int wanted = stats.getOrDefault("wanted", 0);

        double bonus = 0.0;
        if (node.has("item_bonuses")) {
            var fields = node.get("item_bonuses").fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (session.getInventory().getOrDefault(entry.getKey(), 0) > 0) {
                    bonus += entry.getValue().asDouble(0.0);
                }
            }
        }

        double p = base + statValue * statScale - wanted * wantedScale + bonus;
        p = clamp(p, 0.05, 0.95);

        SplittableRandom rng = rng(session.getRngSeed(), session.getStep(), choiceKey);
        double roll = rng.nextDouble();
        return roll < p;
    }

    private List<Map<String, Object>> applyEffects(StorySession session, String effectsJson) {
        List<Map<String, Object>> effects = readEffects(effectsJson);
        if (effects.isEmpty()) {
            return Collections.emptyList();
        }
        for (Map<String, Object> effect : effects) {
            String op = String.valueOf(effect.get("op"));
            switch (op) {
                case "stat.add" -> applyStatAdd(session, effect);
                case "inventory.add" -> applyInventoryAdd(session, effect);
                case "flag.add" -> applyFlagAdd(session, effect);
                case "earned.add" -> applyEarnedAdd(session, effect);
                default -> {
                }
            }
        }
        return effects;
    }

    private List<Map<String, Object>> applyAutoEffects(StorySession session, StoryNode node, String salt) {
        JsonNode autoNode = readJsonNode(node.autoEffectsJson());
        if (autoNode == null || autoNode.isEmpty()) {
            return Collections.emptyList();
        }
        double chance = autoNode.path("chance").asDouble(0.0);
        if (chance <= 0.0) {
            return Collections.emptyList();
        }
        SplittableRandom rng = rng(session.getRngSeed(), session.getStep(), node.nodeKey() + ":" + salt);
        if (rng.nextDouble() > chance) {
            return Collections.emptyList();
        }
        String effectsJson = autoNode.path("effects").toString();
        return applyEffects(session, effectsJson);
    }

    private int applyPayout(StorySession session, StoryNode node, String username, String memberName) {
        JsonNode reward = readJsonNode(node.rewardJson());
        JsonNode payoutNode = reward != null ? reward.path("payout") : null;
        if (payoutNode == null || payoutNode.isMissingNode()) {
            return 0;
        }
        String mode = payoutNode.path("mode").asText("keep_all");
        double losePercent = payoutNode.path("lose_percent").asDouble(0.0);
        int base = session.getEarnedTemp();
        int payout;
        switch (mode) {
            case "lose_percent" -> payout = (int) Math.floor(base * (1.0 - losePercent));
            case "lose_all" -> payout = 0;
            default -> payout = base;
        }

        JsonNode bonusNode = payoutNode.path("terminal_bonus");
        int min = bonusNode.path("min").asInt(0);
        int max = bonusNode.path("max").asInt(0);
        if (max >= min && max > 0) {
            SplittableRandom rng = rng(session.getRngSeed(), session.getStep(), node.nodeKey() + ":bonus");
            payout += rng.nextInt(max - min + 1) + min;
        }

        int capDaily = payoutNode.path("cap_daily").asInt(0);
        if (capDaily > 0) {
            payout = applyDailyCap(session, payout, capDaily);
        }

        if (payout > 0) {
            economyAdapter.addCoins(session.getGuildId(), session.getUserId(), username, memberName, payout);
        }
        return payout;
    }

    private int applyDailyCap(StorySession session, int payout, int capDaily) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        StoryCooldown cooldown = sessionStore.findCooldown(session.getGuildId(), session.getUserId(), session.getCampaignKey())
                .orElseGet(() -> {
                    StoryCooldown cd = new StoryCooldown();
                    cd.setGuildId(session.getGuildId());
                    cd.setUserId(session.getUserId());
                    cd.setCampaignKey(session.getCampaignKey());
                    cd.setDailyDate(today);
                    cd.setDailyEarned(0);
                    return cd;
                });

        if (!today.equals(cooldown.getDailyDate())) {
            cooldown.setDailyDate(today);
            cooldown.setDailyEarned(0);
        }

        int remaining = Math.max(0, capDaily - cooldown.getDailyEarned());
        int finalPayout = Math.min(payout, remaining);

        cooldown.setDailyEarned(cooldown.getDailyEarned() + finalPayout);
        cooldown.setLastFinishedAt(Instant.now());
        sessionStore.upsertCooldown(cooldown);
        return finalPayout;
    }

    private String statusFromTerminal(String terminalType) {
        return switch (terminalType) {
            case "SUCCESS" -> STATUS_SUCCESS;
            case "FAIL" -> STATUS_FAIL;
            case "DEAD" -> STATUS_DEAD;
            default -> STATUS_FAIL;
        };
    }

    private void applyStatAdd(StorySession session, Map<String, Object> effect) {
        String key = String.valueOf(effect.get("key"));
        int value = toInt(effect.get("value"));
        Map<String, Integer> stats = new HashMap<>(session.getStats());
        int current = stats.getOrDefault(key, 0);
        int next = current + value;
        if ("hp".equals(key)) {
            next = clamp(next, 0, 100);
        }
        if ("wanted".equals(key)) {
            next = clamp(next, 0, 100);
        }
        if (Set.of("street", "charisma", "discipline").contains(key)) {
            next = clamp(next, 0, 10);
        }
        stats.put(key, next);
        session.setStats(stats);
    }

    private void applyInventoryAdd(StorySession session, Map<String, Object> effect) {
        String item = String.valueOf(effect.get("item"));
        int count = toInt(effect.get("count"));
        Map<String, Integer> inventory = new HashMap<>(session.getInventory());
        int next = inventory.getOrDefault(item, 0) + count;
        if (next <= 0) {
            inventory.remove(item);
        } else {
            inventory.put(item, next);
        }
        session.setInventory(inventory);
    }

    private void applyFlagAdd(StorySession session, Map<String, Object> effect) {
        String flag = String.valueOf(effect.get("flag"));
        Set<String> flags = session.getFlags().isEmpty() ? new java.util.HashSet<>() : new java.util.HashSet<>(session.getFlags());
        flags.add(flag);
        session.setFlags(flags);
    }

    private void applyEarnedAdd(StorySession session, Map<String, Object> effect) {
        int value = toInt(effect.get("value"));
        session.setEarnedTemp(Math.max(0, session.getEarnedTemp() + value));
    }

    private Map<String, Object> buildDelta(List<Map<String, Object>> effects, int payout) {
        Map<String, Object> delta = new HashMap<>();
        delta.put("effects", effects);
        if (payout > 0) {
            delta.put("payout", payout);
        }
        return delta;
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> readEffects(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(json, EFFECT_LIST);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private JsonNode readJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private SplittableRandom rng(long seed, int step, String salt) {
        return new SplittableRandom(Objects.hash(seed, step, salt));
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Integer> defaultStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("hp", 100);
        stats.put("cash", 0);
        stats.put("wanted", 0);
        stats.put("street", 1);
        stats.put("charisma", 1);
        stats.put("discipline", 1);
        return stats;
    }

    private String buttonId(long sessionId, String choiceKey, int version) {
        return "prado_game:" + sessionId + ":" + choiceKey + ":" + version;
    }
}
