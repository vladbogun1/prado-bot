package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.config.GameProperties;
import com.bogun.prado_bot.domain.game.GameMission;
import com.bogun.prado_bot.domain.game.GameMissionLog;
import com.bogun.prado_bot.domain.game.MissionChoice;
import com.bogun.prado_bot.domain.game.MissionStatus;
import com.bogun.prado_bot.game.NarratorResponse;
import com.bogun.prado_bot.repo.GameMissionLogRepository;
import com.bogun.prado_bot.repo.GameMissionRepository;
import com.bogun.prado_bot.repo.MissionChoiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class GameMissionService {

    private final GameMissionRepository missionRepository;
    private final GameMissionLogRepository logRepository;
    private final MissionChoiceRepository choiceRepository;
    private final GameNarratorService narratorService;
    private final GameRecapService recapService;
    private final GameRewardService rewardService;
    private final GameProperties gameProperties;
    private final ObjectMapper objectMapper;

    public GameMissionService(GameMissionRepository missionRepository,
                              GameMissionLogRepository logRepository,
                              MissionChoiceRepository choiceRepository,
                              GameNarratorService narratorService,
                              GameRecapService recapService,
                              GameRewardService rewardService,
                              GameProperties gameProperties,
                              ObjectMapper objectMapper) {
        this.missionRepository = missionRepository;
        this.logRepository = logRepository;
        this.choiceRepository = choiceRepository;
        this.narratorService = narratorService;
        this.recapService = recapService;
        this.rewardService = rewardService;
        this.gameProperties = gameProperties;
        this.objectMapper = objectMapper;
    }

    public Optional<GameMission> findActiveMission(long guildId, long userId) {
        return missionRepository.findFirstByGuildIdAndUserIdAndStatusOrderByCreatedAtDesc(guildId, userId,
                MissionStatus.ACTIVE);
    }

    @Transactional
    public MissionStartResult startMission(long guildId, long channelId, long userId) {
        GameMission mission = new GameMission();
        mission.setGuildId(guildId);
        mission.setChannelId(channelId);
        mission.setUserId(userId);
        mission.setStepIndex(0);
        mission.setWanted(0);
        mission.setStatus(MissionStatus.ACTIVE);
        mission.setCreatedAt(Instant.now());
        mission.setUpdatedAt(Instant.now());
        missionRepository.save(mission);

        List<String> memoryFacts = new ArrayList<>();
        List<String> memoryNpcs = new ArrayList<>();
        String history = buildHistory(mission.getId(), mission.getStepIndex());
        NarratorResponse response = narratorService.nextScene(mission, memoryFacts, memoryNpcs, null, history);
        updateMemory(mission, memoryFacts, memoryNpcs, response);
        logNarrator(mission, response);
        mission.setUpdatedAt(Instant.now());
        missionRepository.save(mission);
        return new MissionStartResult(mission, response);
    }

    @Transactional
    public MissionStepResult applyChoice(long missionId, long userId, String choiceId) {
        GameMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalStateException("Mission not found"));
        if (mission.getUserId() != userId) {
            throw new IllegalStateException("Mission owner mismatch");
        }
        if (mission.getStatus() != MissionStatus.ACTIVE) {
            throw new IllegalStateException("Mission already finished");
        }
        if (choiceRepository.existsByMissionIdAndStepIndex(missionId, mission.getStepIndex())) {
            throw new IllegalStateException("Duplicate choice");
        }

        NarratorResponse previousScene = loadNarratorResponse(missionId, mission.getStepIndex());
        NarratorResponse.Choice choice = previousScene.choices().stream()
                .filter(c -> c.id().equalsIgnoreCase(choiceId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Choice not found"));

        MissionChoice missionChoice = new MissionChoice();
        missionChoice.setMission(mission);
        missionChoice.setStepIndex(mission.getStepIndex());
        missionChoice.setChoiceId(choice.id());
        missionChoice.setLabel(choice.label());
        missionChoice.setIntent(choice.intent());
        missionChoice.setNote(choice.note());
        missionChoice.setCreatedAt(Instant.now());
        choiceRepository.save(missionChoice);

        mission.setWanted(updateWanted(mission.getWanted(), choice.intent()));
        logChoice(mission, missionChoice);

        mission.setStepIndex(mission.getStepIndex() + 1);
        mission.setUpdatedAt(Instant.now());

        boolean failure = mission.getWanted() >= 5;
        boolean reachedMax = mission.getStepIndex() >= gameProperties.maxSteps();
        if (failure || reachedMax) {
            mission.setStatus(failure ? MissionStatus.FAILURE : MissionStatus.SUCCESS);
            missionRepository.save(mission);
            logOutcome(mission);
            int rewardCoins = 0;
            if (!failure) {
                rewardCoins = gameProperties.rewardCoins();
                rewardService.grantCoins(mission.getGuildId(), mission.getUserId(), rewardCoins);
            }
            String recap = buildRecap(mission);
            return new MissionStepResult(mission, null, true, recap, rewardCoins);
        }

        List<String> memoryFacts = readList(mission.getMemoryFactsJson());
        List<String> memoryNpcs = readList(mission.getMemoryNpcsJson());
        String history = buildHistory(mission.getId(), mission.getStepIndex());
        NarratorResponse response = narratorService.nextScene(mission, memoryFacts, memoryNpcs, missionChoice, history);
        updateMemory(mission, memoryFacts, memoryNpcs, response);
        logNarrator(mission, response);
        mission.setUpdatedAt(Instant.now());
        missionRepository.save(mission);

        return new MissionStepResult(mission, response, false, null, 0);
    }

    private void logNarrator(GameMission mission, NarratorResponse response) {
        GameMissionLog log = new GameMissionLog();
        log.setMission(mission);
        log.setStepIndex(mission.getStepIndex());
        log.setRole("NARRATOR_JSON");
        log.setContent(writeJson(response));
        log.setCreatedAt(Instant.now());
        logRepository.save(log);
    }

    private void logChoice(GameMission mission, MissionChoice choice) {
        GameMissionLog log = new GameMissionLog();
        log.setMission(mission);
        log.setStepIndex(choice.getStepIndex());
        log.setRole("CHOICE");
        log.setContent("Choice " + choice.getChoiceId() + ": " + choice.getLabel() + " (intent: " + choice.getIntent()
                + ", wanted: " + mission.getWanted() + "⭐)");
        log.setCreatedAt(Instant.now());
        logRepository.save(log);
    }

    private void logOutcome(GameMission mission) {
        GameMissionLog log = new GameMissionLog();
        log.setMission(mission);
        log.setStepIndex(mission.getStepIndex());
        log.setRole("OUTCOME");
        log.setContent("Outcome: " + mission.getStatus() + ", wanted: " + mission.getWanted() + "⭐");
        log.setCreatedAt(Instant.now());
        logRepository.save(log);
    }

    private String buildHistory(long missionId, int currentStep) {
        List<GameMissionLog> logs = logRepository.findByMissionIdOrderByStepIndexAscIdAsc(missionId);
        StringBuilder builder = new StringBuilder();
        for (GameMissionLog log : logs) {
            if (log.getStepIndex() > currentStep) {
                break;
            }
            if ("NARRATOR_JSON".equals(log.getRole())) {
                try {
                    NarratorResponse response = objectMapper.readValue(log.getContent(), NarratorResponse.class);
                    builder.append("- ").append(response.scene().title()).append(": ")
                            .append(response.scene().text()).append("\n");
                } catch (JsonProcessingException ignored) {
                    builder.append("- Сцена (не удалось прочитать)\n");
                }
            } else if ("CHOICE".equals(log.getRole())) {
                builder.append("- ").append(log.getContent()).append("\n");
            }
        }
        if (builder.isEmpty()) {
            return "История начинается сейчас.";
        }
        return builder.toString().trim();
    }

    private String buildRecap(GameMission mission) {
        List<GameMissionLog> logs = logRepository.findByMissionIdOrderByStepIndexAscIdAsc(mission.getId());
        logs.sort(Comparator.comparing(GameMissionLog::getStepIndex).thenComparing(GameMissionLog::getId));
        StringBuilder builder = new StringBuilder();
        for (GameMissionLog log : logs) {
            builder.append("[").append(log.getRole()).append("] ")
                    .append(log.getContent()).append("\n");
        }
        return recapService.buildRecap(builder.toString().trim());
    }

    private NarratorResponse loadNarratorResponse(long missionId, int stepIndex) {
        return logRepository.findByMissionIdOrderByStepIndexAscIdAsc(missionId).stream()
                .filter(log -> log.getStepIndex() == stepIndex && "NARRATOR_JSON".equals(log.getRole()))
                .findFirst()
                .map(log -> {
                    try {
                        return objectMapper.readValue(log.getContent(), NarratorResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Invalid narrator JSON");
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Narrator response not found"));
    }

    private void updateMemory(GameMission mission, List<String> memoryFacts, List<String> memoryNpcs,
                              NarratorResponse response) {
        if (response.memoryUpdate() != null) {
            if (response.memoryUpdate().addFacts() != null) {
                response.memoryUpdate().addFacts().forEach(fact -> {
                    if (!memoryFacts.contains(fact)) {
                        memoryFacts.add(fact);
                    }
                });
            }
            if (response.memoryUpdate().removeFacts() != null) {
                memoryFacts.removeAll(response.memoryUpdate().removeFacts());
            }
            if (response.memoryUpdate().npcs() != null) {
                response.memoryUpdate().npcs().forEach(npc -> {
                    if (!memoryNpcs.contains(npc)) {
                        memoryNpcs.add(npc);
                    }
                });
            }
        }
        mission.setMemoryFactsJson(writeJson(memoryFacts));
        mission.setMemoryNpcsJson(writeJson(memoryNpcs));
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private int updateWanted(int current, String intent) {
        int delta = switch (intent) {
            case "fight" -> 2;
            case "drive" -> 1;
            default -> 0;
        };
        int next = current + delta;
        if (next < 0) {
            return 0;
        }
        return Math.min(next, 5);
    }
}
