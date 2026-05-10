package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.integrafty.opexy.entity.NotificationEntity;
import com.integrafty.opexy.repository.NotificationRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final JDA jda;
    private final NotificationRepository notificationRepository;
    private final KickService kickService;
    private final TwitchService twitchService;
    private final YouTubeService youtubeService;

    private static final String LIVE_CHANNEL_ID = "1487140470172815574";
    private static final String VIDEO_CHANNEL_ID = "1487204004076191946";
    private static final String LIVE_ROLE_MENTION = "<@&1487196786488770610>";
    private static final String VIDEO_ROLE_MENTION = "<@&1500269236583399454>";

    @Scheduled(fixedRate = 60000) // 1 Minute
    @org.springframework.transaction.annotation.Transactional
    public void checkNotifications() {
        log.info("Starting notification check cycle...");
        List<NotificationEntity> entities = notificationRepository.findAll();
        for (NotificationEntity entity : entities) {
            if (!entity.isActive()) continue;

            try {
                switch (entity.getPlatform().toUpperCase()) {
                    case "KICK":
                        handleKick(entity);
                        break;
                    case "TWITCH":
                        handleTwitch(entity);
                        break;
                    case "YOUTUBE":
                        handleYouTube(entity);
                        break;
                }
            } catch (Exception e) {
                log.error("Error checking notification for {}: {}", entity.getDisplayName(), e.getMessage());
            }
        }
    }

    private void handleKick(NotificationEntity entity) {
        kickService.getStreamStatus(entity.getChannelId()).ifPresent(livestream -> {
            String streamId = livestream.get("id").getAsString();
            if (entity.getLastContentId() == null) {
                // First run - set baseline
                entity.setLastContentId(streamId);
                notificationRepository.save(entity);
                return;
            }
            if (!streamId.equals(entity.getLastContentId())) {
                String title = livestream.get("session_title").getAsString();
                
                String thumbnail = "";
                if (livestream.has("thumbnail") && !livestream.get("thumbnail").isJsonNull()) {
                    thumbnail = livestream.getAsJsonObject("thumbnail").get("url").getAsString();
                }
                
                log.info("Kick Live: {} | Title: {} | Thumbnail: {}", entity.getDisplayName(), title, thumbnail);
                
                String url = "https://kick.com/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "KICK");
            }
        });
    }

    private void handleTwitch(NotificationEntity entity) {
        twitchService.getStreamStatus(entity.getChannelId()).ifPresent(json -> {
            String streamId = json.get("id").getAsString();
            if (entity.getLastContentId() == null) {
                entity.setLastContentId(streamId);
                notificationRepository.save(entity);
                return;
            }
            if (!streamId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail_url").getAsString().replace("{width}", "1280").replace("{height}", "720");
                String url = "https://twitch.tv/" + entity.getChannelId();
                sendLiveNotification(entity, streamId, url, title, thumbnail, "TWITCH");
            }
        });
    }

    private void handleYouTube(NotificationEntity entity) {
        if (entity.getChannelId() != null && !entity.getChannelId().startsWith("UC")) {
            String resolved = youtubeService.resolveChannelId(entity.getChannelId());
            if (resolved != null && resolved.startsWith("UC")) {
                entity.setChannelId(resolved);
                notificationRepository.save(entity);
            } else {
                return;
            }
        }

        // 1. Check for Live Stream first
        Optional<JsonObject> liveStream = youtubeService.getLiveStream(entity.getChannelId());
        if (liveStream.isPresent()) {
            JsonObject json = liveStream.get();
            String videoId = json.get("videoId").getAsString();
            if (entity.getLastContentId() == null) {
                entity.setLastContentId(videoId);
                notificationRepository.save(entity);
                log.info("Baseline set for YouTube Live {}: {}", entity.getDisplayName(), videoId);
                return;
            }
            if (!videoId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail").getAsString();
                String url = "https://youtube.com/watch?v=" + videoId;
                sendLiveNotification(entity, videoId, url, title, thumbnail, "YOUTUBE");
            }
            return; // Stop here if we found a live stream (whether new or already notified)
        }

        // 2. Main RSS check (No scraper fallback to prevent old video false positives on 500 errors)
        youtubeService.getLatestVideo(entity.getChannelId()).ifPresent(json -> {
            String videoId = json.get("videoId").getAsString();
            if (entity.getLastContentId() == null) {
                // Baseline - don't notify for old videos
                entity.setLastContentId(videoId);
                notificationRepository.save(entity);
                log.info("Baseline set for YouTube {}: {}", entity.getDisplayName(), videoId);
                return;
            }
            if (!videoId.equals(entity.getLastContentId())) {
                String title = json.get("title").getAsString();
                String thumbnail = json.get("thumbnail").getAsString();
                String url = "https://youtube.com/watch?v=" + videoId;
                sendVideoNotification(entity, videoId, url, title, thumbnail);
            }
        });
    }

    private void sendLiveNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail, String platform) {
        TextChannel channel = jda.getTextChannelById(LIVE_CHANNEL_ID);
        if (channel == null) return;

        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        String body = String.format("%s\n\n### %s\n**%s is Live now on %s !**\n\nClick the button below to join the stream.", LIVE_ROLE_MENTION, title, entity.getDisplayName(), platform);
        Container container = EmbedUtil.containerBranded(platform, "Live Stream", body, thumbnail, ActionRow.of(Button.link(url, "Watch Stream")));

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setComponents(container)
            .useComponentsV2(true);
        
        channel.sendMessage(builder.build())
            .useComponentsV2(true)
            .queue();
    }

    private void sendVideoNotification(NotificationEntity entity, String contentId, String url, String title, String thumbnail) {
        TextChannel channel = jda.getTextChannelById(VIDEO_CHANNEL_ID);
        if (channel == null) return;

        entity.setLastContentId(contentId);
        notificationRepository.save(entity);

        log.info("Sending video notification for {} to channel {}", entity.getDisplayName(), VIDEO_CHANNEL_ID);
        String body = String.format("%s\n\n### %s\n**New Video from %s !**\n\nClick the button below to watch the video.", VIDEO_ROLE_MENTION, title, entity.getDisplayName());
        Container container = EmbedUtil.containerBranded("YOUTUBE", "New Upload", body, thumbnail, ActionRow.of(Button.link(url, "Watch Video")));

        MessageCreateBuilder builder = new MessageCreateBuilder()
            .setComponents(container)
            .useComponentsV2(true);
        
        channel.sendMessage(builder.build())
            .useComponentsV2(true)
            .queue(msg -> log.info("Video notification sent successfully for {}", entity.getDisplayName()));
    }
}
