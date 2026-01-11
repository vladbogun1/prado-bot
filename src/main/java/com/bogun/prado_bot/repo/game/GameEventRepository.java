package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    @Query("""
            select e from GameEvent e
            where (e.missionTypeKey is null or e.missionTypeKey = :missionTypeKey)
              and (e.nodeKey is null or e.nodeKey = :nodeKey)
           """)
    List<GameEvent> findAllForContext(
            @Param("missionTypeKey") String missionTypeKey,
            @Param("nodeKey") String nodeKey
    );
}
