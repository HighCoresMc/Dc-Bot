package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.CraftManager;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CraftCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final CraftManager craftManager;

    public CraftCommand(EventManager eventManager, CraftManager craftManager) {
        this.eventManager = eventManager;
        this.craftManager = craftManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("craft", "بدء لعبة التخمين من خلال الصناعة (فردية)")
                .addOptions(new OptionData(OptionType.STRING, "difficulty", "درجة الصعوبة", true)
                        .addChoice("سهل (20 opex)", "EASY")
                        .addChoice("وسط (30 opex)", "MEDIUM")
                        .addChoice("صعب (40 opex)", "HARD")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("craft")) return;

        String diffStr = event.getOption("difficulty").getAsString();
        CraftManager.Difficulty difficulty = CraftManager.Difficulty.valueOf(diffStr);
        long userId = event.getUser().getIdLong();
        
        String sessionId = event.getId();
        String grid = craftManager.startCraft(sessionId, userId, difficulty, event.getGuild(), event.getMember());

        String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, difficulty.seconds);
        String description = timerFormat + "\n\n" +
                String.format("أمامك طاولة كرافتنق خاصة بك يا %s... خمن ما هو الشيء الذي يتم صنعه؟\n\n", event.getUser().getAsMention()) +
                grid + "\n" +
                "💰 الجائزة: **" + difficulty.reward + " opex**\n" +
                "📊 الصعوبة: **" + difficulty.displayName + "**\n\n" +
                "💡 اكتب الإجابة مباشرة في الشات!";

        event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("CRAFTING", "🛠️ ماذا نصنع؟", description, EmbedUtil.BANNER_MAIN)).build()).queue(hook -> craftManager.initTimer(sessionId, difficulty, event));
    }
}
