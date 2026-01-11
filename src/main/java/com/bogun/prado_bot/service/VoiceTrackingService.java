package com.bogun.prado_bot.service;

import com.bogun.prado_bot.domain.VoiceSession;
import com.bogun.prado_bot.domain.VoiceUser;
import com.bogun.prado_bot.domain.VoiceUser.VoiceUserId;
import com.bogun.prado_bot.repo.VoiceSessionRepository;
import com.bogun.prado_bot.repo.VoiceUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceTrackingService {

    private final VoiceUserRepository userRepo;
    private final VoiceSessionRepository sessionRepo;

    public record NowState(
            long guildId,
            long userId,
            long channelId,
            String channelName,
            boolean muted,
            boolean deafened,
            boolean suppressed
    ) {
        public boolean inVoice() { return true; }
    }

    public record VoiceFlags(boolean muted, boolean deafened, boolean suppressed) {
        public boolean paused() { return muted || deafened || suppressed; }
    }

    public Map<Long, NowState> snapshotNow(long guildId) {
        return sessionRepo.findAllByGuildIdAndEndedAtIsNull(guildId).stream()
                .sorted(Comparator.comparing(VoiceSession::getStartedAt).reversed())
                .map(s -> new NowState(
                        s.getGuildId(),
                        s.getUserId(),
                        s.getVoiceChannelId(),
                        s.getVoiceChannelName(),
                        s.isMuted(),
                        s.isDeafened(),
                        s.isSuppressed()
                ))
                .collect(Collectors.toMap(
                        NowState::userId,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Transactional
    public void onJoin(Long guildId, Long userId, String username, String memberName,
                       Long voiceId, String voiceName, VoiceFlags flags) {

        upsertUser(guildId, userId, username, memberName);

        sessionRepo.findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(guildId, userId)
                .ifPresentOrElse(s -> {
                    // если активная сессия уже есть — просто обновим канал/флаги
                    Instant now = Instant.now();
                    flushActiveSeconds(s, now);
                    s.setVoiceChannelId(voiceId);
                    s.setVoiceChannelName(voiceName);
                    s.setMuted(flags.muted());
                    s.setDeafened(flags.deafened());
                    s.setSuppressed(flags.suppressed());
                    s.setPaused(flags.paused());
                    s.setLastStateAt(now);
                    sessionRepo.save(s);
                }, () -> {
                    Instant now = Instant.now();
                    VoiceSession s = new VoiceSession();
                    s.setGuildId(guildId);
                    s.setUserId(userId);
                    s.setStartedAt(now);
                    s.setEndedAt(null);
                    s.setActiveSeconds(0);
                    s.setMuted(flags.muted());
                    s.setDeafened(flags.deafened());
                    s.setSuppressed(flags.suppressed());
                    s.setPaused(flags.paused());
                    s.setLastStateAt(now);
                    s.setVoiceChannelId(voiceId);
                    s.setVoiceChannelName(voiceName);
                    sessionRepo.save(s);
                });
    }

    @Transactional
    public void onLeave(Long guildId, Long userId) {
        sessionRepo.findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(guildId, userId)
                .ifPresent(s -> closeSession(s, Instant.now()));
    }

    @Transactional
    public void onMove(Long guildId, Long userId, Long newVoiceId, String newVoiceName, VoiceFlags flags) {
        Instant now = Instant.now();

        sessionRepo.findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(guildId, userId)
                .ifPresent(s -> closeSession(s, now));

        VoiceSession s = new VoiceSession();
        s.setGuildId(guildId);
        s.setUserId(userId);
        s.setStartedAt(now);
        s.setEndedAt(null);
        s.setActiveSeconds(0);
        s.setMuted(flags.muted());
        s.setDeafened(flags.deafened());
        s.setSuppressed(flags.suppressed());
        s.setPaused(flags.paused());
        s.setLastStateAt(now);
        s.setVoiceChannelId(newVoiceId);
        s.setVoiceChannelName(newVoiceName);
        sessionRepo.save(s);
    }

    @Transactional
    public void onVoiceStateChange(Long guildId, Long userId, VoiceFlags flags) {
        Instant now = Instant.now();
        sessionRepo.findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(guildId, userId)
                .ifPresent(s -> {
                    flushActiveSeconds(s, now);
                    s.setMuted(flags.muted());
                    s.setDeafened(flags.deafened());
                    s.setSuppressed(flags.suppressed());
                    s.setPaused(flags.paused());
                    s.setLastStateAt(now);
                    sessionRepo.save(s);
                });
    }

    @Transactional
    public void closeActiveSessionsBefore(Long guildId, Instant boundary) {
        sessionRepo.findAllByGuildIdAndEndedAtIsNull(guildId).forEach(s -> {
            if (s.getStartedAt().isBefore(boundary)) {
                closeSession(s, boundary);
            }
        });
    }

    @Transactional
    public void ensureSessionAtBoundary(Long guildId, Long userId, String username, String memberName,
                                        Long voiceId, String voiceName, VoiceFlags flags, Instant boundary) {
        upsertUser(guildId, userId, username, memberName);

        sessionRepo.findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(guildId, userId)
                .ifPresentOrElse(s -> {
                    if (s.getStartedAt().isBefore(boundary)) {
                        closeSession(s, boundary);
                        createSessionAtBoundary(guildId, userId, voiceId, voiceName, flags, boundary);
                    }
                }, () -> createSessionAtBoundary(guildId, userId, voiceId, voiceName, flags, boundary));
    }

    private void createSessionAtBoundary(Long guildId, Long userId, Long voiceId, String voiceName,
                                         VoiceFlags flags, Instant boundary) {
        VoiceSession s = new VoiceSession();
        s.setGuildId(guildId);
        s.setUserId(userId);
        s.setStartedAt(boundary);
        s.setEndedAt(null);
        s.setActiveSeconds(0);
        s.setMuted(flags.muted());
        s.setDeafened(flags.deafened());
        s.setSuppressed(flags.suppressed());
        s.setPaused(flags.paused());
        s.setLastStateAt(boundary);
        s.setVoiceChannelId(voiceId);
        s.setVoiceChannelName(voiceName);
        sessionRepo.save(s);
    }

    private void closeSession(VoiceSession s, Instant endAt) {
        flushActiveSeconds(s, endAt);
        s.setEndedAt(endAt);
        s.setLastStateAt(endAt);
        sessionRepo.save(s);
    }

    private void flushActiveSeconds(VoiceSession s, Instant now) {
        Instant last = s.getLastStateAt();
        if (last == null) {
            s.setLastStateAt(now);
            return;
        }
        if (!s.isPaused() && s.getEndedAt() == null) {
            long delta = Duration.between(last, now).getSeconds();
            if (delta > 0) s.setActiveSeconds(s.getActiveSeconds() + (int) delta);
        }
        s.setLastStateAt(now);
    }

    public void upsertUser(Long guildId, Long userId, String username, String memberName) {
        Instant now = Instant.now();
        VoiceUserId id = new VoiceUserId(guildId, userId);

        VoiceUser u = userRepo.findById(id).orElseGet(() -> {
            VoiceUser nu = new VoiceUser();
            nu.setId(id);
            nu.setPoints(0);
            nu.setCreatedAt(now);
            return nu;
        });

        u.setUsername(username);
        u.setMemberName(memberName);
        u.setUpdatedAt(now);

        userRepo.save(u);
    }
}
