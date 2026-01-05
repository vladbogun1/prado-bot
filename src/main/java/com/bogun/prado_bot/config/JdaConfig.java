package com.bogun.prado_bot.config;

import com.bogun.prado_bot.discord.DiscordRouterListener;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@EnableConfigurationProperties(DiscordProperties.class)
public class JdaConfig {

//    private final DiscordRouterListener discordRouterListener;

    @Bean(destroyMethod = "shutdown")
    public JDA jda(DiscordProperties props, List<EventListener> eventListeners) {
        JDABuilder builder = JDABuilder.createDefault(props.getToken())
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT);


        builder.addEventListeners(eventListeners.toArray());
        return builder.build();
    }
}
