package com.bogun.prado_bot.discord.game;

import com.bogun.prado_bot.game.NarratorResponse;
import com.bogun.prado_bot.service.game.GameMissionService;
import com.bogun.prado_bot.service.game.MissionStartResult;
import com.bogun.prado_bot.service.game.MissionStepResult;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@RequiredArgsConstructor
public class PradoGameHandler {

    private static final String GAME_COMMAND = "prado_game";
    private static final String GAME_BUTTON_PREFIX = "prado-game:";

    private final GameMissionService missionService;

    public boolean onSlash(SlashCommandInteractionEvent e) {
        if (!GAME_COMMAND.equals(e.getName())) {
            return false;
        }

        if (e.getGuild() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return true;
        }

        var existing = missionService.findActiveMission(e.getGuild().getIdLong(), e.getUser().getIdLong());
        if (existing.isPresent()) {
            e.reply("У тебя уже идёт миссия. Заверши её, прежде чем начинать новую.").setEphemeral(true).queue();
            return true;
        }

        e.deferReply(true).queue(hook -> CompletableFuture.supplyAsync(() -> missionService.startMission(
                        e.getGuild().getIdLong(),
                        e.getChannel().getIdLong(),
                        e.getUser().getIdLong()
                ))
                .thenAccept(startResult -> {
                    NarratorResponse response = startResult.response();
                    String content = formatScene(response, 1);
                    List<Button> buttons = buildButtons(response, startResult.mission().getId());
                    hook.editOriginal(content).setComponents(ActionRow.of(buttons)).queue();
                })
                .exceptionally(ex -> {
                    hook.editOriginal("Не удалось начать миссию. Попробуй ещё раз.").setComponents().queue();
                    return null;
                }));
        return true;
    }

    public boolean onButton(ButtonInteractionEvent e) {
        String id = e.getComponentId();
        if (!id.startsWith(GAME_BUTTON_PREFIX)) {
            return false;
        }
        String[] parts = id.substring(GAME_BUTTON_PREFIX.length()).split(":", 2);
        if (parts.length != 2) {
            e.reply("Не удалось обработать кнопку.").setEphemeral(true).queue();
            return true;
        }

        long missionId;
        try {
            missionId = Long.parseLong(parts[0]);
        } catch (NumberFormatException ex) {
            e.reply("Не удалось обработать кнопку.").setEphemeral(true).queue();
            return true;
        }

        String choiceId = parts[1];
        e.deferEdit().queue(hook -> CompletableFuture.supplyAsync(() -> missionService.applyChoice(
                        missionId,
                        e.getUser().getIdLong(),
                        choiceId
                ))
                .thenAccept(result -> {
                    if (result.completed()) {
                        String finishText = result.mission().getStatus().name().equals("SUCCESS")
                                ? "Миссия завершена. Монеты зачислены: " + result.coinsAwarded() + ". Отчёт улетел в общий чат."
                                : "Миссия провалена. Монеты не зачислены. Отчёт улетел в общий чат.";
                        hook.editOriginal(finishText).setComponents().queue();
                        sendRecap(e.getChannel(), result.recap());
                    } else {
                        var mission = result.mission();
                        String content = formatScene(result.response(), mission.getStepIndex() + 1);
                        List<Button> buttons = buildButtons(result.response(), mission.getId());
                        hook.editOriginal(content).setComponents(ActionRow.of(buttons)).queue();
                    }
                })
                .exceptionally(ex -> {
                    String message = "Не удалось обработать ход. Попробуй ещё раз.";
                    Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                    if (cause instanceof IllegalStateException) {
                        message = "Ход не принят: " + cause.getMessage();
                    }
                    hook.editOriginal(message).setComponents().queue();
                    return null;
                }));
        return true;
    }

    private List<Button> buildButtons(NarratorResponse response, long missionId) {
        return response.choices().stream()
                .map(choice -> Button.primary(GAME_BUTTON_PREFIX + missionId + ":" + choice.id(), choice.label()))
                .toList();
    }

    private String formatScene(NarratorResponse response, int stepNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append(response.scene().title()).append("\n")
                .append(response.scene().text()).append("\n")
                .append("Локация: ").append(response.scene().locationLine()).append("\n")
                .append("Шаг ").append(stepNumber);

        if (response.choices() != null && !response.choices().isEmpty()) {
            builder.append("\n\nВарианты:");
            for (NarratorResponse.Choice choice : response.choices()) {
                builder.append("\n").append(choice.id()).append(") ").append(choice.label());
                if (choice.note() != null && !choice.note().isBlank()) {
                    builder.append(" — ").append(choice.note());
                }
            }
        }
        return builder.toString();
    }

    private void sendRecap(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, String recap) {
        if (recap == null || recap.isBlank()) {
            channel.sendMessage("Итог миссии готов, но рассказ не удалось сформировать.").queue();
            return;
        }
        int max = 1900;
        int index = 0;
        while (index < recap.length()) {
            int end = Math.min(recap.length(), index + max);
            String chunk = recap.substring(index, end);
            channel.sendMessage(chunk).queue();
            index = end;
        }
    }

}
