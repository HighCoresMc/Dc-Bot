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

    public static final java.util.Set<Long> moderatedMessages = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(
                    new java.util.LinkedHashMap<Long, Boolean>() {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<Long, Boolean> eldest) {
                            return size() > 100;
                        }
                    }
            )
    );

    private final java.util.Map<Long, Integer> scamOffenses = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild())
            return;
        if (event.getAuthor().isBot())
            return;
        if (moderatedMessages.contains(event.getMessageIdLong()))
            return;

        String content = event.getMessage().getContentRaw();

        // Staff bypass word filter
        boolean isStaff = event.getMember() != null &&
                event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(staffRoleId) || r.getId().equals("1487152572207861870"));

        // Word Filter
        String forbidden = isStaff ? null : wordFilterService.findForbiddenWord(content);
        if (forbidden != null) {
            moderatedMessages.add(event.getMessageIdLong());
            // 1. Delete message
            event.getMessage().delete().queue(null, err -> {
            });

            boolean isStrict = wordFilterService.isStrict(forbidden);
            boolean timedOut = false;

            if (isStrict && event.getMember() != null && event.getGuild().getSelfMember().canInteract(event.getMember())) {
                try {
                    event.getMember().timeoutFor(java.time.Duration.ofMinutes(3))
                            .reason("Restricted word filter (Strict Match): " + forbidden)
                            .queue();
                    timedOut = true;
                } catch (Exception ignored) {}
            }

            // 2. Alert user (auto-delete after 5s)
            String alertMessage = "⚠️ <@" + event.getAuthor().getId()
                    + ">, your message was removed for containing a restricted word.";
            if (timedOut) {
                alertMessage = "⚠️ <@" + event.getAuthor().getId()
                        + ">, your message was removed and you have been timed out for **3 minutes** for using a restricted word.";
            }

            event.getChannel()
                    .sendMessage(alertMessage)
                    .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {
                    });

            // 3. Log to centralized mod-log channel
            String logBody = "### 🛡️ RESTRICTED WORD DETECTED\n" +
                    "▫️ **User:** " + event.getAuthor().getAsMention() + " (`" + event.getAuthor().getId() + "`)\n" +
                    "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "▫️ **Forbidden term:** `" + forbidden + "`\n" +
                    "▫️ **Severity:** " + (isStrict ? "🔴 STRIKE/STRICT" : "🟡 CONTEXT") + "\n" +
                    "▫️ **Action:** " + (timedOut ? "Muted (Timeout 3 Minutes)" : "Message Deleted") + "\n" +
                    "▫️ **Original content:** ```" + content + "```";

            logManager.logEmbed(event.getGuild(), LogManager.LOG_BLOCKED_WORDS,
                    EmbedUtil.createOldLogEmbed("word-filter", logBody, event.getMember(), event.getAuthor(),
                            event.getMember(), EmbedUtil.DANGER));
            return;
        }

        // Word filter for forwarded messages
        if (!isStaff && forbidden == null) {
            for (net.dv8tion.jda.api.entities.messages.MessageSnapshot snapshot : event.getMessage().getMessageSnapshots()) {
                String snapContent = snapshot.getContentRaw();
                forbidden = wordFilterService.findForbiddenWord(snapContent);
                if (forbidden != null) {
                    content = snapContent; // Overwrite content so it logs the forwarded text
                    
                    moderatedMessages.add(event.getMessageIdLong());
                    event.getMessage().delete().queue(null, err -> {});

                    boolean isStrict = wordFilterService.isStrict(forbidden);
                    boolean timedOut = false;
                    if (isStrict && event.getMember() != null && event.getGuild().getSelfMember().canInteract(event.getMember())) {
                        try {
                            event.getMember().timeoutFor(java.time.Duration.ofMinutes(3))
                                    .reason("Restricted word filter (Strict Match) in forwarded message: " + forbidden)
                                    .queue();
                            timedOut = true;
                        } catch (Exception ignored) {}
                    }

                    String alertMessage = "⚠️ <@" + event.getAuthor().getId()
                            + ">, your message was removed for containing a restricted word.";
                    if (timedOut) {
                        alertMessage = "⚠️ <@" + event.getAuthor().getId()
                                + ">, your message was removed and you have been timed out for **3 minutes** for using a restricted word.";
                    }

                    event.getChannel()
                            .sendMessage(alertMessage)
                            .delay(5, java.util.concurrent.TimeUnit.SECONDS)
                            .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                            .queue(null, err -> {});

                    String logBody = "### 🛡️ RESTRICTED WORD DETECTED (FORWARDED)\n" +
                            "▫️ **User:** " + event.getAuthor().getAsMention() + " (`" + event.getAuthor().getId() + "`)\n" +
                            "▫️ **Channel:** " + event.getChannel().getAsMention() + "\n" +
                            "▫️ **Forbidden term:** `" + forbidden + "`\n" +
                            "▫️ **Severity:** " + (isStrict ? "🔴 STRIKE/STRICT" : "🟡 CONTEXT") + "\n" +
                            "▫️ **Action:** " + (timedOut ? "Muted (Timeout 3 Minutes)" : "Message Deleted") + "\n" +
                            "▫️ **Original content:** ```" + content + "```";

                    logManager.logEmbed(event.getGuild(), LogManager.LOG_BLOCKED_WORDS,
                            EmbedUtil.createOldLogEmbed("word-filter", logBody, event.getMember(), event.getAuthor(),
                                    event.getMember(), EmbedUtil.DANGER));
                    return;
                }
            }
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
        if (moderatedMessages.contains(event.getMessageIdLong()))
            return;
        
        // Auto NSFW Media Filter (No bypass for staff)
        checkNsfw(event.getMessage(), event.getMessage().getContentRaw(), event.getMember(), event.getAuthor(), event.getChannel(), event.getGuild());
    }

    private boolean checkNsfw(net.dv8tion.jda.api.entities.Message message, String content, net.dv8tion.jda.api.entities.Member member, net.dv8tion.jda.api.entities.User author, net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, net.dv8tion.jda.api.entities.Guild guild) {
        log.info("[NSFW Filter] checkNsfw triggered. Content: '{}', Attachments: {}, Embeds: {}, Stickers: {}", 
                 content, message.getAttachments().size(), message.getEmbeds().size(), message.getStickers().size());
        if (moderatedMessages.contains(message.getIdLong())) {
            return false;
        }
        String blockReason = null;
        String offendingUrl = null;
        
        // 1. Check Stickers
        for (net.dv8tion.jda.api.entities.sticker.StickerItem sticker : message.getStickers()) {
            String res = imageModerationService.checkImage(sticker.getIconUrl());
            if (res != null) {
                blockReason = res;
                offendingUrl = sticker.getIconUrl();
                break;
            }
        }

        // 2. Check Attachments
        if (blockReason == null) {
            for (net.dv8tion.jda.api.entities.Message.Attachment attachment : message.getAttachments()) {
                if (attachment.isImage() || attachment.isVideo()) {
                    String res = imageModerationService.checkImage(attachment.getUrl());
                    if (res != null) {
                        blockReason = res;
                        offendingUrl = attachment.getUrl();
                        break;
                    }
                }
            }
        }
        
        // 2.5 Check Forwarded Messages (MessageSnapshots)
        if (blockReason == null) {
            for (net.dv8tion.jda.api.entities.messages.MessageSnapshot snapshot : message.getMessageSnapshots()) {
                // Check snapshot stickers
                for (net.dv8tion.jda.api.entities.sticker.StickerItem sticker : snapshot.getStickers()) {
                    String res = imageModerationService.checkImage(sticker.getIconUrl());
                    if (res != null) {
                        blockReason = res;
                        offendingUrl = sticker.getIconUrl();
                        content = snapshot.getContentRaw();
                        break;
                    }
                }
                if (blockReason != null) break;

                // Check snapshot attachments
                for (net.dv8tion.jda.api.entities.Message.Attachment attachment : snapshot.getAttachments()) {
                    if (attachment.isImage() || attachment.isVideo()) {
                        String res = imageModerationService.checkImage(attachment.getUrl());
                        if (res != null) {
                            blockReason = res;
                            offendingUrl = attachment.getUrl();
                            content = snapshot.getContentRaw();
                            break;
                        }
                    }
                }
                if (blockReason != null) break;

                // Check snapshot text for URLs
                String snapshotContent = snapshot.getContentRaw();
                if (snapshotContent != null && snapshotContent.contains("http")) {
                    String[] words = snapshotContent.split("\\s+");
                    for (String w : words) {
                        if (w.startsWith("http://") || w.startsWith("https://")) {
                            if (w.contains("tenor.com") || w.contains("giphy.com") || w.contains("gfycat.com")) continue;
                            String res = imageModerationService.checkImage(w);
                            if (res != null) {
                                blockReason = res;
                                offendingUrl = w;
                                content = snapshotContent;
                                break;
                            }
                        }
                    }
                }
                if (blockReason != null) break;

                // Check snapshot embeds
                for (net.dv8tion.jda.api.entities.MessageEmbed embed : snapshot.getEmbeds()) {
                    if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                        String url = embed.getImage().getUrl();
                        if (isImageUrl(url)) {
                            String res = imageModerationService.checkImage(url);
                            if (res != null) {
                                blockReason = res;
                                offendingUrl = url;
                                content = snapshot.getContentRaw();
                                break;
                            }
                        }
                    }
                }
                if (blockReason != null) break;
            }
        }
        
        // 3. Check Content URLs
        if (blockReason == null && content.contains("http")) {
            String[] words = content.split("\\s+");
            for (String w : words) {
                if (w.startsWith("http://") || w.startsWith("https://")) {
                    if (w.contains("tenor.com/view") || w.contains("giphy.com/gifs") || w.contains("gfycat.com/")) {
                        continue;
                    }
                    String res = imageModerationService.checkImage(w);
                    if (res != null) {
                        blockReason = res;
                        offendingUrl = w;
                        break;
                    }
                }
            }
        }

        // 4. Check Embeds (Tenor GIFs)
        if (blockReason == null) {
            for (net.dv8tion.jda.api.entities.MessageEmbed embed : message.getEmbeds()) {
                if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                    String url = embed.getImage().getUrl();
                    if (isImageUrl(url)) {
                        String res = imageModerationService.checkImage(url);
                        if (res != null) {
                            blockReason = res;
                            offendingUrl = url;
                            break;
                        }
                    }
                }
                
                if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null) {
                    String url = embed.getThumbnail().getUrl();
                    if (isImageUrl(url)) {
                        String res = imageModerationService.checkImage(url);
                        if (res != null) {
                            blockReason = res;
                            offendingUrl = url;
                            break;
                        }
                    }
                }
            }
        }

        if (blockReason != null) {
            moderatedMessages.add(message.getIdLong());
            
            // Download image before deleting message (so CDN link is still valid)
            byte[] offendingImageBytes = null;
            if (offendingUrl != null) {
                try {
                    java.net.URL url = new java.net.URL(offendingUrl);
                    java.net.URLConnection conn = url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) OpexyBot/1.0");
                    java.io.InputStream is = conn.getInputStream();
                    offendingImageBytes = is.readAllBytes();
                    is.close();
                } catch (Exception e) {
                    log.warn("[Image Filter] Failed to download offending image from {}: {}", offendingUrl, e.getMessage());
                }
            }

            message.delete().queue(null, err -> {});

            String reasonMsg = "restricted media (NSFW/Pornographic content)";
            boolean timedOut = false;
            boolean banned = false;

            if (blockReason.equals("SCAM_QR")) {
                reasonMsg = "malicious trade/scam links";
            } else if (blockReason.equals("SCAM_CRYPTO")) {
                reasonMsg = "crypto scams/fake promotions";
                // Enforce punishments
                int count = scamOffenses.getOrDefault(author.getIdLong(), 0);
                if (member != null && guild.getSelfMember().canInteract(member)) {
                    if (count >= 1) {
                        try {
                            member.ban(0, java.util.concurrent.TimeUnit.DAYS)
                                  .reason("Repeated crypto scam/fake promotion offenses")
                                  .queue();
                            banned = true;
                        } catch (Exception ignored) {}
                    } else {
                        try {
                            member.timeoutFor(java.time.Duration.ofDays(1))
                                  .reason("Crypto scam/fake promotion image detected")
                                  .queue();
                            timedOut = true;
                            scamOffenses.put(author.getIdLong(), count + 1);
                        } catch (Exception ignored) {}
                    }
                }
            }

            String actionTaken = banned ? "Banned" : (timedOut ? "Muted (Timeout 1 Day)" : "Message Deleted");
            String alertMessage = "⚠️ <@" + author.getId() + ">, your message was removed for containing " + reasonMsg + ".";
            if (banned) {
                alertMessage = "⚠️ <@" + author.getId() + "> has been banned for repeated " + reasonMsg + ".";
            } else if (timedOut) {
                alertMessage = "⚠️ <@" + author.getId() + ">, your message was removed and you have been timed out for **1 Day** for posting " + reasonMsg + ".";
            }

            channel.sendMessage(alertMessage)
                    .delay(10, java.util.concurrent.TimeUnit.SECONDS)
                    .flatMap(net.dv8tion.jda.api.entities.Message::delete)
                    .queue(null, err -> {});

            String displayContent = content.isBlank() ? "*(No text, attachment only)*" : "```\n" + content + "\n```";
            String logBody = "### 🛡️ RESTRICTED " + blockReason + " DETECTED\n" +
                    "▫️ **User:** " + author.getAsMention() + " (`" + author.getId() + "`)\n" +
                    "▫️ **Channel:** " + channel.getAsMention() + "\n" +
                    "▫️ **Type:** `AI_AUTO_DETECT`\n" +
                    "▫️ **Action:** `" + actionTaken + "`\n" +
                    "▫️ **Original content:**\n" + displayContent;

            net.dv8tion.jda.api.EmbedBuilder builder = new net.dv8tion.jda.api.EmbedBuilder(EmbedUtil.createOldLogEmbed("image-filter", logBody, member, author, member, EmbedUtil.DANGER));
            
            if (offendingImageBytes != null) {
                builder.setImage("attachment://offense.png");
                net.dv8tion.jda.api.entities.MessageEmbed embed = builder.build();
                logManager.logEmbedWithImage(guild, LogManager.LOG_BLOCKED_WORDS, embed, offendingImageBytes, "offense.png");
            } else {
                net.dv8tion.jda.api.entities.MessageEmbed embed = builder.build();
                logManager.logEmbed(guild, LogManager.LOG_BLOCKED_WORDS, embed);
            }
            return true;
        }
        return false;
    }

    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || 
            lower.contains(".mp4?") || lower.contains(".webm?") || lower.contains(".mov?")) {
            return false;
        }
        return true;
    }
}
