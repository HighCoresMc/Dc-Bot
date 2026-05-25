package com.integrafty.opexy;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import club.minnced.discord.jdave.interop.JDaveSessionFactory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.StringHttpMessageConverter;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class OpexyApplication {

    private static org.springframework.context.ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
        
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        // Railway DATABASE_URL support (Enhanced Parsing)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            try {
                log.info("Parsing Railway DATABASE_URL...");
                URI uri = new URI(databaseUrl);
                String[] userInfo = uri.getUserInfo().split(":");
                String username = userInfo[0];
                String password = userInfo[1];
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath();
                
                String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s", host, port, path);
                
                System.setProperty("spring.datasource.url", jdbcUrl);
                System.setProperty("spring.datasource.username", username);
                System.setProperty("spring.datasource.password", password);
                
                log.info("Database configuration updated successfully from DATABASE_URL.");
            } catch (Exception e) {
                log.error("Failed to parse DATABASE_URL: {}", e.getMessage());
            }
        }
        
        context = SpringApplication.run(OpexyApplication.class, args);
    }

    public static org.springframework.context.ConfigurableApplicationContext getContext() {
        return context;
    }

    @Bean
    public JDA jda(@Value("${discord.bot.token}") String token) {
        try {
            log.info("Configuring JDA with JDAVE support...");
            
            return JDABuilder.createDefault(token)
                .setAudioModuleConfig(new AudioModuleConfig()
                    .withDaveSessionFactory(new JDaveSessionFactory()))
                .enableIntents(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_VOICE_STATES
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();
        } catch (Exception e) {
            log.error("Failed to start Discord Bot", e);
            throw new RuntimeException("Failed to connect to Discord", e);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
