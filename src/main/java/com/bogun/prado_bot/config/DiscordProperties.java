package com.bogun.prado_bot.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Getter
@Validated
@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {
    @NotBlank
    private String token;
}