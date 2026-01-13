package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryCampaign {
    private String campaignKey;
    private String name;
    private String description;
    private String startNodeKey;
    private int cooldownMinutes;
}
