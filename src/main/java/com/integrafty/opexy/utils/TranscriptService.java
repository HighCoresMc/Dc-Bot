package com.integrafty.opexy.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TranscriptService {

    public static byte[] generateSimpleTranscript(TextChannel channel, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- TRANSCRIPT FOR CHANNEL: ").append(channel.getName()).append(" ---\n");
        sb.append("Generated on: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            sb.append("[").append(msg.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("] ");
            sb.append(msg.getAuthor().getEffectiveName()).append(": ");
            sb.append(msg.getContentDisplay());
            if (!msg.getAttachments().isEmpty()) {
                sb.append(" [Attachments: ").append(msg.getAttachments().size()).append("]");
            }
            sb.append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String generateHtmlTranscript(String tid, String channelName, String type, String status, String opener, List<Message> messages) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Transcript #").append(tid).append("</title>");
        html.append("<style>");
        html.append("body { background-color: #313338; color: #dbdee1; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; }");
        html.append(".header { background-color: #2b2d31; padding: 20px 30px; box-shadow: 0 1px 2px rgba(0,0,0,0.2); }");
        html.append(".header h1 { color: #f2f3f5; font-size: 24px; margin: 0 0 10px 0; }");
        html.append(".header p { color: #b5bac1; margin: 5px 0; font-size: 14px; }");
        html.append(".chat { padding: 20px 0; }");
        html.append(".msg-group { display: flex; padding: 2px 30px; margin-top: 17px; }");
        html.append(".msg-group:hover { background-color: rgba(2,2,2,0.06); }");
        html.append(".avatar { width: 40px; height: 40px; border-radius: 50%; overflow: hidden; margin-right: 16px; flex-shrink: 0; }");
        html.append(".avatar img { width: 100%; height: 100%; object-fit: cover; }");
        html.append(".msg-content { flex-grow: 1; min-width: 0; }");
        html.append(".msg-header { display: flex; align-items: baseline; margin-bottom: 2px; }");
        html.append(".author { color: #f2f3f5; font-weight: 500; font-size: 16px; margin-right: 8px; }");
        html.append(".bot-tag { background-color: #5865F2; color: #fff; font-size: 10px; padding: 2px 4px; border-radius: 3px; font-weight: 500; margin-right: 8px; vertical-align: middle; }");
        html.append(".time { color: #949ba4; font-size: 12px; }");
        html.append(".text { color: #dbdee1; font-size: 16px; line-height: 1.375rem; white-space: pre-wrap; word-wrap: break-word; margin-bottom: 8px; }");
        
        // Embeds
        html.append(".embed { max-width: 520px; display: flex; margin-top: 8px; border-radius: 4px; overflow: hidden; background-color: #2b2d31; }");
        html.append(".embed-color { width: 4px; flex-shrink: 0; }");
        html.append(".embed-inner { padding: 12px 16px 16px 12px; display: flex; flex-direction: column; gap: 8px; width: 100%; box-sizing: border-box; }");
        html.append(".embed-author { display: flex; align-items: center; gap: 8px; }");
        html.append(".embed-author img { width: 24px; height: 24px; border-radius: 50%; }");
        html.append(".embed-author-name { color: #f2f3f5; font-size: 14px; font-weight: 600; }");
        html.append(".embed-title { color: #00a8fc; font-size: 16px; font-weight: 600; text-decoration: none; }");
        html.append(".embed-desc { color: #dbdee1; font-size: 14px; white-space: pre-wrap; }");
        html.append(".embed-fields { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }");
        html.append(".embed-field { flex: 1 1 100%; min-width: 0; }");
        html.append(".embed-field.inline { flex: 1 1 30%; }");
        html.append(".embed-field-name { color: #f2f3f5; font-size: 14px; font-weight: 600; margin-bottom: 2px; }");
        html.append(".embed-field-value { color: #dbdee1; font-size: 14px; white-space: pre-wrap; }");
        html.append(".embed-image img { max-width: 100%; border-radius: 4px; margin-top: 8px; }");
        html.append(".embed-footer { display: flex; align-items: center; gap: 8px; margin-top: 8px; }");
        html.append(".embed-footer img { width: 20px; height: 20px; border-radius: 50%; }");
        html.append(".embed-footer-text { color: #949ba4; font-size: 12px; }");
        
        // Attachments
        html.append(".attachment { margin-top: 8px; }");
        html.append(".attachment img { max-width: 400px; max-height: 300px; border-radius: 8px; display: block; }");
        html.append(".attachment video { max-width: 400px; max-height: 300px; border-radius: 8px; display: block; }");
        
        html.append("</style></head><body>");
        
        html.append("<div class='header'>");
        html.append("<h1>\uD83D\uDCDC Ticket Transcript: #").append(tid).append("</h1>");
        html.append("<p><strong>Channel:</strong> ").append(channelName).append(" | <strong>Category:</strong> ").append(type).append("</p>");
        html.append("<p><strong>Opener:</strong> ").append(opener).append(" | <strong>Status:</strong> ").append(status).append("</p>");
        html.append("</div>");

        html.append("<div class='chat'>");
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            String avatarUrl = m.getAuthor().getEffectiveAvatarUrl();
            if (avatarUrl == null) avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
            boolean isBot = m.getAuthor().isBot();
            
            html.append("<div class='msg-group'>");
            html.append("<div class='avatar'><img src='").append(avatarUrl).append("' alt='avatar'></div>");
            html.append("<div class='msg-content'>");
            
            html.append("<div class='msg-header'>");
            html.append("<span class='author'>").append(escapeHtml(m.getAuthor().getEffectiveName())).append("</span>");
            if (isBot) {
                html.append("<span class='bot-tag'>BOT</span>");
            }
            html.append("<span class='time'>").append(m.getTimeCreated().format(DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a"))).append("</span>");
            html.append("</div>");
            
            if (m.getContentDisplay() != null && !m.getContentDisplay().isEmpty()) {
                html.append("<div class='text'>").append(escapeHtml(m.getContentDisplay())).append("</div>");
            }
            
            // Attachments
            for (Message.Attachment att : m.getAttachments()) {
                html.append("<div class='attachment'>");
                if (att.isImage()) {
                    html.append("<img src='").append(att.getUrl()).append("' alt='Attachment'>");
                } else if (att.isVideo()) {
                    html.append("<video controls><source src='").append(att.getUrl()).append("'></video>");
                } else {
                    html.append("<a href='").append(att.getUrl()).append("' style='color: #00a8fc;' target='_blank'>\uD83D\uDCCE ").append(escapeHtml(att.getFileName())).append("</a>");
                }
                html.append("</div>");
            }
            
            // Embeds
            for (MessageEmbed embed : m.getEmbeds()) {
                String colorHex = embed.getColor() != null ? String.format("#%06x", embed.getColor().getRGB() & 0xFFFFFF) : "#202225";
                html.append("<div class='embed'>");
                html.append("<div class='embed-color' style='background-color: ").append(colorHex).append(";'></div>");
                html.append("<div class='embed-inner'>");
                
                if (embed.getAuthor() != null) {
                    html.append("<div class='embed-author'>");
                    if (embed.getAuthor().getIconUrl() != null) {
                        html.append("<img src='").append(embed.getAuthor().getIconUrl()).append("' alt=''>");
                    }
                    html.append("<span class='embed-author-name'>").append(escapeHtml(embed.getAuthor().getName())).append("</span>");
                    html.append("</div>");
                }
                
                if (embed.getTitle() != null) {
                    html.append("<div class='embed-title'>");
                    if (embed.getUrl() != null) {
                        html.append("<a href='").append(embed.getUrl()).append("' target='_blank' class='embed-title'>").append(escapeHtml(embed.getTitle())).append("</a>");
                    } else {
                        html.append(escapeHtml(embed.getTitle()));
                    }
                    html.append("</div>");
                }
                
                if (embed.getDescription() != null) {
                    html.append("<div class='embed-desc'>").append(escapeHtml(embed.getDescription())).append("</div>");
                }
                
                if (!embed.getFields().isEmpty()) {
                    html.append("<div class='embed-fields'>");
                    for (MessageEmbed.Field field : embed.getFields()) {
                        html.append("<div class='embed-field ").append(field.isInline() ? "inline" : "").append("'>");
                        html.append("<div class='embed-field-name'>").append(escapeHtml(field.getName())).append("</div>");
                        html.append("<div class='embed-field-value'>").append(escapeHtml(field.getValue())).append("</div>");
                        html.append("</div>");
                    }
                    html.append("</div>");
                }
                
                if (embed.getImage() != null) {
                    html.append("<div class='embed-image'><img src='").append(embed.getImage().getUrl()).append("' alt='Embed Image'></div>");
                }
                
                if (embed.getThumbnail() != null && embed.getImage() == null) {
                     html.append("<div class='embed-image'><img src='").append(embed.getThumbnail().getUrl()).append("' style='max-width: 100px;' alt='Thumbnail'></div>");
                }
                
                if (embed.getFooter() != null) {
                    html.append("<div class='embed-footer'>");
                    if (embed.getFooter().getIconUrl() != null) {
                        html.append("<img src='").append(embed.getFooter().getIconUrl()).append("' alt=''>");
                    }
                    html.append("<span class='embed-footer-text'>").append(escapeHtml(embed.getFooter().getText())).append("</span>");
                    html.append("</div>");
                }
                
                html.append("</div>"); // inner
                html.append("</div>"); // embed
            }
            
            html.append("</div>"); // msg-content
            html.append("</div>"); // msg-group
        }
        html.append("</div>"); // chat

        html.append("</body></html>");
        return html.toString();
    }
    
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;")
                   .replace("\n", "<br>");
    }
}
