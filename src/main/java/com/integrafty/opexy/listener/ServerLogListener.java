package com.integrafty.opexy.listener;

import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServerLogListener extends ListenerAdapter {

    private final JDA jda;
    private final LogManager logManager;

    private static class CachedMessage {
        String authorId;
        String authorMention;
        String content;
        boolean isBot;
        java.util.List<String> imageAttachments = new java.util.ArrayList<>();
    }

    private final java.util.Map<Long, CachedMessage> messageCache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<Long, CachedMessage>() {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, CachedMessage> eldest) {
                    return size() > 2000;
                }
            });

    // ─────────────────────────── join・left・logs ───────────────────────────

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        long age = (Instant.now().getEpochSecond() - event.getUser().getTimeCreated().toEpochSecond()) / 86400;
        String details = "### 🟢 Access Granted: New Unit Arrival\n" +
                "▫️ **Account Age:** `" + age + " Days`\n" +
                "▫️ **Registry Date:** `"
                + DateTimeFormatter.ISO_INSTANT.format(event.getUser().getTimeCreated().toInstant()) + "`\n" +
                "▫️ **Current Population:** `" + event.getGuild().getMemberCount() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT,
                EmbedUtil.createOldLogEmbed("member-join", details, null, event.getUser(), event.getMember(),
                        EmbedUtil.SUCCESS));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        String roles = event.getMember() != null
                ? event.getMember().getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))
                : "Unknown";
        String details = "### 🔴 Access Revoked: Unit Departure\n" +
                "▫️ **Last Roles:** `" + (roles.isEmpty() ? "None" : roles) + "`\n" +
                "▫️ **Current Population:** `" + event.getGuild().getMemberCount() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT,
                EmbedUtil.createOldLogEmbed("member-leave", details, null, event.getUser(), null, EmbedUtil.DANGER));
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        String details = "### 🔨 Entity Blacklisted\n" +
                "▫️ **Target:** " + event.getUser().getAsMention() + " (`" + event.getUser().getId() + "`)\n" +
                "▫️ **Status:** `TERMINATED`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT,
                EmbedUtil.createOldLogEmbed("guild-ban", details, null, event.getUser(), null, java.awt.Color.BLACK));
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        String details = "### ✅ Blacklist Revoked\n" +
                "▫️ **Target:** " + event.getUser().getAsMention() + " (`" + event.getUser().getId() + "`)\n" +
                "▫️ **Status:** `REINSTATED`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_JOIN_LEFT,
                EmbedUtil.createOldLogEmbed("guild-unban", details, null, event.getUser(), null, EmbedUtil.SUCCESS));
    }

    // ─────────────────────────── message・logs ───────────────────────────

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot())
            return;
        String newContent = event.getMessage().getContentRaw();
        CachedMessage oldCached = messageCache.get(event.getMessageIdLong());

        String oldContent = oldCached != null ? oldCached.content : "*Not cached*";
        if (oldContent.length() > 500)
            oldContent = oldContent.substring(0, 500) + "...";
        if (newContent.length() > 500)
            newContent = newContent.substring(0, 500) + "...";

        java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> embedsList = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("https?://\\S+\\.(png|jpg|jpeg|gif|webp|bmp)(?:\\?\\S*)?",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(newContent);

        String details = "### ✏️ Transmission Modified\n" +
                "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "▫️ **Author:** " + event.getAuthor().getAsMention() + "\n" +
                "▫️ **Old Data:** ```" + oldContent + "```\n" +
                "▫️ **New Data:** ```" + newContent + "```";

        embedsList.add(EmbedUtil.createOldLogEmbed("message-edit", details, event.getMember(), event.getAuthor(), null,
                EmbedUtil.WARNING));

        for (net.dv8tion.jda.api.entities.Message.Attachment att : event.getMessage().getAttachments()) {
            if (att.isImage()) {
                embedsList.add(new net.dv8tion.jda.api.EmbedBuilder().setImage(att.getUrl()).setColor(EmbedUtil.WARNING)
                        .build());
            }
        }

        while (matcher.find()) {
            if (embedsList.size() < 10) {
                embedsList.add(new net.dv8tion.jda.api.EmbedBuilder().setImage(matcher.group())
                        .setColor(EmbedUtil.WARNING).build());
            }
        }

        logManager.logEmbed(event.getGuild(), LogManager.LOG_MESSAGE,
                embedsList.toArray(new net.dv8tion.jda.api.entities.MessageEmbed[0]));

        // Update Cache
        if (oldCached != null) {
            oldCached.content = event.getMessage().getContentRaw();
        }
    }

    @Override
    public void onMessageReceived(@NotNull net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;
        String content = event.getMessage().getContentRaw();

        // Cache Message
        CachedMessage cached = new CachedMessage();
        cached.authorId = event.getAuthor().getId();
        cached.authorMention = event.getAuthor().getAsMention();
        cached.content = content;
        cached.isBot = event.getAuthor().isBot();
        for (net.dv8tion.jda.api.entities.Message.Attachment att : event.getMessage().getAttachments()) {
            if (att.isImage())
                cached.imageAttachments.add(att.getUrl());
        }
        java.util.regex.Matcher cacheMatcher = java.util.regex.Pattern
                .compile("https?://\\S+\\.(png|jpg|jpeg|gif|webp|bmp)(?:\\?\\S*)?",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(content);
        while (cacheMatcher.find()) {
            cached.imageAttachments.add(cacheMatcher.group());
        }
        messageCache.put(event.getMessageIdLong(), cached);

        if (event.getAuthor().isBot())
            return;

        if (content.isEmpty() && event.getMessage().getAttachments().isEmpty())
            return;

        java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> embedsList = new java.util.ArrayList<>();

        if (content.length() > 1000)
            content = content.substring(0, 1000) + "...";

        String details = "### 📩 New Transmission\n" +
                "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "▫️ **Content:** " + (content.isEmpty() ? "*Attachment Only*" : "```" + content + "```");

        embedsList.add(EmbedUtil.createOldLogEmbed("message-send", details, event.getMember(), event.getAuthor(), null,
                EmbedUtil.INFO));

        for (String imgUrl : cached.imageAttachments) {
            if (embedsList.size() < 10) {
                embedsList
                        .add(new net.dv8tion.jda.api.EmbedBuilder().setImage(imgUrl).setColor(EmbedUtil.INFO).build());
            }
        }

        logManager.logEmbed(event.getGuild(), LogManager.LOG_MESSAGE,
                embedsList.toArray(new net.dv8tion.jda.api.entities.MessageEmbed[0]));
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        CachedMessage cached = messageCache.get(event.getMessageIdLong());
        if (cached != null && cached.isBot)
            return;
        
        if (MessageListener.moderatedMessages.contains(event.getMessageIdLong())) {
            return; // Skip logging here since MessageListener already logged it with the image!
        }

        event.getGuild().retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).queue(logs -> {
            AuditLogEntry entry = logs.isEmpty() ? null : logs.get(0);

            String operator = "Author / System (Self-Delete / Bot)";
            if (entry != null && cached != null && entry.getTargetId().equals(cached.authorId)) {
                if (Instant.now().getEpochSecond() - entry.getTimeCreated().toEpochSecond() < 5) {
                    operator = entry.getUser().getAsMention();
                }
            } else if (entry != null && cached == null) {
                if (Instant.now().getEpochSecond() - entry.getTimeCreated().toEpochSecond() < 5) {
                    operator = entry.getUser().getAsMention();
                }
            }

            String content = cached != null ? cached.content : "*Message content not cached*";
            if (content.isEmpty())
                content = "*Attachment Only*";
            if (content.length() > 500)
                content = content.substring(0, 500) + "...";

            String authorInfo = cached != null ? cached.authorMention : "Unknown";

            String details = "### 🗑️ Transmission Terminated\n" +
                    "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "▫️ **Author:** " + authorInfo + "\n" +
                    "▫️ **Operator:** " + operator + "\n" +
                    "▫️ **Message ID:** `" + event.getMessageId() + "`\n" +
                    "▫️ **Deleted Content:** " + (content.contains("Not cached") ? content : "```" + content + "```");

            net.dv8tion.jda.api.entities.User resolvedAuthor = cached != null
                    ? event.getJDA().getUserById(cached.authorId)
                    : null;

            java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> embedsList = new java.util.ArrayList<>();
            embedsList.add(EmbedUtil.createOldLogEmbed("message-delete", details, null, resolvedAuthor, null,
                    EmbedUtil.DANGER));

            if (cached != null) {
                for (String imgUrl : cached.imageAttachments) {
                    if (embedsList.size() < 10) {
                        embedsList.add(new net.dv8tion.jda.api.EmbedBuilder().setImage(imgUrl)
                                .setColor(EmbedUtil.DANGER).build());
                    }
                }
            }

            logManager.logEmbed(event.getGuild(), LogManager.LOG_MESSAGE,
                    embedsList.toArray(new net.dv8tion.jda.api.entities.MessageEmbed[0]));
        });
    }

    // ─────────────────────────── voice・logs ───────────────────────────

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        AudioChannel joined = event.getChannelJoined();
        AudioChannel left = event.getChannelLeft();
        net.dv8tion.jda.api.entities.Member m = event.getMember();
        String details;

        if (left == null && joined != null) {
            details = "### 🔊 Voice Link Established\n" +
                    "▫️ **Protocol:** `CONNECTION_JOIN`\n" +
                    "▫️ **Target Channel:** `" + joined.getName() + "`\n" +
                    "▫️ **Occupancy:** `" + joined.getMembers().size() + "`";
            logManager.logEmbed(event.getGuild(), LogManager.LOG_VOICE,
                    EmbedUtil.createOldLogEmbed("voice-join", details, m, null, null, EmbedUtil.SUCCESS));
        } else if (left != null && joined == null) {
            details = "### 🔇 Voice Link Severed\n" +
                    "▫️ **Protocol:** `CONNECTION_DISCONNECT`\n" +
                    "▫️ **Last Channel:** `" + left.getName() + "`";
            logManager.logEmbed(event.getGuild(), LogManager.LOG_VOICE,
                    EmbedUtil.createOldLogEmbed("voice-leave", details, m, null, null, EmbedUtil.DANGER));
        } else if (left != null) {
            details = "### 🔀 Voice Link Rerouted\n" +
                    "▫️ **Protocol:** `CONNECTION_SWITCH`\n" +
                    "▫️ **From:** `" + left.getName() + "`\n" +
                    "▫️ **To:** `" + joined.getName() + "`";
            logManager.logEmbed(event.getGuild(), LogManager.LOG_VOICE,
                    EmbedUtil.createOldLogEmbed("voice-switch", details, m, null, null, EmbedUtil.WARNING));
        }
    }

    // ─────────────────────────── channels・logs ───────────────────────────

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        String details = "### ➕ Structural Node Created\n" +
                "▫️ **Type:** `" + event.getChannel().getType().name() + "`\n" +
                "▫️ **Identifier:** " + event.getChannel().getAsMention();
        logManager.logEmbed(event.getGuild(), LogManager.LOG_CHANNELS,
                EmbedUtil.createOldLogEmbed("channel-init", details, null, null, null, EmbedUtil.SUCCESS));
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        String details = "### ➖ Structural Node Decommissioned\n" +
                "▫️ **Type:** `" + event.getChannel().getType().name() + "`\n" +
                "▫️ **Name:** `" + event.getChannel().getName() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_CHANNELS,
                EmbedUtil.createOldLogEmbed("channel-delete", details, null, null, null, EmbedUtil.DANGER));
    }

    @Override
    public void onChannelUpdateName(@NotNull ChannelUpdateNameEvent event) {
        String details = "### ✏️ Structural Node Renamed\n" +
                "▫️ **Old Name:** `" + event.getOldValue() + "`\n" +
                "▫️ **New Name:** `" + event.getNewValue() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_CHANNELS,
                EmbedUtil.createOldLogEmbed("channel-rename", details, null, null, null, EmbedUtil.WARNING));
    }

    // ─────────────────────────── roles・logs ───────────────────────────

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(logs -> {
            AuditLogEntry entry = logs.isEmpty() ? null : logs.get(0);
            String operator = (entry != null && entry.getTargetId().equals(event.getUser().getId()))
                    ? entry.getUser().getAsMention()
                    : "Higher Authority";
            String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
            String details = "### ➕ Clearance Level Granted\n" +
                    "▫️ **Target:** " + event.getUser().getAsMention() + "\n" +
                    "▫️ **Operator:** " + operator + "\n" +
                    "▫️ **Role:** `" + roles + "`";
            logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                    EmbedUtil.createOldLogEmbed("clearance-add", details, null, event.getUser(), event.getMember(),
                            EmbedUtil.SUCCESS));
        });
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(logs -> {
            AuditLogEntry entry = logs.isEmpty() ? null : logs.get(0);
            String operator = (entry != null && entry.getTargetId().equals(event.getUser().getId()))
                    ? entry.getUser().getAsMention()
                    : "Higher Authority";
            String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
            String details = "### ➖ Clearance Level Revoked\n" +
                    "▫️ **Target:** " + event.getUser().getAsMention() + "\n" +
                    "▫️ **Operator:** " + operator + "\n" +
                    "▫️ **Role:** `" + roles + "`";
            logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                    EmbedUtil.createOldLogEmbed("clearance-remove", details, null, event.getUser(), event.getMember(),
                            EmbedUtil.DANGER));
        });
    }

    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event) {
        String details = "### ➕ New Role Issued\n" +
                "▫️ **Name:** `" + event.getRole().getName() + "`\n" +
                "▫️ **ID:** `" + event.getRole().getId() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                EmbedUtil.createOldLogEmbed("role-init", details, null, null, null, EmbedUtil.SUCCESS));
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        String details = "### ➖ Role Deleted\n" +
                "▫️ **Name:** `" + event.getRole().getName() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                EmbedUtil.createOldLogEmbed("role-delete", details, null, null, null, EmbedUtil.DANGER));
    }

    @Override
    public void onRoleUpdateName(@NotNull RoleUpdateNameEvent event) {
        String details = "### ✏️ Role Renamed\n" +
                "▫️ **Old Name:** `" + event.getOldName() + "`\n" +
                "▫️ **New Name:** `" + event.getNewName() + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                EmbedUtil.createOldLogEmbed("role-rename", details, null, null, null, EmbedUtil.WARNING));
    }

    @Override
    public void onRoleUpdatePermissions(@NotNull RoleUpdatePermissionsEvent event) {
        String details = "### 🔒 Role Permissions Updated\n" +
                "▫️ **Role:** `" + event.getRole().getName() + "`\n" +
                "▫️ **Status:** Permissions synchronized.";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_ROLES,
                EmbedUtil.createOldLogEmbed("role-perms", details, null, null, null, EmbedUtil.WARNING));
    }

    // ─────────────────────────── users・logs ───────────────────────────

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        String details = "### 🏷️ Account Name Changed\n" +
                "▫️ **Old:** `" + event.getOldName() + "`\n" +
                "▫️ **New:** `" + event.getNewName() + "`";
        broadcastUserLog("user-name-update", details, event.getUser(), null);
    }

    @Override
    public void onUserUpdateAvatar(@NotNull UserUpdateAvatarEvent event) {
        String details = "### 🖼️ Profile Avatar Updated\n" +
                "▫️ **Image:** [View Source](" + event.getNewAvatarUrl() + ")";
        broadcastUserLog("user-avatar-update", details, event.getUser(), null);
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        String old = event.getOldNickname() != null ? event.getOldNickname() : "Original Name";
        String curr = event.getNewNickname() != null ? event.getNewNickname() : "Reverted to Original";
        String details = "### 🏷️ Server Nickname Updated\n" +
                "▫️ **Old:** `" + old + "`\n" +
                "▫️ **New:** `" + curr + "`";
        logManager.logEmbed(event.getGuild(), LogManager.LOG_USERS,
                EmbedUtil.createOldLogEmbed("user-nickname-update", details, null, event.getUser(), event.getMember(),
                        EmbedUtil.GOLD));
    }

    // ─────────────────────────── commands・logs ───────────────────────────

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null)
            return;
        String options = event.getOptions().stream()
                .map(opt -> opt.getName() + ": " + resolveOption(opt))
                .collect(Collectors.joining("\n"));
        String details = "### 📡 Command Execution\n" +
                "▫️ **Command:** `/" + event.getName() + "`\n" +
                "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "▫️ **Parameters:**\n" + (options.isEmpty() ? "`None`" : "```\n" + options + "\n```");
        logManager.logEmbed(event.getGuild(), LogManager.LOG_COMMANDS,
                EmbedUtil.createOldLogEmbed("command-intercept", details, event.getMember(), null, null,
                        EmbedUtil.INFO));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getGuild() == null)
            return;
        String details = "### 🔘 Button Interaction\n" +
                "▫️ **ID:** `" + event.getComponentId() + "`\n" +
                "▫️ **Label:** `" + event.getComponent().getLabel() + "`\n" +
                "▫️ **Channel:** " + event.getChannel().getAsMention();
        logManager.logEmbed(event.getGuild(), LogManager.LOG_COMMANDS,
                EmbedUtil.createOldLogEmbed("button-intercept", details, event.getMember(), null, null,
                        EmbedUtil.INFO));
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getGuild() == null)
            return;
        String values = String.join(", ", event.getValues());
        String details = "### 📑 Menu Interaction\n" +
                "▫️ **ID:** `" + event.getComponentId() + "`\n" +
                "▫️ **Selection:** `" + values + "`\n" +
                "▫️ **Channel:** " + event.getChannel().getAsMention();
        logManager.logEmbed(event.getGuild(), LogManager.LOG_COMMANDS,
                EmbedUtil.createOldLogEmbed("menu-intercept", details, event.getMember(), null, null, EmbedUtil.INFO));
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private void broadcastUserLog(String type, String details, net.dv8tion.jda.api.entities.User user,
            net.dv8tion.jda.api.entities.Member member) {
        user.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(user) != null) {
                logManager.logEmbed(guild, LogManager.LOG_USERS,
                        EmbedUtil.createOldLogEmbed(type, details, null, user, member, EmbedUtil.GOLD));
            }
        });
    }

    private String resolveOption(OptionMapping opt) {
        if (opt.getType() == OptionType.USER)
            return opt.getAsUser().getName();
        if (opt.getType() == OptionType.ROLE)
            return opt.getAsRole().getName();
        if (opt.getType() == OptionType.CHANNEL)
            return opt.getAsChannel().getName();
        return opt.getAsString();
    }
}
