package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameNodeTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameNodeTransitionRepository extends JpaRepository<GameNodeTransition, Long> {
    List<GameNodeTransition> findAllByFromNodeKey(String fromNodeKey);
}
