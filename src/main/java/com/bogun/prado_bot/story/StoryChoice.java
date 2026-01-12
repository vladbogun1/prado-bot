package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryChoice {
    private String campaignKey;
    private String nodeKey;
    private String choiceKey;
    private String label;
    private int sortOrder;
    private String conditionsJson;
    private String checkJson;
    private String successNodeKey;
    private String failNodeKey;
    private String successText;
    private String failText;
    private String successEffectsJson;
    private String failEffectsJson;
}
