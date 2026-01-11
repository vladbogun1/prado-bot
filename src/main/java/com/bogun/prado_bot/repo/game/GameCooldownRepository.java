package com.bogun.prado_bot.repo.game;

import com.bogun.prado_bot.domain.game.GameCooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameCooldownRepository extends JpaRepository<GameCooldown, Long> {
    Optional<GameCooldown> findByGuildIdAndUserId(Long guildId, Long userId);
}
