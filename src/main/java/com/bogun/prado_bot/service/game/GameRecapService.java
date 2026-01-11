package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.config.GameProperties;
import com.bogun.prado_bot.config.OllamaProperties;
import org.springframework.stereotype.Service;

@Service
public class GameRecapService {

    private final OllamaClient ollamaClient;
    private final GameProperties gameProperties;
    private final OllamaProperties ollamaProperties;

    public GameRecapService(OllamaClient ollamaClient,
                            GameProperties gameProperties,
                            OllamaProperties ollamaProperties) {
        this.ollamaClient = ollamaClient;
        this.gameProperties = gameProperties;
        this.ollamaProperties = ollamaProperties;
    }

    public String buildRecap(String missionLogText) {
        String prompt = gameProperties.prompts().recap() + "\n\nЛог миссии:\n" + missionLogText + "\n\nОтвет:";
        try {
            return ollamaClient.generate(ollamaProperties.recapModel(), prompt).trim();
        } catch (Exception e) {
            return fallbackRecap(missionLogText);
        }
    }

    private String fallbackRecap(String missionLogText) {
        return "Хроники Лос-Сантоса получились шумными. Вот сухой расклад:\n" + missionLogText;
    }
}
