package com.bogun.prado_bot.story.editor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryNodeDto {
    private String campaignKey;
    private String nodeKey;
    private String title;
    private String variantsJson;
    private String autoEffectsJson;
    private boolean terminal;
    private String terminalType;
    private String rewardJson;
}
