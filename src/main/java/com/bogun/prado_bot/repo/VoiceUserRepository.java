package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.VoiceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoiceUserRepository extends JpaRepository<VoiceUser, VoiceUser.VoiceUserId> {}