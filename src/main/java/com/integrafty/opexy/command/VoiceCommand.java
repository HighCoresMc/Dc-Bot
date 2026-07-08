package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

@Component
public class VoiceCommand implements SlashCommand {

    private static final String DASHBOARD_CHANNEL_ID = "1486872077263835157";

    @Override
    public String getName() {
        return "voice-setup";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("voice-setup", "إنشاء بنل التحكم في الرومات الصوتية")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        String body = "### لوحة تحكم الرومات الصوتية | VOICE CONTROL\n<divider>\n" +
                "تحكم في إعدادات رومتك الصوتية المؤقتة بكل سهولة من خلال الخيارات أدناه.\n\n" +
                "▫️ **الحماية (Security):** قفل أو إخفاء الروم\n" +
                "▫️ **الهوية (Identity):** تغيير الاسم أو تحديد عدد الأعضاء\n" +
                "▫️ **الإدارة (Management):** طرد الأعضاء أو نقل الملكية";
 
        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_rename", "Rename"),
            Button.secondary("voice_limit", "Limit"),
            Button.secondary("voice_bitrate", "Bitrate"),
            Button.primary("voice_region", "Region")
        );
        
        ActionRow row2 = ActionRow.of(
            Button.success("voice_speak_perm", "Speak"),
            Button.success("voice_write_perm", "Write"),
            Button.success("voice_video_perm", "Video"),
            Button.secondary("voice_lock", "Lock/Unlock")
        );
 
        ActionRow row3 = ActionRow.of(
            Button.danger("voice_kick", "Kick"),
            Button.primary("voice_trust", "Trust"),
            Button.primary("voice_block", "Block"),
            Button.primary("voice_panel", "Members")
        );
 
        ActionRow row4 = ActionRow.of(
            Button.primary("voice_ownership", "Owner"),
            Button.success("voice_shop", "Shop"),
            Button.danger("voice_request_staff", "Staff"),
            Button.danger("voice_delete", "Delete")
        );
 
        Container container = EmbedUtil.containerBranded("Voice Control", "Voice Management Center", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4);

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel dashboard = event.getGuild().getTextChannelById(DASHBOARD_CHANNEL_ID);
        if (dashboard != null) {
            dashboard.sendMessage(builder.build()).useComponentsV2(true).queue();
            event.reply("Dashboard sent successfully to " + dashboard.getAsMention()).setEphemeral(true).queue();
        } else {
            event.reply("Dashboard channel not found.").setEphemeral(true).queue();
        }
    }
}
