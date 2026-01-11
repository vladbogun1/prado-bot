package com.bogun.prado_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game")
public record GameProperties(int maxSteps, int narratorRetryAttempts, int rewardCoins, Prompts prompts) {

    public record Prompts(String narrator, String recap) {}
}
