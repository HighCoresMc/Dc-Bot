package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.service.event.ScavengerHuntManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScavengerHuntCommand implements MultiSlashCommand {

    private final ScavengerHuntManager huntManager;
    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("hunt", "بدء فعالية الصيد (Scavenger Hunt)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("hunt")) return;

        // Check permissions
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(hypeManagerId) || r.getId().equals(hypeEventsId));
        
        if (!hasRole) {
            event.reply("❌ عذراً، هذا الأمر مخصص لمشرفي الفعاليات فقط.").setEphemeral(true).queue();
            return;
        }

        if (!eventManager.startGroupEvent("فعالية الصيد")) {
            event.reply("⚠️ هناك فعالية جماعية قائمة بالفعل.").setEphemeral(true).queue();
            return;
        }

        long reward = 5000;
        String code = huntManager.startHunt(reward, event.getGuild(), event.getMember());

        List<TextChannel> channels = event.getGuild().getTextChannels().stream()
                .filter(ch -> ch.canTalk())
                .filter(ch -> event.getGuild().getPublicRole().hasPermission(ch, Permission.VIEW_CHANNEL))
                .filter(ch -> {
                    String n = ch.getName().toLowerCase();
                    return !n.contains("log") && !n.contains("staff") && !n.contains("admin") && !n.contains("mod") && !n.contains("hidden");
                })
                .collect(Collectors.toList());

        TextChannel targetChannel = channels.get(random.nextInt(channels.size()));
        
        // Hide code in target channel
        targetChannel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("DISCOVERY", "🔍 لقد وجدت شيئاً!", 
                        "لقد عثرت على الكود السري! الكود هو: **" + code + "**\nأسرع واكتبه في قناة الفعاليات لتفوز!", 
                        com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();

        String body = "🔎 **فعالية الصيد بدأت!**\n\nتم إخفاء كود سري في إحدى قنوات السيرفر!\n\n**المهمة:** ابحث عن الكود واكتبه هنا في الشات لتفوز بـ **" + reward + " opex**!\n\n*ملاحظة: الكود يبدأ بـ OP- ويتبعه 6 أحرف.*";

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("EVENT", "فعالية الصيد — Scavenger Hunt", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .setEphemeral(true)
                .useComponentsV2(true).queue();

        // Notify the supervisor (Ephemeral)
        event.getHook().sendMessage("🤫 تم إخفاء الكود **" + code + "** في قناة: " + targetChannel.getAsMention())
                .setEphemeral(true).queue();
    }
}
