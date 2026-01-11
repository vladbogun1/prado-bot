package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameLocationRepository extends JpaRepository<GameLocation, Long> {
    Optional<GameLocation> findByKey(String key);
}
