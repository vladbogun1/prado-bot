package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.domain.game.GameMission;
import com.bogun.prado_bot.game.NarratorResponse;

public record MissionStepResult(GameMission mission, NarratorResponse response, boolean completed, String recap) {}
