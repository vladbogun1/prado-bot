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

        var sb = VoiceBoardFormatter.formatRows(rows, nowMap);

        var tz = ZoneId.of("Asia/Tbilisi");
        var today = LocalDate.now(tz);

        var embed = new EmbedBuilder()
                .setTitle("🎧 Voice статистика за " + today)
                .setDescription(sb.isEmpty() ? "Пока нет данных за сегодня." : sb)
                .setFooter("обновлено: " + Instant.now().atZone(tz).toLocalTime().withNano(0))
                .build();

        return MessageEditData.fromEmbeds(embed);
    }

    
}
