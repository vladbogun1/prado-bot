package com.bogun.prado_bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.prado-game")
public class PradoGameProperties {

    private int cooldownMinutes = 5;
    private int sessionTimeoutMinutes = 15;
    private int dailyCap = 40;
    private int maxSteps = 12;
    private boolean ephemeral = true;
    private long voiceBonusSeconds = 1800;
    private double voiceSuccessBonus = 0.05;

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public int getDailyCap() {
        return dailyCap;
    }

    public void setDailyCap(int dailyCap) {
        this.dailyCap = dailyCap;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public long getVoiceBonusSeconds() {
        return voiceBonusSeconds;
    }

    public void setVoiceBonusSeconds(long voiceBonusSeconds) {
        this.voiceBonusSeconds = voiceBonusSeconds;
    }

    public double getVoiceSuccessBonus() {
        return voiceSuccessBonus;
    }

    public void setVoiceSuccessBonus(double voiceSuccessBonus) {
        this.voiceSuccessBonus = voiceSuccessBonus;
    }
}
