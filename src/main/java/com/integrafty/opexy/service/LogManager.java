package com.integrafty.opexy.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LogManager {

    // Channel ID constants — mapped by key name for fast lookup
    public static final String LOG_JOIN_LEFT = "1500219204845047989";
    public static final String LOG_MESSAGE    = "1500219221118947573";
    public static final String LOG_VOICE      = "1500219237342515201";
    public static final String LOG_CHANNELS   = "1500219278094241852";
    public static final String LOG_TICKETS    = "1500224160532791388";
    public static final String LOG_COMMANDS   = "1500224210667044965";
    public static final String LOG_MODS_CMD   = "1500224277453078548";
    public static final String LOG_ROLES      = "1500224341399441639";
    public static final String LOG_USERS      = "1500224421963632650";
    public static final String LOG_GAMES      = "1500808391633801216";
    public static final String LOG_BLOCKED_WORDS = "1500224385351553185";

    private final Map<String, TextChannel> channelCache = new ConcurrentHashMap<>();

    // Resolve and cache channel by ID
    private TextChannel resolve(Guild guild, String channelId) {
        return channelCache.computeIfAbsent(channelId, id -> {
            TextChannel ch = guild.getTextChannelById(id);
            if (ch == null) log.warn("[LogManager] Channel not found: {}", id);
            return ch;
        });
    }

    public void logEmbed(Guild guild, String channelId, MessageEmbed embed) {
        if (guild == null || embed == null || channelId == null) return;
        TextChannel ch = resolve(guild, channelId);
        if (ch != null) {
            ch.sendMessageEmbeds(embed)
              .setAllowedMentions(java.util.Collections.emptyList())
              .queue(null, err -> log.warn("[LogManager] Failed to send embed to {}: {}", channelId, err.getMessage()));
        }
    }
}
