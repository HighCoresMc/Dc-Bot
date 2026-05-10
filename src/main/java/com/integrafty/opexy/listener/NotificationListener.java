package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.NotificationEntity;
import com.integrafty.opexy.repository.NotificationRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import com.integrafty.opexy.service.notification.KickService;
import com.integrafty.opexy.service.notification.YouTubeService;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final JDA jda;
    private final NotificationRepository notificationRepository;
    private final YouTubeService youtubeService;
    private final KickService kickService;

    /*@PostConstruct
    public void init() {
        jda.addEventListener(this);
    }*/

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("notify_")) return;

        switch (id) {
            case "notify_add_live":
                event.replyModal(Modal.create("modal_notify_add_live", "Add Channel")
                    .addComponents(
                        Label.of("Channel URL", TextInput.create("url", TextInputStyle.SHORT).setPlaceholder("https://kick.com/itvrz or https://twitch.tv/shroud").setRequired(true).build())
                    ).build()).queue();
                break;
            case "notify_add_video":
                event.replyModal(Modal.create("modal_notify_add_video", "Add YouTube Channel")
                    .addComponents(
                        Label.of("YouTube URL", TextInput.create("url", TextInputStyle.SHORT).setPlaceholder("https://youtube.com/@MrBeast").setRequired(true).build())
                    ).build()).queue();
                break;
            case "notify_remove_live":
                handleRemoveMenu(event, false);
                break;
            case "notify_remove_video":
                handleRemoveMenu(event, true);
                break;
        }
    }

    private void handleRemoveMenu(ButtonInteractionEvent event, boolean isVideo) {
        List<NotificationEntity> list = notificationRepository.findAll().stream()
                .filter(e -> isVideo ? e.getPlatform().equals("YOUTUBE") : !e.getPlatform().equals("YOUTUBE"))
                .toList();

        if (list.isEmpty()) {
            event.reply("No channels found to remove.").setEphemeral(true).queue();
            return;
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("menu_notify_remove")
                .setPlaceholder("Select channel to remove");

        for (NotificationEntity e : list) {
            menu.addOption(e.getDisplayName() + " (" + e.getPlatform() + ")", String.valueOf(e.getId()));
        }

        event.reply("Select the channel you want to stop tracking:").setEphemeral(true).addComponents(ActionRow.of(menu.build())).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (!id.startsWith("modal_notify_")) return;

        // Defer reply because YouTube resolution might take > 3 seconds
        event.deferReply(true).queue();

        String url = event.getValue("url").getAsString();
        String urlLower = url.toLowerCase();
        NotificationEntity entity = new NotificationEntity();
        entity.setGuildId(event.getGuild().getId());

        String rawId;
        if (urlLower.contains("kick.com/")) {
            entity.setPlatform("KICK");
            rawId = extractFromUrl(url, "kick.com/");
            entity.setChannelId(rawId);
        } else if (urlLower.contains("twitch.tv/")) {
            entity.setPlatform("TWITCH");
            rawId = extractFromUrl(url, "twitch.tv/");
            entity.setChannelId(rawId);
        } else if (urlLower.contains("youtube.com/") || urlLower.contains("youtu.be/")) {
            entity.setPlatform("YOUTUBE");
            if (urlLower.contains("/@")) rawId = extractFromUrl(url, "/@");
            else if (urlLower.contains("/channel/")) rawId = extractFromUrl(url, "/channel/");
            else if (urlLower.contains("/user/")) rawId = extractFromUrl(url, "/user/");
            else rawId = extractFromUrl(url, "youtube.com/");
            
            // Resolve to UC... ID right now!
            String resolved = youtubeService.resolveChannelId(rawId);
            if (resolved == null) {
                event.getHook().sendMessage("Could not find a valid YouTube channel ID for: " + rawId).setEphemeral(true).queue();
                return;
            }
            entity.setChannelId(resolved);
            entity.setDisplayName(rawId); // Keep the handle as display name
        } else {
            event.getHook().sendMessage("Invalid URL!").setEphemeral(true).queue();
            return;
        }

        if (entity.getDisplayName() == null) entity.setDisplayName(entity.getChannelId());
        
        // Prevent duplicates
        boolean exists = notificationRepository.findAll().stream()
                .anyMatch(e -> e.getGuildId().equals(entity.getGuildId()) 
                        && e.getPlatform().equals(entity.getPlatform()) 
                        && e.getChannelId().equalsIgnoreCase(entity.getChannelId()));
        
        if (exists) {
            event.getHook().sendMessage("This channel is already being tracked!").setEphemeral(true).queue();
            return;
        }

        try {
            notificationRepository.save(entity);
            event.getHook().sendMessage("Successfully added **" + entity.getDisplayName() + "** from " + entity.getPlatform() + "!").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("Failed to save notification entity: {}", e.getMessage());
            event.getHook().sendMessage("Failed to save to database. The channel might already be added.").setEphemeral(true).queue();
        }
    }

    private String extractFromUrl(String url, String marker) {
        String urlLower = url.toLowerCase();
        String markerLower = marker.toLowerCase();
        int index = urlLower.indexOf(markerLower);
        if (index == -1) return url;
        
        String part = url.substring(index + marker.length());
        if (part.contains("/")) part = part.substring(0, part.indexOf("/"));
        if (part.contains("?")) part = part.substring(0, part.indexOf("?"));
        return part;
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("menu_notify_remove")) return;

        try {
            Long id = Long.parseLong(event.getValues().get(0));
            notificationRepository.deleteById(id);
            event.reply("Channel removed from tracking.").setEphemeral(true).queue();
        } catch (Exception e) {
            event.reply("Error removing channel.").setEphemeral(true).queue();
        }
    }
}
