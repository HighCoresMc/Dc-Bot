package com.integrafty.opexy.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TranscriptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    public static byte[] buildHtml(String id, String channelName, String type, String status, String openedAt,
            String openerName, String staffName, String closedBy, JsonArray messages) {

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>Transcript #").append(id).append("</title>");
        sb.append("<style>");
        // Basic Reset
        sb.append("*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}");
        sb.append("body { background-color: #313338; color: #dbdee1; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; }");
        
        // HighCore Header
        sb.append(".hc-hero { background:#111214; padding:32px 48px; border-bottom:1px solid #1e1f22; }");
        sb.append(".hc-hero-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:24px; }");
        sb.append(".hc-logo-group { display:flex; align-items:center; gap:12px; }");
        sb.append(".hc-logo-mark { width:40px; height:40px; background:linear-gradient(135deg, #FFD700, #B8860B); border-radius:8px; display:flex; align-items:center; justify-content:center; color:#000; font-weight:800; font-size:16px; }");
        sb.append(".hc-logo-text { font-size:18px; font-weight:800; color:#fff; letter-spacing:0.5px; }");
        sb.append(".hc-logo-sub { font-size:12px; font-weight:600; color:#B8860B; text-transform:uppercase; letter-spacing:1px; margin-top:2px; }");
        sb.append(".hc-channel-name { font-size:16px; font-weight:600; color:#80848e; }");
        sb.append(".hc-channel-name span { color:#4e5058; margin-right:4px; font-size:20px; }");
        
        sb.append(".hc-stats { display:flex; gap:16px; flex-wrap:wrap; }");
        sb.append(".hc-stat-card { background:#1e1f22; border:1px solid #2b2d31; border-radius:8px; padding:16px; min-width:140px; flex:1; }");
        sb.append(".hc-stat-label { font-size:11px; font-weight:700; color:#80848e; text-transform:uppercase; margin-bottom:8px; letter-spacing:0.5px; }");
        sb.append(".hc-stat-value { font-size:14px; font-weight:600; color:#fff; word-break:break-word; }");
        sb.append(".badge { display:inline-block; padding:4px 8px; border-radius:4px; font-size:11px; font-weight:700; }");
        sb.append(".badge-type { background:rgba(88,101,242,0.15); color:#5865f2; border:1px solid rgba(88,101,242,0.3); }");
        sb.append(".badge-closed { background:rgba(237,66,69,0.15); color:#ed4245; border:1px solid rgba(237,66,69,0.3); }");
        
        // Discord Chat Layout
        sb.append(".chat-wrap { padding:24px 0 64px; }");
        sb.append(".msg-group { display: flex; padding: 4px 48px; margin-top: 17px; }");
        sb.append(".msg-group:hover { background-color: rgba(2,2,2,0.06); }");
        sb.append(".avatar { width: 40px; height: 40px; border-radius: 50%; display:flex; align-items:center; justify-content:center; color:#fff; font-size:18px; font-weight:bold; flex-shrink: 0; margin-right: 16px; overflow: hidden; }");
        sb.append(".avatar img { width: 100%; height: 100%; object-fit: cover; }");
        sb.append(".msg-content { flex-grow: 1; min-width: 0; }");
        sb.append(".msg-header { display: flex; align-items: baseline; margin-bottom: 2px; }");
        sb.append(".author { color: #f2f3f5; font-weight: 500; font-size: 16px; margin-right: 8px; }");
        sb.append(".bot-tag { background-color: #5865F2; color: #fff; font-size: 10px; padding: 2px 4px; border-radius: 3px; font-weight: 500; margin-right: 8px; vertical-align: middle; display:inline-flex; align-items:center; }");
        sb.append(".time { color: #949ba4; font-size: 12px; }");
        sb.append(".text { color: #dbdee1; font-size: 16px; line-height: 1.375rem; white-space: pre-wrap; word-wrap: break-word; margin-bottom: 4px; }");
        
        // Embeds
        sb.append(".embed { max-width: 520px; display: flex; margin-top: 8px; border-radius: 4px; overflow: hidden; background-color: #2b2d31; }");
        sb.append(".embed-color { width: 4px; flex-shrink: 0; }");
        sb.append(".embed-inner { padding: 12px 16px 16px 12px; display: flex; flex-direction: column; gap: 8px; width: 100%; box-sizing: border-box; }");
        sb.append(".embed-author { display: flex; align-items: center; gap: 8px; }");
        sb.append(".embed-author img { width: 24px; height: 24px; border-radius: 50%; }");
        sb.append(".embed-author-name { color: #f2f3f5; font-size: 14px; font-weight: 600; }");
        sb.append(".embed-title { color: #00a8fc; font-size: 16px; font-weight: 600; text-decoration: none; }");
        sb.append(".embed-desc { color: #dbdee1; font-size: 14px; white-space: pre-wrap; }");
        sb.append(".embed-fields { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }");
        sb.append(".embed-field { flex: 1 1 100%; min-width: 0; }");
        sb.append(".embed-field.inline { flex: 1 1 30%; }");
        sb.append(".embed-field-name { color: #f2f3f5; font-size: 14px; font-weight: 600; margin-bottom: 2px; }");
        sb.append(".embed-field-value { color: #dbdee1; font-size: 14px; white-space: pre-wrap; }");
        sb.append(".embed-image img { max-width: 100%; border-radius: 4px; margin-top: 8px; }");
        sb.append(".embed-footer { display: flex; align-items: center; gap: 8px; margin-top: 8px; }");
        sb.append(".embed-footer img { width: 20px; height: 20px; border-radius: 50%; }");
        sb.append(".embed-footer-text { color: #949ba4; font-size: 12px; }");
        
        // Attachments
        sb.append(".attachment { margin-top: 8px; }");
        sb.append(".attachment img { max-width: 400px; max-height: 300px; border-radius: 8px; display: block; cursor: pointer; }");
        sb.append(".attachment video { max-width: 400px; max-height: 300px; border-radius: 8px; display: block; }");
        
        sb.append("</style></head><body>");

        // HighCore Hero Section
        sb.append("<div class='hc-hero'><div class='hc-hero-top'>");
        sb.append("<div class='hc-logo-group'><div class='hc-logo-mark'>HC</div><div><div class='hc-logo-text'>HIGH CORE MC</div><div class='hc-logo-sub'>Ticket Transcript</div></div></div>");
        sb.append("<div class='hc-channel-name'><span>#</span>").append(escapeHtml(channelName)).append("</div></div>");
        sb.append("<div class='hc-stats'>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Ticket ID</div><div class='hc-stat-value'>#").append(id).append("</div></div>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Type</div><div class='hc-stat-value'><span class='badge badge-type'>").append(type).append("</span></div></div>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Status</div><div class='hc-stat-value'><span class='badge badge-closed'>").append(status.toUpperCase()).append("</span></div></div>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Opened By</div><div class='hc-stat-value'>").append(escapeHtml(openerName)).append("</div></div>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Opened At</div><div class='hc-stat-value'>").append(formatDate(openedAt)).append("</div></div>");
        sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Messages</div><div class='hc-stat-value'>").append(messages.size()).append("</div></div>");
        if (staffName != null && !staffName.equals("Not Handled")) {
            sb.append("<div class='hc-stat-card'><div class='hc-stat-label'>Handler ID</div><div class='hc-stat-value' id='ticket-handler-id'>").append(escapeHtml(staffName)).append("</div></div>");
        }
        sb.append("</div></div>");

        // Chat Section
        sb.append("<div class='chat-wrap'>");

        String lastUser = "";
        for (int i = 0; i < messages.size(); i++) {
            JsonObject m = messages.get(i).getAsJsonObject();
            String uId = safe(m, "user_id");
            String uName = safe(m, "user_name");
            String rawDbContent = safe(m, "content");
            String time = formatTime(safe(m, "created_at"));
            boolean isBot = uName.toLowerCase().contains("bot") || uName.toLowerCase().contains("agency") || uName.equals("Highcore") || uName.contains("Opexy");

            if (!uId.equals(lastUser)) {
                if (!lastUser.isEmpty()) sb.append("</div></div>"); // Close previous msg-content & msg-group
                
                sb.append("<div class='msg-group'>");
                sb.append("<div class='avatar' style='background:").append(getAvatarColor(uName)).append("'>").append(uName.substring(0, 1).toUpperCase()).append("</div>");
                sb.append("<div class='msg-content'>");
                sb.append("<div class='msg-header'>");
                sb.append("<span class='author'>").append(escapeHtml(uName)).append("</span>");
                if (isBot) sb.append("<span class='bot-tag'>BOT</span>");
                sb.append("<span class='time'>").append(time).append("</span>");
                sb.append("</div>");
            }

            // Parse JSON content if it exists
            if (rawDbContent.startsWith("JSON:")) {
                try {
                    JsonObject payload = JsonParser.parseString(rawDbContent.substring(5)).getAsJsonObject();
                    String rawText = safe(payload, "raw");
                    if (!rawText.isEmpty()) {
                        sb.append("<div class='text'>").append(escapeHtml(rawText)).append("</div>");
                    }
                    
                    if (payload.has("attachments")) {
                        JsonArray atts = payload.getAsJsonArray("attachments");
                        for (JsonElement ae : atts) {
                            JsonObject a = ae.getAsJsonObject();
                            String url = safe(a, "url");
                            String name = safe(a, "name");
                            boolean isImage = a.has("isImage") && a.get("isImage").getAsBoolean();
                            boolean isVideo = a.has("isVideo") && a.get("isVideo").getAsBoolean();
                            
                            sb.append("<div class='attachment'>");
                            if (isImage) {
                                sb.append("<a href='").append(url).append("' target='_blank'><img src='").append(url).append("' alt='Attachment'></a>");
                            } else if (isVideo) {
                                sb.append("<video controls><source src='").append(url).append("'></video>");
                            } else {
                                sb.append("<a href='").append(url).append("' style='color: #00a8fc;' target='_blank'>&#x1F4CE; ").append(escapeHtml(name)).append("</a>");
                            }
                            sb.append("</div>");
                        }
                    }
                    
                    if (payload.has("embeds")) {
                        JsonArray embeds = payload.getAsJsonArray("embeds");
                        for (JsonElement ee : embeds) {
                            JsonObject e = ee.getAsJsonObject();
                            String colorHex = safe(e, "color").isEmpty() ? "#202225" : safe(e, "color");
                            sb.append("<div class='embed'>");
                            sb.append("<div class='embed-color' style='background-color: ").append(colorHex).append(";'></div>");
                            sb.append("<div class='embed-inner'>");
                            
                            if (e.has("author")) {
                                JsonObject au = e.getAsJsonObject("author");
                                sb.append("<div class='embed-author'>");
                                if (!safe(au, "icon").isEmpty()) sb.append("<img src='").append(safe(au, "icon")).append("' alt=''>");
                                sb.append("<span class='embed-author-name'>").append(escapeHtml(safe(au, "name"))).append("</span>");
                                sb.append("</div>");
                            }
                            
                            if (!safe(e, "title").isEmpty()) {
                                sb.append("<div class='embed-title'>").append(escapeHtml(safe(e, "title"))).append("</div>");
                            }
                            
                            if (!safe(e, "description").isEmpty()) {
                                sb.append("<div class='embed-desc'>").append(escapeHtml(safe(e, "description"))).append("</div>");
                            }
                            
                            if (e.has("fields")) {
                                JsonArray fields = e.getAsJsonArray("fields");
                                if (fields.size() > 0) {
                                    sb.append("<div class='embed-fields'>");
                                    for (JsonElement fe : fields) {
                                        JsonObject f = fe.getAsJsonObject();
                                        boolean inline = f.has("inline") && f.get("inline").getAsBoolean();
                                        sb.append("<div class='embed-field ").append(inline ? "inline" : "").append("'>");
                                        sb.append("<div class='embed-field-name'>").append(escapeHtml(safe(f, "name"))).append("</div>");
                                        sb.append("<div class='embed-field-value'>").append(escapeHtml(safe(f, "value"))).append("</div>");
                                        sb.append("</div>");
                                    }
                                    sb.append("</div>");
                                }
                            }
                            
                            if (!safe(e, "image").isEmpty()) {
                                sb.append("<div class='embed-image'><img src='").append(safe(e, "image")).append("' alt='Embed Image'></div>");
                            } else if (!safe(e, "thumbnail").isEmpty()) {
                                sb.append("<div class='embed-image'><img src='").append(safe(e, "thumbnail")).append("' style='max-width: 100px;' alt='Thumbnail'></div>");
                            }
                            
                            if (e.has("footer")) {
                                JsonObject ft = e.getAsJsonObject("footer");
                                sb.append("<div class='embed-footer'>");
                                if (!safe(ft, "icon").isEmpty()) sb.append("<img src='").append(safe(ft, "icon")).append("' alt=''>");
                                sb.append("<span class='embed-footer-text'>").append(escapeHtml(safe(ft, "text"))).append("</span>");
                                sb.append("</div>");
                            }
                            
                            sb.append("</div></div>");
                        }
                    }
                } catch (Exception ex) {
                    sb.append("<div class='text'>[Error Parsing Message Data]</div>");
                }
            } else {
                // Fallback for old messages
                if (!rawDbContent.trim().isEmpty()) {
                    sb.append("<div class='text'>").append(processOldContent(rawDbContent)).append("</div>");
                }
            }
            
            lastUser = uId;
        }
        
        if (messages.size() > 0) sb.append("</div></div>"); // Close the final msg-content & msg-group

        sb.append("</div>"); // End chat-wrap
        sb.append("</body></html>");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String formatDate(String iso) {
        try {
            return DATE_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String formatTime(String iso) {
        try {
            return TIME_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String getAvatarColor(String name) {
        int hash = name.hashCode();
        String[] colors = { "#5865f2", "#3ba55c", "#fac418", "#ed4245", "#eb459e", "#57c97e" };
        return colors[Math.abs(hash) % colors.length];
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

    private static String processOldContent(String content) {
        String html = escapeHtml(content);

        if (html.contains("[ATTACHMENT: ")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[ATTACHMENT: (.*?)\\]").matcher(html);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String url = m.group(1);
                String lowerUrl = url.toLowerCase();
                if (lowerUrl.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp)(?:\\?.*)?$")) {
                    m.appendReplacement(sb, "<div class='attachment'><a href='" + url + "' target='_blank'><img src='" + url + "' alt='Image Attachment'></a></div>");
                } else if (lowerUrl.matches(".*\\.(mp4|mov|webm|avi)(?:\\?.*)?$")) {
                    m.appendReplacement(sb, "<div class='attachment'><video controls><source src='" + url + "'></video></div>");
                } else {
                    String fileName = "Download File/Video";
                    try {
                        String[] parts = url.split("\\?")[0].split("/");
                        fileName = parts[parts.length - 1];
                    } catch (Exception ignored) { }
                    m.appendReplacement(sb, "<div class='attachment'><a href='" + url + "' target='_blank' style='color: #00a8fc;'>&#x1F4CE; " + fileName + "</a></div>");
                }
            }
            m.appendTail(sb);
            html = sb.toString();
        }
        return html;
    }
}
