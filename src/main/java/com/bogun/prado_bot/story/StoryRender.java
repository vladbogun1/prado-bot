package com.bogun.prado_bot.story;

import net.dv8tion.jda.api.components.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public record StoryRender(MessageEmbed embed, List<ActionRow> rows) {}
