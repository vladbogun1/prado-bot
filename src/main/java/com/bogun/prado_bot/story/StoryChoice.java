package com.bogun.prado_bot.story;

public record StoryChoice(
        String campaignKey,
        String nodeKey,
        String choiceKey,
        String label,
        int sortOrder,
        String conditionsJson,
        String checkJson,
        String successNodeKey,
        String failNodeKey,
        String successText,
        String failText,
        String successEffectsJson,
        String failEffectsJson
) {}
