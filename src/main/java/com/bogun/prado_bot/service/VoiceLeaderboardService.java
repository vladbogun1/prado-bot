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
        ZoneId zone = ZoneId.of(appTimezone);
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, zone);
        return getTopForDate(guildId, today, limit, now, zone);
    }

    @Transactional(readOnly = true)
    public List<TodayRow> getTopForDate(long guildId, LocalDate date, int limit) {
        ZoneId zone = ZoneId.of(appTimezone);
        Instant now = Instant.now();
        Range range = dayRange(zone, date);
        boolean includeActive = date.equals(LocalDate.ofInstant(now, zone));
        return getTopForRange(guildId, range.start(), range.end(), limit, includeActive, now);
    }

    /* ===================== helpers ===================== */

    @Transactional(readOnly = true)
    public List<TodayRow> getTopForRange(long guildId, Instant start, Instant end, int limit, boolean includeActive) {
        return getTopForRange(guildId, start, end, limit, includeActive, Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<TodayRow> getUserForRange(long guildId, long userId, Instant start, Instant end,
                                              boolean includeActive) {
        Instant now = Instant.now();
        var totals = sessionRepo.aggregateForUserRange(guildId, userId, start, end);
        long sessions = totals != null ? totals.getSessions() : 0;
        long totalSeconds = totals != null ? totals.getSeconds() : 0;

        if (includeActive) {
            List<VoiceSession> active =
                    sessionRepo.findAllByGuildIdAndUserIdAndEndedAtIsNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                            guildId, userId, start, end
                    );
            for (VoiceSession s : active) {
                if (s.isPaused()) continue;
                Instant last = s.getLastStateAt();
                if (last == null) continue;
                long extra = Duration.between(last, now).getSeconds();
                if (extra < 0) extra = 0;
                totalSeconds += extra;
            }
        }

        var user = userRepo.findById(new VoiceUser.VoiceUserId(guildId, userId));
        long points = user.map(VoiceUser::getPoints).orElse(0L);

        if (sessions == 0 && totalSeconds == 0) {
            return Optional.empty();
        }

        return Optional.of(new TodayRow(userId, points, (int) sessions, totalSeconds));
    }

    private List<TodayRow> getTopForDate(long guildId, LocalDate date, int limit, Instant now, ZoneId zone) {
        int lim = limit > 0 ? limit : 20;

        Range r = dayRange(zone, date);
        Instant start = r.start();
        Instant end = r.end();
        boolean includeActive = date.equals(LocalDate.ofInstant(now, zone));
        return getTopForRange(guildId, start, end, lim, includeActive, now);
    }

    private List<TodayRow> getTopForRange(long guildId, Instant start, Instant end, int limit,
                                          boolean includeActive, Instant now) {
        int lim = limit > 0 ? limit : 20;

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

        if (includeActive) {
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

    private static Range dayRange(ZoneId zone, LocalDate date) {
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
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
