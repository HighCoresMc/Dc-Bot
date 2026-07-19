package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.service.event.MafiaManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;

@Component
public class MafiaCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final MafiaManager mafiaManager;

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public MafiaCommand(EventManager eventManager, MafiaManager mafiaManager) {
        this.eventManager = eventManager;
        this.mafiaManager = mafiaManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("mafia", "بدء لعبة المافيا (Staff Only)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mafia")) return;

        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        if (!eventManager.startGroupEvent("المافيا")) {
            event.reply("⚠️ هناك فعالية جماعية قائمة بالفعل: **" + eventManager.getActiveEventName() + "**").setEphemeral(true).queue();
            return;
        }

        mafiaManager.startNewGame(event.getChannel().getIdLong(), event.getGuild(), event.getMember());

        String body = "تم فتح باب الانضمام للعبة المافيا!\n\n**القوانين:**\n• الحد الأدنى للاعبين: 5.\n• الأدوار: مافيا، طبيب، محقق، مواطن.\n• النهار للمناقشة، والليل لتنفيذ الأدوار.";

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("GAME", "🕵️ لعبة المافيا — Mafia Game", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.primary("mafia_join", "انضمام"),
                                net.dv8tion.jda.api.components.buttons.Button.danger("mafia_start", "بدء اللعبة (المنظم فقط)"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("mafia_stop", "إغلاق الفعالية")
                        )))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }
}
