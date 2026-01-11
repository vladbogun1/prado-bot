package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.domain.game.GameSessionStatus;
import com.bogun.prado_bot.repo.game.GameSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GameSessionExpiryJob {

    private final GameSessionRepository sessionRepository;

    @Scheduled(fixedDelayString = "${app.prado-game.expire-check-ms}")
    @Transactional
    public void expireSessions() {
        Instant now = Instant.now();
        var expired = sessionRepository.findAllByStatusAndExpiresAtBefore(GameSessionStatus.ACTIVE, now);
        expired.forEach(session -> {
            session.setStatus(GameSessionStatus.EXPIRED);
            session.setLastOutcomeText("Сессия истекла по таймауту.");
            session.setLastActionAt(now);
        });
        sessionRepository.saveAll(expired);
    }
}
