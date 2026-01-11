package com.bogun.prado_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(String baseUrl, String narratorModel, String recapModel, Duration timeout) {}
