package com.bogun.prado_bot.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryCooldownRepository extends JpaRepository<StoryCooldownEntity, Long> {
    Optional<StoryCooldownEntity> findByGuildIdAndUserIdAndCampaignKey(long guildId, long userId, String campaignKey);
}
