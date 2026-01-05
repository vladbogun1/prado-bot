package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.VoiceBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoiceBoardRepository extends JpaRepository<VoiceBoard, Long> {
    Optional<VoiceBoard> findByGuildIdAndChannelIdAndViewKey(long guildId, long channelId, String viewKey);
}
