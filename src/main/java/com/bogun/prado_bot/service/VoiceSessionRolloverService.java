package com.bogun.prado_bot.service;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoiceSessionRolloverService {

    private final VoiceTrackingService tracking;
    private final ObjectProvider<JDA> jdaProvider;

    @Value("${app.timezone:UTC}")
    private String appTimezone;

    private JDA jda() {
        return jdaProvider.getIfAvailable();
    }

    @Scheduled(fixedDelay = 60000)
    public void tick() {
        JDA jda = jda();
        if (jda == null) return;

        Instant now = Instant.now();
        ZoneId zone = ZoneId.of(appTimezone);
        Instant boundary = LocalDate.ofInstant(now, zone).atStartOfDay(zone).toInstant();

        jda.getGuilds().forEach(guild -> {
            tracking.closeActiveSessionsBefore(guild.getIdLong(), boundary);

            guild.getVoiceStates().forEach(voiceState -> {
                var member = voiceState.getMember();
                if (member == null || member.getUser().isBot()) return;
                var joined = voiceState.getChannel();
                if (joined == null) return;

                tracking.ensureSessionAtBoundary(
                        guild.getIdLong(),
                        member.getIdLong(),
                        member.getUser().getName(),
                        member.getEffectiveName(),
                        Objects.requireNonNull(joined).getIdLong(),
                        joined.getName(),
                        new VoiceTrackingService.VoiceFlags(
                                voiceState.isMuted(),
                                voiceState.isDeafened(),
                                voiceState.isSuppressed()
                        ),
                        boundary
                );
            });
        });
    }
}
