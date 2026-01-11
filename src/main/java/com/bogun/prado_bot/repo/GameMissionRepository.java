package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.game.GameMission;
import com.bogun.prado_bot.domain.game.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameMissionRepository extends JpaRepository<GameMission, Long> {

    Optional<GameMission> findFirstByGuildIdAndUserIdAndStatusOrderByCreatedAtDesc(long guildId, long userId,
                                                                                  MissionStatus status);
}
