package com.bogun.prado_bot.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "story.game")
public class StoryGameProperties {
    @NotBlank
    private String defaultCampaignKey = "BUM_TO_BOSS";

    @Positive
    private int cooldownMinutes = 10;

    @Positive
    private int sessionTtlMinutes = 30;
}
