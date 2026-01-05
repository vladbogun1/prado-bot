package com.bogun.prado_bot.discord.board;

import com.bogun.prado_bot.domain.VoiceBoard;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public interface BoardView {
    String key();
    MessageEditData render(JDA jda, VoiceBoard board);
}
