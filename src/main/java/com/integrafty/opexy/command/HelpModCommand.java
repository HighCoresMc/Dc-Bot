package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class HelpModCommand extends ListenerAdapter implements MultiSlashCommand {

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("help-mod", "قـــائـــمـــة الـــمـــســـاعـــدة لـــلإدارة والـــفـــعـــالـــيـــات")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("help-mod")) return;

        // Ensure only the specific role can use this command
        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals("1487195816220430406"));

        if (!hasRole) {
            event.reply(new MessageCreateBuilder().setComponents(EmbedUtil.accessDenied()).useComponentsV2(true).build())
                    .setEphemeral(true).useComponentsV2(true).queue();
            return;
        }

        StringSelectMenu menu = StringSelectMenu.create("help_mod_menu")
                .setPlaceholder("اخـتـر قـسـم الـمـسـاعـدة...")
                .addOption("الـفـعـالـيـات وشـرحـهـا 🎯", "events_info", "شـرح لـجـمـيـع فـعـالـيـات الـبـوت")
                .addOption("أوامـر الـفـعـالـيـات 🎮", "events_cmds", "أوامـر إدارة الـفـعـالـيـات مـثـل lock")
                .addOption("أوامـر الإدارة والـحـمـايـة 🛡️", "mod_cmds", "الأوامـر الـحـسـاسـة والإداريـة")
                .build();

        String body = "### 📋 قـــائـــمـــة الـــمـــســـاعـــدة (مـــخـــصـــص للإدارة)\n\n" +
                      "مـرحـبـاً بـك فـي قـائـمـة مـسـاعـدة الإدارة والـفـعـالـيـات.\n" +
                      "يـرجـى اخـتـيـار الـقـسـم الـذي تـود تـصـفـحـه مـن الـقـائـمـة الـمـنـسـدلـة بـالأسـفـل.";

        event.reply(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("HELP", "Help & Guidance", body, EmbedUtil.BANNER_MAIN, ActionRow.of(menu)))
                .useComponentsV2(true).build())
                .setEphemeral(true).useComponentsV2(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("help_mod_menu")) return;

        String selection = event.getValues().get(0);
        String title = "";
        String body = "";

        switch (selection) {
            case "events_info" -> {
                title = "شـــرح الـــفـــعـــالـــيـــات 🎯";
                body = "### 1️⃣ فـــعـــالـــيـــة جـــولـــة (TD)\n" +
                       "فـعـالـيـة تـنـافـسـيـة بـيـن فـريـقـيـن تـعـتـمـد عـلـى أسـئـلـة بـفـئـات مـخـتـلـفـة (إسـلامـي، ريـاضـي، ثـقـافـي، خـمـن).\n" +
                       "يـقـوم الـمـنـظـم بـتـجـهـيـز الـلـعـبـة وتـقـسـيـم الـفـرق ثـم يـخـتـار مـن الـلـوحـة رئـيـسـيـة لـلأسـئـلـة.\n\n" +
                       "### 2️⃣ فـــعـــالـــيـــة الـــصـــيـــد (Hunt)\n" +
                       "يـقـوم الـبـوت بـإخـفـاء كـود فـي إحـدى قـنـوات الـسـيـرفـر بـشـكـل عـشـوائـي. أول شـخـص يـجـده ويـكـتـبـه يـفـوز بـالـجـائـزة!\n\n" +
                       "### 3️⃣ فـــعـــالـــيـــة الـــمـــافـــيـــا (Mafia)\n" +
                       "لـعـبـة أدوار تـوزع أوتـومـاتـيـكـيـاً (مـافـيـا، طـبـيـب، مـحـقـق، مـواطـن). تـتـعـاقـب الـلـعـبـة بـيـن الـلـيـل والـنـهـار ويـتـم الـتـصـويـت لـلإعـدام.\n\n" +
                       "### 4️⃣ فـــعـــالـــيـــة الـــقـــنـــبـــلـــة (Bomb)\n" +
                       "قـنـبـلـة مـوقـوتـة تـتـنـقـل بـيـن الـلاعـبـيـن. بـعـد مـرور الـوقـت تـنـفـجـر وبـخـرج الـمـمـسـك بـهـا حـتـى يـبـقـى فـائـز واحـد.";
            }
            case "events_cmds" -> {
                title = "أوامـــر الـــفـــعـــالـــيـــات 🎮";
                body = "### 🛠️ أوامـــر تـــشـــغـــيـــل الـــفـــعـــالـــيـــات:\n" +
                       "▫️ `/jawlah_setup` : لـبـدء فـعـالـيـة جـولـة (تـحـدي).\n" +
                       "▫️ `/hunt` : لـبـدء فـعـالـيـة الـصـيـد.\n" +
                       "▫️ `/mafia_setup` : لـتـجـهـيـز وبـدء لـعـبـة الـمـافـيـا.\n" +
                       "▫️ `/bomb_start` : لـبـدء فـعـالـيـة الـقـنـبـلـة.\n\n" +
                       "### 🔧 أوامـــر الـــتـــحـــكـــم بـــالـــشـــات لـــلـــفـــعـــالـــيـــات:\n" +
                       "▫️ `/lock` : قـفـل الـشـات الـحـالـي لـمـنـع إرسـال الـرسـائـل.\n" +
                       "▫️ `/unlock` : فـتـح الـشـات الـحـالـي لـلـسـمـاح بـالـرسـائـل.\n" +
                       "▫️ `/hide` : إخـفـاء الـروم عـن الأعـضـاء.\n" +
                       "▫️ `/show` : إظـهـار الـروم لـلأعـضـاء.\n" +
                       "▫️ `/slowmode <seconds>` : تـفـعـيـل وضـع الـتـبـاطـؤ.";
            }
            case "mod_cmds" -> {
                title = "أوامـــر الإدارة والـــحـــمـــايـــة 🛡️";
                body = "### 🚨 أوامـــر الـــعـــقـــوبـــات:\n" +
                       "▫️ `/ban` / `/unban` : لـحـظـر وإلـغـاء حـظـر عـضـو مـن الـسـيـرفـر.\n" +
                       "▫️ `/kick` : لـطـرد عـضـو مـن الـسـيـرفـر.\n" +
                       "▫️ `/vkick` : لـطـرد عـضـو مـن الـروم الـصـوتـي.\n" +
                       "▫️ `/timeout` / `/untimeout` : مـنـع الـعـضـو مـن الـتـفـاعـل مـؤقـتـاً.\n" +
                       "▫️ `/mute-voice` / `/unmute-voice` : مـيـوت صـوتـي.\n" +
                       "▫️ `/warn-add` / `/warn-remove` / `/warnings` : نـظـام الـتـحـذيـرات الآلـي.\n\n" +
                       "### ⚙️ أوامـــر الـــتـــحـــكـــم والـــصـــلاحـــيـــات:\n" +
                       "▫️ `/opstage` / `/clstage` : فـتـح واغـلاق روم الـسـتـيـج للـ public.\n" +
                       "▫️ `/clear <1-100>` : مـسـح الـرسـائـل.\n" +
                       "▫️ `/role` / `/rar` : إعـطـاء وسـحـب الـرتـب.\n" +
                       "▫️ `/move` : نـقـل عـضـو لـروم صـوتـي آخـر.\n" +
                       "▫️ `/add-emoji` : إضـافـة ايـمـوجـي جـديـد.";
            }
        }

        StringSelectMenu menu = StringSelectMenu.create("help_mod_menu")
                .setPlaceholder("اخـتـر قـسـم الـمـسـاعـدة...")
                .addOption("الـفـعـالـيـات وشـرحـهـا 🎯", "events_info", "شـرح لـجـمـيـع فـعـالـيـات الـبـوت")
                .addOption("أوامـر الـفـعـالـيـات 🎮", "events_cmds", "أوامـر إدارة الـفـعـالـيـات مـثـل lock")
                .addOption("أوامـر الإدارة والـحـمـايـة 🛡️", "mod_cmds", "الأوامـر الـحـسـاسـة والإداريـة")
                .build();

        event.editMessage(new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("HELP", title, body, EmbedUtil.BANNER_MAIN, ActionRow.of(menu)))
                .useComponentsV2(true).build()).queue();
    }
}
