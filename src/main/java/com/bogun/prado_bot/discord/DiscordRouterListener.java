package com.bogun.prado_bot.discord;

import com.bogun.prado_bot.discord.board.VoiceBoardFormatter;
import com.bogun.prado_bot.config.PradoGameProperties;
import com.bogun.prado_bot.service.game.GameSessionService;
import com.bogun.prado_bot.service.VoiceBoardService;
import com.bogun.prado_bot.service.VoiceLeaderboardService;
import com.bogun.prado_bot.service.VoiceTrackingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceSelfDeafenEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceSelfMuteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceSuppressEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.Objects;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscordRouterListener implements EventListener {

    private final VoiceTrackingService tracking;
    private final VoiceBoardService boards;
    private final VoiceLeaderboardService leaderboard;
    private final GameSessionService gameSessionService;
    private final PradoGameProperties pradoGameProperties;

    private static final String VOICE_INFO_COMMAND = "voice_info";
    private static final String VOICE_INFO_BUTTON_PREFIX = "voice-info:";
    private static final String PRADO_GAME_COMMAND = "prado_game";

    private enum VoiceInfoPeriod {
        DAY,
        WEEK,
        MONTH
    }

    @Value("${app.timezone:UTC}")
    private String appTimezone;

    @Override
    public void onEvent(GenericEvent event) {

        // 1) Ready -> register commands
        if (event instanceof ReadyEvent e) {
            e.getJDA().updateCommands().addCommands(
                    Commands.slash("voiceboard", "Создать/обновить табло voice-статистики")
                            .addOption(OptionType.INTEGER, "refresh", "сек между обновлениями", false)
                            .addOption(OptionType.INTEGER, "limit", "сколько строк показывать", false),
                    Commands.slash(VOICE_INFO_COMMAND, "Показать voice-статистику по дням"),
                    Commands.slash(PRADO_GAME_COMMAND, "Запустить мини-игру Prado Job")
                            .addOption(OptionType.STRING, "mode", "start/status/quit", false,
                                    option -> option.addChoices(
                                            new net.dv8tion.jda.api.interactions.commands.Command.Choice("start", "start"),
                                            new net.dv8tion.jda.api.interactions.commands.Command.Choice("status", "status"),
                                            new net.dv8tion.jda.api.interactions.commands.Command.Choice("quit", "quit")
                                    )
                            )
            ).queue();
            initialVoicesScan(e);
            return;
        }

        // 2) Slash commands
        if (event instanceof SlashCommandInteractionEvent e) {
            onSlash(e);
            return;
        }

        if (event instanceof ButtonInteractionEvent e) {
            onButton(e);
            return;
        }

        // 3) Join/Leave/Move -> unified event
        if (event instanceof GuildVoiceUpdateEvent e) {
            onVoiceUpdate(e);
            return;
        }

        // 4) Pause-state changes (mute/deafen/suppress)
        //    ВАЖНО: в JDA 6.x это отдельные события, join/leave они не покрывают.
        if (event instanceof GuildVoiceMuteEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceDeafenEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceSelfMuteEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceSelfDeafenEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceGuildMuteEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceGuildDeafenEvent e) {
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
            return;
        }
        if (event instanceof GuildVoiceSuppressEvent e) {
            // suppress обычно про Stage (не может говорить). Если хочешь — тоже считаем как паузу:
            onPauseLikeEvent(e.getGuild().getIdLong(), e.getMember().getIdLong(), e.getMember());
        }
    }

    private void initialVoicesScan(GenericEvent event) {
        event.getJDA().getGuilds().forEach(guild -> {
            Instant now = Instant.now();
            ZoneId zone = ZoneId.of(appTimezone);
            Instant boundary = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant();
            tracking.closeActiveSessionsBefore(guild.getIdLong(), boundary);

            guild.getVoiceStates().forEach(voiceState -> {
                var member = voiceState.getMember();
                var userId = member.getIdLong();
                var username = member.getUser().getName();
                var memberName = member.getEffectiveName();
                var joined = voiceState.getChannel();

                if (isBot(member)) return;

                tracking.ensureSessionAtBoundary(
                        guild.getIdLong(),
                        userId,
                        username,
                        memberName,
                        Objects.requireNonNull(joined).getIdLong(),
                        joined.getName(),
                        flags(member),
                        boundary);
            });
        });
    }

    private boolean isBot(Member member) {
        return member.getUser().isBot();
    }

    private void onVoiceUpdate(GuildVoiceUpdateEvent e) {
        var guildId = e.getGuild().getIdLong();
        var member = e.getMember();
        if (isBot(member)) return;
        var userId = member.getIdLong();
        var username = member.getUser().getName();
        var memberName = member.getEffectiveName();

        var joined = e.getChannelJoined(); // не null, если вошёл/перешёл
        var left = e.getChannelLeft();     // не null, если вышел/перешёл

        if (left == null && joined != null) {
            tracking.onJoin(
                    guildId, userId,
                    username, memberName,
                    joined.getIdLong(),
                    joined.getName(),
                    flags(member)
            );
            return;
        }

        if (left != null && joined == null) {
            tracking.onLeave(guildId, userId);
            return;
        }

        if (left != null) {
            tracking.onMove(
                    guildId, userId,
                    joined.getIdLong(), joined.getName(),
                    flags(member)
            );
        }
    }

    private void onPauseLikeEvent(long guildId, long userId, net.dv8tion.jda.api.entities.Member member) {
        if(isBot(member)) return;
        tracking.onVoiceStateChange(guildId, userId, flags(member));
    }

    private void onSlash(SlashCommandInteractionEvent e) {
        if ("voiceboard".equals(e.getName())) {
            onVoiceBoardSlash(e);
            return;
        }
        if (VOICE_INFO_COMMAND.equals(e.getName())) {
            onVoiceInfoSlash(e);
            return;
        }
        if (PRADO_GAME_COMMAND.equals(e.getName())) {
            onPradoGameSlash(e);
        }
    }

    private void onVoiceBoardSlash(SlashCommandInteractionEvent e) {

        if (e.getGuild() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return;
        }

        Integer refresh = e.getOption("refresh") != null ? e.getOption("refresh").getAsInt() : null;
        Integer limit = e.getOption("limit") != null ? e.getOption("limit").getAsInt() : null;

        e.reply("Создаю табло…").setEphemeral(true).queue();

        e.getChannel().sendMessage("🎧 Табло загружается…").queue(msg -> boards.registerBoard(
                e.getGuild().getIdLong(),
                e.getChannel().getIdLong(),
                msg.getIdLong(),
                msg.getJumpUrl(),
                refresh,
                limit,
                "voice-today"
        ));
    }

    private void onVoiceInfoSlash(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return;
        }

        ZoneId zone = ZoneId.of(appTimezone);
        LocalDate today = LocalDate.now(zone);
        var period = VoiceInfoPeriod.DAY;
        var embed = buildVoiceInfoEmbed(e.getGuild().getIdLong(), today, period, 20, zone);
        var buttons = buildVoiceInfoButtons(today, today, period, e.getUser().getIdLong());

        e.replyEmbeds(embed).setComponents(ActionRow.of(buttons)).setEphemeral(true).queue();
    }

    private void onButton(ButtonInteractionEvent e) {
        String id = e.getComponentId();
        if (id.startsWith(VOICE_INFO_BUTTON_PREFIX)) {
            handleVoiceInfoButton(e, id);
            return;
        }
        if (id.startsWith(gameSessionService.buttonPrefix())) {
            handlePradoGameButton(e, id);
        }
    }

    private void handleVoiceInfoButton(ButtonInteractionEvent e, String id) {
        String payload = id.substring(VOICE_INFO_BUTTON_PREFIX.length());
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) return;

        long ownerId;
        VoiceInfoPeriod period;
        LocalDate date;
        try {
            ownerId = Long.parseLong(parts[0]);
            period = VoiceInfoPeriod.valueOf(parts[1]);
            date = LocalDate.parse(parts[2]);
        } catch (RuntimeException ex) {
            e.reply("Не удалось обработать запрос.").setEphemeral(true).queue();
            return;
        }

        if (e.getUser().getIdLong() != ownerId) {
            e.reply("Эта панель доступна только автору команды.").setEphemeral(true).queue();
            return;
        }

        if (e.getGuild() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return;
        }

        ZoneId zone = ZoneId.of(appTimezone);
        LocalDate today = LocalDate.now(zone);

        var embed = buildVoiceInfoEmbed(e.getGuild().getIdLong(), date, period, 20, zone);
        var buttons = buildVoiceInfoButtons(date, today, period, ownerId);

        e.editMessageEmbeds(embed).setComponents(ActionRow.of(buttons)).queue();
    }

    private void handlePradoGameButton(ButtonInteractionEvent e, String id) {
        String payload = id.substring(gameSessionService.buttonPrefix().length());
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            e.reply("Кнопка устарела.").setEphemeral(true).queue();
            return;
        }

        long sessionId;
        int version;
        try {
            sessionId = Long.parseLong(parts[0]);
            version = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            e.reply("Кнопка устарела.").setEphemeral(true).queue();
            return;
        }

        String actionKey = parts[1];
        var member = e.getMember();
        if (member == null) {
            e.reply("Не удалось определить пользователя.").setEphemeral(true).queue();
            return;
        }

        GameSessionService.GameView view;
        try {
            view = gameSessionService.applyAction(
                    sessionId,
                    e.getUser().getIdLong(),
                    actionKey,
                    version,
                    e.getUser().getName(),
                    member.getEffectiveName()
            );
        } catch (RuntimeException ex) {
            e.reply("Не удалось обработать ход.").setEphemeral(true).queue();
            return;
        }

        if (view.errorMessage() != null) {
            e.reply(view.errorMessage()).setEphemeral(true).queue();
            return;
        }

        e.editMessageEmbeds(view.embed()).setComponents(view.components()).queue();

        if (view.ended() && view.publicSummary() != null && view.publicChannelId() != null) {
            var channel = e.getJDA().getTextChannelById(view.publicChannelId());
            if (channel != null) {
                channel.sendMessage("<@" + e.getUser().getId() + "> завершил миссию.\n" + view.publicSummary()).queue();
            }
        }
    }

    private void onPradoGameSlash(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) {
            e.reply("Эта команда работает только на сервере.").setEphemeral(true).queue();
            return;
        }
        String mode = e.getOption("mode") != null ? e.getOption("mode").getAsString() : "start";

        var member = e.getMember();
        if (member == null) {
            e.reply("Не удалось определить пользователя.").setEphemeral(true).queue();
            return;
        }

        long guildId = e.getGuild().getIdLong();
        long userId = e.getUser().getIdLong();
        long channelId = e.getChannel().getIdLong();
        String username = e.getUser().getName();
        String memberName = member.getEffectiveName();

        GameSessionService.GameView view = switch (mode) {
            case "quit" -> gameSessionService.quitActiveSession(guildId, userId);
            case "status" -> gameSessionService.status(guildId, userId);
            default -> gameSessionService.startSession(guildId, userId, channelId, username, memberName);
        };

        if (view.errorMessage() != null) {
            e.reply(view.errorMessage()).setEphemeral(true).queue();
            return;
        }

        e.replyEmbeds(view.embed()).setComponents(view.components()).setEphemeral(pradoGameProperties.isEphemeral()).queue();

        if (view.ended() && view.publicSummary() != null && view.publicChannelId() != null) {
            var channel = e.getJDA().getTextChannelById(view.publicChannelId());
            if (channel != null) {
                channel.sendMessage("<@" + e.getUser().getId() + "> завершил миссию.\n" + view.publicSummary()).queue();
            }
        }
    }

    private net.dv8tion.jda.api.entities.MessageEmbed buildVoiceInfoEmbed(long guildId, LocalDate date,
                                                                         VoiceInfoPeriod period,
                                                                         int limit, ZoneId zone) {
        var range = rangeFor(date, period, zone);
        boolean includeActive = isRangeIncludingNow(range, zone);
        var rows = leaderboard.getTopForRange(guildId, range.start(), range.end(), limit, includeActive);
        var nowMap = tracking.snapshotNow(guildId);
        var description = VoiceBoardFormatter.formatRows(rows, nowMap);

        String title = switch (period) {
            case DAY -> "🎧 Voice статистика за " + date;
            case WEEK -> "🎧 Voice статистика за неделю " + range.startDate() + " — " + range.endDate();
            case MONTH -> "🎧 Voice статистика за " + date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        };

        return new net.dv8tion.jda.api.EmbedBuilder()
                .setTitle(title)
                .setDescription(description.isEmpty()
                        ? emptyMessageFor(period, date, range)
                        : description)
                .setFooter("обновлено: " + Instant.now().atZone(zone).toLocalTime().withNano(0))
                .build();
    }

    private List<Button> buildVoiceInfoButtons(LocalDate date, LocalDate today,
                                               VoiceInfoPeriod period, long ownerId) {
        var prevDate = switch (period) {
            case DAY -> date.minusDays(1);
            case WEEK -> date.minusWeeks(1);
            case MONTH -> date.minusMonths(1);
        };
        var nextDate = switch (period) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };

        var prev = Button.primary(voiceInfoPayload(ownerId, period, prevDate), "⬅️ Назад");
        var next = Button.primary(voiceInfoPayload(ownerId, period, nextDate), "Вперёд ➡️");
        var toggle = buildToggleButton(ownerId, period, date);

        if (nextDate.isAfter(today)) {
            next = next.asDisabled();
        }

        return List.of(prev, toggle, next);
    }

    private Button buildToggleButton(long ownerId, VoiceInfoPeriod period, LocalDate date) {
        if (period == VoiceInfoPeriod.MONTH) {
            return Button.secondary(
                    voiceInfoPayload(ownerId, VoiceInfoPeriod.WEEK, date),
                    "Неделя"
            );
        }
        if (period == VoiceInfoPeriod.WEEK) {
            return Button.secondary(
                    voiceInfoPayload(ownerId, VoiceInfoPeriod.MONTH, date),
                    "Месяц"
            );
        }
        return Button.secondary(
                voiceInfoPayload(ownerId, VoiceInfoPeriod.WEEK, date),
                "Неделя"
        );
    }

    private String voiceInfoPayload(long ownerId, VoiceInfoPeriod period, LocalDate date) {
        return VOICE_INFO_BUTTON_PREFIX + ownerId + ":" + period + ":" + date;
    }

    private VoiceInfoRange rangeFor(LocalDate date, VoiceInfoPeriod period, ZoneId zone) {
        return switch (period) {
            case DAY -> {
                LocalDate start = date;
                LocalDate end = date.plusDays(1);
                yield new VoiceInfoRange(start, end.minusDays(1),
                        start.atStartOfDay(zone).toInstant(),
                        end.atStartOfDay(zone).toInstant());
            }
            case WEEK -> {
                LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = start.plusWeeks(1);
                yield new VoiceInfoRange(start, end.minusDays(1),
                        start.atStartOfDay(zone).toInstant(),
                        end.atStartOfDay(zone).toInstant());
            }
            case MONTH -> {
                LocalDate start = date.withDayOfMonth(1);
                LocalDate end = start.plusMonths(1);
                yield new VoiceInfoRange(start, end.minusDays(1),
                        start.atStartOfDay(zone).toInstant(),
                        end.atStartOfDay(zone).toInstant());
            }
        };
    }

    private boolean isRangeIncludingNow(VoiceInfoRange range, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        return !today.isBefore(range.startDate()) && !today.isAfter(range.endDate());
    }

    private String emptyMessageFor(VoiceInfoPeriod period, LocalDate date, VoiceInfoRange range) {
        return switch (period) {
            case DAY -> "Пока нет данных за " + date + ".";
            case WEEK -> "Пока нет данных за неделю " + range.startDate() + " — " + range.endDate() + ".";
            case MONTH -> "Пока нет данных за " + date.getYear() + "-" + String.format("%02d", date.getMonthValue()) + ".";
        };
    }

    private record VoiceInfoRange(LocalDate startDate, LocalDate endDate, Instant start, Instant end) {}

    private VoiceTrackingService.VoiceFlags flags(Member member) {
        var vs = member.getVoiceState();
        if (vs == null) return new VoiceTrackingService.VoiceFlags(false, false, false);
        return new VoiceTrackingService.VoiceFlags(
                vs.isMuted(),
                vs.isDeafened(),
                vs.isSuppressed()
        );
    }
}
