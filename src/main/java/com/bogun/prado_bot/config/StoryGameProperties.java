package com.bogun.prado_bot.config;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "story.game")
public class StoryGameProperties {
    @Positive
    private int sessionTtlMinutes = 30;
}
