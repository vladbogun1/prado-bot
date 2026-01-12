package com.bogun.prado_bot.story;

public record StoryNode(
        String campaignKey,
        String nodeKey,
        String title,
        String variantsJson,
        String autoEffectsJson,
        boolean terminal,
        String terminalType,
        String rewardJson
) {}
