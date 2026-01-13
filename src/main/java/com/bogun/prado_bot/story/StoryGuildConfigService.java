package com.bogun.prado_bot.story;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoryGuildConfigService {

    private final StoryGuildConfigRepository repository;

    public Optional<StoryGuildConfig> find(long guildId) {
        return repository.findById(guildId);
    }

    public StoryGuildConfig setStoryChannel(long guildId, long channelId) {
        StoryGuildConfig config = repository.findById(guildId)
                .orElseGet(() -> StoryGuildConfig.builder().guildId(guildId).build());
        config.setStoryChannelId(channelId);
        return repository.save(config);
    }
}
