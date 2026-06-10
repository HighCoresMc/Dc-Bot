package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.entity.UserEntity;
import com.integrafty.opexy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationCommands implements MultiSlashCommand {

    private final LogManager logManager;
    private final UserRepository userRepository;

    private static final String ROLE_WARN_1 = "1487196789399490711";
    private static final String ROLE_WARN_2 = "1487196790892794067";
    private static final String ROLE_WARN_3 = "1487196791144190143";
    private static final String ROLE_ABOVE_LAW = "1487152572207861870";

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("setnick", "تـــغـــيـــيـــر لـــقـــب عـــضـــو مـــعـــيـــن")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.STRING, "nick", "الـــلـــقـــب الـــجـــديـــد", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.NICKNAME_MANAGE)));

        list.add(Commands.slash("ban", "حـــظـــر عـــضـــو مـــن الـــســـيـــرفـــر")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.STRING, "reason", "الـــســـبـــب", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)));

        list.add(Commands.slash("unban", "إلـــغـــاء حـــظـــر عـــضـــو مـــن الـــســـيـــرفـــر")
                .addOption(OptionType.STRING, "user_id", "أي دي الـــعـــضـــو", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)));

        list.add(Commands.slash("unban-all", "إلـــغـــاء حـــظـــر الـــجـــمـــيـــع مـــن الـــســـيـــرفـــر")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

        list.add(Commands.slash("kick", "طـــرد عـــضـــو مـــن الـــســـيـــرفـــر")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.STRING, "reason", "الـــســـبـــب", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("vkick", "طـــرد عـــضـــو مـــن الـــروم الـــصـــوتـــي")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("mute-text", "إســـكـــات عـــضـــو مـــن الـــشـــات الـــكـــتـــابـــي")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("unmute-text", "إلـــغـــاء إســـكـــات عـــضـــو مـــن الـــشـــات الـــكـــتـــابـــي")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("mute-check", "الـــتـــحـــقـــق مـــن حـــالـــة الـــكـــتـــم لـــعـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("mute-voice", "إســـكـــات عـــضـــو مـــن الـــروم الـــصـــوتـــي")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_MUTE_OTHERS)));

        list.add(Commands.slash("unmute-voice", "إلـــغـــاء إســـكـــات عـــضـــو مـــن الـــصـــوت")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_MUTE_OTHERS)));

        list.add(Commands.slash("timeout", "إعـــطـــاء تـــايـــم أوت لـــعـــضـــو مـــعـــيـــن")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.INTEGER, "duration", "الـــمـــدة بـــالـــدقـــائـــق", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("untimeout", "إلـــغـــاء الـــتـــايـــم أوت عـــن عـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("clear", "تـــنـــظـــيـــف الـــرســـائـــل مـــن الـــقـــنـــاة")
                .addOption(OptionType.INTEGER, "amount", "عـــدد الـــرســـائـــل (1-100)", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)));

        list.add(Commands.slash("move", "نـــقـــل عـــضـــو إلـــى روم صـــوتـــي آخـــر")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.CHANNEL, "channel", "الـــروم الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.VOICE_MOVE_OTHERS)));

        list.add(Commands.slash("role", "إعـــطـــاء أو ســـحـــب رتـــبـــة مـــن عـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.ROLE, "role", "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)));

        list.add(Commands.slash("temprole", "إعـــطـــاء رتـــبـــة مـــؤقـــتـــة لـــعـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.ROLE, "role", "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                .addOption(OptionType.INTEGER, "duration", "الـــمـــدة بـــالـــســـاعـــات", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)));

        list.add(Commands.slash("rar", "ســـحـــب جـــمـــيـــع الـــرتـــب مـــن الـــعـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)));

        list.add(Commands.slash("inrole", "عـــرض جـــمـــيـــع الأعـــضـــاء فـــي رتـــبـــة مـــعـــيـــنـــة")
                .addOption(OptionType.ROLE, "role", "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("warn-add", "إعـــطـــاء تـــحـــذيـــر لـــعـــضـــو مـــع رتـــبـــة")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .addOption(OptionType.STRING, "reason", "ســـبـــب الـــتـــحـــذيـــر", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("warn-remove", "إلـــغـــاء الـــتـــحـــذيـــرات عـــن عـــضـــو")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("warnings", "عـــرض تـــحـــذيـــرات عـــضـــو مـــعـــيـــن")
                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)));

        list.add(Commands.slash("lock", "قـــفـــل الـــقـــنـــاة الـــحـــالـــيـــة لـــلـــجـــمـــيـــع")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));
        list.add(Commands.slash("unlock", "فـــتـــح الـــقـــنـــاة الـــحـــالـــيـــة لـــلـــجـــمـــيـــع")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));
        list.add(Commands.slash("hide", "إخـــفـــاء الـــقـــنـــاة الـــحـــالـــيـــة عـــن الـــجـــمـــيـــع")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));
        list.add(Commands.slash("show", "إظـــهـــار الـــقـــنـــاة الـــحـــالـــيـــة لـــلـــجـــمـــيـــع")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));

        list.add(Commands.slash("opstage", "فـــتـــح الـــســـتـــيـــج (ســـمـــاح بـــالـــدخـــول)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));

        list.add(Commands.slash("clstage", "إغـــلاق الـــســـتـــيـــج (مـــنـــع الـــدخـــول)")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));

        list.add(Commands.slash("slowmode", "تـــفـــعـــيـــل وضـــع الـــتـــبـــاطـــؤ فـــي الـــقـــنـــاة")
                .addOption(OptionType.INTEGER, "seconds", "عـــدد الـــثـــوانـــي", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)));

        list.add(Commands.slash("add-emoji", "إضـــافـــة إيـــمـــوجـــي جـــديـــد لـــلـــســـيـــرفـــر")
                .addOption(OptionType.STRING, "name", "إســـم الإيـــمـــوجـــي", true)
                .addOption(OptionType.ATTACHMENT, "image", "صـــورة الإيـــمـــوجـــي", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_GUILD_EXPRESSIONS)));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();
        
        switch (name) {
            case "setnick" -> handleSetNick(event);
            case "ban" -> handleBan(event);
            case "unban" -> handleUnban(event);
            case "unban-all" -> handleUnbanAll(event);
            case "kick" -> handleKick(event);
            case "vkick" -> handleVoiceKick(event);
            case "mute-text" -> handleMuteText(event);
            case "unmute-text" -> handleUnmuteText(event);
            case "mute-check" -> handleMuteCheck(event);
            case "mute-voice" -> handleMuteVoice(event);
            case "unmute-voice" -> handleUnmuteVoice(event);
            case "timeout" -> handleTimeout(event);
            case "untimeout" -> handleUntimeout(event);
            case "clear" -> handleClear(event);
            case "move" -> handleMove(event);
            case "role" -> handleRole(event);
            case "temprole" -> handleTempRole(event);
            case "rar" -> handleRemoveAllRoles(event);
            case "inrole" -> handleInRole(event);
            case "warn-add" -> handleWarnAdd(event);
            case "warn-remove" -> handleWarnRemove(event);
            case "warnings" -> handleWarnings(event);
            case "lock" -> handleLock(event, false);
            case "unlock" -> handleLock(event, true);
            case "hide" -> handleVisibility(event, false);
            case "show" -> handleVisibility(event, true);
            case "opstage" -> handleStageConnection(event, true);
            case "clstage" -> handleStageConnection(event, false);
            case "slowmode" -> handleSlowmode(event);
            case "add-emoji" -> handleAddEmoji(event);
        }
    }

    // ─────────────────────────── Handlers ───────────────────────────

    private void handleSetNick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.NICKNAME_MANAGE)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String nick = event.getOption("nick", OptionMapping::getAsString);
        if (m == null) return;
        if (!canInteract(event, m)) return;

        m.modifyNickname(nick).queue(v -> {
            reply(event, EmbedUtil.success("Update Nickname", "Nickname for " + m.getUser().getName() + " changed to: `" + (nick == null ? "Reset" : nick) + "`"));
            logModAction(event, "setnick", "Nickname synchronized: " + (nick == null ? "Reset" : nick), m, EmbedUtil.INFO);
        });
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        String reason = getReason(event);
        if (u == null) return;
        
        Member m = event.getGuild().getMember(u);
        if (m != null && !canInteract(event, m)) return;

        // Notify User in DM
        u.openPrivateChannel().queue(pc -> {
            String body = String.format("""
                    ### 🚫 إشـــعـــار حـــظـــر | BAN NOTIFICATION
                    
                    تـــم حـــظـــرك مـــن ســـيـــرفـــر: **%s**
                    بـــســـبـــب: `%s`
                    
                    *إذا كـــنـــت تـــعـــتـــقـــد أن هـــذا الـــحـــظـــر خـــطـــأ، يـــرجـــى الـــتـــواصـــل مـــع الإدارة.*
                    """, event.getGuild().getName(), reason);
            Container dmEmbed = EmbedUtil.containerBranded("SECURITY", "Ban Protocol", body, EmbedUtil.BANNER_MAIN);
            pc.sendMessage(new MessageCreateBuilder().setComponents(dmEmbed).useComponentsV2(true).build()).useComponentsV2(true).queue(null, e -> {});
        });

        event.getGuild().ban(u, 7, TimeUnit.DAYS).reason(reason).queue(v -> {
            reply(event, EmbedUtil.success("Ban Enforcement", "تـــم حـــظـــر الـــمـــســـتـــخـــدم **" + u.getName() + "** بـــنـــجـــاح.\nالـــســـبـــب: `" + reason + "`"));
            logModAction(event, "ban", "Global Blacklist. Reason: " + reason, m != null ? m : null, EmbedUtil.DANGER);
        });
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        String userId = event.getOption("user_id").getAsString();
        event.getGuild().unban(User.fromId(userId)).queue(v -> {
            reply(event, EmbedUtil.success("Unban System", "Restrictions removed for user ID: `" + userId + "`"));
            logModAction(event, "unban", "Access Reinstated for ID: " + userId, null, EmbedUtil.SUCCESS);
        });
    }

    private void handleUnbanAll(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        event.getGuild().retrieveBanList().queue(bans -> {
            bans.forEach(ban -> event.getGuild().unban(ban.getUser()).queue());
            reply(event, EmbedUtil.success("Database Wipe", bans.size() + " members unbanned."));
            logModAction(event, "unban-all", "Full Ban Registry Purge. Count: " + bans.size(), null, EmbedUtil.WARNING);
        });
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String reason = getReason(event);
        if (m == null || !canInteract(event, m)) return;

        // Notify User in DM
        m.getUser().openPrivateChannel().queue(pc -> {
            String body = String.format("""
                    ### 👢 إشـــعـــار طـــرد | KICK NOTIFICATION
                    
                    تـــم طـــردك مـــن ســـيـــرفـــر: **%s**
                    بـــســـبـــب: `%s`
                    
                    *يـــمـــكـــنـــك الـــعـــودة لـــلـــســـيـــرفـــر مـــرة أخـــرى.*
                    """, event.getGuild().getName(), reason);
            Container dmEmbed = EmbedUtil.containerBranded("SECURITY", "Kick Protocol", body, EmbedUtil.BANNER_MAIN);
            pc.sendMessage(new MessageCreateBuilder().setComponents(dmEmbed).useComponentsV2(true).build()).useComponentsV2(true).queue(null, e -> {});
        });

        m.kick().reason(reason).queue(v -> {
            reply(event, EmbedUtil.success("Kick System", "تـــم طـــرد الـــعـــضـــو **" + m.getUser().getName() + "** مـــن الـــســـيـــرفـــر.\nالـــســـبـــب: `" + reason + "`"));
            logModAction(event, "kick", "Manual Disconnect. Reason: " + reason, m, EmbedUtil.WARNING);
        });
    }

    private void handleVoiceKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null || !canInteract(event, m)) return;
        if (m.getVoiceState() == null || !m.getVoiceState().inAudioChannel()) {
            replyEphemeral(event, EmbedUtil.error("NOT IN VOICE", "Member is not in a voice channel."));
            return;
        }

        event.getGuild().kickVoiceMember(m).queue(v -> {
            reply(event, EmbedUtil.success("Voice Kick", m.getUser().getName() + " ejected from voice."));
            logModAction(event, "vkick", "Voice Ejection", m, EmbedUtil.INFO);
        });
    }

    private void handleMuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null || !canInteract(event, m)) return;
        m.timeoutFor(24, TimeUnit.HOURS).reason("Text Mute").queue(v -> {
            reply(event, EmbedUtil.success("Mute System", m.getUser().getName() + " muted (24h)."));
            logModAction(event, "mute-text", "Text Isolation (24h)", m, EmbedUtil.WARNING);
        });
    }

    private void handleUnmuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.removeTimeout().queue(v -> {
            reply(event, EmbedUtil.success("Unmute System", "Permissions restored for " + m.getUser().getName()));
            logModAction(event, "unmute-text", "Isolation Revocation", m, EmbedUtil.SUCCESS);
        });
    }

    private void handleMuteCheck(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        boolean muted = m.isTimedOut();
        reply(event, EmbedUtil.containerBranded("STATUS", "Mute Status", "Member: " + m.getUser().getName() + "\nMuted: `" + muted + "`", EmbedUtil.BANNER_MAIN));
    }

    private void handleMuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null || !canInteract(event, m)) return;
        m.mute(true).queue(v -> {
            reply(event, EmbedUtil.success("Voice Mute", "Mic disabled for " + m.getUser().getName()));
            logModAction(event, "mute-voice", "Microphone Restriction", m, EmbedUtil.WARNING);
        });
    }

    private void handleUnmuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.mute(false).queue(v -> {
            reply(event, EmbedUtil.success("Voice Unmute", "Mic enabled for " + m.getUser().getName()));
            logModAction(event, "unmute-voice", "Voice Restoration", m, EmbedUtil.SUCCESS);
        });
    }

    private void handleTimeout(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        int min = event.getOption("duration").getAsInt();
        if (m == null || !canInteract(event, m)) return;
        m.timeoutFor(min, TimeUnit.MINUTES).queue(v -> {
            reply(event, EmbedUtil.success("Timeout", m.getUser().getName() + " timed out for " + min + "m."));
            logModAction(event, "timeout", "Temporal Isolation: " + min + "m", m, EmbedUtil.WARNING);
        });
    }

    private void handleUntimeout(SlashCommandInteractionEvent event) {
        handleUnmuteText(event);
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MESSAGE_MANAGE)) return;
        int amt = event.getOption("amount").getAsInt();
        if (amt < 1 || amt > 100) {
            replyEphemeral(event, EmbedUtil.error("INVALID AMOUNT", "Provide a value between 1 and 100."));
            return;
        }

        // Defer reply immediately to avoid timeout
        event.deferReply(true).queue();

        event.getChannel().getIterableHistory().takeAsync(amt).thenAccept(msgs -> {
            try {
                // Filter messages older than 2 weeks (Discord limit for bulk delete)
                long twoWeeksAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
                List<net.dv8tion.jda.api.entities.Message> toDelete = msgs.stream()
                        .filter(m -> m.getTimeCreated().toInstant().toEpochMilli() > twoWeeksAgo)
                        .toList();

                if (toDelete.isEmpty()) {
                    event.getHook().sendMessage(new MessageCreateBuilder()
                            .setComponents(EmbedUtil.error("FAIL", "تـــم الـــعـــثـــور عـــلـــى رســـائـــل قـــديـــمـــة جـــداً (أكـــثـــر مـــن أسبوعـــيـــن) ولا يـــمـــكـــن حـــذفـــهـــا بـــالـــجـــمـــلـــة."))
                            .useComponentsV2(true).build()).queue();
                    return;
                }

                event.getGuildChannel().deleteMessages(toDelete).queue(v -> {
                    event.getHook().sendMessage(new MessageCreateBuilder()
                            .setComponents(EmbedUtil.success("Purge Complete", toDelete.size() + " messages purged successfully."))
                            .useComponentsV2(true).build()).queue();
                    logModAction(event, "clear", "Intelligence Wipe: " + toDelete.size() + " units in " + event.getChannel().getAsMention(), null, EmbedUtil.DANGER);
                }, e -> {
                    event.getHook().sendMessage(new MessageCreateBuilder()
                            .setComponents(EmbedUtil.error("ERROR", "حدث خطأ أثناء الحذف: " + e.getMessage()))
                            .useComponentsV2(true).build()).queue();
                });
            } catch (Exception e) {
                event.getHook().sendMessage(new MessageCreateBuilder()
                        .setComponents(EmbedUtil.error("SYSTEM ERROR", e.getMessage()))
                        .useComponentsV2(true).build()).queue();
            }
        });
    }

    private void handleMove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MOVE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        var ch = event.getOption("channel").getAsChannel().asAudioChannel();
        if (m == null) return;
        event.getGuild().moveVoiceMember(m, ch).queue(v -> {
            reply(event, EmbedUtil.success("Relocation", m.getUser().getName() + " moved to " + ch.getName()));
            logModAction(event, "move", "Station Relocation to " + ch.getName(), m, EmbedUtil.INFO);
        });
    }

    private void handleRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (m == null || r == null || !canInteract(event, m)) return;

        if (m.getRoles().contains(r)) {
            event.getGuild().removeRoleFromMember(m, r).queue(v -> {
                reply(event, EmbedUtil.success("Role Strip", "Role " + r.getName() + " removed."));
                logModAction(event, "role-remove", "Clearance Level Revoked: " + r.getName(), m, EmbedUtil.INFO);
            });
        } else {
            event.getGuild().addRoleToMember(m, r).queue(v -> {
                reply(event, EmbedUtil.success("Role Add", "Role " + r.getName() + " added."));
                logModAction(event, "role-add", "Clearance Level Granted: " + r.getName(), m, EmbedUtil.SUCCESS);
            });
        }
    }

    private void handleTempRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        // Logic would require a scheduler, skipping for now as per professional Opexy focus on current tools
        reply(event, EmbedUtil.error("UNIMPLEMENTED", "TempRole requires external scheduler integration."));
    }

    private void handleRemoveAllRoles(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null || !canInteract(event, m)) return;
        m.getRoles().forEach(r -> {
            if (!r.isManaged()) event.getGuild().removeRoleFromMember(m, r).queue();
        });
        reply(event, EmbedUtil.success("Registry Strip", "All removable roles purged from " + m.getUser().getName()));
        logModAction(event, "rar", "Full Clearance Strip", m, EmbedUtil.DANGER);
    }

    private void handleInRole(SlashCommandInteractionEvent event) {
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (r == null) return;
        event.getGuild().loadMembers().onSuccess(members -> {
            var list = members.stream().filter(m -> m.getRoles().contains(r)).toList();
            String names = list.stream().map(m -> m.getUser().getName()).limit(20).reduce((a, b) -> a + ", " + b).orElse("None");
            reply(event, EmbedUtil.containerBranded("QUERY", "Role Membership", "Role: " + r.getName() + "\nCount: " + list.size() + "\nMembers: " + names, EmbedUtil.BANNER_MAIN));
        });
    }

    private void handleWarnAdd(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member target = event.getOption("user").getAsMember();
        String reason = getReason(event);
        if (target == null) return;

        UserEntity user = userRepository.findByUserIdAndGuildId(target.getId(), event.getGuild().getId())
                .orElse(new UserEntity(target.getId(), event.getGuild().getId(), 0, 0, false, null, null, 0, 0));
        
        user.setWarningCount(user.getWarningCount() + 1);
        userRepository.save(user);

        int count = user.getWarningCount();
        String roleId = switch (count) {
            case 1 -> ROLE_WARN_1;
            case 2 -> ROLE_WARN_2;
            case 3 -> ROLE_WARN_3;
            default -> count > 3 ? ROLE_WARN_3 : null;
        };

        if (roleId != null) {
            Role r = event.getGuild().getRoleById(roleId);
            if (r != null) event.getGuild().addRoleToMember(target, r).queue();
        }

        // Notify User in DM
        target.getUser().openPrivateChannel().queue(pc -> {
            String body = String.format("""
                    ### ⚠️ إشـــعـــار تـــحـــذيـــر | WARN NOTIFICATION
                    
                    تـــم تـــحـــذيـــرك فـــي ســـيـــرفـــر: **%s**
                    بـــســـبـــب: `%s`
                    
                    ▫️ **رقـــم الـــتـــحـــذيـــر:** `%d`
                    
                    *يـــرجـــى الالـــتـــزام بـــالـــقـــوانـــيـــن لـــتـــجـــنـــب الـــحـــظـــر.*
                    """, event.getGuild().getName(), reason, count);
            Container dmEmbed = EmbedUtil.containerBranded("MODERATION", "Warn Protocol", body, EmbedUtil.BANNER_MAIN);
            pc.sendMessage(new MessageCreateBuilder().setComponents(dmEmbed).useComponentsV2(true).build()).useComponentsV2(true).queue(null, e -> {});
        });

        reply(event, EmbedUtil.success("Warning Protocol", "تـــم تـــحـــذيـــر الـــعـــضـــو " + target.getAsMention() + " بـــنـــجـــاح.\nالـــتـــحـــذيـــر رقـــم: **" + count + "**"));
        logModAction(event, "warn-add", "Warn Issued #" + count + ". Reason: " + reason, target, EmbedUtil.WARNING);
    }

    private void handleWarnRemove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;

        UserEntity user = userRepository.findByUserIdAndGuildId(target.getId(), event.getGuild().getId()).orElse(null);
        if (user != null) {
            user.setWarningCount(0);
            userRepository.save(user);
            // Remove roles
            Role r1 = event.getGuild().getRoleById(ROLE_WARN_1);
            Role r2 = event.getGuild().getRoleById(ROLE_WARN_2);
            Role r3 = event.getGuild().getRoleById(ROLE_WARN_3);
            if (r1 != null) event.getGuild().removeRoleFromMember(target, r1).queue();
            if (r2 != null) event.getGuild().removeRoleFromMember(target, r2).queue();
            if (r3 != null) event.getGuild().removeRoleFromMember(target, r3).queue();
        }

        reply(event, EmbedUtil.success("History Purge", "All warnings cleared for " + target.getUser().getName()));
        logModAction(event, "warn-remove", "Infraction History Wiped", target, EmbedUtil.SUCCESS);
    }

    private void handleWarnings(SlashCommandInteractionEvent event) {
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;
        UserEntity user = userRepository.findByUserIdAndGuildId(target.getId(), event.getGuild().getId()).orElse(null);
        int count = user != null ? user.getWarningCount() : 0;
        reply(event, EmbedUtil.containerBranded("QUERY", "Warning Profile", "User: " + target.getAsMention() + "\nActive Warnings: **" + count + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleLock(SlashCommandInteractionEvent event, boolean unlock) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        tc.upsertPermissionOverride(event.getGuild().getPublicRole())
                .setAllowed(unlock ? EnumSet.of(Permission.MESSAGE_SEND) : null)
                .setDenied(unlock ? null : EnumSet.of(Permission.MESSAGE_SEND))
                .queue(v -> {
                    reply(event, EmbedUtil.success("Security", "Protocol: " + (unlock ? "Unlocked" : "Locked")));
                    logModAction(event, "lock-toggle", "Security Override: " + (unlock ? "UNLOCKED" : "LOCKED"), null, unlock ? EmbedUtil.SUCCESS : EmbedUtil.DANGER);
                });
    }

    private void handleVisibility(SlashCommandInteractionEvent event, boolean show) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        tc.upsertPermissionOverride(event.getGuild().getPublicRole())
                .setAllowed(show ? EnumSet.of(Permission.VIEW_CHANNEL) : null)
                .setDenied(show ? null : EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(v -> {
                    reply(event, EmbedUtil.success("Visibility", "Status: " + (show ? "Visible" : "Hidden")));
                    logModAction(event, "visibility-toggle", "Optical Toggle: " + (show ? "VISIBLE" : "HIDDEN"), null, show ? EmbedUtil.SUCCESS : EmbedUtil.WARNING);
                });
    }

    private void handleStageConnection(SlashCommandInteractionEvent event, boolean open) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        
        net.dv8tion.jda.api.entities.channel.middleman.AudioChannel channel = null;
        if (event.getChannel().getType().isAudio()) {
            channel = (net.dv8tion.jda.api.entities.channel.middleman.AudioChannel) event.getChannel();
        } else if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel()) {
            channel = event.getMember().getVoiceState().getChannel();
        }

        if (channel == null) {
            replyEphemeral(event, EmbedUtil.error("NOT IN VOICE", "يجب أن تكون في روم صوتي/ستيج أو تستخدم الأمر داخله."));
            return;
        }

        net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer permChannel = (net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer) channel;
        
        final net.dv8tion.jda.api.entities.channel.middleman.AudioChannel finalChannel = channel;

        permChannel.upsertPermissionOverride(event.getGuild().getPublicRole())
                .setAllowed(open ? EnumSet.of(Permission.VOICE_CONNECT) : null)
                .setDenied(open ? null : EnumSet.of(Permission.VOICE_CONNECT))
                .queue(v -> {
                    reply(event, EmbedUtil.success("Stage Connection", "Stage is now: " + (open ? "OPEN" : "CLOSED")));
                    logModAction(event, "stage-toggle", "Connection Toggle: " + (open ? "OPENED" : "CLOSED") + " in " + finalChannel.getAsMention(), null, open ? EmbedUtil.SUCCESS : EmbedUtil.DANGER);
                });
    }

    private void handleSlowmode(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        int sec = event.getOption("seconds").getAsInt();
        event.getChannel().asTextChannel().getManager().setSlowmode(sec).queue(v -> {
            reply(event, EmbedUtil.success("Traffic Control", "Delay set to " + sec + "s"));
            logModAction(event, "slowmode", "Operational Pace set to " + sec + "s", null, EmbedUtil.INFO);
        });
    }

    private void handleAddEmoji(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_GUILD_EXPRESSIONS)) return;
        String name = event.getOption("name").getAsString();
        var attach = event.getOption("image").getAsAttachment();
        attach.getProxy().download().thenAccept(stream -> {
            try (stream) {
                event.getGuild().createEmoji(name, net.dv8tion.jda.api.entities.Icon.from(stream)).queue(v -> {
                    reply(event, EmbedUtil.success("Asset Registry", "Emoji :" + name + ": initialized."));
                    logModAction(event, "add-emoji", "Creative Asset Deployment: :" + name + ":", null, EmbedUtil.SUCCESS);
                });
            } catch (Exception e) { reply(event, EmbedUtil.error("FAIL", e.getMessage())); }
        });
    }

    // ─────────────────────────── Utils ───────────────────────────

    private boolean hasPerm(SlashCommandInteractionEvent e, Permission p) {
        if (e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(ROLE_ABOVE_LAW))) return true;
        if (!e.getMember().hasPermission(p)) {
            replyEphemeral(e, EmbedUtil.accessDenied());
            return false;
        }
        return true;
    }

    private boolean canInteract(SlashCommandInteractionEvent e, Member m) {
        // Target is "Above Law" - Untouchable
        if (m.getRoles().stream().anyMatch(r -> r.getId().equals(ROLE_ABOVE_LAW))) {
            replyEphemeral(e, EmbedUtil.error("SECURITY", "تـــنـــبـــيـــه: هـــذا الـــعـــضـــو لـــديـــه حـــصـــانـــة نـــهـــائـــيـــة."));
            return false;
        }

        // Bot Hierarchy Check
        if (!e.getGuild().getSelfMember().canInteract(m)) {
            replyEphemeral(e, EmbedUtil.error("HIERARCHY", "خـــطـــأ فـــي الـــرتـــب: رتـــبـــة الـــبـــوت أقـــل مـــن رتـــبـــة الـــعـــضـــو الـــمـــســـتـــهـــدف."));
            return false;
        }
        return true;
    }

    private String getReason(SlashCommandInteractionEvent e) {
        return e.getOption("reason") != null ? e.getOption("reason").getAsString() : "None";
    }

    private void reply(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        if (e.isAcknowledged()) e.getHook().sendMessage(msg).useComponentsV2(true).queue();
        else e.reply(msg).useComponentsV2(true).queue();
    }

    private void replyEphemeral(SlashCommandInteractionEvent e, Container c) {
        var msg = new MessageCreateBuilder().setComponents(c).useComponentsV2(true).build();
        e.reply(msg).setEphemeral(true).useComponentsV2(true).queue();
    }

    private void logModAction(SlashCommandInteractionEvent event, String type, String details, Member target, java.awt.Color color) {
        String logBody = String.format("### 🛡️ MOD ACTION: %s\n▫️ **Operator:** %s\n▫️ **Target:** %s\n▫️ **Channel:** %s\n▫️ **Details:** %s",
                type.toUpperCase(), event.getMember().getAsMention(), target != null ? target.getAsMention() : "None", event.getChannel().getAsMention(), details);
        logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed(type, logBody, event.getMember(), target != null ? target.getUser() : null, target, color));
    }
}
