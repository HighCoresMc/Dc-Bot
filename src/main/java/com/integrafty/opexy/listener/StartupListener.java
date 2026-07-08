package com.integrafty.opexy.listener;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import com.integrafty.opexy.utils.EmbedUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupListener extends ListenerAdapter {

    private final JDA jda;

    private static final Map<String, String> COLOR_ROLES = Map.of(
        "color_red", "1499885720209195059",
        "color_turquoise", "1499885336703275029",
        "color_orange", "1499885645563166914",
        "color_gray", "1499885533277589656",
        "color_navy", "1499885778413813810",
        "color_blurple", "1499884810338832394",
        "color_asphalt", "1499885394752176190"
    );

    private static final Map<String, String> PING_ROLES = Map.of(
        "ping_minecraft", "1487196787142819961",
        "ping_event", "1487196787893731428",
        "ping_mcserver", "1499896841150402692",
        "ping_dcserver", "1499896994003681310",
        "ping_apply", "1499897044897366056",
        "ping_social", "1500269236583399454"
    );


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        switch (id) {
            case "startup_map":
                showServerMap(event);
                break;
            case "startup_colors":
                showColors(event);
                break;
            case "startup_pings":
                showPings(event);
                break;
            case "startup_socials":
                showSocials(event);
                break;
            case "support_direct":
                event.reply("توجه إلى روم الدعم الفني من هنا: <#1487143271586074624>").setEphemeral(true).queue();
                break;
            default:
                if (id.startsWith("color_")) {
                    handleColorSelection(event, id);
                } else if (id.startsWith("ping_")) {
                    handlePingSelection(event, id);
                }
                break;
        }
    }

    private void showServerMap(ButtonInteractionEvent event) {
        String mapContent = 
            "### خريطة السيرفر | SERVER MAP\n<divider>\n" +
            "┣ <#1487138386258165820> • استقبل الأعضاء الجدد بحماس\n" +
            "┃\n" +
            "┣ <#1488279212786843850> • وثق حسابك لفتح الرومات\n" +
            "┃\n" +
            "┣ <#1487138587827900486> • قوانين السيرفر والمعلومات الأساسية\n" +
            "┃\n" +
            "┣ <#1487138659147845724> • آخر أخبار وتحديثات السيرفر\n" +
            "┃\n" +
            "┣ <#1487204004076191946> • أحدث مقاطع اليوتيوب الحصرية\n" +
            "┃\n" +
            "┣ <#1487140470172815574> • تابع البث المباشر الآن\n" +
            "┃\n" +
            "┣ <#1487139870433476720> • شرح مفصل للعب بالسيرفر\n" +
            "┃\n" +
            "┣ <#1487138843378323486> • شاركنا أفكارك لتطوير السيرفر\n" +
            "┃\n" +
            "┣ <#1487139736748425236> • حالة السيرفر التقنية الحالية\n" +
            "┃\n" +
            "┣ <#1487140811404611695> • شارك في تصويتات السيرفر\n" +
            "┃\n" +
            "┣ <#1488278004919435335> • دردشة خاصة للاعبي ماينكرافت\n" +
            "┃\n" +
            "┣ <#1487140532965867600> • استخدم أوامر البوت هنا\n" +
            "┃\n" +
            "┣ <#1487140589500629034> • شارك صورك ومقاطعك هنا\n" +
            "┃\n" +
            "┣ <#1487142537666760735> • جدول فعاليات السيرفر القادمة\n" +
            "┃\n" +
            "┣ <#1487142391474557069> • اقترح فعاليات جديدة ممتعة\n" +
            "┃\n" +
            "┣ <#1487142175765430393> • شات خاص أثناء الفعاليات\n" +
            "┃\n" +
            "┣ <#1487141372325531709> • روم صوتي عام للاجتماع\n" +
            "┃\n" +
            "┣ <#1487141441854767184> • روم صوتي إضافي للجميع\n" +
            "┃\n" +
            "┣ <#1486872077263835157> • لوحة تحكم إعداداتك الخاصة\n" +
            "┃\n" +
            "┣ <#1500190617353851037> • أنشئ رومتك الصوتية فوراً\n" +
            "┃\n" +
            "┗ <#1487143271586074624> • تواصل مع الإدارة مباشرة";

        Container container = EmbedUtil.containerBranded("MAP", "HighCore Explorer", mapContent, null);
        event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void showColors(ButtonInteractionEvent event) {
        String body = "اختر لونك المفضل (لون واحد فقط):";
        ActionRow row1 = ActionRow.of(
            Button.secondary("color_red", "Soft Red"),
            Button.secondary("color_turquoise", "Turquoise"),
            Button.secondary("color_orange", "Carrot Orange"),
            Button.secondary("color_gray", "Light Gray"),
            Button.secondary("color_navy", "Midnight Navy")
        );
        ActionRow row2 = ActionRow.of(
            Button.secondary("color_blurple", "Blurple"),
            Button.secondary("color_asphalt", "Wet Asphalt")
        );

        Container container = EmbedUtil.containerBranded("COLORS", "Color Selection", body, null, row1, row2);
        event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void showPings(ButtonInteractionEvent event) {
        String body = "اختر الأقسام التي ترغب بمتابعتها:";
        ActionRow row1 = ActionRow.of(
            Button.secondary("ping_social", "Social Notify"),
            Button.secondary("ping_minecraft", "Minecraft"),
            Button.secondary("ping_event", "Events"),
            Button.secondary("ping_mcserver", "MC-Server"),
            Button.secondary("ping_dcserver", "DC-Server")
        );
        ActionRow row2 = ActionRow.of(
            Button.secondary("ping_apply", "Staff Apply"),
            Button.secondary("support_direct", "Support")
        );

        Container container = EmbedUtil.containerBranded("PINGS", "Notification Pings", body, null, row1, row2);
        event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void showSocials(ButtonInteractionEvent event) {
        String body = "تابعنا على منصات التواصل الاجتماعي:";
        ActionRow row = ActionRow.of(
            Button.link("https://www.instagram.com/highcoremc", "Instagram"),
            Button.link("https://www.threads.com/@highcoremc", "Threads"),
            Button.link("https://www.youtube.com/@higcoremc", "YouTube"),
            Button.link("https://x.com/highcoremc", "X"),
            Button.link("https://www.tiktok.com/@highcoremcmc", "TikTok")
        );

        Container container = EmbedUtil.containerBranded("SOCIAL", "Social Media", body, null, row);
        event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void handleColorSelection(ButtonInteractionEvent event, String buttonId) {
        String targetRoleId = COLOR_ROLES.get(buttonId);
        if (targetRoleId == null) return;

        Role targetRole = event.getGuild().getRoleById(targetRoleId);
        if (targetRole == null) return;

        event.deferReply(true).queue();

        for (String roleId : COLOR_ROLES.values()) {
            Role r = event.getGuild().getRoleById(roleId);
            if (r != null && event.getMember().getRoles().contains(r)) {
                event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
            }
        }

        event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(
            success -> event.getHook().sendMessage("تم تحديث لونك إلى: **" + targetRole.getName() + "**").queue()
        );
    }

    private void handlePingSelection(ButtonInteractionEvent event, String buttonId) {
        String targetRoleId = PING_ROLES.get(buttonId);
        if (targetRoleId == null) return;

        Role targetRole = event.getGuild().getRoleById(targetRoleId);
        if (targetRole == null) return;

        event.deferReply(true).queue();

        if (event.getMember().getRoles().contains(targetRole)) {
            event.getGuild().removeRoleFromMember(event.getMember(), targetRole).queue(
                success -> event.getHook().sendMessage("تم إزالة تنبيهات: **" + targetRole.getName() + "**").queue()
            );
        } else {
            event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(
                success -> event.getHook().sendMessage("تم تفعيل تنبيهات: **" + targetRole.getName() + "**").queue()
            );
        }
    }
}
