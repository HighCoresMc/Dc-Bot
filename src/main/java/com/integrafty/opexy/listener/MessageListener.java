package com.integrafty.opexy.listener;

import com.integrafty.opexy.service.AutoReplyService;
import com.integrafty.opexy.service.WordFilterService;
import com.integrafty.opexy.utils.EmbedUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.integrafty.opexy.service.LogManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener extends ListenerAdapter {

    private final JDA jda;
    private final AutoReplyService autoReplyService;
    private final WordFilterService wordFilterService;
    private final com.integrafty.opexy.service.ImageModerationService imageModerationService;

    @Value("${opexy.roles.op-staff}")
    private String staffRoleId;

    private final LogManager logManager;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;
        if (event.getAuthor().isBot())
            return;

        String content = event.getMessage().getContentRaw();

        // Staff bypass word filter
        boolean isStaff = event.getMember() != null &&
                event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(staffRoleId));

        // Word Filter
        String forbidden = isStaff ? null : wordFilterService.findForbiddenWord(content);
        if (forbidden != null) {
            // 1. Delete message
            event.getMessage().delete().queue(null, err -> {
            });

            // 2. Alert user (auto-delete after 5s)
            event.getChannel()
                    .sendMessage("⚠️ <@" + event.getAuthor().getId()
                            + ">, your message was removed for containing a restricted word.")
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {
                    });

            // 3. Log to centralized mod-log channel
            String logBody = "### 🛡️ RESTRICTED WORD DETECTED\n" +
                    "▫️ **User:** " + event.getAuthor().getAsMention() + " (`" + event.getAuthor().getId() + "`)\n" +
                    "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "▫️ **Forbidden term:** `" + forbidden + "`\n" +
                    "▫️ **Original content:** ```" + content + "```";

            logManager.logEmbed(event.getGuild(), LogManager.LOG_BLOCKED_WORDS,
                    EmbedUtil.createOldLogEmbed("word-filter", logBody, event.getMember(), event.getAuthor(),
                            event.getMember(), EmbedUtil.DANGER));
            return;
        }

        // Auto NSFW Media Filter (No bypass for staff)
        if (checkNsfw(event.getMessage(), content, event.getMember(), event.getAuthor(), event.getChannel(), event.getGuild())) {
            return;
        }

        // Auto Replies
        String autoReply = autoReplyService.getResponse(content);
        if (autoReply != null) {
            event.getMessage().reply(autoReply).queue();
        }
    }

    @Override
    public void onMessageUpdate(net.dv8tion.jda.api.events.message.MessageUpdateEvent event) {
        if (!event.isFromGuild())
            return;
        if (event.getAuthor().isBot())
            return;
        
        // Auto NSFW Media Filter (No bypass for staff)
        checkNsfw(event.getMessage(), event.getMessage().getContentRaw(), event.getMember(), event.getAuthor(), event.getChannel(), event.getGuild());
    }

    private boolean checkNsfw(net.dv8tion.jda.api.entities.Message message, String content, net.dv8tion.jda.api.entities.Member member, net.dv8tion.jda.api.entities.User author, net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, net.dv8tion.jda.api.entities.Guild guild) {
        log.info("[NSFW Filter] checkNsfw triggered. Content: '{}', Attachments: {}, Embeds: {}, Stickers: {}", 
                 content, message.getAttachments().size(), message.getEmbeds().size(), message.getStickers().size());
        boolean isNsfw = false;
        
        // 1. Check Stickers
        for (net.dv8tion.jda.api.entities.sticker.StickerItem sticker : message.getStickers()) {
            if (imageModerationService.isPornographic(sticker.getIconUrl())) {
                isNsfw = true;
                break;
            }
        }

        // 2. Check Attachments
        if (!isNsfw) {
            for (net.dv8tion.jda.api.entities.Message.Attachment attachment : message.getAttachments()) {
                if (attachment.isImage() || attachment.isVideo()) {
                    if (imageModerationService.isPornographic(attachment.getUrl())) {
                        isNsfw = true;
                        break;
                    }
                }
            }
        }
        
        // 3. Check Content URLs
        if (!isNsfw && content.contains("http")) {
            String[] words = content.split("\\s+");
            for (String w : words) {
                if (w.startsWith("http://") || w.startsWith("https://")) {
                    if (imageModerationService.isPornographic(w)) {
                        isNsfw = true;
                        break;
                    }
                }
            }
        }

        // 4. Check Embeds (Tenor GIFs)
        if (!isNsfw) {
            for (net.dv8tion.jda.api.entities.MessageEmbed embed : message.getEmbeds()) {
                if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                    if (imageModerationService.isPornographic(embed.getImage().getUrl())) {
                        isNsfw = true;
                        break;
                    }
                } else if (embed.getVideoInfo() != null && embed.getVideoInfo().getUrl() != null) {
                    if (imageModerationService.isPornographic(embed.getVideoInfo().getUrl())) {
                        isNsfw = true;
                        break;
                    }
                } else if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null) {
                    if (imageModerationService.isPornographic(embed.getThumbnail().getUrl())) {
                        isNsfw = true;
                        break;
                    }
                }
            }
        }

        if (isNsfw) {
            message.delete().queue(null, err -> {});

            channel.sendMessage("⚠️ <@" + author.getId() + ">, your message was removed for containing restricted media (NSFW/Pornographic content).")
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {});

            String logBody = "### 🛡️ RESTRICTED NSFW MEDIA DETECTED\n" +
                    "▫️ **User:** " + author.getAsMention() + " (`" + author.getId() + "`)\n" +
                    "▫️ **Channel:** " + channel.getAsMention() + "\n" +
                    "▫️ **Type:** `AI_AUTO_DETECT`\n" +
                    "▫️ **Original content:** ```" + content + "```";

            logManager.logEmbed(guild, LogManager.LOG_BLOCKED_WORDS, 
                    EmbedUtil.createOldLogEmbed("nsfw-filter", logBody, member, author, member, EmbedUtil.DANGER));
            return true;
        }
        return false;
    }
}
