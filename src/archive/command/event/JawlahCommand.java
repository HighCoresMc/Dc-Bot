package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.service.event.JawlahManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JawlahCommand implements SlashCommand {

    private final JawlahManager jawlahManager;
    private final EventManager eventManager;

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("jawlah", "بدء لعبة جولة (للمنظمين فقط)");
    }

    @Override
    public String getName() {
        return "jawlah";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Check permissions (Staff Only)
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        if (!eventManager.startGroupEvent("جولة")) {
            event.reply("⚠️ هناك فعالية جماعية قائمة بالفعل: **" + eventManager.getActiveEventName() + "**").setEphemeral(true).queue();
            return;
        }

        jawlahManager.initiateSetup(event);
    }
}
