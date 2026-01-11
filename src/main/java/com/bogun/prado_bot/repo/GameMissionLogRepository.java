package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.game.GameMissionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameMissionLogRepository extends JpaRepository<GameMissionLog, Long> {

    List<GameMissionLog> findByMissionIdOrderByStepIndexAscIdAsc(long missionId);
}
