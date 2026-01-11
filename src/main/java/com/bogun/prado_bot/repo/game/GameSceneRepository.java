package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameSceneRepository extends JpaRepository<GameScene, Long> {
    @Query("""
            select s from GameScene s
            where s.missionTypeKey = :missionTypeKey
              and s.locationKey = :locationKey
              and (s.nodeKey is null or s.nodeKey = :nodeKey)
           """)
    List<GameScene> findAllForContext(
            @Param("missionTypeKey") String missionTypeKey,
            @Param("locationKey") String locationKey,
            @Param("nodeKey") String nodeKey
    );
}
