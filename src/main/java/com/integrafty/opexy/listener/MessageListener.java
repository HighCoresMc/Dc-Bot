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
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw();

        // Staff bypass word filter
        boolean isStaff = event.getMember() != null &&
                event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(staffRoleId));

        // Word Filter
        String forbidden = isStaff ? null : wordFilterService.findForbiddenWord(content);
        if (forbidden != null) {
            // 1. Delete message
            event.getMessage().delete().queue(null, err -> {});

            // 2. Alert user (auto-delete after 5s)
            event.getChannel()
                    .sendMessage("⚠️ <@" + event.getAuthor().getId() + ">, your message was removed for containing a restricted word.")
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {});

            // 3. Log to centralized mod-log channel
            String logBody = "### 🛡️ RESTRICTED WORD DETECTED\n" +
                    "▫️ **User:** " + event.getAuthor().getAsMention() + " (`" + event.getAuthor().getId() + "`)\n" +
                    "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "▫️ **Forbidden term:** `" + forbidden + "`\n" +
                    "▫️ **Original content:** ```" + content + "```";

            logManager.logEmbed(event.getGuild(), LogManager.LOG_BLOCKED_WORDS, 
                    EmbedUtil.createOldLogEmbed("word-filter", logBody, event.getMember(), event.getAuthor(), event.getMember(), EmbedUtil.DANGER));
            return;
        }

        // Auto NSFW Media Filter (No bypass for staff)
        boolean isNsfw = false;
        
        // 1. Check Stickers
        for (net.dv8tion.jda.api.entities.sticker.StickerItem sticker : event.getMessage().getStickers()) {
            if (imageModerationService.isPornographic(sticker.getIconUrl())) {
                isNsfw = true;
                break;
            }
        }

        // 2. Check Attachments
        if (!isNsfw) {
            for (net.dv8tion.jda.api.entities.Message.Attachment attachment : event.getMessage().getAttachments()) {
                if (attachment.isImage() || attachment.isVideo()) {
                    if (imageModerationService.isPornographic(attachment.getUrl())) {
                        isNsfw = true;
                        break;
                    }
                }
            }
        }
        
        // 3. Check Content URLs (like Tenor GIFs)
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

        if (isNsfw) {
            event.getMessage().delete().queue(null, err -> {});

            event.getChannel()
                    .sendMessage("⚠️ <@" + event.getAuthor().getId() + ">, your message was removed for containing restricted media (NSFW/Pornographic content).")
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {});

            String logBody = "### 🛡️ RESTRICTED NSFW MEDIA DETECTED\n" +
                    "▫️ **User:** " + event.getAuthor().getAsMention() + " (`" + event.getAuthor().getId() + "`)\n" +
                    "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "▫️ **Type:** `AI_AUTO_DETECT`\n" +
                    "▫️ **Original content:** ```" + content + "```";

            logManager.logEmbed(event.getGuild(), LogManager.LOG_BLOCKED_WORDS, 
                    EmbedUtil.createOldLogEmbed("nsfw-filter", logBody, event.getMember(), event.getAuthor(), event.getMember(), EmbedUtil.DANGER));
            return;
        }

        // Auto Replies
        String autoReply = autoReplyService.getResponse(content);
        if (autoReply != null) {
            event.getMessage().reply(autoReply).queue();
        }
    }
}
