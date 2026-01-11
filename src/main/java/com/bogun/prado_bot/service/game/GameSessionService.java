package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.config.PradoGameProperties;
import com.bogun.prado_bot.domain.VoiceUser;
import com.bogun.prado_bot.domain.VoiceUser.VoiceUserId;
import com.bogun.prado_bot.domain.game.GameAction;
import com.bogun.prado_bot.domain.game.GameCooldown;
import com.bogun.prado_bot.domain.game.GameEvent;
import com.bogun.prado_bot.domain.game.GameEventLog;
import com.bogun.prado_bot.domain.game.GameItem;
import com.bogun.prado_bot.domain.game.GameLocation;
import com.bogun.prado_bot.domain.game.GameMissionType;
import com.bogun.prado_bot.domain.game.GameNodeTransition;
import com.bogun.prado_bot.domain.game.GameScene;
import com.bogun.prado_bot.domain.game.GameSession;
import com.bogun.prado_bot.domain.game.GameSessionStatus;
import com.bogun.prado_bot.repo.VoiceSessionRepository;
import com.bogun.prado_bot.repo.VoiceUserRepository;
import com.bogun.prado_bot.repo.game.GameActionRepository;
import com.bogun.prado_bot.repo.game.GameCooldownRepository;
import com.bogun.prado_bot.repo.game.GameEventLogRepository;
import com.bogun.prado_bot.repo.game.GameEventRepository;
import com.bogun.prado_bot.repo.game.GameItemRepository;
import com.bogun.prado_bot.repo.game.GameLocationRepository;
import com.bogun.prado_bot.repo.game.GameMissionTypeRepository;
import com.bogun.prado_bot.repo.game.GameNodeRepository;
import com.bogun.prado_bot.repo.game.GameNodeTransitionRepository;
import com.bogun.prado_bot.repo.game.GameSceneRepository;
import com.bogun.prado_bot.repo.game.GameSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameSessionService {

    public record GameView(
            MessageEmbed embed,
            List<ActionRow> components,
            String errorMessage,
            boolean ended,
            String publicSummary,
            Long publicChannelId
    ) {}

    private static final String BUTTON_PREFIX = "prado_game:";

    private final PradoGameProperties properties;
    private final ObjectMapper objectMapper;
    private final GameSessionRepository sessionRepository;
    private final GameMissionTypeRepository missionTypeRepository;
    private final GameLocationRepository locationRepository;
    private final GameSceneRepository sceneRepository;
    private final GameActionRepository actionRepository;
    private final GameEventRepository eventRepository;
    private final GameItemRepository itemRepository;
    private final GameCooldownRepository cooldownRepository;
    private final GameEventLogRepository eventLogRepository;
    private final GameNodeRepository nodeRepository;
    private final GameNodeTransitionRepository nodeTransitionRepository;
    private final VoiceUserRepository voiceUserRepository;
    private final VoiceSessionRepository voiceSessionRepository;

    @Value("${app.timezone:UTC}")
    private String appTimezone;

    public String buttonPrefix() {
        return BUTTON_PREFIX;
    }

    @Transactional
    public GameView startSession(long guildId, long userId, long channelId, String username, String memberName) {
        Optional<GameSession> existing = sessionRepository.findFirstByGuildIdAndUserIdAndStatus(guildId, userId, GameSessionStatus.ACTIVE);
        if (existing.isPresent()) {
            return render(existing.get(), "Сессия уже активна.", 0);
        }

        Instant now = Instant.now();
        GameCooldown cooldown = cooldownRepository.findByGuildIdAndUserId(guildId, userId)
                .orElseGet(() -> createCooldown(guildId, userId, now));
        if (cooldown.getLastGameFinishedAt() != null) {
            Instant next = cooldown.getLastGameFinishedAt().plusSeconds(properties.getCooldownMinutes() * 60L);
            if (now.isBefore(next)) {
                long seconds = next.getEpochSecond() - now.getEpochSecond();
                return new GameView(null, List.of(), "Подожди ещё " + (seconds / 60 + 1) + " мин, откат не закончился.", false, null, null);
            }
        }

        upsertUser(guildId, userId, username, memberName);

        if (missionTypeRepository.count() == 0 || locationRepository.count() == 0 || nodeRepository.count() == 0) {
            return new GameView(null, List.of(), "Игровой контент ещё не загружен. Проверь миграцию данных.", false, null, null);
        }

        GameMissionType missionType = pickRandom(missionTypeRepository.findAll(), seed(userId));
        GameLocation location = pickRandom(locationRepository.findAll(), seed(userId + 7));
        String nodeKey = nodeRepository.findFirstByMissionTypeKeyAndStartIsTrue(missionType.getKey())
                .orElseGet(() -> nodeRepository.findAllByMissionTypeKey(missionType.getKey()).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("content missing")))
                .getKey();

        Map<String, Integer> stats = new HashMap<>();
        Random rng = new Random(seed(userId));
        stats.put("drive", 2 + rng.nextInt(4));
        stats.put("stealth", 2 + rng.nextInt(4));
        stats.put("talk", 2 + rng.nextInt(4));

        Map<String, Integer> inventory = new HashMap<>();
        List<String> flags = new ArrayList<>();
        List<String> checkpoints = new ArrayList<>();

        GameSession session = new GameSession();
        session.setGuildId(guildId);
        session.setUserId(userId);
        session.setChannelId(channelId);
        session.setStatus(GameSessionStatus.ACTIVE);
        session.setMissionTypeKey(missionType.getKey());
        session.setLocationKey(location.getKey());
        session.setNodeKey(nodeKey);
        session.setProgress(0);
        session.setHeat(0);
        session.setTailLevel(0);
        session.setStep(1);
        session.setRngSeed(seed(userId));
        session.setStatsJson(writeJson(stats));
        session.setFlagsJson(writeJson(flags));
        session.setCheckpointsJson(writeJson(checkpoints));
        session.setInventoryJson(writeJson(inventory));
        session.setLastSceneText(sceneTextFor(missionType.getKey(), location.getKey(), nodeKey, 0, 0, rng));
        session.setLastOutcomeText("Ты в игре. Первый ход — твой.");
        session.setLastDeltaCoins(0);
        session.setEarnedTemp(0);
        List<String> actions = chooseActions(session, stats, inventory, flags, checkpoints, rng);
        session.setAvailableActionsJson(writeJson(actions));
        session.setVersion(1);
        session.setCreatedAt(now);
        session.setLastActionAt(now);
        session.setExpiresAt(now.plusSeconds(properties.getSessionTimeoutMinutes() * 60L));

        sessionRepository.save(session);
        return render(session, null, 0);
    }

    public GameView status(long guildId, long userId) {
        Optional<GameSession> existing = sessionRepository.findFirstByGuildIdAndUserIdAndStatus(guildId, userId, GameSessionStatus.ACTIVE);
        return existing.map(session -> render(session, null, 0))
                .orElseGet(() -> new GameView(null, List.of(), "Активной сессии нет. Запусти /prado_game start.", false, null, null));
    }

    public Optional<GameSession> findActiveSession(long guildId, long userId) {
        return sessionRepository.findFirstByGuildIdAndUserIdAndStatus(guildId, userId, GameSessionStatus.ACTIVE);
    }

    @Transactional
    public GameView quitActiveSession(long guildId, long userId) {
        Optional<GameSession> session = findActiveSession(guildId, userId);
        return session.map(value -> quitSession(value.getId(), userId))
                .orElseGet(() -> new GameView(null, List.of(), "Активной сессии нет.", false, null, null));
    }

    @Transactional
    public GameView quitSession(long sessionId, long userId) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found"));
        if (!Objects.equals(session.getUserId(), userId)) {
            return new GameView(null, List.of(), "Это не твоя сессия.", false, null, null);
        }
        if (session.getStatus() != GameSessionStatus.ACTIVE) {
            return new GameView(null, List.of(), "Сессия уже завершена.", true, buildSummary(session), session.getChannelId());
        }
        session.setStatus(GameSessionStatus.QUIT);
        session.setLastOutcomeText("Ты свернул дело. Миссия отменена.");
        session.setLastActionAt(Instant.now());
        sessionRepository.save(session);
        updateCooldown(session.getGuildId(), session.getUserId(), Instant.now());
        return withPublicSummary(render(session, null, 0), buildSummary(session));
    }

    @Transactional
    public GameView applyAction(long sessionId, long userId, String actionKey, int expectedVersion, String username, String memberName) {
        GameSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found"));

        if (!Objects.equals(session.getUserId(), userId)) {
            return new GameView(null, List.of(), "Это не твоя сессия.", false, null, null);
        }
        if (session.getStatus() != GameSessionStatus.ACTIVE) {
            return new GameView(null, List.of(), "Сессия уже завершена.", true, buildSummary(session), session.getChannelId());
        }
        if (session.getVersion() != expectedVersion) {
            return render(session, "Кнопка уже использована.", 0);
        }

        Instant now = Instant.now();
        if (now.isAfter(session.getExpiresAt())) {
            session.setStatus(GameSessionStatus.EXPIRED);
            session.setLastOutcomeText("Сессия истекла. Попробуй снова.");
            session.setLastActionAt(now);
            sessionRepository.save(session);
            updateCooldown(session.getGuildId(), session.getUserId(), now);
            return withPublicSummary(render(session, null, 0), buildSummary(session));
        }

        upsertUser(session.getGuildId(), session.getUserId(), username, memberName);

        Map<String, Integer> stats = readMap(session.getStatsJson());
        Map<String, Integer> inventory = readMap(session.getInventoryJson());
        List<String> flags = readList(session.getFlagsJson());
        List<String> checkpoints = readList(session.getCheckpointsJson());
        List<String> available = readList(session.getAvailableActionsJson());

        if ("inspect".equals(actionKey)) {
            Random rng = new Random(session.getRngSeed() + session.getStep());
            session.setLastSceneText(sceneTextFor(session.getMissionTypeKey(), session.getLocationKey(),
                    session.getNodeKey(), session.getProgress(), session.getHeat(), rng));
            session.setLastOutcomeText("Ты осмотрелся и нашёл свежие детали.");
            session.setLastDeltaCoins(0);
            session.setVersion(session.getVersion() + 1);
            session.setLastActionAt(now);
            sessionRepository.save(session);
            return render(session, null, 0);
        }

        if ("inventory".equals(actionKey)) {
            session.setLastOutcomeText("Твой инвентарь под рукой.\nСтаты: "
                    + "drive " + stats.getOrDefault("drive", 0)
                    + ", stealth " + stats.getOrDefault("stealth", 0)
                    + ", talk " + stats.getOrDefault("talk", 0));
            session.setLastDeltaCoins(0);
            session.setVersion(session.getVersion() + 1);
            session.setLastActionAt(now);
            sessionRepository.save(session);
            return render(session, null, 0);
        }

        if ("quit".equals(actionKey)) {
            session.setStatus(GameSessionStatus.QUIT);
            session.setLastOutcomeText("Ты решил выйти из дела.");
            session.setLastDeltaCoins(0);
            session.setLastActionAt(now);
            sessionRepository.save(session);
            updateCooldown(session.getGuildId(), session.getUserId(), now);
            return withPublicSummary(render(session, null, 0), buildSummary(session));
        }

        if (!available.contains(actionKey)) {
            return new GameView(null, List.of(), "Это действие больше недоступно.", false, null, null);
        }

        GameAction action = actionRepository.findByKey(actionKey)
                .orElseThrow(() -> new IllegalStateException("Action not found"));

        Random rng = new Random(session.getRngSeed() + session.getStep() * 31L + actionKey.hashCode());
        double successChance = successChance(session, action, stats, inventory, rng);
        boolean success = rng.nextDouble() < successChance;

        EffectDelta actionDelta = applyEffects(success ? action.getSuccessEffectsJson() : action.getFailEffectsJson(),
                inventory, flags, checkpoints, rng);
        int heatDelta = actionDelta.deltaHeat() + action.getRisk();
        int progressDelta = actionDelta.deltaProgress();

        consumeRequiredItems(action.getRequirementsJson(), inventory);

        Optional<GameEvent> randomEvent = pickEvent(session, action, stats, inventory, flags, checkpoints, rng);
        EffectDelta eventDelta = randomEvent.map(event -> applyEffects(event.getEffectsJson(), inventory, flags, checkpoints, rng))
                .orElse(EffectDelta.empty());

        int totalCoinsDelta = actionDelta.deltaCoins() + eventDelta.deltaCoins();
        int totalHeatDelta = heatDelta + eventDelta.deltaHeat();
        int totalProgressDelta = progressDelta + eventDelta.deltaProgress();

        int tempDelta = totalCoinsDelta;

        int currentStep = session.getStep();
        session.setProgress(clamp(session.getProgress() + totalProgressDelta, 0, 100));
        session.setHeat(clamp(session.getHeat() + totalHeatDelta, 0, 100));
        session.setInventoryJson(writeJson(inventory));
        session.setFlagsJson(writeJson(flags));
        session.setCheckpointsJson(writeJson(checkpoints));
        session.setTailLevel(clamp(session.getTailLevel() + actionDelta.deltaTail() + eventDelta.deltaTail(), 0, 3));
        session.setEarnedTemp(session.getEarnedTemp() + tempDelta);
        session.setLastDeltaCoins(tempDelta);

        String outcome = buildOutcome(action, success, tempDelta, totalHeatDelta, totalProgressDelta, randomEvent);
        session.setLastOutcomeText(outcome);

        session.setStep(currentStep + 1);
        session.setVersion(session.getVersion() + 1);
        session.setLastActionAt(now);
        session.setExpiresAt(now.plusSeconds(properties.getSessionTimeoutMinutes() * 60L));

        GameSessionStatus terminal = resolveTerminal(session);
        if (terminal != GameSessionStatus.ACTIVE) {
            session.setStatus(terminal);
        }

        String nodeFrom = session.getNodeKey();
        String nodeTo = pickNextNode(session, stats, inventory, flags, checkpoints, rng);
        if (nodeTo != null) {
            session.setNodeKey(nodeTo);
        }

        GameEventLog log = new GameEventLog();
        log.setSessionId(session.getId());
        log.setStep(currentStep);
        log.setActionKey(action.getKey());
        log.setNodeFromKey(nodeFrom);
        log.setNodeToKey(session.getNodeKey());
        log.setSuccess(success);
        log.setDeltaCoins(tempDelta);
        log.setDeltaHeat(totalHeatDelta);
        log.setDeltaProgress(totalProgressDelta);
        log.setEventKeysJson(writeJson(randomEvent.map(GameEvent::getKey).map(List::of).orElseGet(List::of)));
        log.setOutcomeText(outcome);
        log.setCreatedAt(now);
        eventLogRepository.save(log);

        if (session.getStatus() == GameSessionStatus.ACTIVE) {
            List<String> actions = chooseActions(session, stats, inventory, flags, checkpoints, rng);
            session.setAvailableActionsJson(writeJson(actions));
            session.setLastSceneText(sceneTextFor(session.getMissionTypeKey(), session.getLocationKey(),
                    session.getNodeKey(), session.getProgress(), session.getHeat(), rng));
        }

        sessionRepository.save(session);

        if (session.getStatus() != GameSessionStatus.ACTIVE) {
            int payout = payoutEarnings(session, now);
            updateCooldown(session.getGuildId(), session.getUserId(), now);
            return withPublicSummary(render(session, null, payout), buildSummary(session));
        }

        return render(session, null, tempDelta);
    }

    public GameView render(GameSession session, String message, int deltaCoins) {
        GameMissionType missionType = missionTypeRepository.findByKey(session.getMissionTypeKey())
                .orElseThrow(() -> new IllegalStateException("Mission type not found"));
        GameLocation location = locationRepository.findByKey(session.getLocationKey())
                .orElseThrow(() -> new IllegalStateException("Location not found"));
        String nodeTitle = nodeRepository.findByKey(session.getNodeKey())
                .map(node -> node.getTitle())
                .orElse("Ситуация");

        Map<String, Integer> stats = readMap(session.getStatsJson());
        Map<String, Integer> inventory = readMap(session.getInventoryJson());
        List<String> actions = readList(session.getAvailableActionsJson());

        long coins = voiceUserRepository.findById(new VoiceUserId(session.getGuildId(), session.getUserId()))
                .map(VoiceUser::getPoints)
                .orElse(0L);

        String description = session.getLastSceneText() + "\n\n" + session.getLastOutcomeText();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🕶️ Prado Job: " + missionType.getName())
                .setDescription(description)
                .addField("📍 Локация", location.getName(), true)
                .addField("🎯 Цель", missionType.getObjective(), true)
                .addField("🧭 Узел", nodeTitle, true)
                .addField("🔥 Heat", heatLabel(session.getHeat()), true)
                .addField("🧨 Хвост", String.valueOf(session.getTailLevel()), true)
                .addField("📈 Прогресс", "Ход " + session.getStep() + "/" + properties.getMaxSteps(), true)
                .addField("🎒 Инвентарь", inventoryLabel(inventory), false)
                .addField("💰 Баланс", coins + formatDelta(deltaCoins != 0 ? deltaCoins : session.getLastDeltaCoins()), false)
                .addField("🧾 Накоплено", String.valueOf(session.getEarnedTemp()), false);

        if (message != null) {
            embed.setFooter(message);
        }

        if (session.getStatus() != GameSessionStatus.ACTIVE) {
            return new GameView(embed.build(), List.of(), null, true, null, session.getChannelId());
        }

        List<Button> actionButtons = actions.stream()
                .map(key -> actionRepository.findByKey(key).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GameAction::getId))
                .map(action -> Button.primary(buttonId(session, action.getKey()), action.getLabel()))
                .limit(5)
                .toList();

        List<ActionRow> rows = new ArrayList<>();
        if (!actionButtons.isEmpty()) {
            rows.add(ActionRow.of(actionButtons));
        }

        rows.add(ActionRow.of(
                Button.secondary(buttonId(session, "inspect"), "🗺️ Осмотреться"),
                Button.secondary(buttonId(session, "inventory"), "🎒 Инвентарь"),
                Button.danger(buttonId(session, "quit"), "❌ Завершить")
        ));

        return new GameView(embed.build(), rows, null, false, null, session.getChannelId());
    }

    private String buttonId(GameSession session, String actionKey) {
        return BUTTON_PREFIX + session.getId() + ":" + actionKey + ":" + session.getVersion();
    }

    private String sceneTextFor(String missionTypeKey, String locationKey, String nodeKey, int progress, int heat, Random rng) {
        List<GameScene> scenes = sceneRepository.findAllForContext(missionTypeKey, locationKey, nodeKey);
        List<GameScene> filtered = scenes.stream()
                .filter(scene -> progress >= scene.getMinProgress() && progress <= scene.getMaxProgress())
                .filter(scene -> heat >= scene.getMinHeat() && heat <= scene.getMaxHeat())
                .toList();
        if (filtered.isEmpty()) {
            return "В городе Лос-Сантос всё идёт по плану, но напряжение растёт.";
        }
        return filtered.get(rng.nextInt(filtered.size())).getSceneText();
    }

    private List<String> chooseActions(GameSession session, Map<String, Integer> stats,
                                       Map<String, Integer> inventory, List<String> flags,
                                       List<String> checkpoints, Random rng) {
        List<GameAction> actions = actionRepository.findAllForContext(session.getMissionTypeKey(), session.getNodeKey());
        List<GameAction> filtered = actions.stream()
                .filter(action -> session.getProgress() >= action.getMinProgress() && session.getProgress() <= action.getMaxProgress())
                .filter(action -> session.getHeat() >= action.getMinHeat() && session.getHeat() <= action.getMaxHeat())
                .filter(action -> meetsRequirements(action.getRequirementsJson(), stats, inventory, flags, checkpoints, session.getTailLevel()))
                .toList();

        if (filtered.isEmpty()) {
            return List.of();
        }

        List<GameAction> shuffled = new ArrayList<>(filtered);
        Collections.shuffle(shuffled, rng);
        int desired = Math.min(5, Math.max(3, shuffled.size() >= 3 ? 3 + rng.nextInt(3) : shuffled.size()));
        return shuffled.subList(0, Math.min(desired, shuffled.size())).stream()
                .map(GameAction::getKey)
                .toList();
    }

    private boolean meetsRequirements(String json, Map<String, Integer> stats, Map<String, Integer> inventory,
                                      List<String> flags, List<String> checkpoints, int tailLevel) {
        if (json == null || json.isBlank()) return true;
        JsonNode node = readJson(json);
        if (node == null || node.isNull()) return true;

        JsonNode requiredItems = node.get("requiredItems");
        if (requiredItems != null && requiredItems.isArray()) {
            for (JsonNode item : requiredItems) {
                String key = item.asText();
                if (inventory.getOrDefault(key, 0) <= 0) {
                    return false;
                }
            }
        }

        JsonNode minStats = node.get("minStats");
        if (minStats != null && minStats.isObject()) {
            var fields = minStats.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String statKey = entry.getKey();
                int value = entry.getValue().asInt();
                if (stats.getOrDefault(statKey, 0) < value) {
                    return false;
                }
            }
        }

        JsonNode requiredFlags = node.get("requiredFlags");
        if (requiredFlags != null && requiredFlags.isArray()) {
            for (JsonNode flag : requiredFlags) {
                if (!flags.contains(flag.asText())) {
                    return false;
                }
            }
        }

        JsonNode missingFlags = node.get("missingFlags");
        if (missingFlags != null && missingFlags.isArray()) {
            for (JsonNode flag : missingFlags) {
                if (flags.contains(flag.asText())) {
                    return false;
                }
            }
        }

        JsonNode requiredCheckpoints = node.get("requiredCheckpoints");
        if (requiredCheckpoints != null && requiredCheckpoints.isArray()) {
            for (JsonNode checkpoint : requiredCheckpoints) {
                if (!checkpoints.contains(checkpoint.asText())) {
                    return false;
                }
            }
        }

        JsonNode missingCheckpoints = node.get("missingCheckpoints");
        if (missingCheckpoints != null && missingCheckpoints.isArray()) {
            for (JsonNode checkpoint : missingCheckpoints) {
                if (checkpoints.contains(checkpoint.asText())) {
                    return false;
                }
            }
        }

        int minTail = node.has("minTail") ? node.get("minTail").asInt() : Integer.MIN_VALUE;
        int maxTail = node.has("maxTail") ? node.get("maxTail").asInt() : Integer.MAX_VALUE;
        if (tailLevel < minTail || tailLevel > maxTail) {
            return false;
        }

        return true;
    }

    private Optional<GameEvent> pickEvent(GameSession session, GameAction action, Map<String, Integer> stats,
                                          Map<String, Integer> inventory, List<String> flags,
                                          List<String> checkpoints, Random rng) {
        List<GameEvent> events = eventRepository.findAllForContext(session.getMissionTypeKey(), session.getNodeKey());

        Optional<GameEvent> reaction = pickEventByKind(events, "REACTION", session, action, stats, inventory, flags, checkpoints, rng);
        if (reaction.isPresent()) {
            return reaction;
        }

        return pickEventByKind(events, "AMBIENT", session, action, stats, inventory, flags, checkpoints, rng);
    }

    private Optional<GameEvent> pickEventByKind(List<GameEvent> events, String kind, GameSession session, GameAction action,
                                                Map<String, Integer> stats, Map<String, Integer> inventory,
                                                List<String> flags, List<String> checkpoints, Random rng) {
        List<GameEvent> filtered = events.stream()
                .filter(event -> kind.equalsIgnoreCase(eventKind(event)))
                .filter(event -> session.getProgress() >= event.getMinProgress() && session.getProgress() <= event.getMaxProgress())
                .filter(event -> session.getHeat() >= event.getMinHeat() && session.getHeat() <= event.getMaxHeat())
                .filter(event -> matchesTrigger(event, action))
                .filter(event -> meetsRequirements(event.getRequirementsJson(), stats, inventory, flags, checkpoints, session.getTailLevel()))
                .filter(event -> rng.nextDouble() < event.getBaseChance())
                .toList();

        if (filtered.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = filtered.stream().mapToInt(GameEvent::getWeight).sum();
        int roll = rng.nextInt(totalWeight);
        int acc = 0;
        for (GameEvent event : filtered) {
            acc += event.getWeight();
            if (roll < acc) {
                return Optional.of(event);
            }
        }
        return Optional.of(filtered.get(0));
    }

    private boolean matchesTrigger(GameEvent event, GameAction action) {
        if (action == null) return true;
        List<String> actionTypes = readList(event.getTriggerActionTypesJson());
        List<String> actionKeys = readList(event.getTriggerActionKeysJson());
        boolean typeMatch = actionTypes.isEmpty() || actionTypes.contains(action.getType());
        boolean keyMatch = actionKeys.isEmpty() || actionKeys.contains(action.getKey());
        return typeMatch && keyMatch;
    }

    private String eventKind(GameEvent event) {
        if (event.getEventKind() == null || event.getEventKind().isBlank()) {
            return "AMBIENT";
        }
        return event.getEventKind();
    }

    private String pickNextNode(GameSession session, Map<String, Integer> stats, Map<String, Integer> inventory,
                                List<String> flags, List<String> checkpoints, Random rng) {
        List<GameNodeTransition> transitions = nodeTransitionRepository.findAllByFromNodeKey(session.getNodeKey());
        List<GameNodeTransition> filtered = transitions.stream()
                .filter(transition -> meetsTransitionConditions(transition.getConditionJson(), session, stats, inventory, flags, checkpoints))
                .toList();
        if (filtered.isEmpty()) {
            return null;
        }
        int totalWeight = filtered.stream().mapToInt(GameNodeTransition::getWeight).sum();
        int roll = rng.nextInt(totalWeight);
        int acc = 0;
        for (GameNodeTransition transition : filtered) {
            acc += transition.getWeight();
            if (roll < acc) {
                return transition.getToNodeKey();
            }
        }
        return filtered.get(0).getToNodeKey();
    }

    private boolean meetsTransitionConditions(String json, GameSession session, Map<String, Integer> stats,
                                              Map<String, Integer> inventory, List<String> flags,
                                              List<String> checkpoints) {
        if (json == null || json.isBlank()) return true;
        JsonNode node = readJson(json);
        if (node == null || node.isNull()) return true;

        int minHeat = node.has("minHeat") ? node.get("minHeat").asInt() : Integer.MIN_VALUE;
        int maxHeat = node.has("maxHeat") ? node.get("maxHeat").asInt() : Integer.MAX_VALUE;
        int minProgress = node.has("minProgress") ? node.get("minProgress").asInt() : Integer.MIN_VALUE;
        int maxProgress = node.has("maxProgress") ? node.get("maxProgress").asInt() : Integer.MAX_VALUE;
        int minTail = node.has("minTail") ? node.get("minTail").asInt() : Integer.MIN_VALUE;
        int maxTail = node.has("maxTail") ? node.get("maxTail").asInt() : Integer.MAX_VALUE;

        if (session.getHeat() < minHeat || session.getHeat() > maxHeat) return false;
        if (session.getProgress() < minProgress || session.getProgress() > maxProgress) return false;
        if (session.getTailLevel() < minTail || session.getTailLevel() > maxTail) return false;

        JsonNode requiredFlags = node.get("requiredFlags");
        if (requiredFlags != null && requiredFlags.isArray()) {
            for (JsonNode flag : requiredFlags) {
                if (!flags.contains(flag.asText())) {
                    return false;
                }
            }
        }

        JsonNode missingFlags = node.get("missingFlags");
        if (missingFlags != null && missingFlags.isArray()) {
            for (JsonNode flag : missingFlags) {
                if (flags.contains(flag.asText())) {
                    return false;
                }
            }
        }

        JsonNode requiredCheckpoints = node.get("requiredCheckpoints");
        if (requiredCheckpoints != null && requiredCheckpoints.isArray()) {
            for (JsonNode checkpoint : requiredCheckpoints) {
                if (!checkpoints.contains(checkpoint.asText())) {
                    return false;
                }
            }
        }

        JsonNode missingCheckpoints = node.get("missingCheckpoints");
        if (missingCheckpoints != null && missingCheckpoints.isArray()) {
            for (JsonNode checkpoint : missingCheckpoints) {
                if (checkpoints.contains(checkpoint.asText())) {
                    return false;
                }
            }
        }

        return true;
    }

    private double successChance(GameSession session, GameAction action,
                                 Map<String, Integer> stats, Map<String, Integer> inventory, Random rng) {
        double base = action.getBaseSuccess();
        double statBonus = stats.getOrDefault(action.getStatKey(), 0) / 10.0 * action.getStatScale();
        double heatPenalty = session.getHeat() / 100.0 * 0.25;
        double itemBonus = 0.0;
        for (var entry : inventory.entrySet()) {
            GameItem item = itemRepository.findByKey(entry.getKey()).orElse(null);
            if (item != null) {
                itemBonus += itemEffectBonus(item, entry.getValue());
            }
        }

        double voiceBonus = voiceBonus(session.getGuildId(), session.getUserId());

        double chance = base + statBonus + itemBonus + voiceBonus - heatPenalty;
        return Math.max(0.05, Math.min(0.95, chance));
    }

    private double itemEffectBonus(GameItem item, int count) {
        JsonNode effects = readJson(item.getEffectsJson());
        if (effects == null) return 0.0;
        double bonus = effects.has("successBonus") ? effects.get("successBonus").asDouble() : 0.0;
        return bonus * Math.max(1, count);
    }

    private double voiceBonus(long guildId, long userId) {
        ZoneId zone = ZoneId.of(appTimezone);
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        long seconds = voiceSessionRepository.sumActiveSecondsForUser(guildId, userId, start, end);
        return seconds >= properties.getVoiceBonusSeconds() ? properties.getVoiceSuccessBonus() : 0.0;
    }

    private EffectDelta applyEffects(String json, Map<String, Integer> inventory, List<String> flags,
                                     List<String> checkpoints, Random rng) {
        if (json == null || json.isBlank()) return EffectDelta.empty();
        JsonNode node = readJson(json);
        if (node == null || node.isNull()) return EffectDelta.empty();

        int deltaCoins = readDelta(node.get("deltaCoins"), rng);
        int deltaHeat = readDelta(node.get("deltaHeat"), rng);
        int deltaProgress = readDelta(node.get("deltaProgress"), rng);
        int deltaTail = readDelta(node.get("deltaTail"), rng);

        JsonNode addItems = node.get("addItem");
        if (addItems != null) {
            applyItems(addItems, inventory, 1);
        }
        JsonNode removeItems = node.get("removeItem");
        if (removeItems != null) {
            applyItems(removeItems, inventory, -1);
        }

        applyFlags(node.get("setFlag"), flags, true);
        applyFlags(node.get("clearFlag"), flags, false);
        applyFlags(node.get("addCheckpoint"), checkpoints, true);
        applyFlags(node.get("removeCheckpoint"), checkpoints, false);

        return new EffectDelta(deltaCoins, deltaHeat, deltaProgress, deltaTail);
    }

    private void applyFlags(JsonNode node, List<String> target, boolean add) {
        if (node == null) return;
        if (node.isArray()) {
            for (JsonNode item : node) {
                applyFlag(target, item.asText(), add);
            }
        } else {
            applyFlag(target, node.asText(), add);
        }
    }

    private void applyFlag(List<String> target, String value, boolean add) {
        if (value == null || value.isBlank()) return;
        if (add) {
            if (!target.contains(value)) {
                target.add(value);
            }
        } else {
            target.remove(value);
        }
    }

    private void applyItems(JsonNode node, Map<String, Integer> inventory, int delta) {
        if (node.isArray()) {
            for (JsonNode item : node) {
                applyItemDelta(inventory, item.asText(), delta);
            }
        } else {
            applyItemDelta(inventory, node.asText(), delta);
        }
    }

    private void consumeRequiredItems(String json, Map<String, Integer> inventory) {
        JsonNode node = readJson(json);
        if (node == null || node.isNull()) return;
        JsonNode requiredItems = node.get("requiredItems");
        if (requiredItems == null || !requiredItems.isArray()) return;
        for (JsonNode itemNode : requiredItems) {
            String key = itemNode.asText();
            itemRepository.findByKey(key).ifPresent(item -> {
                if (item.isConsumable()) {
                    applyItemDelta(inventory, key, -1);
                }
            });
        }
    }

    private void applyItemDelta(Map<String, Integer> inventory, String key, int delta) {
        int next = inventory.getOrDefault(key, 0) + delta;
        if (next <= 0) {
            inventory.remove(key);
        } else {
            inventory.put(key, next);
        }
    }

    private int readDelta(JsonNode node, Random rng) {
        if (node == null || node.isNull()) return 0;
        if (node.isNumber()) return node.asInt();
        if (node.isObject()) {
            int min = node.has("min") ? node.get("min").asInt() : 0;
            int max = node.has("max") ? node.get("max").asInt() : min;
            if (max < min) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            return min + rng.nextInt(max - min + 1);
        }
        return 0;
    }

    private int applyCoinsDelta(long guildId, long userId, int delta, Instant now) {
        if (delta == 0) return 0;
        GameCooldown cooldown = cooldownRepository.findByGuildIdAndUserId(guildId, userId)
                .orElseGet(() -> createCooldown(guildId, userId, now));

        LocalDate today = LocalDate.now(ZoneId.of(appTimezone));
        if (!today.equals(cooldown.getDailyDate())) {
            cooldown.setDailyDate(today);
            cooldown.setDailyEarned(0);
        }

        int appliedDelta = delta;
        if (delta > 0) {
            int remaining = Math.max(0, properties.getDailyCap() - cooldown.getDailyEarned());
            appliedDelta = Math.min(delta, remaining);
            cooldown.setDailyEarned(cooldown.getDailyEarned() + appliedDelta);
        }

        VoiceUserId id = new VoiceUserId(guildId, userId);
        VoiceUser user = voiceUserRepository.findById(id).orElseGet(() -> {
            VoiceUser u = new VoiceUser();
            u.setId(id);
            u.setPoints(0);
            u.setCreatedAt(now);
            u.setUpdatedAt(now);
            return u;
        });
        user.setPoints(user.getPoints() + appliedDelta);
        user.setUpdatedAt(now);
        voiceUserRepository.save(user);

        cooldownRepository.save(cooldown);
        return appliedDelta;
    }

    private int payoutEarnings(GameSession session, Instant now) {
        int payout = computePayout(session);
        int applied = applyCoinsDelta(session.getGuildId(), session.getUserId(), payout, now);
        session.setLastDeltaCoins(applied);
        sessionRepository.save(session);
        return applied;
    }

    private int computePayout(GameSession session) {
        int earned = session.getEarnedTemp();
        if (earned <= 0) return 0;
        if (session.getStatus() == GameSessionStatus.SUCCESS) {
            return earned;
        }
        if (session.getStatus() == GameSessionStatus.FAIL) {
            return session.getHeat() >= 70 ? Math.max(0, (int) Math.floor(earned * 0.2)) : Math.max(0, (int) Math.floor(earned * 0.5));
        }
        if (session.getStatus() == GameSessionStatus.QUIT) {
            return Math.max(0, (int) Math.floor(earned * 0.5));
        }
        return Math.max(0, (int) Math.floor(earned * 0.3));
    }

    private void updateCooldown(long guildId, long userId, Instant now) {
        GameCooldown cooldown = cooldownRepository.findByGuildIdAndUserId(guildId, userId)
                .orElseGet(() -> createCooldown(guildId, userId, now));
        cooldown.setLastGameFinishedAt(now);
        cooldownRepository.save(cooldown);
    }

    private GameCooldown createCooldown(long guildId, long userId, Instant now) {
        GameCooldown cooldown = new GameCooldown();
        cooldown.setGuildId(guildId);
        cooldown.setUserId(userId);
        cooldown.setDailyDate(LocalDate.now(ZoneId.of(appTimezone)));
        cooldown.setDailyEarned(0);
        cooldown.setLastGameFinishedAt(null);
        return cooldownRepository.save(cooldown);
    }

    private GameSessionStatus resolveTerminal(GameSession session) {
        if (session.getHeat() >= 100) {
            return GameSessionStatus.FAIL;
        }
        if (session.getProgress() >= 100) {
            return GameSessionStatus.SUCCESS;
        }
        if (session.getStep() > properties.getMaxSteps()) {
            return GameSessionStatus.FAIL;
        }
        return GameSessionStatus.ACTIVE;
    }

    private String buildOutcome(GameAction action, boolean success, int deltaCoins, int deltaHeat, int deltaProgress,
                                Optional<GameEvent> event) {
        StringBuilder sb = new StringBuilder();
        sb.append(success ? "✅ " : "❌ ").append(action.getLabel());
        if (deltaCoins != 0) {
            sb.append(" | ").append(deltaCoins > 0 ? "+" : "").append(deltaCoins).append(" монет");
        }
        if (deltaProgress != 0) {
            sb.append(" | прогресс ").append(deltaProgress > 0 ? "+" : "").append(deltaProgress);
        }
        if (deltaHeat != 0) {
            sb.append(" | heat ").append(deltaHeat > 0 ? "+" : "").append(deltaHeat);
        }
        event.ifPresent(ev -> sb.append("\nСобытие: ").append(ev.getTitle()));
        return sb.toString();
    }

    private String buildSummary(GameSession session) {
        GameMissionType missionType = missionTypeRepository.findByKey(session.getMissionTypeKey())
                .orElseThrow(() -> new IllegalStateException("Mission type not found"));
        GameLocation location = locationRepository.findByKey(session.getLocationKey())
                .orElseThrow(() -> new IllegalStateException("Location not found"));
        List<GameEventLog> logs = eventLogRepository.findAllBySessionIdOrderByStepAsc(session.getId());
        String header = "Итог миссии: " + missionType.getName() + " в " + location.getName();
        String status = "Результат: " + statusLabel(session.getStatus());
        int earned = session.getEarnedTemp();
        int payout = computePayout(session);
        String coins = "Монеты: +" + payout + " (накоплено " + earned + ")";

        String story = logs.stream()
                .limit(6)
                .map(log -> "• " + log.getOutcomeText())
                .collect(Collectors.joining("\n"));

        return header + "\n" + status + "\n" + coins + "\n" + (story.isBlank() ? "История пуста." : story);
    }

    private String statusLabel(GameSessionStatus status) {
        return switch (status) {
            case SUCCESS -> "успех";
            case FAIL -> "провал";
            case QUIT -> "выход";
            case EXPIRED -> "истекло время";
            case ACTIVE -> "в процессе";
        };
    }

    private void upsertUser(long guildId, long userId, String username, String memberName) {
        VoiceUserId id = new VoiceUserId(guildId, userId);
        Instant now = Instant.now();
        VoiceUser user = voiceUserRepository.findById(id).orElseGet(() -> {
            VoiceUser u = new VoiceUser();
            u.setId(id);
            u.setPoints(0);
            u.setCreatedAt(now);
            return u;
        });
        user.setUsername(username);
        user.setMemberName(memberName);
        user.setUpdatedAt(now);
        voiceUserRepository.save(user);
    }

    private String inventoryLabel(Map<String, Integer> inventory) {
        if (inventory.isEmpty()) {
            return "пусто";
        }
        return inventory.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    String name = itemRepository.findByKey(key).map(GameItem::getName).orElse(key);
                    return name + " x" + entry.getValue();
                })
                .collect(Collectors.joining(", "));
    }

    private String heatLabel(int heat) {
        if (heat < 34) return "Low";
        if (heat < 67) return "Med";
        return "High";
    }

    private String formatDelta(int delta) {
        if (delta == 0) return "";
        return " (" + (delta > 0 ? "+" : "") + delta + ")";
    }

    private <T> T pickRandom(List<T> list, long seed) {
        if (list.isEmpty()) {
            throw new IllegalStateException("content missing");
        }
        Random rng = new Random(seed);
        return list.get(rng.nextInt(list.size()));
    }

    private long seed(long value) {
        return value ^ Instant.now().toEpochMilli();
    }

    private Map<String, Integer> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record EffectDelta(int deltaCoins, int deltaHeat, int deltaProgress, int deltaTail) {
        static EffectDelta empty() {
            return new EffectDelta(0, 0, 0, 0);
        }
    }

    private GameView withPublicSummary(GameView view, String summary) {
        return new GameView(view.embed(), view.components(), view.errorMessage(), true, summary, view.publicChannelId());
    }
}
