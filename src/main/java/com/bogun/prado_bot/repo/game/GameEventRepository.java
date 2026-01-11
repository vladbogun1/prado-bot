package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    List<GameEvent> findAllByMissionTypeKeyIsNullOrMissionTypeKey(String missionTypeKey);
}
