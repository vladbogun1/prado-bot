package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameItemRepository extends JpaRepository<GameItem, Long> {
    Optional<GameItem> findByKey(String key);
}
