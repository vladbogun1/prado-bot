package com.bogun.prado_bot.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface StorySessionRepository extends JpaRepository<StorySessionEntity, Long> {
    Optional<StorySessionEntity> findFirstByGuildIdAndUserIdAndCampaignKeyAndStatusOrderByCreatedAtDesc(
            long guildId,
            long userId,
            String campaignKey,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StorySessionEntity s where s.id = :id")
    Optional<StorySessionEntity> findByIdForUpdate(@Param("id") long id);
}
