package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameEventLogRepository extends JpaRepository<GameEventLog, Long> {
    List<GameEventLog> findAllBySessionIdOrderByStepAsc(Long sessionId);
}
