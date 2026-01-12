package com.bogun.prado_bot.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
import java.util.List;

public interface StoryLogRepository extends JpaRepository<StoryLogEntity, Long> {
    List<StoryLogEntity> findBySessionIdOrderByStepAsc(long sessionId);
}
