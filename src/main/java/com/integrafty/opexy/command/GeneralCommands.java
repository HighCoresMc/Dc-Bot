package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeneralCommands implements MultiSlashCommand {

    private final TranslationService translationService;
    private final com.integrafty.opexy.service.EconomyService economyService;
    private final com.integrafty.opexy.service.event.EventManager eventManager;

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("help", "عـــرض قـــائـــمـــة الأوامـــر"));
        list.add(Commands.slash("ping", "عـــرض ســـرعـــة اتـــصـــال الـــبـــوت"));
        list.add(Commands.slash("roll", "رمـــي حـــجـــر الـــنـــرد (1-100)"));
        list.add(Commands.slash("balance", "عـــرض رصـــيـــدك الـــحـــالـــي")
                .addOption(OptionType.USER, "user", "الـــمـــســـتـــخـــدم الـــمـــراد عـــرض رصـــيـــده", false));
        
        list.add(Commands.slash("colors", "عـــرض جـــمـــيـــع الألـــوان الـــمـــتـــاحـــة"));
        list.add(Commands.slash("color-set", "تـــغـــيـــيـــر لـــون اســـمـــك")
                .addOption(OptionType.STRING, "color_id", "أي دي الـــلـــون", true));

        list.add(Commands.slash("translate", "تـــرجـــمـــة نـــص إلـــى الـــعـــربـــيـــة")
                .addOption(OptionType.STRING, "text", "الـــنـــص الـــمـــراد تـــرجـــمـــتـــه", true));

        list.add(Commands.slash("get-emojis", "ســـحـــب جـــمـــيـــع إيـــمـــوجـــيـــات الـــســـيـــرفـــر")
                .addOption(OptionType.STRING, "guild_id", "أي دي الـــســـيـــرفـــر", true));

        list.add(Commands.slash("give", "شـــحـــن رصـــيـــد لـــعـــضـــو (خاص بالإدارة)")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.INTEGER, "amount", "الـــمـــبـــلـــغ", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        list.add(Commands.slash("take", "ســـحـــب رصـــيـــد مـــن عـــضـــو (خاص بالإدارة)")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.INTEGER, "amount", "الـــمـــبـــلـــغ", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        list.add(Commands.slash("transfer", "تـــحـــويـــل رصـــيـــد لـــمـــســـتـــخـــدم آخـــر")
                .addOption(OptionType.USER, "user", "الـــمـــســـتـــخـــدم الـــمـــرســـل إلـــيـــه", true)
                .addOption(OptionType.INTEGER, "amount", "الـــمـــبـــلـــغ الـــمـــراد تـــحـــويـــلـــه", true));

        list.add(Commands.slash("kill", "إيقاف فعالية جماعية قائمة حالياً (خاص بالإدارة)")
                .addOptions(new net.dv8tion.jda.api.interactions.commands.build.OptionData(OptionType.STRING, "type", "نوع الفعالية المراد إيقافها", false)
                        .addChoice("المزاد (Auction)", "auction")
                        .addChoice("المافيا (Mafia)", "mafia")
                        .addChoice("جولة (Jawlah)", "jawlah")
                        .addChoice("الكل (All)", "all"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        list.add(Commands.slash("azkar", "عـــرض قـــائـــمـــة أوامـــر الأذكـــار (خاص بالإدارة)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();

        switch (name) {
            case "help" -> handleHelp(event);
            case "ping" -> handlePing(event);
            case "roll" -> handleRoll(event);
            case "balance" -> handleBalance(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "translate" -> handleTranslate(event);
            case "get-emojis" -> handleGetEmojis(event);
            case "give" -> handleGive(event);
            case "take" -> handleTake(event);
            case "transfer" -> handleTransfer(event);
            case "kill" -> handleKill(event);
            case "azkar" -> handleAzkar(event);
        }
    }

    private void handleAzkar(SlashCommandInteractionEvent event) {
        boolean hasAccess = event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals("1487195816220430406"));

        if (!hasAccess && event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            replyEphemeral(event, EmbedUtil.error("ACCESS DENIED", "هذا الأمر مخصص للإدارة فقط."));
            return;
        }

        String body = """
                **قائمة أوامر الأذكار اليدوية:**
                `!ص` : سبحان الله وبحمده، سبحان الله العظيم
                `!س` : أستغفر الله العظيم وأتوب إليه
                `!ح` : لا حول ولا قوة إلا بالله العلي العظيم
                `!ت` : لا إله إلا الله وحده لا شريك له
                `!ع` : الصلاة على النبي ﷺ
                `!جمعه` : دعاء وأذكار يوم الجمعة (سورة الكهف)
                `!الصباح` : إرسال لوحة أذكار الصباح
                `!المساء` : إرسال لوحة أذكار المساء
                """;
        replyEphemeral(event, EmbedUtil.containerBranded(null, "أوامر الأذكار (Admin)", body, null));
    }

    private void handleKill(SlashCommandInteractionEvent event) {
        boolean hasAccess = event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals("1487152572207861870"));

        if (!hasAccess) {
            replyEphemeral(event, EmbedUtil.error("ACCESS DENIED", "هذا الأمر مخصص لرتبة الإدارة العليا فقط."));
            return;
        }

        String type = event.getOption("type") != null ? event.getOption("type").getAsString() : "all";
        
        if (type.equals("all")) {
            eventManager.endGroupEvent();
        }

        reply(event, EmbedUtil.success("TERMINATION", "تم إيقاف الفعاليات المحددة (" + type + ") قسرياً بنجاح."));
    }

    private void handleGive(SlashCommandInteractionEvent event) {
        boolean hasAccess = event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals("1487152572207861870"));

        if (!hasAccess) {
            replyEphemeral(event, EmbedUtil.error("ACCESS DENIED", "هذا الأمر مخصص لرتبة الإدارة العليا فقط."));
            return;
        }

        net.dv8tion.jda.api.entities.User target = event.getOption("user").getAsUser();
        int amount = event.getOption("amount").getAsInt();

        economyService.addBalance(target.getId(), event.getGuild().getId(), amount);
        
        String body = String.format("تم إضافة **%d opex** بنجاح لحساب <@%s>.", amount, target.getId());
        reply(event, EmbedUtil.success("ADMIN GIFT", body));
    }

    private void handleTake(SlashCommandInteractionEvent event) {
        boolean hasAccess = event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals("1487152572207861870"));

        if (!hasAccess) {
            replyEphemeral(event, EmbedUtil.error("ACCESS DENIED", "هذا الأمر مخصص لرتبة الإدارة العليا فقط."));
            return;
        }

        net.dv8tion.jda.api.entities.User target = event.getOption("user").getAsUser();
        int amount = event.getOption("amount").getAsInt();

        boolean success = economyService.subtractBalance(target.getId(), event.getGuild().getId(), amount);
        
        if (success) {
            String body = String.format("تم سحب **%d opex** بنجاح من حساب <@%s>.", amount, target.getId());
            reply(event, EmbedUtil.success("ADMIN PENALTY", body));
        } else {
            replyEphemeral(event, EmbedUtil.error("FAILED", "رصيد العضو أقل من المبلغ المطلوب سحبه."));
        }
    }

    private void handleTransfer(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User target = event.getOption("user").getAsUser();
        int amount = event.getOption("amount").getAsInt();

        if (target.getId().equals(event.getUser().getId())) {
            replyEphemeral(event, EmbedUtil.error("INVALID ACTION", "لا يمكنك تحويل الأموال لنفسك!"));
            return;
        }

        if (amount <= 0) {
            replyEphemeral(event, EmbedUtil.error("INVALID AMOUNT", "يجب أن يكون المبلغ أكبر من 0."));
            return;
        }

        long senderBalance = economyService.getBalance(event.getUser().getId(), event.getGuild().getId());
        if (senderBalance < amount) {
            replyEphemeral(event, EmbedUtil.error("INSUFFICIENT FUNDS", "رصيدك غير كافٍ لإتمام عملية التحويل."));
            return;
        }

        economyService.subtractBalance(event.getUser().getId(), event.getGuild().getId(), amount);
        economyService.addBalance(target.getId(), event.getGuild().getId(), amount);

        String body = String.format("تم تحويل **%d opex** بنجاح إلى <@%s>.", amount, target.getId());
        reply(event, EmbedUtil.success("TRANSFER SUCCESS", body));
    }

    private void handleBalance(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User target = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        long balance = economyService.getBalance(target.getId(), event.getGuild().getId());

        String body = String.format("""
                ### رصيد المحفظة | WALLET BALANCE
                <divider>
                
                ▫️ **المستخدم:** <@%s>
                ▫️ **الرصيد الحالي:** `%d opex`
                """, target.getId(), balance);

        reply(event, EmbedUtil.containerBranded("ECONOMY", "Financial Status", body, EmbedUtil.BANNER_MAIN));
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("color-set") && event.getFocusedOption().getName().equals("role")) {
            List<Role> colorRoles = event.getGuild().getRoles().stream()
                    .filter(this::isColorRole)
                    .limit(25)
                    .collect(Collectors.toList());

            List<Command.Choice> choices = colorRoles.stream()
                    .filter(role -> role.getName().toLowerCase().contains(event.getFocusedOption().getValue().toLowerCase()))
                    .map(role -> new Command.Choice(role.getName(), role.getId()))
                    .limit(25)
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }
    }

    private boolean isColorRole(Role role) {
        // A color role usually has no permissions and is not a separator (no ---)
        // Also exclude @everyone
        return !role.isPublicRole() && 
               !role.getName().contains("---") && 
               !role.getName().contains("Roles") &&
               role.getPermissions().isEmpty();
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        String body = """
                ### 🛡️ Moderation
                `/ban`, `/kick`, `/mute-text`, `/timeout`, `/clear`, `/role`, `/warn-add`, `/lock`, `/hide`
                
                ### 📡 Info & Stats
                `/user`, `/server`, `/avatar`, `/roles`, `/profile`, **`/achive`**
                
                ### ⚙️ General
                `/ping`, `/roll`, `/colors`, `/translate`, `/get-emojis`
                
                ### 💰 Economy & Shop
                `/balance`, `/transfer`, `/shop`
                
                ### 🎮 Minigames (For Everyone)
                `/pipes`, `/speed`
                """;

        Container container = EmbedUtil.containerBranded("SYSTEM", "Bot Assistance", body, EmbedUtil.BANNER_MAIN);
        reply(event, container);
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        String body = String.format("""
                ### سرعة الاتصال | BOT LATENCY
                <divider>
                
                ▫️ **Gateway Latency:** `%dms`
                ▫️ **API Latency:** `Calculating...`
                """, gatewayPing);

        long startTime = System.currentTimeMillis();
        event.reply(new MessageCreateBuilder().setComponents(EmbedUtil.containerBranded("NETWORK", "Ping Check", body, EmbedUtil.BANNER_MAIN)).useComponentsV2(true).build())
             .useComponentsV2(true)
             .queue(msg -> {
                 long apiPing = System.currentTimeMillis() - startTime;
                 String updatedBody = String.format("""
                         ### سرعة الاتصال | BOT LATENCY
                         <divider>
                         
                         ▫️ **Gateway Latency:** `%dms`
                         ▫️ **API Latency:** `%dms`
                         """, gatewayPing, apiPing);
                 msg.editOriginal(new MessageEditBuilder().setComponents(EmbedUtil.containerBranded("NETWORK", "Ping Check", updatedBody, EmbedUtil.BANNER_MAIN)).useComponentsV2(true).build()).useComponentsV2(true).queue();
             });
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int result = new Random().nextInt(100) + 1;
        reply(event, EmbedUtil.success("Random Luck", "Your dice roll result is: **" + result + "**"));
    }

    private static final Map<String, String> COLOR_ROLES = Map.of(
        "color_red", "1499884576397316106",
        "color_turquoise", "1499884694936735744",
        "color_orange", "1499884764042072115",
        "color_gray", "1499885533277589656",
        "color_navy", "1499885778413813810",
        "color_blurple", "1499884810338832394",
        "color_asphalt", "1499885394752176190"
    );

    private void handleColors(SlashCommandInteractionEvent event) {
        String body = """
                ### نظام الألوان | COLOR SYSTEM
                <divider>
                
                اختر لونك المفضل من الأزرار أدناه.
                *سيتم تحديث لون رتبتك تلقائياً.*
                """;

        net.dv8tion.jda.api.components.buttons.Button b1 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_red", "Soft Red");
        net.dv8tion.jda.api.components.buttons.Button b2 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_turquoise", "Turquoise");
        net.dv8tion.jda.api.components.buttons.Button b3 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_orange", "Carrot Orange");
        net.dv8tion.jda.api.components.buttons.Button b4 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_gray", "Light Gray");
        net.dv8tion.jda.api.components.buttons.Button b5 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_navy", "Midnight Navy");
        
        net.dv8tion.jda.api.components.buttons.Button b6 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_blurple", "Blurple");
        net.dv8tion.jda.api.components.buttons.Button b7 = net.dv8tion.jda.api.components.buttons.Button.secondary("color_asphalt", "Wet Asphalt");

        net.dv8tion.jda.api.components.actionrow.ActionRow row1 = net.dv8tion.jda.api.components.actionrow.ActionRow.of(b1, b2, b3, b4, b5);
        net.dv8tion.jda.api.components.actionrow.ActionRow row2 = net.dv8tion.jda.api.components.actionrow.ActionRow.of(b6, b7);

        Container container = EmbedUtil.containerBranded("IDENTITY", "Color Selection", body, EmbedUtil.BANNER_MAIN, row1, row2);
        reply(event, container);
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        String roleId = event.getOption("role").getAsString();
        Role targetRole = event.getGuild().getRoleById(roleId);

        if (targetRole == null) {
            replyEphemeral(event, EmbedUtil.error("INVALID SELECTION", "الـــرتـــبـــة الـــمـــخـــتـــارة غـــيـــر صـــحـــيـــحـــة. يـــرجـــى الاخـــتـــيـــار مـــن الـــقـــائـــمـــة."));
            return;
        }

        // Identify all "Color Roles" currently on the member to remove them
        List<Role> currentMemberRoles = event.getMember().getRoles();
        List<Role> colorRolesToRemove = currentMemberRoles.stream()
                .filter(this::isColorRole)
                .collect(Collectors.toList());
        
        for (Role r : colorRolesToRemove) {
            if (!r.equals(targetRole)) {
                event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
            }
        }

        event.getGuild().addRoleToMember(event.getMember(), targetRole).queue(v -> {
            reply(event, EmbedUtil.success("Identity Update", "تـــم تـــحـــديـــد رتـــبـــة الـــلـــون [" + targetRole.getName() + "] بـــنـــجـــاح."));
        }, error -> {
            replyEphemeral(event, EmbedUtil.error("PERMISSION ERROR", "لا أمـــلـــك صـــلاحـــيـــات لـــإعـــطـــائـــك هـــذه الـــرتـــبـــة."));
        });
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text").getAsString();
        String lang = event.getOption("language").getAsString();
        
        event.deferReply().queue();

        String result = translationService.translate(text, lang);
        
        String body = String.format("""
                ### نتيجة الترجمة | TRANSLATION RESULT
                <divider>
                
                ▫️ **اللغة المستهدفة:** `%s`
                ▫️ **النص الأصلي:**
                > %s
                
                ▫️ **الترجمة:**
                > %s
                """, lang.toUpperCase(), text, result);

        Container container = EmbedUtil.containerBranded("LANGUAGE", "Translation Engine", body, EmbedUtil.BANNER_MAIN);
        event.getHook().sendMessage(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build()).useComponentsV2(true).queue();
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String emojiStr = event.getOption("emoji").getAsString();
        
        String body;
        String imageUrl = null;

        try {
            Emoji emoji = Emoji.fromFormatted(emojiStr);
            if (emoji instanceof CustomEmoji custom) {
                body = String.format("""
                        ### تفاصيل الإيموجي | EMOJI DETAILS
                        <divider>
                        
                        ▫️ **الاسم:** `%s`
                        ▫️ **المعرف (ID):** `%s`
                        ▫️ **الصيغة:** `<:%s:%s>`
                        ▫️ **رابط الصورة:** [Click Here](%s)
                        """, custom.getName(), custom.getId(), custom.getName(), custom.getId(), custom.getImageUrl());
                imageUrl = custom.getImageUrl();
            } else {
                body = String.format("""
                        ### تفاصيل الإيموجي | EMOJI DETAILS
                        <divider>
                        
                        ▫️ **النوع:** إيموجي افتراضي (Standard)
                        ▫️ **الرمز:** %s
                        ▫️ **الاسم:** `%s`
                        """, emoji.getName(), emoji.getName());
            }
        } catch (Exception e) {
            body = "❌ لـــم يـــتـــم الـــتـــعـــرف عـــلـــى الإيـــمـــوجـــي.";
        }

        reply(event, EmbedUtil.containerBranded("ASSET", "Emoji Inspection", body, imageUrl != null ? imageUrl : EmbedUtil.BANNER_MAIN));
    }

    private void reply(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder()
                .setComponents(c)
                .setAllowedMentions(java.util.Collections.emptyList())
                .useComponentsV2(true)
                .build();
        e.reply(msg).useComponentsV2(true).queue();
    }

    private void replyEphemeral(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder()
                .setComponents(c)
                .setAllowedMentions(java.util.Collections.emptyList())
                .useComponentsV2(true)
                .build();
        e.reply(msg).setEphemeral(true).useComponentsV2(true).queue();
    }
}
