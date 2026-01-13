package com.bogun.prado_bot.story;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoryRecap {
    private long guildId;
    private long userId;
    private String markdown;
}
