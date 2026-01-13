package com.bogun.prado_bot.discord;

import com.bogun.prado_bot.story.StoryGameEngine;
import com.bogun.prado_bot.story.StoryGameException;
import com.bogun.prado_bot.story.StoryGuildConfigService;
import com.bogun.prado_bot.story.StoryRecap;
import com.bogun.prado_bot.story.StoryRender;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.springframework.stereotype.Component;

@Component
public class StoryGameHandler {

    public static final String COMMAND = "prado_game";
    public static final String BUTTON_PREFIX = "prado_game:";

    private final StoryGameEngine engine;
    private final StoryGuildConfigService configService;

    public StoryGameHandler(StoryGameEngine engine, StoryGuildConfigService configService) {
        this.engine = engine;
        this.configService = configService;
    }

    public boolean handleSlash(SlashCommandInteractionEvent e) {
        if (!COMMAND.equals(e.getName())) {
            return false;
        }
        if (e.getGuild() == null || e.getMember() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return true;
        }
        if (e.getOption("action") != null) {
            String action = e.getOption("action").getAsString();
            if (!"start".equalsIgnoreCase(action)) {
                e.reply("Неизвестная команда.").setEphemeral(true).queue();
                return true;
            }
        }
        try {
            StoryRender render = engine.startOrResume(
                    e.getGuild().getIdLong(),
                    e.getUser().getIdLong(),
                    e.getChannel().getIdLong(),
                    e.getUser().getName(),
                    e.getMember().getEffectiveName()
            );
            e.replyEmbeds(render.getEmbed()).setComponents(render.getRows()).setEphemeral(true).queue();
        } catch (StoryGameException ex) {
            e.reply(ex.getMessage()).setEphemeral(true).queue();
        }
        return true;
    }

    public boolean handleButton(ButtonInteractionEvent e) {
        String id = e.getComponentId();
        if (!id.startsWith(BUTTON_PREFIX)) {
            return false;
        }
        String payload = id.substring(BUTTON_PREFIX.length());
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            e.reply("Не удалось обработать выбор.").setEphemeral(true).queue();
            return true;
        }
        long sessionId;
        int version;
        try {
            sessionId = Long.parseLong(parts[0]);
            version = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            e.reply("Не удалось обработать выбор.").setEphemeral(true).queue();
            return true;
        }
        String choiceKey = parts[1];
        try {
            StoryRender render = engine.applyChoice(
                    sessionId,
                    choiceKey,
                    version,
                    e.getUser().getIdLong(),
                    e.getUser().getName(),
                    e.getMember() != null ? e.getMember().getEffectiveName() : e.getUser().getName()
            );
            e.editMessageEmbeds(render.getEmbed()).setComponents(render.getRows()).queue();
            if (render.getRecap() != null && e.getGuild() != null) {
                publishRecap(e.getGuild().getIdLong(), render.getRecap(), e.getJDA()::getTextChannelById);
            }
        } catch (StoryGameException ex) {
            e.reply(ex.getMessage()).setEphemeral(true).queue();
        }
        return true;
    }

    private void publishRecap(long guildId, StoryRecap recap, java.util.function.LongFunction<TextChannel> channelResolver) {
        configService.find(guildId).ifPresent(config -> {
            TextChannel channel = channelResolver.apply(config.getStoryChannelId());
            if (channel != null) {
                channel.sendMessage(recap.getMarkdown()).queue();
            }
        });
    }
}
