package com.bogun.prado_bot.story;

public record StoryCampaign(
        String campaignKey,
        String name,
        String description,
        String startNodeKey
) {}
