package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameActionRepository extends JpaRepository<GameAction, Long> {
    Optional<GameAction> findByKey(String key);

    List<GameAction> findAllByMissionTypeKeyIsNullOrMissionTypeKey(String missionTypeKey);
}
