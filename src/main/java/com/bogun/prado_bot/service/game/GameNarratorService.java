package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.config.GameProperties;
import com.bogun.prado_bot.config.OllamaProperties;
import com.bogun.prado_bot.domain.game.GameMission;
import com.bogun.prado_bot.domain.game.MissionChoice;
import com.bogun.prado_bot.game.NarratorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GameNarratorService {

    private static final Set<String> ALLOWED_INTENTS = Set.of(
            "drive", "stealth", "talk", "fight", "inspect", "use_item", "wait"
    );

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final GameProperties gameProperties;
    private final OllamaProperties ollamaProperties;

    public GameNarratorService(OllamaClient ollamaClient,
                               ObjectMapper objectMapper,
                               GameProperties gameProperties,
                               OllamaProperties ollamaProperties) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
        this.gameProperties = gameProperties;
        this.ollamaProperties = ollamaProperties;
    }

    public NarratorResponse nextScene(GameMission mission, List<String> memoryFacts, List<String> memoryNpcs,
                                      MissionChoice previousChoice, String history) {
        String prompt = buildPrompt(mission, memoryFacts, memoryNpcs, previousChoice, history);

        int attempts = Math.max(1, gameProperties.narratorRetryAttempts());
        for (int i = 0; i < attempts; i++) {
            try {
                String raw = ollamaClient.generate(ollamaProperties.narratorModel(), prompt);
                NarratorResponse parsed = objectMapper.readValue(raw, NarratorResponse.class);
                if (isValid(parsed)) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // Retry with same prompt
            }
        }

        return fallbackResponse(mission, previousChoice);
    }

    private String buildPrompt(GameMission mission, List<String> memoryFacts, List<String> memoryNpcs,
                               MissionChoice previousChoice, String history) {
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", Instant.now().toString());
        context.put("wanted", mission.getWanted());
        context.put("step_index", mission.getStepIndex());
        context.put("step_total", gameProperties.maxSteps());
        context.put("memory_facts", memoryFacts);
        context.put("memory_npcs", memoryNpcs);
        if (previousChoice != null) {
            Map<String, String> choice = new HashMap<>();
            choice.put("id", previousChoice.getChoiceId());
            choice.put("label", previousChoice.getLabel());
            choice.put("intent", previousChoice.getIntent());
            context.put("previous_choice", choice);
        }

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            contextJson = "{\"wanted\":" + mission.getWanted() + "}";
        }

        String stageHint = stageHint(mission.getStepIndex(), gameProperties.maxSteps());
        return gameProperties.prompts().narrator()
                + "\n\nКонтекст:\n" + contextJson
                + "\n\nИстория (кратко):\n" + history
                + "\n\nЭтап миссии:\n" + stageHint
                + "\n\nОтвет:";
    }

    private boolean isValid(NarratorResponse response) {
        if (response == null || response.scene() == null || response.choices() == null) {
            return false;
        }
        if (response.choices().size() < 3 || response.choices().size() > 5) {
            return false;
        }
        Set<String> ids = new HashSet<>();
        for (NarratorResponse.Choice choice : response.choices()) {
            if (choice == null || isBlank(choice.id()) || isBlank(choice.label()) || isBlank(choice.intent())) {
                return false;
            }
            if (!ALLOWED_INTENTS.contains(choice.intent())) {
                return false;
            }
            ids.add(choice.id());
        }
        return ids.size() == response.choices().size();
    }

    private NarratorResponse fallbackResponse(GameMission mission, MissionChoice previousChoice) {
        String title = "Глухой поворот";
        String text = previousChoice == null
                ? "Ночь давит на Лос-Сантос. Ты стоишь у края тихой улицы, руки в карманах, дела ждут."
                : "Двигатель урчит, фонари бегут по лобовому. Ты сделал ход, но улицы требуют следующего решения.";
        String location = "Южный Лос-Сантос";

        List<NarratorResponse.Choice> choices = List.of(
                new NarratorResponse.Choice("A", "Свернуть в переулок", "stealth", "тише будет"),
                new NarratorResponse.Choice("B", "Рвануть по трассе", "drive", "скорость привлекает внимание"),
                new NarratorResponse.Choice("C", "Поговорить с типом", "talk", "может знает выход")
        );

        var memory = new NarratorResponse.MemoryUpdate(List.of("Лос-Сантос живёт ночью."), List.of(), List.of("Случайный тип"));

        return new NarratorResponse(new NarratorResponse.Scene(title, text, location), choices, memory);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String stageHint(int stepIndex, int maxSteps) {
        if (maxSteps <= 1) {
            return "Финал: быстрый исход и завершение миссии.";
        }
        int current = stepIndex + 1;
        if (current <= Math.max(2, maxSteps / 4)) {
            return "Завязка: представь угрозу и цель, не закрывай конфликт.";
        }
        if (current < maxSteps - 2) {
            return "Развитие: усиливай ставки и последствия, веди к развязке.";
        }
        if (current == maxSteps - 1) {
            return "Предфинал: дай решающее препятствие и подведи к выбору перед концом.";
        }
        return "Финал: логическое завершение миссии и явный исход.";
    }
}
