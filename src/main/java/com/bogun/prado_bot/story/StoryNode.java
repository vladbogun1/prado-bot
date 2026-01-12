package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryNode {
    private String campaignKey;
    private String nodeKey;
    private String title;
    private String variantsJson;
    private String autoEffectsJson;
    private boolean terminal;
    private String terminalType;
    private String rewardJson;
}
