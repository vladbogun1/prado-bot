package com.bogun.prado_bot.story.editor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryGraphDto {
    private List<StoryNodeDto> nodes;
    private List<StoryChoiceDto> choices;
}
