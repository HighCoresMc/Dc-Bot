package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.TicketEntity;
import com.integrafty.opexy.entity.TicketMessageEntity;
import com.integrafty.opexy.repository.TicketRepository;
import com.integrafty.opexy.service.WhitelistSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.JDA;
import com.integrafty.opexy.service.LogManager;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketListener extends ListenerAdapter {

    private final JDA jda;
    private final TicketRepository ticketRepository;
    private final WhitelistSyncService whitelistSyncService;
    private final LogManager logManager;

    private static final String TICKET_CATEGORY_ID = "1487143174567628840";
    private static final String TRANSCRIPT_CHANNEL_ID = "1487147026427940955";
    private static final String STAFF_ROLE = "1487195816220430406";
    private static final java.util.List<String> PRIVILEGED_ROLES = java.util.Arrays.asList(
            "1190305586710073427", // owner-id
            "1350531070222794804", // owner-id-2
            "1487152572207861870", // op-staff
            "1489671552730402909", // secret-team
            "1487152572207861870", // opex-founder
            "1487152882074521771", // opex-manager
            "1487152897773797637" // department-manager
    );

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("ticket_manage_menu")) {
            handleManageMenu(event);
        }
    }

    private void handleManageMenu(StringSelectInteractionEvent event) {
        String selected = event.getValues().get(0);
        switch (selected) {
            case "ticket_manage_add" -> {
                Modal modal = Modal.create("modal_ticket_add", "إضـافـة عـضـو لـلـتـذكـرة")
                        .addComponents(Label.of("ID الـعـضـو",
                                TextInput.create("user_id", TextInputStyle.SHORT)
                                        .setPlaceholder("ادخل الأيدي الخاص بالعضو لإضافته...").build()))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_remove" -> {
                Modal modal = Modal.create("modal_ticket_remove", "إزالـة عـضـو مـن الـتـذكـرة")
                        .addComponents(Label.of("ID الـعـضـو",
                                TextInput.create("user_id", TextInputStyle.SHORT)
                                        .setPlaceholder("ادخل الأيدي الخاص بالعضو لإزالته...").build()))
                        .build();
                event.replyModal(modal).queue();
            }
            case "ticket_manage_rename" -> {
                Modal modal = Modal.create("modal_ticket_rename", "تـغـيـيـر اسـم الـتـذكـرة")
                        .addComponents(Label.of("الاسـم الـجـديـد",
                                TextInput.create("new_name", TextInputStyle.SHORT)
                                        .setPlaceholder("ادخل الاسم الجديد للتذكرة هنا...").build()))
                        .build();
                event.replyModal(modal).queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        // Check if it's an administrative button
        if (buttonId.startsWith("ticket_") && !buttonId.equals("ticket_support") &&
                !buttonId.equals("ticket_complaint") && !buttonId.equals("ticket_hire") &&
                !buttonId.equals("ticket_whitelist") && !buttonId.equals("ticket_team")) {

            boolean isStaff = event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(STAFF_ROLE));
            boolean isPrivileged = event.getMember().getRoles().stream()
                    .anyMatch(r -> PRIVILEGED_ROLES.contains(r.getId()));

            if (!isStaff && !isPrivileged) {
                event.reply("❌ عذراً، هذه الأزرار مخصصة للفريق الإداري فقط.").setEphemeral(true).queue();
                return;
            }
        }

        if (buttonId.equals("ticket_close")) {
            handleTicketClose(event);
        } else if (buttonId.equals("ticket_claim")) {
            handleClaim(event);
        } else if (buttonId.equals("ticket_unclaim")) {
            handleUnclaim(event);
        } else if (buttonId.equals("ticket_close_final")) {
            handleFinalClose(event);
        } else if (buttonId.equals("ticket_close_cancel")) {
            event.reply("✅ تـم الـتـراجـع عـن الإغـلاق.").setEphemeral(true).queue();
        } else if (buttonId.equals("ticket_reopen")) {
            handleReopen(event);
        } else if (buttonId.equals("ticket_transcript")) {
            handleTranscript(event);
        } else if (buttonId.equals("ticket_delete_init")) {
            handleDeleteRequest(event);
        } else if (buttonId.equals("ticket_delete_final")) {
            event.getChannel().delete().queue();
        } else if (buttonId.equals("ticket_delete_cancel")) {
            event.reply("✅ تـم الـتـراجـع عـن الـحـذف.").setEphemeral(true).queue();
        } else if (buttonId.startsWith("ticket_")) {
            handleTicketModal(event, buttonId);
        }
    }

    private static final String WHITELIST_ROLE_ID = "1499355941752012900";

    private void handleTicketModal(ButtonInteractionEvent event, String buttonId) {
        String categoryId = buttonId.replace("ticket_", "");

        // Whitelist
        if (categoryId.equals("whitelist")) {
            boolean alreadyWhitelisted = event.getMember().getRoles().stream()
                    .anyMatch(r -> r.getId().equals(WHITELIST_ROLE_ID));
            if (alreadyWhitelisted) {
                event.reply("❌ أنـت مـسـجـل بـالـفـعـل فـي قـائـمـة الـوايـت لـيـسـت.").setEphemeral(true).queue();
                return;
            }
        }

        Modal modal = switch (categoryId) {
            case "support" -> Modal.create("modal_ticket_support", "الـدعم الـفـنـي")
                    .addComponents(
                            Label.of("نـوع الـدعم (مايـن / دـسكورد)",
                                    TextInput.create("support_type", TextInputStyle.SHORT)
                                            .setPlaceholder("اكتب النوع هنا").build()),
                            Label.of("شـرح الـمـشـكـلـة", TextInput.create("issue", TextInputStyle.PARAGRAPH)
                                    .setPlaceholder("اكتب تفاصيل المشكلة هنا").build()))
                    .build();
            case "complaint" -> Modal.create("modal_ticket_complaint", "الـشـكـاوى")
                    .addComponents(
                            Label.of("ID الـشـخـص / Username",
                                    TextInput.create("target_user", TextInputStyle.SHORT)
                                            .setPlaceholder("اكتب بيانات الشخص هنا").build()),
                            Label.of("أيـن (مـايـنـكـرافـت / دـسـكـورد)",
                                    TextInput.create("location", TextInputStyle.SHORT).setPlaceholder("اكتب المكان هنا")
                                            .build()))
                    .build();
            case "hire" -> Modal.create("modal_ticket_hire", "الـتـقـديـم عـلـى الإدارة")
                    .addComponents(
                            Label.of("الاسـم", TextInput.create("name", TextInputStyle.SHORT).build()),
                            Label.of("الـعـمـر", TextInput.create("age", TextInputStyle.SHORT).build()),
                            Label.of("الـمـهـارات", TextInput.create("skills", TextInputStyle.PARAGRAPH).build()),
                            Label.of("الأقـسـام (Discord, Minecraft, Hype)",
                                    TextInput.create("depts", TextInputStyle.SHORT).build()))
                    .build();
            case "whitelist" -> Modal.create("modal_ticket_whitelist", "الـوايـت لـيـسـت")
                    .addComponents(
                            Label.of("MC Username",
                                    TextInput.create("mc_name", TextInputStyle.SHORT)
                                            .setPlaceholder("اكتب اسمك في اللعبة").build()),
                            Label.of("Version",
                                    TextInput.create("version", TextInputStyle.SHORT).setPlaceholder("Java/Bedrock")
                                            .build()),
                            Label.of("Type (كراك/اصليه)", TextInput.create("account_type", TextInputStyle.SHORT)
                                    .setPlaceholder("Example: اصلية").build()))
                    .build();
            case "team" -> Modal.create("modal_ticket_team", "الـتـقـديـم عـلـى فـريـق")
                    .addComponents(
                            Label.of("اسـم الـتـيـم *",
                                    TextInput.create("team_name", TextInputStyle.SHORT)
                                            .setPlaceholder("اكتب اسم التيم هنا").setRequired(true).build()),
                            Label.of("كـود الـلـون *",
                                    TextInput.create("team_color", TextInputStyle.SHORT).setPlaceholder("مثال: #FFFF")
                                            .setRequired(true).build()),
                            Label.of("الـعـضـو الـثـانـي *",
                                    TextInput.create("member_2", TextInputStyle.SHORT)
                                            .setPlaceholder("ID / Mention / Username").setRequired(true).build()),
                            Label.of("الـعـضـو الـثـالـث والـرّابـع",
                                    TextInput.create("member_3", TextInputStyle.SHORT)
                                            .setPlaceholder("ID / Mention / Username لعضو 3 وعضو 4 (اختياري)").setRequired(false).build()),
                            Label.of("هـل يـوجـد شـعـار؟ *",
                                    TextInput.create("has_logo", TextInputStyle.SHORT)
                                            .setPlaceholder("نعم / لا").setRequired(true).build()))
                    .build();
            default -> null;
        };

        if (modal != null) {
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (modalId.startsWith("modal_ticket_")) {
            if (modalId.equals("modal_ticket_add")) {
                handleAddMember(event);
            } else if (modalId.equals("modal_ticket_remove")) {
                handleRemoveMember(event);
            } else if (modalId.equals("modal_ticket_rename")) {
                handleRenameTicket(event);
            } else if (modalId.equals("modal_ticket_team")) {
                handleTeamTicketCreation(event);
            } else {
                handleTicketCreationFromModal(event);
            }
        }
    }

    private void handleAddMember(ModalInteractionEvent event) {
        String userId = event.getValue("user_id").getAsString();
        event.getGuild().retrieveMemberById(userId).queue(member -> {
            event.getChannel().asTextChannel().getManager().putPermissionOverride(member,
                    java.util.EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
            event.reply("✅ تم إضافة " + member.getAsMention() + " إلى التذكرة.").queue();
        }, error -> event.reply("❌ لم يتم العثور على عضو بهذا الأيدي.").setEphemeral(true).queue());
    }

    private void handleRemoveMember(ModalInteractionEvent event) {
        String userId = event.getValue("user_id").getAsString();
        event.getGuild().retrieveMemberById(userId).queue(member -> {
            event.getChannel().asTextChannel().getManager()
                    .putPermissionOverride(member, null, java.util.EnumSet.of(Permission.VIEW_CHANNEL)).queue();
            event.reply("✅ تـم إزالـة " + member.getAsMention() + " مـن الـتـذكـرة.").queue();
        }, error -> event.reply("❌ لـم يـتـم الـعـثـور عـلـى عـضـو بـهـذا الأيـدي.").setEphemeral(true).queue());
    }

    private void handleRenameTicket(ModalInteractionEvent event) {
        String newName = event.getValue("new_name").getAsString();
        event.getChannel().asTextChannel().getManager().setName(newName).queue();
        event.reply("✅ تـم تـغـيـيـر اسـم الـتـذكـرة إلـى: " + newName).queue();
    }

    private void handleTicketCreationFromModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();

        boolean isExempt = event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("1487152572207861870"));

        if (!isExempt && ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لـديـك تـذكـرة مـفـتـوحـة بـالـفـعـل! يـرجـى إغـلاقـهـا أولاً.").setEphemeral(true).queue();
            return;
        }

        String categoryName = "";
        String categoryId = event.getModalId().replace("modal_ticket_", "");

        switch (categoryId) {
            case "support":
                categoryName = "support";
                break;
            case "complaint":
                categoryName = "complaint";
                break;
            case "hire":
                categoryName = "Hire";
                break;
            case "whitelist":
                categoryName = "Whitelist";
                break;
        }

        Integer lastNum = ticketRepository.findMaxTicketNumberByCategory(categoryId);
        int nextNum = (lastNum == null) ? 1 : lastNum + 1;
        String formattedNum = String.format("%03d", nextNum);

        Guild guild = event.getGuild();
        Member member = event.getMember();
        String channelName = categoryName + "-" + formattedNum;

        final String finalCategoryName = categoryName;
        final int finalNextNum = nextNum;
        final String finalCategoryId = categoryId;

        boolean isWhitelist = finalCategoryId.equals("whitelist");
        EnumSet<Permission> memberPerms = isWhitelist ? EnumSet.of(Permission.VIEW_CHANNEL)
                : EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

        guild.createTextChannel(channelName)
                .setParent(guild.getCategoryById(TICKET_CATEGORY_ID))
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, memberPerms, isWhitelist ? EnumSet.of(Permission.MESSAGE_SEND) : null)
                .addPermissionOverride(guild.getRoleById(STAFF_ROLE),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .queue(channel -> {
                    // Set overrides for privileged roles to ensure they always see and can write
                    for (String roleId : PRIVILEGED_ROLES) {
                        net.dv8tion.jda.api.entities.Role role = guild.getRoleById(roleId);
                        if (role != null) {
                            channel.getManager().putRolePermissionOverride(role.getIdLong(),
                                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
                        }
                    }

                    if (isWhitelist) {
                        guild.addRoleToMember(member, guild.getRoleById("1499355941752012900")).queue();
                    }
                    TicketEntity ticket = new TicketEntity();
                    ticket.setUserId(userId);
                    ticket.setChannelId(channel.getId());
                    ticket.setCategory(finalCategoryId);
                    ticket.setTicketNumber(finalNextNum);
                    ticketRepository.save(ticket);

                    StringBuilder ticketBody = new StringBuilder("Welcome " + member.getAsMention() + " 👋\n\n");
                    String subject = "";
                    String details = "";
                    String sector = "";

                    if (finalCategoryId.equals("support")) {
                        sector = "SUPPORT CENTER";
                        subject = "Support Request • " + event.getValue("support_type").getAsString();
                        details = "Issue: " + event.getValue("issue").getAsString() + "\nService: Minecraft/Discord";
                    } else if (finalCategoryId.equals("complaint")) {
                        sector = "COMPLAINT CENTER";
                        subject = "Complaint Request • " + event.getValue("location").getAsString();
                        details = "Target: " + event.getValue("target_user").getAsString();
                    } else if (finalCategoryId.equals("hire")) {
                        sector = "HIRE CENTER";
                        subject = "Staff Application • " + event.getValue("depts").getAsString();
                        details = "Name: " + event.getValue("name").getAsString() + "\nAge: "
                                + event.getValue("age").getAsString();
                    } else if (finalCategoryId.equals("whitelist")
                            || finalCategoryId.equals("modal_ticket_whitelist")) {
                        sector = "WHITELIST CENTER";
                        subject = "Whitelist Request • " + event.getValue("mc_name").getAsString();
                        details = "Discord: " + member.getUser().getName() + " (`" + member.getId() + "`)" +
                                "\nVersion: " + event.getValue("version").getAsString() +
                                "\nAccount: " + event.getValue("account_type").getAsString();
                    }

                    ticketBody.append("**Subject:** ").append(subject).append("\n");
                    ticketBody.append("**Details:** ").append(details).append("\n\n");

                    if (isWhitelist) {
                        ticketBody.append("### ✅ تـم قـبـول طـلـبـك مـبـدئـيـاً\n")
                                .append("يـرجـى اتـبـاع الـخـطـوات الـمـرسـلـة بـالأسـفـل لـتـكـمـلـة الـتـفـعـيـل.");
                    } else {
                        ticketBody.append(
                                "A staff member will be with you shortly — please describe your issue in full detail.");
                    }

                    Container welcomeContainer;
                    if (isWhitelist) {
                        welcomeContainer = EmbedUtil.containerBranded(
                                sector,
                                "Case #" + finalCategoryName.toUpperCase() + "-" + formattedNum,
                                ticketBody.toString(),
                                EmbedUtil.BANNER_SUPPORT);
                    } else {
                        welcomeContainer = EmbedUtil.containerBranded(
                                sector,
                                "Case #" + finalCategoryName.toUpperCase() + "-" + formattedNum,
                                ticketBody.toString(),
                                EmbedUtil.BANNER_SUPPORT,
                                ActionRow.of(
                                        net.dv8tion.jda.api.components.selections.StringSelectMenu
                                                .create("ticket_manage_menu")
                                                .setPlaceholder("إدارة الـتـذكـرة...")
                                                .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                                                .addOption("إضـافـة عـضـو", "ticket_manage_add")
                                                .addOption("إزالـة عـضـو", "ticket_manage_remove")
                                                .build()),
                                ActionRow.of(
                                        Button.secondary("ticket_claim", "اسـتـلام الـتـذكـرة"),
                                        Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")));
                    }

                    String ping = "<@&1487152917763981574> " + member.getAsMention();
                    channel.sendMessage(ping).queue();

                    channel.sendMessage(
                            new MessageCreateBuilder().setComponents(welcomeContainer).useComponentsV2(true).build())
                            .useComponentsV2(true)
                            .queue();

                    // LOGGING
                    String logDetails = "### 🎫 Ticket Created\n" +
                            "▫️ **User:** " + member.getAsMention() + " (`" + member.getId() + "`)\n" +
                            "▫️ **Category:** `" + finalCategoryName + "`\n" +
                            "▫️ **Channel:** " + channel.getAsMention();
                    logManager.logEmbed(guild, LogManager.LOG_TICKETS,
                            EmbedUtil.createOldLogEmbed("ticket-create", logDetails, member, null, null,
                                    EmbedUtil.SUCCESS));

                    if (isWhitelist) {
                        String discordInfo = member.getUser().getName() + " (" + member.getId() + ")";
                        whitelistSyncService.syncToSupabase(
                                discordInfo,
                                event.getValue("mc_name").getAsString(),
                                event.getValue("version").getAsString(),
                                event.getValue("account_type").getAsString());

                        // Send detailed steps as a separate message
                        String steps = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                                "### 📋 خـطـوات الـتـفـعـيـل الـنـهـائـيـة\n\n" +
                                "**1.** تـم تـسـجـيـل بـيـانـاتـك فـي قـاعـدة بـيـانـات الـسـيـرفـر بـنـجـاح.\n" +
                                "**2.** تـوجـه الآن إلـى روم الـتـفـعـيـل: <#1488279212786843850>.\n" +
                                "**3.** قـم بـالـضـغـط عـلـى إيـمـوجـي 🟢 ريـأكـشـن لـتـفـعـيـل حـسـابـك آلياً.\n\n" +
                                "**مـلاحـظـة:** سـيـتـم تـفـعـيـلـك فـور وضـع الـريـأكـشـن، مـرحـبـاً بـك فـي الـسـيـرفـر!\n"
                                +
                                "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
                        channel.sendMessage(steps).queue();
                    }

                    Container successCont = EmbedUtil.success("الإنـشـاء",
                            "تـم إنـشـاء تـذكـرتـك بـنـجـاح: " + channel.getAsMention());
                    event.reply(new MessageCreateBuilder().setComponents(successCont).useComponentsV2(true).build())
                            .setEphemeral(true)
                            .useComponentsV2(true)
                            .queue();
                }, error -> {
                    Container errorCont = EmbedUtil.error("ERROR",
                            "حدث خطأ أثناء إنشاء الغرفة، يرجى التأكد من صلاحيات البوت.");
                    event.reply(new MessageCreateBuilder().setComponents(errorCont).useComponentsV2(true).build())
                            .setEphemeral(true).useComponentsV2(true).queue();
                    log.error("Error creating ticket channel", error);
                });
    }

    // Member Resolver Helper
    private Member resolveMemberInput(Guild guild, String input) {
        if (input == null || input.isBlank()) return null;
        String cleanId = input.replaceAll("[^0-9]", "");
        if (cleanId.length() >= 17 && cleanId.length() <= 20) {
            Member m = guild.getMemberById(cleanId);
            if (m != null) return m;
            try {
                return guild.retrieveMemberById(cleanId).complete();
            } catch (Exception ignored) {}
        }
        String searchStr = input.replaceAll("[<@!>]", "").trim();
        java.util.List<Member> byName = guild.getMembersByName(searchStr, true);
        if (!byName.isEmpty()) return byName.get(0);
        java.util.List<Member> byEffective = guild.getMembersByEffectiveName(searchStr, true);
        if (!byEffective.isEmpty()) return byEffective.get(0);
        return null;
    }

    // Team Ticket Handling
    private void handleTeamTicketCreation(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        Guild guild = event.getGuild();
        Member creator = event.getMember();

        boolean isExempt = creator.getRoles().stream().anyMatch(r -> r.getId().equals("1487152572207861870"));
        if (!isExempt && ticketRepository.existsByUserIdAndStatus(userId, "OPEN")) {
            event.reply("❌ لـديـك تـذكـرة مـفـتـوحـة بـالـفـعـل! يـرجـى إغـلاقـهـا أولاً.").setEphemeral(true).queue();
            return;
        }

        String teamName = event.getValue("team_name").getAsString().trim();
        String teamColor = event.getValue("team_color").getAsString().trim();
        String member2Input = event.getValue("member_2") != null ? event.getValue("member_2").getAsString().trim() : "";
        String member3Input = event.getValue("member_3") != null ? event.getValue("member_3").getAsString().trim() : "";
        String hasLogoInput = event.getValue("has_logo") != null ? event.getValue("has_logo").getAsString().trim() : "";

        String formattedColor = teamColor;
        if (!formattedColor.startsWith("#")) {
            formattedColor = "#" + formattedColor;
        }

        Member member2 = resolveMemberInput(guild, member2Input);
        if (member2 == null) {
            event.reply("❌ لم يتم العثور على العضو الثاني (`" + member2Input + "`) في السيرفر.").setEphemeral(true).queue();
            return;
        }

        Member member3 = null;
        if (!member3Input.isBlank()) {
            member3 = resolveMemberInput(guild, member3Input);
            if (member3 == null) {
                event.reply("❌ لم يتم العثور على العضو الثالث (`" + member3Input + "`) في السيرفر.").setEphemeral(true).queue();
                return;
            }
        }

        java.util.List<Member> targetMembers = new java.util.ArrayList<>();
        if (!member2.getId().equals(creator.getId())) targetMembers.add(member2);
        if (member3 != null && !member3.getId().equals(creator.getId()) && !member3.getId().equals(member2.getId())) {
            targetMembers.add(member3);
        }

        java.util.List<String> missingWhitelist = new java.util.ArrayList<>();
        if (!creator.getRoles().stream().anyMatch(r -> r.getId().equals(WHITELIST_ROLE_ID))) {
            missingWhitelist.add("العضو #1 (صاحب الطلب) " + creator.getAsMention());
        }
        if (!member2.getRoles().stream().anyMatch(r -> r.getId().equals(WHITELIST_ROLE_ID))) {
            missingWhitelist.add("العضو #2 " + member2.getAsMention());
        }
        if (member3 != null && !member3.getRoles().stream().anyMatch(r -> r.getId().equals(WHITELIST_ROLE_ID))) {
            missingWhitelist.add("العضو #3 " + member3.getAsMention());
        }

        if (!missingWhitelist.isEmpty()) {
            String details = String.join("، ", missingWhitelist);
            event.reply("❌ " + details + " ليس لديه رتبة الوايت ليست!\nيرجى إبلاغه بفتح تكت وايت ليست من اللوحة بالأعلى أولاً.").setEphemeral(true).queue();
            return;
        }

        Integer lastNum = ticketRepository.findMaxTicketNumberByCategory("team");
        int nextNum = (lastNum == null) ? 1 : lastNum + 1;
        String formattedNum = String.format("%03d", nextNum);

        String channelName = "Team-" + teamName.replaceAll("[^a-zA-Z0-9\\u0600-\\u06FF_-]", "-");
        if (channelName.length() > 30) {
            channelName = channelName.substring(0, 30);
        }

        final int finalNextNum = nextNum;
        final String finalTeamName = teamName;
        final String finalTeamColor = formattedColor;
        final String finalHasLogo = hasLogoInput;
        final java.util.List<Member> finalTeamMembers = targetMembers;

        guild.createTextChannel(channelName)
            .setParent(guild.getCategoryById(TICKET_CATEGORY_ID))
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(creator, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            .addPermissionOverride(guild.getRoleById(STAFF_ROLE), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            .queue(channel -> {
                for (String roleId : PRIVILEGED_ROLES) {
                    net.dv8tion.jda.api.entities.Role role = guild.getRoleById(roleId);
                    if (role != null) {
                        channel.getManager().putRolePermissionOverride(role.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
                    }
                }

                for (Member m : finalTeamMembers) {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
                }

                TicketEntity ticket = new TicketEntity();
                ticket.setUserId(userId);
                ticket.setChannelId(channel.getId());
                ticket.setCategory("team");
                ticket.setTicketNumber(finalNextNum);
                ticketRepository.save(ticket);

                StringBuilder membersMentionList = new StringBuilder(creator.getAsMention());
                for (Member m : finalTeamMembers) {
                    membersMentionList.append(" ").append(m.getAsMention());
                }

                StringBuilder ticketBody = new StringBuilder("Welcome " + creator.getAsMention() + " 👋\n\n");
                ticketBody.append("**Team Name:** ").append(finalTeamName).append("\n");
                ticketBody.append("**Team Color :** ").append(finalTeamColor).append("\n");
                ticketBody.append("**Team Members :** ").append(membersMentionList).append("\n\n");
                ticketBody.append("A staff member will be with you shortly — please Make Sure That All Of The Team Members In The Game.");

                Container welcomeContainer = EmbedUtil.containerBranded(
                    "TEAM APPLICATION",
                    "Case #TEAM-" + formattedNum,
                    ticketBody.toString(),
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(
                        net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                            .setPlaceholder("إدارة الـتـذكـرة...")
                            .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                            .addOption("إضـافـة عـضـو", "ticket_manage_add")
                            .addOption("إزالـة عـضـو", "ticket_manage_remove")
                            .build()
                    ),
                    ActionRow.of(
                        Button.secondary("ticket_claim", "اسـتـلام الـتـذكـرة"),
                        Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")
                    )
                );

                String ping = "<@&1487152917763981574> " + membersMentionList;
                channel.sendMessage(ping).queue();

                channel.sendMessage(new MessageCreateBuilder().setComponents(welcomeContainer).useComponentsV2(true).build())
                    .useComponentsV2(true)
                    .queue();

                Container teamNotice = EmbedUtil.containerBranded(
                    "NOTICE",
                    "Team Members",
                    "📡 تمت اضافتكم في تكت انشاء تيم **" + finalTeamName + "** " + membersMentionList,
                    null
                );
                channel.sendMessage(new MessageCreateBuilder().setContent(membersMentionList.toString()).setComponents(teamNotice).useComponentsV2(true).build())
                    .useComponentsV2(true)
                    .queue();

                String lowerLogo = finalHasLogo.toLowerCase();
                boolean hasLogo = lowerLogo.contains("نعم") || lowerLogo.contains("اي") || lowerLogo.contains("ايوا") || lowerLogo.contains("yes") || lowerLogo.contains("y") || lowerLogo.contains("true");
                if (hasLogo) {
                    Container logoNotice = EmbedUtil.containerBranded(
                        "NOTICE",
                        "Team Logo",
                        "📡 Please Sent Your Logo Here Until Staff Team Review Your Order",
                        null
                    );
                    channel.sendMessage(new MessageCreateBuilder().setComponents(logoNotice).useComponentsV2(true).build())
                        .useComponentsV2(true)
                        .queue();
                }

                String logDetails = "### 🎫 Team Ticket Created\n" +
                        "▫️ **User:** " + creator.getAsMention() + " (`" + creator.getId() + "`)\n" +
                        "▫️ **Team:** `" + finalTeamName + "`\n" +
                        "▫️ **Channel:** " + channel.getAsMention();
                logManager.logEmbed(guild, LogManager.LOG_TICKETS,
                        EmbedUtil.createOldLogEmbed("ticket-create", logDetails, creator, null, null, EmbedUtil.SUCCESS));

                Container successCont = EmbedUtil.success("الإنـشـاء", "تـم إنـشـاء تـذكـرة الـتـيـم بـنـجـاح: " + channel.getAsMention());
                event.reply(new MessageCreateBuilder().setComponents(successCont).useComponentsV2(true).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
            }, error -> {
                Container errorCont = EmbedUtil.error("ERROR", "حدث خطأ أثناء إنشاء الغرفة، يرجى التأكد من صلاحيات البوت.");
                event.reply(new MessageCreateBuilder().setComponents(errorCont).useComponentsV2(true).build())
                    .setEphemeral(true).useComponentsV2(true).queue();
                log.error("Error creating team ticket channel", error);
            });
    }

    private void handleTicketClose(ButtonInteractionEvent event) {
        Container confirm = EmbedUtil.containerBranded(
                "تـأكـيـد الإغـلاق",
                "هـل أنـت مـتـأكـد؟",
                "### هـل أنـت مـتـأكـد مـن رغـبـتـك فـي إغـلاق هـذه الـتـذكـرة؟",
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                        Button.secondary("ticket_close_final", "تـأكـيـد الإغـلاق"),
                        Button.secondary("ticket_close_cancel", "تـراجـع")));
        event.reply(new MessageCreateBuilder().setComponents(confirm).useComponentsV2(true).build())
                .setEphemeral(true).queue();
    }

    private void handleFinalClose(ButtonInteractionEvent event) {
        log.info("Starting handleFinalClose for channel: {}", event.getChannel().getName());

        event.deferReply(true).queue(hook -> {
            log.info("Interaction deferred for final close");
            try {
                TextChannel channel = event.getChannel().asTextChannel();
                Member member = event.getMember();
                String channelId = channel.getId();

                log.info("Searching for ticket in DB for channelId: {}", channelId);
                ticketRepository.findByChannelId(channelId).ifPresentOrElse(ticket -> {
                    log.info("Ticket found in DB. Updating status to CLOSED");
                    ticket.setStatus("CLOSED");
                    ticketRepository.save(ticket);
                    log.info("Ticket status updated in DB");

                    // 1. Rename channel
                    log.info("Renaming channel...");
                    String newName = channel.getName();
                    if (!newName.endsWith("-c")) {
                        newName += "-c";
                    }
                    channel.getManager().setName(newName).queue(
                            v -> log.info("Channel renamed successfully"),
                            e -> log.warn("Channel rename failed: {}", e.getMessage()));

                    // 2. Remove client write access
                    log.info("Updating permissions for client: {}", ticket.getUserId());
                    Member client = event.getGuild().getMemberById(ticket.getUserId());
                    if (client != null) {
                        channel.getManager().putMemberPermissionOverride(client.getIdLong(),
                                EnumSet.of(Permission.VIEW_CHANNEL),
                                EnumSet.of(Permission.MESSAGE_SEND)).queue(
                                        v -> log.info("Client permissions updated"),
                                        e -> log.warn("Client permissions update failed: {}", e.getMessage()));
                    }

                    // 3. Send Archive Panel
                    log.info("Sending archive panel...");
                    Container panel = EmbedUtil.containerBranded(
                            "ARCHIVES",
                            "لـوحـة الـتـحـكـم",
                            "### تـم إغـلاق الـتـذكـرة\nبـواسـطـة الـعـضـو **" + member.getEffectiveName()
                                    + "**.\n\nاخـتـر إجـراء مـن الأسـفـل.",
                            EmbedUtil.BANNER_SUPPORT,
                            ActionRow.of(
                                    Button.secondary("ticket_reopen", "إعـادة فـتـح"),
                                    Button.secondary("ticket_transcript", "تـــران ســـــكـــربـــت"),
                                    Button.secondary("ticket_delete_init", "حـذف الـتـذكـرة")));

                    channel.sendMessage(new MessageCreateBuilder().setComponents(panel).useComponentsV2(true).build())
                            .useComponentsV2(true).queue(
                                    v -> log.info("Archive panel sent successfully"),
                                    e -> log.error("Failed to send archive panel", e));

                    // LOGGING
                    String logDetails = "### 🔒 Ticket Closed\n" +
                            "▫️ **Operator:** " + member.getAsMention() + "\n" +
                            "▫️ **Channel:** `" + channel.getName() + "`\n" +
                            "▫️ **Status:** `ARCHIVED`";
                    logManager.logEmbed(event.getGuild(), LogManager.LOG_TICKETS,
                            EmbedUtil.createOldLogEmbed("ticket-close", logDetails, member, null, null,
                                    EmbedUtil.DANGER));

                    hook.sendMessage("✅ تـم إغـلاق الـتـذكـرة بـنـجـاح.").setEphemeral(true).queue();
                }, () -> {
                    log.warn("No ticket found in DB for channelId: {}", channelId);
                    hook.sendMessage("❌ لم يتم العثور على بيانات التذكرة في قاعدة البيانات.").setEphemeral(true)
                            .queue();
                });
            } catch (Exception e) {
                log.error("Error in handleFinalClose deferred logic", e);
                hook.sendMessage("❌ حدث خطأ داخلي أثناء إغلاق التذكرة: " + e.getMessage()).setEphemeral(true).queue();
            }
        }, err -> {
            log.error("Failed to defer interaction for final close", err);
        });
    }

    private String extractMessageBody(net.dv8tion.jda.api.entities.Message message, String defaultUserId) {
        if (message == null) return "مرحباً بك <@" + defaultUserId + "> 👋";
        
        java.util.List<String> textContents = new java.util.ArrayList<>();
        for (net.dv8tion.jda.api.components.Component component : message.getComponents()) {
            if (component instanceof Container container) {
                for (ContainerChildComponent comp : container.getComponents()) {
                    if (comp instanceof TextDisplay td) {
                        String content = td.getContent();
                        if (content != null && !content.startsWith("### ►") && !content.startsWith("## ")) {
                            textContents.add(content);
                        }
                    }
                }
            }
        }
        
        if (!textContents.isEmpty()) {
            return String.join("\n\n", textContents);
        }
        
        if (!message.getEmbeds().isEmpty() && message.getEmbeds().get(0).getDescription() != null) {
            return message.getEmbeds().get(0).getDescription();
        }
        
        return "مرحباً بك <@" + defaultUserId + "> 👋";
    }

    private void handleClaim(ButtonInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("❌ لا تـمـلـك صـلاحـيـة لاسـتـلام الـتـذاكـر.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();

        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();
            ticket.setStaffId(event.getUser().getId());
            ticketRepository.save(ticket);

            String category = channel.getName().split("-")[0];
            String staffName = event.getMember().getEffectiveName().toLowerCase().replace(" ", "");
            channel.getManager().setName(category + "-" + staffName).queue();

            String existingBody = extractMessageBody(event.getMessage(), ticket.getUserId());
            if (!existingBody.contains("📌 **تـم اسـتـلام الـتـذكـرة بـواسـطـة:**")) {
                existingBody = existingBody + "\n\n📌 **تـم اسـتـلام الـتـذكـرة بـواسـطـة:** " + event.getMember().getAsMention();
            }

            Container claimedContainer = EmbedUtil.containerBranded(
                    "نـظام الـتـذاكـر",
                    "تـم الاسـتـلام",
                    existingBody,
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(
                            net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                                    .setPlaceholder("إدارة الـتـذكـرة...")
                                    .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                                    .addOption("إضـافـة عـضـو", "ticket_manage_add")
                                    .addOption("إزالـة عـضـو", "ticket_manage_remove")
                                    .build()),
                    ActionRow.of(
                            Button.secondary("ticket_unclaim", "إلـغـاء الاسـتـلام"),
                            Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")));

            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(claimedContainer)
                    .useComponentsV2(true)
                    .build()).queue();

            Container notice = EmbedUtil.containerBranded(
                    "NOTICE",
                    "Claimed",
                    "📡 Ticket Handled By: " + event.getMember().getAsMention(),
                    null);

            net.dv8tion.jda.api.entities.Role staffRole = event.getGuild().getRoleById(STAFF_ROLE);
            if (staffRole != null) {
                channel.getManager().putRolePermissionOverride(staffRole.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        EnumSet.of(Permission.MESSAGE_SEND)).queue();
            }
            channel.getManager().putMemberPermissionOverride(event.getUser().getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                    null).queue();

            String logDetails = "### 📌 Ticket Claimed\n" +
                    "▫️ **Operator:** " + event.getMember().getAsMention() + "\n" +
                    "▫️ **Channel:** " + channel.getAsMention();
            logManager.logEmbed(event.getGuild(), LogManager.LOG_TICKETS,
                    EmbedUtil.createOldLogEmbed("ticket-claim", logDetails, event.getMember(), null, null,
                            EmbedUtil.WARNING));

            channel.sendMessage(new MessageCreateBuilder().setComponents(notice).useComponentsV2(true).build())
                    .useComponentsV2(true).queue();
        }
    }

    private void handleUnclaim(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());

        if (ticketOpt.isPresent()) {
            TicketEntity ticket = ticketOpt.get();

            if (ticket.getStaffId() == null || !event.getUser().getId().equals(ticket.getStaffId())) {
                event.reply("❌ لا يـمـكـنـك إلـغـاء اسـتـلام تـذكـرة مـسـتـلـمـة مـن قـبـل شـخـص آخـر.").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            ticket.setStaffId(null);
            ticketRepository.save(ticket);

            String existingBody = extractMessageBody(event.getMessage(), ticket.getUserId());
            if (existingBody.contains("\n\n📌 **تـم اسـتـلام الـتـذكـرة بـواسـطـة:**")) {
                int idx = existingBody.indexOf("\n\n📌 **تـم اسـتـلام الـتـذكـرة بـواسـطـة:**");
                existingBody = existingBody.substring(0, idx);
            }

            Container restoredContainer = EmbedUtil.containerBranded(
                    "نـظام الـتـذاكـر",
                    "بـانـتـظـار الـرد",
                    existingBody,
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(
                            net.dv8tion.jda.api.components.selections.StringSelectMenu.create("ticket_manage_menu")
                                    .setPlaceholder("إدارة الـتـذكـرة...")
                                    .addOption("تـغـيـيـر اسـم الـتـذكـرة", "ticket_manage_rename")
                                    .addOption("إضـافـة عـضـو", "ticket_manage_add")
                                    .addOption("إزالـة عـضـو", "ticket_manage_remove")
                                    .build()),
                    ActionRow.of(
                            Button.secondary("ticket_claim", "اسـتـلام الـتـذكـرة"),
                            Button.secondary("ticket_close", "إغـلاق الـتـذكـرة")));

            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(restoredContainer)
                    .useComponentsV2(true)
                    .build()).queue();

            // Send unclaim notice
            Container notice = EmbedUtil.containerBranded(
                    "NOTICE",
                    "Unclaimed",
                    "⤵️ Ticket Unclaimed By: " + event.getMember().getAsMention(),
                    null);
            channel.sendMessage(new MessageCreateBuilder().setComponents(notice).useComponentsV2(true).build())
                    .useComponentsV2(true).queue();

            // LOGGING
            String logDetails = "### 🔓 Ticket Unclaimed\n" +
                    "▫️ **Operator:** " + event.getMember().getAsMention() + "\n" +
                    "▫️ **Channel:** " + channel.getAsMention();
            logManager.logEmbed(event.getGuild(), LogManager.LOG_TICKETS,
                    EmbedUtil.createOldLogEmbed("ticket-unclaim", logDetails, event.getMember(), null, null,
                            EmbedUtil.INFO));

            // Restore permissions: Staff role can write again.
            net.dv8tion.jda.api.entities.Role staffRole = event.getGuild().getRoleById(STAFF_ROLE);
            if (staffRole != null) {
                channel.getManager().putRolePermissionOverride(staffRole.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null).queue();
            }

            event.getHook().sendMessage("🔓 تم إلغاء استلام التذكرة.").setEphemeral(true).queue();
        }
    }

    private void handleReopen(ButtonInteractionEvent event) {
        event.deferReply(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();
        Optional<TicketEntity> ticketOpt = ticketRepository.findByChannelId(channel.getId());
        if (ticketOpt.isEmpty())
            return;
        TicketEntity ticket = ticketOpt.get();

        // 1. Restore client access
        Member client = event.getGuild().getMemberById(ticket.getUserId());
        if (client != null) {
            channel.getManager().putMemberPermissionOverride(client.getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();
        }

        // 2. Update DB
        ticket.setStatus("OPEN");
        ticketRepository.save(ticket);

        // 3. Rename back (remove -c)
        String currentName = channel.getName();
        if (currentName.endsWith("-c")) {
            channel.getManager().setName(currentName.substring(0, currentName.length() - 2)).queue();
        }

        // LOGGING
        String logDetails = "### 🔓 Ticket Reopened\n" +
                "▫️ **Operator:** " + event.getMember().getAsMention() + "\n" +
                "▫️ **Channel:** " + channel.getAsMention();
        logManager.logEmbed(event.getGuild(), LogManager.LOG_TICKETS,
                EmbedUtil.createOldLogEmbed("ticket-reopen", logDetails, event.getMember(), null, null,
                        EmbedUtil.SUCCESS));

        event.getHook().sendMessage("✅ تـم إعـادة فـتـح الـتـذكـرة وإعـادة الـصـلاحـيـات.").queue();
    }

    @Override
    public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        // Log all messages in ticket channels, including from bots
        String channelId = event.getChannel().getId();
        ticketRepository.findByChannelId(channelId).ifPresent(ticket -> {
            TicketMessageEntity msg = new TicketMessageEntity();
            msg.setTicketId(ticket.getId().toString());
            msg.setUserId(event.getAuthor().getId());
            msg.setUserName(event.getAuthor().getName());

            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("raw", event.getMessage().getContentRaw());

            com.google.gson.JsonArray atts = new com.google.gson.JsonArray();
            for (net.dv8tion.jda.api.entities.Message.Attachment att : event.getMessage().getAttachments()) {
                com.google.gson.JsonObject a = new com.google.gson.JsonObject();
                a.addProperty("url", att.getUrl());
                a.addProperty("name", att.getFileName());
                a.addProperty("isImage", att.isImage());
                a.addProperty("isVideo", att.isVideo());
                atts.add(a);
            }
            payload.add("attachments", atts);

            com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
            for (net.dv8tion.jda.api.entities.MessageEmbed e : event.getMessage().getEmbeds()) {
                com.google.gson.JsonObject em = new com.google.gson.JsonObject();
                if (e.getTitle() != null)
                    em.addProperty("title", e.getTitle());
                if (e.getDescription() != null)
                    em.addProperty("description", e.getDescription());
                if (e.getColor() != null)
                    em.addProperty("color", String.format("#%06x", e.getColor().getRGB() & 0xFFFFFF));
                if (e.getImage() != null)
                    em.addProperty("image", e.getImage().getUrl());
                if (e.getThumbnail() != null)
                    em.addProperty("thumbnail", e.getThumbnail().getUrl());

                if (e.getAuthor() != null) {
                    com.google.gson.JsonObject au = new com.google.gson.JsonObject();
                    if (e.getAuthor().getName() != null)
                        au.addProperty("name", e.getAuthor().getName());
                    if (e.getAuthor().getIconUrl() != null)
                        au.addProperty("icon", e.getAuthor().getIconUrl());
                    em.add("author", au);
                }

                if (e.getFooter() != null) {
                    com.google.gson.JsonObject ft = new com.google.gson.JsonObject();
                    if (e.getFooter().getText() != null)
                        ft.addProperty("text", e.getFooter().getText());
                    if (e.getFooter().getIconUrl() != null)
                        ft.addProperty("icon", e.getFooter().getIconUrl());
                    em.add("footer", ft);
                }

                com.google.gson.JsonArray fields = new com.google.gson.JsonArray();
                for (net.dv8tion.jda.api.entities.MessageEmbed.Field f : e.getFields()) {
                    com.google.gson.JsonObject fo = new com.google.gson.JsonObject();
                    if (f.getName() != null)
                        fo.addProperty("name", f.getName());
                    if (f.getValue() != null)
                        fo.addProperty("value", f.getValue());
                    fo.addProperty("inline", f.isInline());
                    fields.add(fo);
                }
                em.add("fields", fields);
                embeds.add(em);
            }
            payload.add("embeds", embeds);

            msg.setContent("JSON:" + payload.toString());
            com.integrafty.opexy.repository.TicketMessageRepository msgRepo = com.integrafty.opexy.OpexyApplication
                    .getContext().getBean(com.integrafty.opexy.repository.TicketMessageRepository.class);
            msgRepo.save(msg);
        });
    }

    private void handleTranscript(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        event.deferReply(true).queue();

        ticketRepository.findByChannelId(channel.getId()).ifPresentOrElse(ticket -> {
            String domain = "https://dc-bot.highcores.co";
            String link = domain + "/view/transcript/" + ticket.getId();

            // Send to user ephemerally
            event.getHook().sendMessage("🔗 **[تـفـضـل بـمـعـايـنـة سـجـل الـتـحـادث مـن هـنـا](" + link + ")**")
                    .setEphemeral(true).queue();

            // Send public confirmation in the ticket channel
            event.getChannel().sendMessage("✅ تم حفظ الترانسكربت في <#1487147026427940955>").queue();

            // Send to logs with EXACT format requested
            TextChannel logCh = event.getGuild().getTextChannelById("1487147026427940955");
            if (logCh != null) {
                Member opener = event.getGuild().getMemberById(ticket.getUserId());
                String openerMention = opener != null ? opener.getAsMention() : "<@" + ticket.getUserId() + ">";
                String openerName = opener != null ? opener.getUser().getName() : "Unknown";

                String claimedBy = ticket.getStaffId() != null ? "<@" + ticket.getStaffId() + ">" : "None";

                String body = String.format(
                        "**User:** %s (%s)\n" +
                                "**Claimed By:** %s\n" +
                                "**Closed By:** %s\n\n" +
                                "🔗 **[View Transcript](%s)**",
                        openerMention, openerName, claimedBy, event.getUser().getAsMention(), link);

                Container transcriptPanel = EmbedUtil.containerBranded(
                        "TRANSCRIPT",
                        "Archive — Case #" + ticket.getId(),
                        body,
                        null);

                logCh.sendMessage(
                        new MessageCreateBuilder().setComponents(transcriptPanel).useComponentsV2(true).build())
                        .useComponentsV2(true).queue();
            }
        }, () -> {
            event.getHook().sendMessage("❌ لم يتم العثور على بيانات التذكرة لإصدار الرابط.").setEphemeral(true).queue();
        });
    }

    private void handleDeleteRequest(ButtonInteractionEvent event) {
        Container confirm = EmbedUtil.containerBranded(
                "تـحـذيـر",
                "حـذف الـقـنـاة",
                "### هـل أنـت مـتـأكـد مـن حـذف الـتـذكـرة نـهـائـيـاً؟\nهـذا الإجـراء لا يـمـكـن الـتـراجـع عـنـه.",
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                        Button.secondary("ticket_delete_final", "تـأكـيـد الـحـذف"),
                        Button.secondary("ticket_delete_cancel", "تـراجـع")));
        event.reply(new MessageCreateBuilder().setComponents(confirm).useComponentsV2(true).build())
                .setEphemeral(true).useComponentsV2(true).queue();
    }
}
