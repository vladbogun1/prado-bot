package com.bogun.prado_bot.service;

import com.bogun.prado_bot.discord.board.BoardViewRegistry;
import com.bogun.prado_bot.domain.VoiceBoard;
import com.bogun.prado_bot.repo.VoiceBoardRepository;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VoiceBoardService {

    private final VoiceBoardRepository repo;
    private final BoardViewRegistry views;

    private final ObjectProvider<JDA> jdaProvider;
    private JDA jda() {
        return jdaProvider.getIfAvailable();
    }

    public void registerBoard(long guildId, long channelId, long messageId, String messageUrl,
                              Integer refreshSeconds, Integer limit, String viewKey) {

        int rs = refreshSeconds != null ? refreshSeconds : 30;
        int lim = limit != null ? limit : 20;

        var board = repo.findByGuildIdAndChannelIdAndViewKey(guildId, channelId, viewKey)
                .orElseGet(() -> VoiceBoard.builder()
                        .guildId(guildId)
                        .channelId(channelId)
                        .viewKey(viewKey)
                        .build());

        board.setMessageId(messageId);
        board.setMessageUrl(messageUrl);
        board.setRefreshSeconds(rs);
        board.setLineLimit(lim);
        board.setUpdatedAt(Instant.EPOCH);
        repo.save(board);
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        var now = Instant.now();

        for (var b : repo.findAll()) {
            var dueAt = b.getUpdatedAt().plusSeconds(b.getRefreshSeconds());
            if (now.isBefore(dueAt)) continue;

            updateBoard(b);
        }
    }

    private void updateBoard(VoiceBoard b) {
        JDA jda = jda();
        if (jda == null) return;

        var channel = jda.getTextChannelById(b.getChannelId());
        if (channel == null) {
            repo.delete(b);
            return;
        }

        var view = views.get(b.getViewKey());
        if (view == null) {
            b.setUpdatedAt(Instant.now());
            repo.save(b);
            return;
        }

        var editData = view.render(jda, b);

        channel.retrieveMessageById(b.getMessageId()).queue(msg -> {
            msg.editMessage(editData).queue(ok -> {
                b.setUpdatedAt(Instant.now());
                repo.save(b);
            }, editFail -> {
                if (shouldDeleteBoard(editFail)) {
                    repo.delete(b);
                    return;
                }
                b.setUpdatedAt(Instant.now());
                repo.save(b);
            });
        }, retrieveFail -> {
            if (shouldDeleteBoard(retrieveFail)) {
                repo.delete(b);
                return;
            }
            b.setUpdatedAt(Instant.now());
            repo.save(b);
        });
    }


    private boolean shouldDeleteBoard(Throwable t) {
        if (!(t instanceof ErrorResponseException ex)) return false;

        ErrorResponse r = ex.getErrorResponse();
        return r == ErrorResponse.UNKNOWN_MESSAGE
                || r == ErrorResponse.UNKNOWN_CHANNEL
                || r == ErrorResponse.MISSING_ACCESS
                || r == ErrorResponse.MISSING_PERMISSIONS;
    }
}
