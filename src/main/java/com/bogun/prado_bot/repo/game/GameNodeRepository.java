package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameNodeRepository extends JpaRepository<GameNode, Long> {
    Optional<GameNode> findByKey(String key);

    List<GameNode> findAllByMissionTypeKey(String missionTypeKey);

    Optional<GameNode> findFirstByMissionTypeKeyAndStartIsTrue(String missionTypeKey);
}
