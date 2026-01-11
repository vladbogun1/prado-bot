package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameMissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameMissionTypeRepository extends JpaRepository<GameMissionType, Long> {
    Optional<GameMissionType> findByKey(String key);
}
