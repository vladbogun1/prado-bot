package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameSession;
import com.bogun.prado_bot.domain.game.GameSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findFirstByGuildIdAndUserIdAndStatus(Long guildId, Long userId, GameSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSession s where s.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") Long id);

    List<GameSession> findAllByStatusAndExpiresAtBefore(GameSessionStatus status, Instant expiresAt);
}
