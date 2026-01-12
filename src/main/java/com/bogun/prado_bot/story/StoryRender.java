package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class StoryRender {
    private MessageEmbed embed;
    private List<ActionRow> rows;
    private StoryRecap recap;
}
