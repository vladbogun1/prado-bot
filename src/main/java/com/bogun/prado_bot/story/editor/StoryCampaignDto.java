package com.bogun.prado_bot.story.editor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryCampaignDto {
    private String campaignKey;
    private String name;
    private String description;
    private String startNodeKey;
    private int cooldownMinutes;
}
