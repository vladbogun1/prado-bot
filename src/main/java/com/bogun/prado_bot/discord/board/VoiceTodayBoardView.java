package com.bogun.prado_bot.discord.board;

import com.bogun.prado_bot.domain.VoiceBoard;
import com.bogun.prado_bot.service.VoiceLeaderboardService;
import com.bogun.prado_bot.service.VoiceTrackingService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class VoiceTodayBoardView implements BoardView {

    private final VoiceLeaderboardService leaderboard;
    private final VoiceTrackingService tracking;

    @Override
    public String key() {
        return "voice-today";
    }

    @Override
    public MessageEditData render(JDA jda, VoiceBoard board) {
        var guildId = board.getGuildId();
        var limit = board.getLineLimit();

        var rows = leaderboard.getTodayTop(guildId, limit);

        var nowMap = tracking.snapshotNow(guildId);

        var sb = new StringBuilder();
        int i = 1;

        for (var r : rows) {
            var now = nowMap.get(r.userId());

            boolean inVoice = now != null && now.inVoice();
            String online = inVoice ? "🟢" : "⭕";

            String voiceStateEmoji;

            if (!inVoice) {
                voiceStateEmoji = "📴";
            } else {
                voiceStateEmoji = voiceEmoji(now.muted(), now.deafened(), now.suppressed());
            }

            sb.append(i++).append(") ")
                    .append(online).append(" ")
                    .append("[🪙: `").append(r.points()).append("`] ")
                    .append(" [⏱️: `").append(fmtHms(r.totalSecondsToday())).append("`]")
                    .append("<@").append(r.userId()).append("> ")
                    .append(voiceStateEmoji).append(" ")
                    .append("\n");
        }

        var tz = ZoneId.of("Asia/Tbilisi");
        var today = LocalDate.now(tz);

        var embed = new EmbedBuilder()
                .setTitle("🎧 Voice статистика за " + today)
                .setDescription(sb.isEmpty() ? "Пока нет данных за сегодня." : sb.toString())
                .setFooter("обновлено: " + Instant.now().atZone(tz).toLocalTime().withNano(0))
                .build();

        return MessageEditData.fromEmbeds(embed);
    }

    private static String voiceEmoji(boolean muted, boolean deafened, boolean suppressed) {
        if (suppressed) return "📛:mute:";
        if (muted && deafened) return ":zipper_mouth::mute:";
        if (muted) return ":zipper_mouth:";
        if (deafened) return ":mute:";
        return ":loud_sound:";
    }

    private static String fmtHms(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
