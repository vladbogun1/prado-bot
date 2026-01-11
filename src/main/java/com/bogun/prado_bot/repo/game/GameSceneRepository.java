package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameScene;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameSceneRepository extends JpaRepository<GameScene, Long> {
    List<GameScene> findAllByMissionTypeKeyAndLocationKey(String missionTypeKey, String locationKey);
}
