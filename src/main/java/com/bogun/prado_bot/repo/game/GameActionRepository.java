package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameActionRepository extends JpaRepository<GameAction, Long> {
    Optional<GameAction> findByKey(String key);

    List<GameAction> findAllByMissionTypeKeyIsNullOrMissionTypeKey(String missionTypeKey);

    @Query("""
            select a from GameAction a
            where (a.missionTypeKey is null or a.missionTypeKey = :missionTypeKey)
              and (a.nodeKey is null or a.nodeKey = :nodeKey)
           """)
    List<GameAction> findAllForContext(
            @Param("missionTypeKey") String missionTypeKey,
            @Param("nodeKey") String nodeKey
    );
}
