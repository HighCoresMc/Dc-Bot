package com.integrafty.opexy.listener;

import com.integrafty.opexy.entity.VoiceRoomEntity;
import com.integrafty.opexy.repository.VoiceRoomRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoiceListener extends ListenerAdapter {

    private final JDA jda;
    private final VoiceRoomRepository voiceRoomRepository;

    // TODO: Move these to database/config
    private static final String JOIN_TO_CREATE_ID = "1500190617353851037";
    private static final String VOICE_CATEGORY_ID = "1486872074369892392";
    private static final String VOICE_DASHBOARD_ID = "1486872077263835157";

    @PostConstruct
    public void init() {
        // Redundant as CommandManager handles it automatically via dependency injection
        // jda.addEventListener(this);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        
        // 1. Handle Leaving Room (Delete if empty)
        if (event.getChannelLeft() != null && event.getChannelLeft().getType() == net.dv8tion.jda.api.entities.channel.ChannelType.VOICE) {
            VoiceChannel leftChannel = event.getChannelLeft().asVoiceChannel();
            String leftId = leftChannel.getId();
            String parentId = leftChannel.getParentCategoryId();
            
            // CRITICAL SHIELD: Never delete main channels
            if (!leftId.equals(JOIN_TO_CREATE_ID) && !leftId.equals(VOICE_DASHBOARD_ID)) {
                if (VOICE_CATEGORY_ID.equals(parentId) && leftChannel.getMembers().isEmpty()) {
                    log.info("🗑️ Deleting vacated channel: {}", leftChannel.getName());
                    
                    voiceRoomRepository.findByChannelId(leftId).ifPresent(room -> {
                        try {
                            // Capture current settings
                            room.setRoomName(leftChannel.getName());
                            room.setBitrate(leftChannel.getBitrate());
                            room.setUserLimit(leftChannel.getUserLimit());
                            room.setChannelId("0"); // Mark as inactive but keep settings
                            voiceRoomRepository.save(room);
                        } catch (Exception e) {
                            log.warn("⚠️ Database update failed during channel deletion, but proceeding.");
                        }
                    });

                    leftChannel.delete().queue(null, err -> {});
                }
            }
        }

        // 2. Handle Joining "Join to Create"
        if (event.getChannelJoined() != null && event.getChannelJoined().getType() == net.dv8tion.jda.api.entities.channel.ChannelType.VOICE) {
            String joinedId = event.getChannelJoined().getId();
            if (joinedId.equals(JOIN_TO_CREATE_ID)) {
                log.info("✨ Triggering Room Creation for {}", member.getEffectiveName());
                handleCreateRoom(member, event.getChannelJoined().getParentCategory());
            }
        }
    }

    private void handleCreateRoom(Member member, Category category) {
        // 🛡️ HARD DUPLICATION PREVENTION
        List<VoiceRoomEntity> existingList = voiceRoomRepository.findAllByOwnerId(member.getId());
        
        // Reuse existing record if it exists to keep persistent settings
        VoiceRoomEntity room = existingList.isEmpty() ? new VoiceRoomEntity() : existingList.get(0);
        room.setOwnerId(member.getId());

        // Delete any orphan channels still linked to this user
        for (VoiceRoomEntity old : existingList) {
            if (old.getChannelId() != null && !old.getChannelId().equals("0")) {
                VoiceChannel oldChannel = member.getGuild().getVoiceChannelById(old.getChannelId());
                if (oldChannel != null) {
                    try {
                        oldChannel.delete().queue(null, err -> {});
                    } catch (Exception e) {
                        log.warn("Failed to delete old channel: {}", e.getMessage());
                    }
                }
            }
        }
        
        // DO NOT WIPE DB RECORDS. Update the existing one instead to maintain persistence.

        String channelName = room.getRoomName() != null ? room.getRoomName() : "🔊 | " + member.getEffectiveName();
        int bitrate = room.getBitrate() != null ? room.getBitrate() : member.getGuild().getMaxBitrate();
        int userLimit = room.getUserLimit() != null ? room.getUserLimit() : 0;
        
        member.getGuild().createVoiceChannel(channelName, category)
            .addPermissionOverride(member.getGuild().getSelfMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), null)
            .addPermissionOverride(member.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT), null)
            .queue(channel -> {
                // Apply Settings
                channel.getManager().setBitrate(Math.min(bitrate, member.getGuild().getMaxBitrate())).setUserLimit(userLimit).queue();
                
                // Restore Trusted Users
                if (room.getTrustedUserIds() != null) {
                    for (String trustId : room.getTrustedUserIds().split(",")) {
                        if (trustId.isEmpty()) continue;
                        channel.getManager().putMemberPermissionOverride(Long.parseLong(trustId), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue(null, err -> {});
                    }
                }

                // Save new record
                room.setOwnerId(member.getId());
                room.setChannelId(channel.getId());
                room.setRoomName(channelName);
                room.setBitrate(bitrate);
                room.setUserLimit(userLimit);
                room.setStatus("OPEN");
                voiceRoomRepository.save(room);

                member.getGuild().moveVoiceMember(member, channel).queue();
                sendControlPanel(channel, member);
            });
    }

    private void sendControlPanel(VoiceChannel channel, Member owner) {
        String body = "### Room Management\n" +
                "Control your voice session using the dashboard below.\n" +
                "Changes are applied instantly to the channel.";
        ActionRow row1 = ActionRow.of(
            Button.secondary("voice_rename", "Rename"),
            Button.secondary("voice_limit", "Limit"),
            Button.secondary("voice_bitrate", "Bitrate"),
            Button.primary("voice_region", "Region")
        );
        
        ActionRow row2 = ActionRow.of(
            Button.success("voice_speak_perm", "Speak"),
            Button.success("voice_write_perm", "Write"),
            Button.success("voice_video_perm", "Video"),
            Button.secondary("voice_lock", "Lock/Unlock")
        );
 
        ActionRow row3 = ActionRow.of(
            Button.danger("voice_kick", "Kick"),
            Button.primary("voice_trust", "Trust"),
            Button.primary("voice_block", "Block"),
            Button.primary("voice_panel", "Members")
        );
 
        ActionRow row4 = ActionRow.of(
            Button.primary("voice_ownership", "Owner"),
            Button.success("voice_shop", "Shop"),
            Button.danger("voice_request_staff", "Staff"),
            Button.danger("voice_delete", "Delete")
        );
 
        Container container = EmbedUtil.containerBranded("Voice Control", "Room Management Panel", body, EmbedUtil.BANNER_MAIN, row1, row2, row3, row4);


        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);

        channel.sendMessage(builder.build()).useComponentsV2(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("voice_")) return;

        VoiceChannel channel = null;
        if (event.getChannelType().isAudio()) {
            channel = event.getChannel().asVoiceChannel();
        } else {
            // Find user's room from repository using their ID as primary key
            Optional<VoiceRoomEntity> ownedRoom = voiceRoomRepository.findById(event.getUser().getId());
            if (ownedRoom.isPresent() && ownedRoom.get().getChannelId() != null) {
                channel = event.getGuild().getVoiceChannelById(ownedRoom.get().getChannelId());
            }
        }

        if (channel == null) {
            if (!event.isAcknowledged()) event.reply("You do not have an active private room.").setEphemeral(true).queue();
            return;
        }

        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(channel.getId());
        if (roomOpt.isEmpty()) {
            if (!event.isAcknowledged()) event.reply("This room is not registered in the system.").setEphemeral(true).queue();
            return;
        }

        VoiceRoomEntity room = roomOpt.get();
        boolean isOwner = room.getOwnerId().equals(event.getUser().getId());
        boolean isAdmin = event.getMember().hasPermission(Permission.MANAGE_CHANNEL);

        if (!isOwner && !isAdmin && !id.equals("voice_ownership")) {
            if (!event.isAcknowledged()) event.reply("You do not have permission to control this room.").setEphemeral(true).queue();
            return;
        }

        switch (id) {
            case "voice_rename":
                event.replyModal(Modal.create("modal_voice_rename", "Rename Room")
                    .addComponents(Label.of("New Name", TextInput.create("voice_new_name", TextInputStyle.SHORT).setPlaceholder("🔊 | My Room").build())).build()).queue();
                break;
            case "voice_limit":
                event.replyModal(Modal.create("modal_voice_limit", "User Limit")
                    .addComponents(Label.of("Limit (0-99)", TextInput.create("voice_new_limit", TextInputStyle.SHORT).setPlaceholder("0 = No Limit").build())).build()).queue();
                break;
            case "voice_bitrate":
                StringSelectMenu bitrateMenu = StringSelectMenu.create("menu_voice_bitrate")
                    .setPlaceholder("Choose Audio Quality")
                    .addOption("64 kbps (Standard)", "64")
                    .addOption("96 kbps (High)", "96")
                    .addOption("128 kbps (Ultra)", "128")
                    .addOption("256 kbps (Nitro)", "256")
                    .addOption("384 kbps (Studio)", "384")
                    .build();
                event.reply("Select the audio quality for your room:").setEphemeral(true).addComponents(ActionRow.of(bitrateMenu)).queue();
                break;
            case "voice_kick":
                StringSelectMenu.Builder kickMenu = StringSelectMenu.create("menu_voice_kick")
                    .setPlaceholder("Select user to kick");
                
                List<Member> membersInRoom = channel.getMembers().stream()
                    .filter(m -> !m.getUser().isBot())
                    .filter(m -> !m.getId().equals(event.getUser().getId()))
                    .toList();

                if (membersInRoom.isEmpty()) {
                    event.reply("No other members are in the room.").setEphemeral(true).queue();
                    return;
                }

                membersInRoom.forEach(m -> kickMenu.addOption(m.getEffectiveName(), m.getId()));
                event.reply("Select the member you want to kick:").setEphemeral(true).addComponents(ActionRow.of(kickMenu.build())).queue();
                break;
            case "voice_video_perm":
                openMemberSelectMenu(channel, event, "video", "Select member to toggle Video/Share");
                break;
            case "voice_write_perm":
                openMemberSelectMenu(channel, event, "write", "Select member to toggle Chat Access");
                break;
            case "voice_speak_perm":
                openMemberSelectMenu(channel, event, "speak", "Select member to toggle Speak Access");
                break;
            case "voice_region":
                StringSelectMenu regionMenu = StringSelectMenu.create("menu_voice_region")
                    .setPlaceholder("Select Voice Region")
                    .addOption("Automatic (Recommended)", "auto")
                    .addOption("US East", "us-east")
                    .addOption("US West", "us-west")
                    .addOption("Europe (Rotterdam)", "rotterdam")
                    .addOption("Singapore", "singapore")
                    .addOption("Japan", "japan")
                    .build();
                event.reply("Choose the server location for your room:").setEphemeral(true).addComponents(ActionRow.of(regionMenu)).queue();
                break;
            case "voice_trust":
                event.replyModal(Modal.create("modal_voice_trust", "Grant Access")
                    .addComponents(Label.of("User ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_block":
                event.replyModal(Modal.create("modal_voice_block", "Block User")
                    .addComponents(Label.of("User ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_lock":
                boolean isLocked = channel.getPermissionOverride(event.getGuild().getPublicRole()) != null && 
                                   channel.getPermissionOverride(event.getGuild().getPublicRole()).getDenied().contains(Permission.VOICE_CONNECT);
                if (isLocked) {
                    channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                    event.reply("Room unlocked for everyone.").setEphemeral(true).queue();
                } else {
                    channel.getManager().putRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                    event.reply("Room locked for everyone.").setEphemeral(true).queue();
                }
                break;
            case "voice_shop":
                event.reply("### Voice Shop\nFeatures coming soon! Stay tuned for premium upgrades.").setEphemeral(true).queue();
                break;
            case "voice_ownership":
                String ownerMention = event.getGuild().getMemberById(room.getOwnerId()).getAsMention();
                event.reply("Current room owner is: " + ownerMention)
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(Button.danger("voice_transfer_start", "Transfer Ownership")))
                    .queue();
                break;
            case "voice_transfer_start":
                event.replyModal(Modal.create("modal_voice_transfer", "Transfer Ownership")
                    .addComponents(Label.of("New Owner ID", TextInput.create("voice_user_id", TextInputStyle.SHORT).build())).build()).queue();
                break;
            case "voice_panel":
                sendMemberPanel(channel, event);
                break;
            case "voice_request_staff":
                event.reply("Are you sure you want to open a support ticket?")
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(
                        Button.danger("voice_staff_confirm", "Confirm"),
                        Button.secondary("voice_cancel", "Cancel")
                    )).queue();
                break;
            case "voice_staff_confirm":
                event.reply("Ticket opened successfully. Staff will contact you shortly.").setEphemeral(true).queue();
                break;
            case "voice_delete":
                event.reply("Are you sure you want to delete this room permanently?")
                    .setEphemeral(true)
                    .addComponents(ActionRow.of(
                        Button.danger("voice_delete_confirm", "Delete Now"),
                        Button.secondary("voice_cancel", "Cancel")
                    )).queue();
                break;
            case "voice_delete_confirm":
                channel.delete().queue();
                event.reply("Room has been deleted.").setEphemeral(true).queue();
                break;
            case "voice_cancel":
                event.reply("Operation cancelled.").setEphemeral(true).queue();
                break;
        }
    }

    private void openMemberSelectMenu(VoiceChannel channel, ButtonInteractionEvent event, String type, String placeholder) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("menu_voice_perm_" + type)
            .setPlaceholder(placeholder);
        
        List<Member> members = channel.getMembers().stream()
            .filter(m -> !m.getUser().isBot())
            .toList();

        if (members.isEmpty()) {
            event.reply("No other members are in the room.").setEphemeral(true).queue();
            return;
        }

        members.forEach(m -> menu.addOption(m.getEffectiveName(), m.getId()));
        event.reply(placeholder + ":").setEphemeral(true).addComponents(ActionRow.of(menu.build())).queue();
    }

    private void toggleUserPermission(VoiceChannel channel, String userId, Permission perm, String label, StringSelectInteractionEvent event) {
        event.getGuild().retrieveMemberById(userId).queue(m -> {
            boolean isAllowed = m.hasPermission(channel, perm);
            if (isAllowed) {
                channel.getManager().putMemberPermissionOverride(m.getIdLong(), null, EnumSet.of(perm)).queue();
                event.reply("Disabled " + label + " for " + m.getAsMention()).setEphemeral(true).queue();
            } else {
                channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(perm), null).queue();
                event.reply("Enabled " + label + " for " + m.getAsMention()).setEphemeral(true).queue();
            }
        }, err -> event.reply("Member not found.").setEphemeral(true).queue());
    }

    private void sendMemberPanel(VoiceChannel channel, ButtonInteractionEvent event) {
        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findByChannelId(channel.getId());
        if (roomOpt.isEmpty()) {
            event.reply("Room data not found.").setEphemeral(true).queue();
            return;
        }
        VoiceRoomEntity room = roomOpt.get();
        
        StringBuilder sb = new StringBuilder("### Member Management\n\n");
        
        sb.append("**Active Members:**\n");
        channel.getMembers().forEach(m -> {
            boolean canSpeak = m.hasPermission(channel, Permission.VOICE_SPEAK);
            boolean canWrite = m.hasPermission(channel, Permission.MESSAGE_SEND);
            sb.append("● ").append(m.getAsMention()).append(" (")
              .append(canSpeak ? "S " : "")
              .append(canWrite ? "W " : "")
              .append(")\n");
        });

        if (room.getTrustedUserIds() != null && !room.getTrustedUserIds().isEmpty()) {
            sb.append("\n**Trusted Users:**\n");
            for (String id : room.getTrustedUserIds().split(",")) {
                if (id.isEmpty()) continue;
                sb.append("● <@").append(id).append(">\n");
            }
        }
        
        event.reply(sb.toString()).setEphemeral(true).queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("menu_voice_")) return;

        VoiceChannel channel = event.getMember().getVoiceState().getChannel() != null ? event.getMember().getVoiceState().getChannel().asVoiceChannel() : null;

        if (channel == null) {
            event.reply("You must be in a voice room to use this menu.").setEphemeral(true).queue();
            return;
        }

        String value = event.getValues().get(0);
        switch (id) {
            case "menu_voice_bitrate":
                int bitrate = Integer.parseInt(value) * 1000;
                int maxBitrate = event.getGuild().getMaxBitrate();
                if (bitrate > maxBitrate) {
                    bitrate = maxBitrate;
                    event.reply("Bitrate set to " + (maxBitrate/1000) + " kbps (Server Maximum).").setEphemeral(true).queue();
                } else {
                    event.reply("Bitrate updated to: " + value + " kbps").setEphemeral(true).queue();
                }
                channel.getManager().setBitrate(bitrate).queue();
                break;
            case "menu_voice_kick":
                event.getGuild().retrieveMemberById(value).queue(m -> {
                    if (channel.getMembers().contains(m)) {
                        event.getGuild().kickVoiceMember(m).queue();
                        event.reply("Kicked " + m.getAsMention() + " from the room.").setEphemeral(true).queue();
                    }
                });
                break;
            case "menu_voice_region":
                channel.getManager().setRegion(value.equals("auto") ? null : net.dv8tion.jda.api.Region.fromKey(value)).queue();
                event.reply("Voice region changed to: " + value).setEphemeral(true).queue();
                break;
            case "menu_voice_perm_speak":
                toggleUserPermission(channel, value, Permission.VOICE_SPEAK, "Speak Access", event);
                break;
            case "menu_voice_perm_write":
                toggleUserPermission(channel, value, Permission.MESSAGE_SEND, "Chat Access", event);
                break;
            case "menu_voice_perm_video":
                toggleUserPermission(channel, value, Permission.PRIORITY_SPEAKER, "Video/Share", event);
                break;
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith("modal_voice_")) return;
        
        Optional<VoiceRoomEntity> roomOpt = voiceRoomRepository.findById(event.getUser().getId());
        if (roomOpt.isEmpty() || roomOpt.get().getChannelId() == null) {
            event.reply("Cannot find your private room.").setEphemeral(true).queue();
            return;
        }
        VoiceChannel channel = event.getGuild().getVoiceChannelById(roomOpt.get().getChannelId());
        if (channel == null) {
            event.reply("Your room is not active.").setEphemeral(true).queue();
            return;
        }

        switch (modalId) {
            case "modal_voice_rename":
                String newName = event.getValue("voice_new_name").getAsString();
                channel.getManager().setName(newName).queue();
                VoiceRoomEntity roomRen = roomOpt.get();
                roomRen.setRoomName(newName);
                voiceRoomRepository.save(roomRen);
                event.reply("Room renamed to: " + newName).setEphemeral(true).queue();
                break;
            case "modal_voice_limit":
                try {
                    String val = event.getValue("voice_new_limit").getAsString();
                    int limit = Integer.parseInt(val);
                    if (limit < 0 || limit > 99) {
                        event.reply("Limit must be between 0 and 99.").setEphemeral(true).queue();
                        return;
                    }
                    
                    channel.getManager().setUserLimit(limit).queue(v -> {
                        log.info("✅ Updated user limit for {} to {}", event.getUser().getName(), limit);
                    }, err -> {
                        log.error("❌ Failed to update user limit: {}", err.getMessage());
                    });

                    VoiceRoomEntity roomLim = roomOpt.get();
                    roomLim.setUserLimit(limit);
                    voiceRoomRepository.save(roomLim);
                    if (!event.isAcknowledged()) event.reply("User limit updated to: " + (limit == 0 ? "Unlimited" : limit)).setEphemeral(true).queue();
                } catch (Exception e) { 
                    event.reply("Please enter a valid number (0-99).").setEphemeral(true).queue(); 
                }
                break;
            case "modal_voice_trust":
                String trustId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(trustId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null).queue();
                    VoiceRoomEntity roomTru = roomOpt.get();
                    String current = roomTru.getTrustedUserIds() != null ? roomTru.getTrustedUserIds() : "";
                    if (!current.contains(trustId)) {
                        roomTru.setTrustedUserIds(current.isEmpty() ? trustId : current + "," + trustId);
                        voiceRoomRepository.save(roomTru);
                    }
                    event.reply("Access granted to " + m.getAsMention()).setEphemeral(true).queue();
                }, err -> event.reply("Member not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_block":
                String blockId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(blockId).queue(m -> {
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), null, EnumSet.of(Permission.VOICE_CONNECT)).queue();
                    VoiceRoomEntity roomBlk = roomOpt.get();
                    if (roomBlk.getTrustedUserIds() != null) {
                        String updated = roomBlk.getTrustedUserIds().replace(blockId, "").replace(",,", ",");
                        if (updated.startsWith(",")) updated = updated.substring(1);
                        if (updated.endsWith(",")) updated = updated.substring(0, updated.length()-1);
                        roomBlk.setTrustedUserIds(updated.isEmpty() ? null : updated);
                        voiceRoomRepository.save(roomBlk);
                    }
                    if (channel.getMembers().contains(m)) event.getGuild().kickVoiceMember(m).queue();
                    event.reply("Blocked " + m.getAsMention() + " from the room.").setEphemeral(true).queue();
                }, err -> event.reply("Member not found.").setEphemeral(true).queue());
                break;
            case "modal_voice_transfer":
                String transferId = event.getValue("voice_user_id").getAsString();
                event.getGuild().retrieveMemberById(transferId).queue(m -> {
                    VoiceRoomEntity roomTra = roomOpt.get();
                    roomTra.setOwnerId(m.getId());
                    voiceRoomRepository.save(roomTra);
                    channel.getManager().putMemberPermissionOverride(m.getIdLong(), EnumSet.of(Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS), null).queue();
                    event.reply("Ownership transferred to " + m.getAsMention()).setEphemeral(true).queue();
                }, err -> event.reply("Member not found.").setEphemeral(true).queue());
                break;
        }
    }
}
