package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.listener.DiscordEventListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.session.ReadyEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandManager extends ListenerAdapter {

    private final JDA jda;
    private final DiscordEventListener discordEventListener;
    private final List<SlashCommand> commands;
    private final List<MultiSlashCommand> multiCommands;
    private final List<ListenerAdapter> listeners;

    @Value("${discord.guild.id}")
    private String guildId;

    @PostConstruct
    public void init() {
        // Register all modular listeners
        for (ListenerAdapter listener : listeners) {
            if (listener != this && listener != discordEventListener) {
                jda.addEventListener(listener);
            }
        }
        
        jda.addEventListener(discordEventListener);
        jda.addEventListener(this); // Register self to listen for ReadyEvent
        
        if (jda.getStatus() == JDA.Status.CONNECTED) {
            log.info("JDA is already connected! Registering commands manually to avoid missing the ReadyEvent.");
            registerCommands();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        log.info("JDA is ready, registering commands...");
        registerCommands();
    }

    private void registerCommands() {
        log.info("Registering modular slash commands...");
        
        var commandDataList = new java.util.ArrayList<net.dv8tion.jda.api.interactions.commands.build.SlashCommandData>();
        
        // Add single commands
        for (SlashCommand cmd : commands) {
            commandDataList.add(cmd.getCommandData());
        }
        
        // Add multi commands
        for (MultiSlashCommand mcmd : multiCommands) {
            commandDataList.addAll(mcmd.getCommandDataList());
        }
        
        log.info("Total commands to register: {}", commandDataList.size());

        // Fetch Guild ID from config
        if (guildId != null && !guildId.isEmpty()) {
            net.dv8tion.jda.api.entities.Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                // Clear global commands to avoid duplicates
                jda.updateCommands().addCommands().queue();
                
                guild.updateCommands().addCommands(commandDataList).queue(
                    success -> log.info("Successfully registered all modular commands to guild: {}", guild.getName()),
                    error -> log.error("Failed to register commands to guild.", error)
                );
            } else {
                log.warn("Guild with ID {} not found. Falling back to global commands.", guildId);
                jda.updateCommands().addCommands(commandDataList).queue();
            }
        } else {
            jda.updateCommands().addCommands(commandDataList).queue();
        }
    }
}
