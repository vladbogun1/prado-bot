package com.bogun.prado_bot.story;

import com.bogun.prado_bot.domain.VoiceUser;
import com.bogun.prado_bot.domain.VoiceUser.VoiceUserId;
import com.bogun.prado_bot.repo.VoiceUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EconomyAdapter {

    private final VoiceUserRepository userRepository;

    public EconomyAdapter(VoiceUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public long addCoins(long guildId, long userId, String username, String memberName, long delta) {
        VoiceUser user = userRepository.findById(new VoiceUserId(guildId, userId))
                .orElseGet(() -> createUser(guildId, userId, username, memberName));
        long next = Math.max(0, user.getPoints() + delta);
        user.setPoints(next);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return next;
    }

    public long getCoins(long guildId, long userId) {
        return userRepository.findById(new VoiceUserId(guildId, userId))
                .map(VoiceUser::getPoints)
                .orElse(0L);
    }

    private VoiceUser createUser(long guildId, long userId, String username, String memberName) {
        VoiceUser user = new VoiceUser();
        user.setId(new VoiceUserId(guildId, userId));
        user.setUsername(username);
        user.setMemberName(memberName);
        user.setPoints(0);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
