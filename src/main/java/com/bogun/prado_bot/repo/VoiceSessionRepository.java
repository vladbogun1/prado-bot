package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VoiceSessionRepository extends JpaRepository<VoiceSession, Long> {

    Optional<VoiceSession> findFirstByGuildIdAndUserIdAndEndedAtIsNullOrderByStartedAtDesc(Long guildId, Long userId);

    List<VoiceSession> findAllByGuildIdAndEndedAtIsNull(Long guildId);

    List<VoiceSession> findAllByGuildIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(Long guildId, Instant start, Instant end);

    interface UserRangeAgg {
        Long getUserId();
        long getSessions();
        long getSeconds();
    }

    @Query("""
            select
                s.userId as userId,
                count(s) as sessions,
                coalesce(sum(s.activeSeconds), 0) as seconds
            from VoiceSession s
            where s.guildId = :guildId
              and s.startedAt >= :start and s.startedAt < :end
            group by s.userId
           """)
    List<UserRangeAgg> aggregateByUserForRange(
            @Param("guildId") Long guildId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    List<VoiceSession> findAllByGuildIdAndEndedAtIsNullAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            Long guildId, Instant start, Instant end
    );

    @Query("""
            select coalesce(sum(s.activeSeconds), 0)
            from VoiceSession s
            where s.guildId = :guildId
              and s.userId = :userId
              and s.startedAt >= :start and s.startedAt < :end
           """)
    long sumActiveSecondsForUser(
            @Param("guildId") Long guildId,
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
