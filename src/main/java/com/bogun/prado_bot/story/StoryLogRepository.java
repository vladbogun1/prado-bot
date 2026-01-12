package com.bogun.prado_bot.story;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryLogRepository extends JpaRepository<StoryLogEntity, Long> {
    List<StoryLogEntity> findBySessionIdOrderByStepAsc(long sessionId);
}
