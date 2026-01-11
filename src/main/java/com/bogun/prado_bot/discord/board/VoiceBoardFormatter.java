package com.bogun.prado_bot.discord.board;

import com.bogun.prado_bot.service.VoiceLeaderboardService;
import com.bogun.prado_bot.service.VoiceTrackingService;

import java.util.List;
import java.util.Map;

public final class VoiceBoardFormatter {

    private VoiceBoardFormatter() {}

    public static String formatRows(List<VoiceLeaderboardService.TodayRow> rows,
                                    Map<Long, VoiceTrackingService.NowState> nowMap) {
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

        return sb.toString();
    }

    public static String voiceEmoji(boolean muted, boolean deafened, boolean suppressed) {
        if (suppressed) return "📛:mute:";
        if (muted && deafened) return ":zipper_mouth::mute:";
        if (muted) return ":zipper_mouth:";
        if (deafened) return ":mute:";
        return ":loud_sound:";
    }

    public static String fmtHms(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
