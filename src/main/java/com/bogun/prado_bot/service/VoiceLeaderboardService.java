package com.bogun.prado_bot.service;

import com.bogun.prado_bot.domain.VoiceSession;
import com.bogun.prado_bot.domain.VoiceUser;
import com.bogun.prado_bot.repo.VoiceSessionRepository;
import com.bogun.prado_bot.repo.VoiceUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceLeaderboardService {

    private final VoiceSessionRepository sessionRepo;
    private final VoiceUserRepository userRepo;

    @Value("${app.timezone:UTC}")
    private String appTimezone;

    public record TodayRow(
            long userId,
            long points,
            int sessionsToday,
            long totalSecondsToday
    ) {}

    @Transactional(readOnly = true)
    public List<TodayRow> getTodayTop(long guildId, int limit) {
        int lim = limit > 0 ? limit : 20;

        ZoneId zone = ZoneId.of(appTimezone);
        Instant now = Instant.now();

        Range r = todayRange(zone, now);
        Instant start = r.start();
        Instant end = r.end();

        List<VoiceSessionRepository.UserRangeAgg> agg =
                sessionRepo.aggregateByUserForRange(guildId, start, end);

        Map<Long, Mutable> map = new HashMap<>();

        for (var a : agg) {
            long userId = a.getUserId();
            map.put(userId, new Mutable(userId, 0, (int) a.getSessions(), a.getSeconds()));
        }

        if (!map.isEmpty()) {
            List<VoiceUser.VoiceUserId> ids = map.keySet().stream()
                    .map(uid -> new VoiceUser.VoiceUserId(guildId, uid))
                    .collect(Collectors.toList());

            userRepo.findAllById(ids).forEach(u -> {
                long uid = u.getId().getUserId();

                map.computeIfAbsent(uid, x -> new Mutable(x, 0, 0, 0)).points = u.getPoints();
            });
        }

        List<VoiceSession> activeToday =
                sessionRepo.findAllByGuildIdAndEndedAtIsNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                        guildId, start, end
                );

        for (VoiceSession s : activeToday) {
            if (s.isPaused()) continue;
            Instant last = s.getLastStateAt();
            if (last == null) continue;

            long extra = Duration.between(last, now).getSeconds();
            if (extra < 0) extra = 0;

            map.computeIfAbsent(s.getUserId(), uid -> new Mutable(uid, 0, 0, 0))
                    .totalSeconds += extra;
        }

        return map.values().stream()
                .sorted(Comparator
                        .comparingLong(Mutable::totalSeconds)
                        .thenComparingLong(Mutable::points).reversed()
                        .thenComparingLong(Mutable::userId))
                .limit(lim)
                .map(m -> new TodayRow(m.userId, m.points, m.sessionsToday, m.totalSeconds))
                .toList();
    }

    /* ===================== helpers ===================== */

    private static Range todayRange(ZoneId zone, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return new Range(start, end);
    }

    private record Range(Instant start, Instant end) {}

    private static final class Mutable {
        private final long userId;
        private long points;
        private int sessionsToday;
        private long totalSeconds;

        private Mutable(long userId, long points, int sessionsToday, long totalSeconds) {
            this.userId = userId;
            this.points = points;
            this.sessionsToday = sessionsToday;
            this.totalSeconds = totalSeconds;
        }

        long userId() { return userId; }
        long points() { return points; }
        long totalSeconds() { return totalSeconds; }
    }
}
