package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.game.MissionChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionChoiceRepository extends JpaRepository<MissionChoice, Long> {

    boolean existsByMissionIdAndStepIndex(long missionId, int stepIndex);

    Optional<MissionChoice> findByMissionIdAndStepIndex(long missionId, int stepIndex);
}
