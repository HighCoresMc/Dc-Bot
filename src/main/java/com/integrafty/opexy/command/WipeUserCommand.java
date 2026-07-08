package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.integrafty.opexy.service.LogManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class WipeUserCommand implements SlashCommand {

    @Value("${opexy.roles.op-staff}")
    private String opStaffRoleId;

    private final LogManager logManager;

    @Override
    public String getName() { return "wipe-user"; }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("wipe-user", "حذف جميع رسائل عضو من السيرفر")
                .addOption(OptionType.STRING, "user_id", "أيدي العضو", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!hasAccess(event.getMember())) {
            sendEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        String targetId = event.getOption("user_id").getAsString();

        // Validate ID format
        if (!targetId.matches("\\d{17,20}")) {
            sendEphemeral(event, EmbedUtil.error("Invalid ID", "The provided user ID is not valid."));
            return;
        }

        // Defer reply since this operation takes time
        event.deferReply().queue();

        AtomicInteger totalDeleted = new AtomicInteger(0);
        List<TextChannel> channels = event.getGuild().getTextChannels();

        // Process channels sequentially to avoid rate limits
        processChannels(channels, 0, targetId, totalDeleted, event);
    }

    private void processChannels(List<TextChannel> channels, int index, String targetId,
                                  AtomicInteger totalDeleted, SlashCommandInteractionEvent event) {
        if (index >= channels.size()) {
            // All channels processed — send summary
            int count = totalDeleted.get();
            String body = "### عملية المسح اكتملت | WIPE COMPLETE\n<divider>\n" +
                    "▫️ **الهدف:** `" + targetId + "`\n" +
                    "▫️ **الرسائل المحذوفة:** `" + count + "`\n" +
                    "▫️ **المسؤول:** " + event.getMember().getAsMention();
            Container container = EmbedUtil.containerBranded("MODERATION", "User Wipe Complete", body, EmbedUtil.BANNER_MAIN);
            MessageCreateBuilder builder = new MessageCreateBuilder().setComponents(container).useComponentsV2(true);
            event.getHook().sendMessage(builder.build()).useComponentsV2(true).queue();
            log.info("[wipe-user] Deleted {} messages from user {} by {}", count, targetId, event.getUser().getId());

            // LOGGING
            String logDetails = String.format("### 🗑️ Action: User History Purge (Wipe)\n▫️ **Target ID:** `%s`\n▫️ **Messages Deleted:** %d\n▫️ **Moderator:** %s",
                    targetId, count, event.getMember().getAsMention());
            logManager.logEmbed(event.getGuild(), LogManager.LOG_MODS_CMD, 
                    EmbedUtil.createOldLogEmbed("wipe-user", logDetails, event.getMember(), null, null, EmbedUtil.DANGER));
            return;
        }

        TextChannel channel = channels.get(index);

        // Fetch up to 100 messages and filter by target user
        channel.getIterableHistory().takeAsync(100).thenAccept(messages -> {
            List<Message> toDelete = messages.stream()
                    .filter(m -> m.getAuthor().getId().equals(targetId))
                    .toList();

            if (toDelete.isEmpty()) {
                processChannels(channels, index + 1, targetId, totalDeleted, event);
                return;
            }

            if (toDelete.size() == 1) {
                // Single message — delete individually
                toDelete.get(0).delete().queue(
                        v -> {
                            totalDeleted.addAndGet(1);
                            processChannels(channels, index + 1, targetId, totalDeleted, event);
                        },
                        err -> processChannels(channels, index + 1, targetId, totalDeleted, event));
            } else {
                // Bulk delete (only works for messages < 14 days old)
                channel.purgeMessages(toDelete);
                totalDeleted.addAndGet(toDelete.size());
                processChannels(channels, index + 1, targetId, totalDeleted, event);
            }
        }).exceptionally(err -> {
            log.warn("[wipe-user] Could not read channel {}: {}", channel.getName(), err.getMessage());
            processChannels(channels, index + 1, targetId, totalDeleted, event);
            return null;
        });
    }

    private boolean hasAccess(net.dv8tion.jda.api.entities.Member member) {
        return member != null && member.getRoles().stream().anyMatch(r -> r.getId().equals(opStaffRoleId));
    }

    private void sendEphemeral(SlashCommandInteractionEvent event, Container container) {
        MessageCreateBuilder builder = new MessageCreateBuilder().setComponents(container).useComponentsV2(true);
        event.reply(builder.build()).setEphemeral(true).useComponentsV2(true).queue();
    }
}
