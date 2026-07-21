package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.Permission;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Instant;

@Component
public class TicketCommand implements SlashCommand {

    @Override
    public String getName() {
        return "tickets";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("tickets", "إرســـال لـــوحـــة الـــتـــذاكـــر لـــلـــســـيـــرفـــر")
                .addOption(OptionType.CHANNEL, "channel", "الـــقـــنـــاة الـــمـــســـتـــهـــدفـــة", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessageComponents(EmbedUtil.accessDenied()).setEphemeral(true).queue();
            return;
        }

        String rules = "### قوانين وشروط الدعم الفني\n<divider>\n" +
                "**الاحترام المتبادل** — يرجى احترام جميع أعضاء الإدارة. أي إساءة قد تعرضك للحظر النهائي.\n\n" +
                "**تذكرة واحدة** — يرجى فتح تذكرة واحدة فقط لمشكلتك وعدم التكرار مطلقاً.\n\n" +
                "**الوضوح** — اشرح مشكلتك بالكامل فور فتح التذكرة لنسرع في خدمتك الفورية.\n\n" +
                "**المنشن** — يمنع عمل منشن (Ping) للإدارة داخل التذكرة، سنقوم بالرد بأقرب وقت ممكن.\n\n" +
                "<divider>\n" +
                "يرجى اختيار القسم المناسب من الأزرار بالأسفل:";

        ActionRow buttons = ActionRow.of(
            Button.secondary("ticket_support", "الدعم الفني"),
            Button.secondary("ticket_complaint", "الشكاوى"),
            Button.secondary("ticket_hire", "التقديم على الإدارة"),
            Button.secondary("ticket_whitelist", "الوايت ليست"),
            Button.secondary("ticket_team", "التقديم على فريق")
        );

        Container container = EmbedUtil.containerBranded(
                "TICKETS", 
                "Support Center", 
                rules, 
                EmbedUtil.BANNER_TICKETS_MENU, 
                buttons
        );

        net.dv8tion.jda.api.entities.channel.middleman.MessageChannel targetChannel = event.getChannel();
        if (event.getOption("channel") != null) {
            targetChannel = event.getOption("channel").getAsChannel().asGuildMessageChannel();
        }

        targetChannel.sendMessage(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build())
            .useComponentsV2(true).queue();

        Container success = EmbedUtil.success("الإمـدادات", "تـم إرسـال لـوحـة الـتـذاكـر بـنـجـاح فـي " + targetChannel.getAsMention());
        event.getHook().sendMessage(new MessageCreateBuilder().setComponents(success).useComponentsV2(true).build())
            .useComponentsV2(true).queue();
    }
}
