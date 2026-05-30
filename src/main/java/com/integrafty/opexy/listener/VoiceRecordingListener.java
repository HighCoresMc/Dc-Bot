package com.integrafty.opexy.listener;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.audio.AudioRecorder;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import com.integrafty.opexy.service.SoundCloudAudioService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@lombok.RequiredArgsConstructor
public class VoiceRecordingListener extends ListenerAdapter implements SlashCommand {
    private static final Logger log = LoggerFactory.getLogger(VoiceRecordingListener.class);
    private static final String LOG_CHANNEL_ID = "1501263192943235092";
    
    private final net.dv8tion.jda.api.JDA jda;
    private final SoundCloudAudioService soundCloudAudioService;

    @Override
    public String getName() {
        return "rec";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("rec", "نظام تسجيل الأصوات (خاص بغرف محددة)");
    }
    
    private final List<Long> ALLOWED_VOICE_CHANNELS = List.of(
        1508535566893977793L,
        1487147689098481774L
    );

    public boolean isRecordingActive(long guildId) {
        AudioRecorder recorder = recorders.get(guildId);
        return recorder != null && recorder.isRecording();
    }

    private final Map<Long, AudioRecorder> recorders = new ConcurrentHashMap<>();
    private final Map<Long, String> activeTextChannels = new ConcurrentHashMap<>();
    private final Map<Long, String> sessionNames = new ConcurrentHashMap<>();
    private final Map<Long, Integer> partCounters = new ConcurrentHashMap<>();
    private final Map<Long, java.util.concurrent.ScheduledFuture<?>> splitTasks = new ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);

    @Override
    public void execute(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        if (event.getName().equals("rec")) {
            Guild guild = event.getGuild();
            if (guild == null) return;
            
            AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
            if (channel == null) {
                // Try to join the user's channel
                net.dv8tion.jda.api.entities.Member member = event.getMember();
                if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                    channel = member.getVoiceState().getChannel();
                    
                    if (!ALLOWED_VOICE_CHANNELS.contains(channel.getIdLong())) {
                        event.reply("❌ Recording is only allowed in designated voice channels. (ID: `" + channel.getId() + "`)").setEphemeral(true).queue();
                        return;
                    }
                    
                    guild.getAudioManager().openAudioConnection(channel);
                } else {
                    event.reply("❌ You must be in an allowed voice channel for the bot to join and record.").setEphemeral(true).queue();
                    return;
                }
            } else if (!ALLOWED_VOICE_CHANNELS.contains(channel.getIdLong())) {
                event.reply("❌ Bot is currently in a restricted channel and cannot record here. (ID: `" + channel.getId() + "`)").setEphemeral(true).queue();
                return;
            }
            
            activeTextChannels.put(guild.getIdLong(), event.getChannel().getId());
            
            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                "PROTOCOL", 
                "Recording System",
                "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                EmbedUtil.BANNER_MAIN,
                net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_start", "Start"),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_stop", "Stop"),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_new", "New Record")
                )
            );
            
            event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build())
                .useComponentsV2(true)
                .queue();
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        AudioChannel joinedChannel = event.getChannelJoined();
        AudioChannel leftChannel = event.getChannelLeft();

        // Stop and send recording if the last human leaves the channel the bot is in
        if (leftChannel != null) {
            AudioChannel connectedChannel = audioManager.getConnectedChannel();
            if (connectedChannel != null && leftChannel.getIdLong() == connectedChannel.getIdLong()) {
                long humanCount = connectedChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                
                if (humanCount <= 0) {
                    com.integrafty.opexy.command.PlayCommand.ActiveTrackInfo trackInfo = com.integrafty.opexy.command.PlayCommand.activeTracks.get(guild.getIdLong());
                    if (trackInfo != null && trackInfo.fixed) {
                        log.info("[VOICE] Last human left channel, but fixed mode is active. Staying in room.");
                        return;
                    }
                    log.info("[VOICE] Last human left channel {}. Stopping and sending recording.", leftChannel.getName());
                    java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guild.getIdLong());
                    if (task != null) task.cancel(true);
                    stopAndSendRecording(guild, connectedChannel);
                }
            }
        }

        // Auto-join/Follow logic
        if (joinedChannel != null && !event.getMember().getUser().isBot()) {
            if (!ALLOWED_VOICE_CHANNELS.contains(joinedChannel.getIdLong())) return;

            if (!audioManager.isConnected()) {
                // Connect if not connected
                audioManager.openAudioConnection(joinedChannel);
                
                // Set active text channel to where the bot "should" report
                if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                    activeTextChannels.put(guild.getIdLong(), msgChannel.getId());

                    // Send control panel
                    net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                        "PROTOCOL", 
                        "Recording System",
                        "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                        EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_start", "Start"),
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_stop", "Stop"),
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_new", "New Record")
                        )
                    );
                    
                    msgChannel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                            .setComponents(container)
                                .useComponentsV2(true)
                                .build())
                            .useComponentsV2(true)
                            .queue();
                }
            } else {
                // Follow if bot is alone in its current channel
                AudioChannel currentChannel = audioManager.getConnectedChannel();
                if (currentChannel != null && currentChannel.getIdLong() != joinedChannel.getIdLong()) {
                    long humanCount = currentChannel.getMembers().stream()
                            .filter(m -> !m.getUser().isBot())
                            .count();
                    if (humanCount == 0) {
                        audioManager.openAudioConnection(joinedChannel);
                        
                        // Send control panel in the new channel too
                        if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                            activeTextChannels.put(guild.getIdLong(), msgChannel.getId());
                            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                                "PROTOCOL", 
                                "Recording System",
                                "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                                EmbedUtil.BANNER_MAIN,
                                net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_start", "Start"),
                                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_stop", "Stop"),
                                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_new", "New Record")
                                )
                            );
                            
                            msgChannel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                                    .setComponents(container)
                                        .useComponentsV2(true)
                                        .build())
                                    .useComponentsV2(true)
                                    .queue();
                        }
                    }
                }
            }
        }

        // Detect if the bot itself is disconnected unexpectedly
        if (event.getMember().equals(guild.getSelfMember())) {
            if (leftChannel != null && joinedChannel == null) {
                if (recorders.containsKey(guild.getIdLong())) {
                    java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guild.getIdLong());
                    if (task != null) task.cancel(true);
                    stopAndSendRecording(guild, leftChannel);
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(
            @NotNull net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("rec_start")) {
            net.dv8tion.jda.api.components.textinput.TextInput nameInput = net.dv8tion.jda.api.components.textinput.TextInput.create("rec_name", net.dv8tion.jda.api.components.textinput.TextInputStyle.SHORT)
                    .setPlaceholder("e.g., Development Meeting")
                    .setRequired(true)
                    .build();

            net.dv8tion.jda.api.modals.Modal modal = net.dv8tion.jda.api.modals.Modal.create("modal_rec_start", "Start Recording")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Meeting Name", nameInput))
                    .build();
            event.replyModal(modal).queue();
            return;
        }

        if (id.equals("rec_stop") || id.equals("rec_new")) {
            String actionName = id.equals("rec_stop") ? "STOP" : "SAVE & NEW";

            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                "VERIFY",
                "Action Confirmation",
                "Are you sure you want to **" + actionName + "** the recording?",
                EmbedUtil.BANNER_MAIN,
                net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                        net.dv8tion.jda.api.components.buttons.Button.success(id + "_confirm", "Confirm"),
                        net.dv8tion.jda.api.components.buttons.Button.danger("rec_cancel", "Cancel")
                )
            );

            event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build())
                    .setEphemeral(false)
                    .useComponentsV2(true)
                    .queue();
            return;
        }

        if (id.equals("rec_cancel")) {
            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                "VERIFY",
                "Action Cancelled",
                "The recording action was cancelled.",
                EmbedUtil.BANNER_MAIN
            );
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();
            return;
        }

        if (id.equals("rec_start_confirm")) {
            // This is now replaced by Modal interaction, but keeping empty for safety
            event.reply("Please use the Start button to name your session.").setEphemeral(true).queue();
            return;
        }

        if (id.equals("rec_stop_confirm")) {
            Guild guild = event.getGuild();
            long guildId = guild.getIdLong();
            
            java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guildId);
            if (task != null) task.cancel(true);

            stopAndSendRecording(guild, guild.getAudioManager().getConnectedChannel());
            
            String name = sessionNames.remove(guildId);
            partCounters.remove(guildId);

            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                "PROTOCOL",
                "Recording Finished",
                "⏹️ Session **" + (name != null ? name : "Recording") + "** has been **STOPPED & SAVED**.",
                EmbedUtil.BANNER_MAIN
            );
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();
            return;
        }

        if (id.equals("rec_new_confirm")) {
            Guild guild = event.getGuild();
            long guildId = guild.getIdLong();
            
            java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guildId);
            if (task != null) task.cancel(true);

            stopAndSendRecording(guild, guild.getAudioManager().getConnectedChannel());
            sessionNames.remove(guildId);
            partCounters.remove(guildId);

            net.dv8tion.jda.api.components.textinput.TextInput nameInput = net.dv8tion.jda.api.components.textinput.TextInput.create("rec_name", net.dv8tion.jda.api.components.textinput.TextInputStyle.SHORT)
                    .setPlaceholder("e.g., Development Meeting")
                    .setRequired(true)
                    .build();

            net.dv8tion.jda.api.modals.Modal modal = net.dv8tion.jda.api.modals.Modal.create("modal_rec_start", "Start New Recording")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Meeting Name", nameInput))
                    .build();
            event.replyModal(modal).queue();
            return;
        }
    }

    @Override
    public void onModalInteraction(@NotNull net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
        if (event.getModalId().equals("modal_rec_start")) {
            String name = event.getValue("rec_name").getAsString();
            Guild guild = event.getGuild();
            if (guild == null) return;

            long guildId = guild.getIdLong();
            sessionNames.put(guildId, name);
            partCounters.put(guildId, 1);

            soundCloudAudioService.stopWithoutDisconnect(guild);
            com.integrafty.opexy.command.PlayCommand.cancelActiveTrackUpdate(guildId);
            com.integrafty.opexy.command.PlayCommand.activeTracks.remove(guildId);

            AudioRecorder recorder = recorders.get(guildId);
            
            // If no recorder exists, try to create one
            if (recorder == null) {
                net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();
                if (audioManager.isConnected() && audioManager.getConnectedChannel() != null) {
                    connectAndStartRecording(guild, audioManager.getConnectedChannel());
                    recorder = recorders.get(guildId);
                }
            }

            if (recorder != null) {
                recorder.setRecording(true);
                log.info("[RECORDING] Started session '{}' for guild: {}", name, guild.getName());
                
                // Schedule splitting every 30 minutes
                java.util.concurrent.ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                    log.info("[VOICE] 30 minutes reached. Splitting recording for session: {}", name);
                    splitAndRestart(guild);
                }, 30, 30, java.util.concurrent.TimeUnit.MINUTES);
                
                splitTasks.put(guildId, task);

                net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded(
                    "PROTOCOL",
                    "Recording Started",
                    "✅ Recording session **" + name + "** has been started.\nIt will automatically split every 30 minutes for stability.",
                    EmbedUtil.BANNER_MAIN
                );
                event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build())
                    .useComponentsV2(true)
                    .queue();
            } else {
                event.reply("❌ Error: Bot failed to initialize recording. Please ensure it's in an allowed voice channel and try again.").setEphemeral(true).queue();
            }
        }
    }

    private void splitAndRestart(Guild guild) {
        long guildId = guild.getIdLong();
        AudioRecorder recorder = recorders.get(guildId);
        if (recorder == null) return;

        try {
            AudioRecorder.SplitResult splitResult = recorder.split();
            final int part = partCounters.getOrDefault(guildId, 1);
            partCounters.put(guildId, part + 1);
            
            final String sessionName = sessionNames.getOrDefault(guildId, "Meeting");
            
            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File recordingsDir = new File("recordings");
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs();
                }
                File wavFile = new File(recordingsDir, "opexy_rec_" + guild.getId() + "_" + timestamp + "_part" + part + ".wav");
                try {
                    log.info("[UPLOAD] Saving split WAV file: {}", wavFile.getName());
                    AudioRecorder.saveAsWav(splitResult.tempFile, splitResult.totalBytes, wavFile);

                    if (wavFile.exists() && wavFile.length() > 100) {
                        log.info("[UPLOAD] Split file saved. Sending Part {} of session '{}'", part, sessionName);
                        final File sendFile = convertWavToM4a(wavFile);
                        final boolean isM4a = sendFile.getName().endsWith(".m4a");

                        final TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        
                        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                        eb.setTitle("🎙️ " + sessionName + " : Part " + part);
                        eb.setColor(EmbedUtil.INFO);
                        eb.setImage(EmbedUtil.BANNER_MAIN);
                        
                        AudioChannel lastChannel = guild.getAudioManager().getConnectedChannel();
                        eb.addField("Channel", "`" + (lastChannel != null ? lastChannel.getName() : "Unknown") + "`", true);
                        eb.addField("Time", "`" + timeStr + "`", true);
                        eb.addField("Quality", isM4a ? "`64kbps AAC Stereo`" : "`48kHz / 16-bit Stereo`", false);
                        eb.setFooter("▪ UNIFIED TERMINAL v1.2.0 ▪ HIGHCORE AGENCY ▪", null);
                        eb.setTimestamp(java.time.Instant.now());

                        String activeChanId = activeTextChannels.get(guild.getIdLong());
                        net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel tempActiveChan = null;
                        if (activeChanId != null) {
                            net.dv8tion.jda.api.entities.channel.middleman.GuildChannel gc = guild.getGuildChannelById(activeChanId);
                            if (gc instanceof net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel) {
                                tempActiveChan = (net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel) gc;
                            }
                        }
                        final net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel activeChan = tempActiveChan;

                        if (activeChan != null) {
                            activeChan.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(sendFile))
                                    .queue(
                                        success -> log.info("[RECORDING] Successfully sent split recording to active channel: {}", activeChan.getName()),
                                        failure -> log.error("[RECORDING] Failed to send split recording to active channel: {}", failure.getMessage(), failure)
                                    );
                        }

                        if (logChannel != null) {
                            logChannel.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(sendFile))
                                    .queue(
                                        success -> log.info("[RECORDING] Successfully sent split recording to log channel: {}", logChannel.getName()),
                                        failure -> log.error("[RECORDING] Failed to send split recording to log channel: {}", failure.getMessage(), failure)
                                    );
                        }
                    }
                } catch (IOException e) {
                    log.error("[RECORDING] Failed to save split WAV file", e);
                } finally {
                    AudioRecorder.cleanup(splitResult.tempFile);
                }
            }).start();
        } catch (IOException e) {
            log.error("[RECORDING] Failed to split recording", e);
        }
    }

    private void connectAndStartRecording(Guild guild, AudioChannel channel) {
        try {
            AudioRecorder recorder = new AudioRecorder();
            AudioManager audioManager = guild.getAudioManager();
            audioManager.setReceivingHandler(recorder);
            audioManager.setSendingHandler(new SilenceSendHandler());
            audioManager.openAudioConnection(channel);
            recorders.put(guild.getIdLong(), recorder);
            log.info("[VOICE] Bot connected and ready to record in guild: {}", guild.getName());
        } catch (IOException e) {
            log.error("[VOICE] Failed to initialize recorder: {}", e.getMessage());
        }
    }

    private void stopAndSendRecording(Guild guild, AudioChannel fallbackChannel) {
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        AudioRecorder recorder = recorders.remove(guildId);

        // Always clear JDA audio handlers when stopping a recording to release the channel and stop SilenceSendHandler
        audioManager.setReceivingHandler(null);
        audioManager.setSendingHandler(null);

        AudioChannel lastChannel = audioManager.getConnectedChannel();
        if (lastChannel == null) lastChannel = fallbackChannel;

        if (lastChannel != null) {
            long humanCount = lastChannel.getMembers().stream()
                    .filter(m -> !m.getUser().isBot())
                    .count();
            if (humanCount <= 0) {
                log.info("[VOICE] Channel is empty or only bots left. Closing connection for guild: {}", guild.getName());
                audioManager.closeAudioConnection();
                sessionNames.remove(guildId);
                partCounters.remove(guildId);
            }
        }

        if (recorder != null) {
            final String sessionName = sessionNames.getOrDefault(guildId, "Meeting");
            final int part = partCounters.getOrDefault(guildId, 1);
            
            final File tempFile = recorder.getTempFile();
            final long totalBytes = recorder.getTotalBytes();
            
            recorder.stop();

            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File recordingsDir = new File("recordings");
                if (!recordingsDir.exists()) {
                    recordingsDir.mkdirs();
                }
                File wavFile = new File(recordingsDir, "opexy_rec_" + guild.getId() + "_" + timestamp + "_part" + part + ".wav");
                try {
                    log.info("[UPLOAD] Saving WAV file: {}", wavFile.getName());
                    AudioRecorder.saveAsWav(tempFile, totalBytes, wavFile);

                    if (wavFile.exists() && wavFile.length() > 100) {
                        log.info("[UPLOAD] File saved. Sending Part {} of session '{}'", part, sessionName);
                        final File sendFile = convertWavToM4a(wavFile);
                        final boolean isM4a = sendFile.getName().endsWith(".m4a");

                        final TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        
                        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                        eb.setTitle("🎙️ " + sessionName + " : Part " + part);
                        eb.setColor(EmbedUtil.INFO);
                        eb.setImage(EmbedUtil.BANNER_MAIN);
                        eb.addField("Channel", "`" + (fallbackChannel != null ? fallbackChannel.getName() : "Unknown") + "`", true);
                        eb.addField("Time", "`" + timeStr + "`", true);
                        eb.addField("Quality", isM4a ? "`64kbps AAC Stereo`" : "`48kHz / 16-bit Stereo`", false);
                        eb.setFooter("▪ UNIFIED TERMINAL v1.2.0 ▪ HIGHCORE AGENCY ▪", null);
                        eb.setTimestamp(java.time.Instant.now());

                        String activeChanId = activeTextChannels.get(guild.getIdLong());
                        net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel tempActiveChan = null;
                        if (activeChanId != null) {
                            net.dv8tion.jda.api.entities.channel.middleman.GuildChannel gc = guild.getGuildChannelById(activeChanId);
                            if (gc instanceof net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel) {
                                tempActiveChan = (net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel) gc;
                            }
                        }
                        final net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel activeChan = tempActiveChan;

                        if (activeChan != null) {
                            activeChan.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(sendFile))
                                    .queue(
                                        success -> log.info("[RECORDING] Successfully sent recording to active channel: {}", activeChan.getName()),
                                        failure -> log.error("[RECORDING] Failed to send recording to active channel: {}", failure.getMessage(), failure)
                                    );
                        } else {
                            log.warn("[RECORDING] Active channel not found for ID {}", activeChanId);
                        }

                        if (logChannel != null) {
                            logChannel.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(sendFile))
                                    .queue(
                                        success -> log.info("[RECORDING] Successfully sent recording to log channel: {}", logChannel.getName()),
                                        failure -> log.error("[RECORDING] Failed to send recording to log channel: {}", failure.getMessage(), failure)
                                    );
                        } else {
                            log.warn("[RECORDING] Log channel with ID {} not found", LOG_CHANNEL_ID);
                        }
                        log.info("[RECORDING] Saved recording persistently to: {}", sendFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    log.error("[RECORDING] Failed to save WAV file", e);
                } finally {
                    AudioRecorder.cleanup(tempFile);
                }
            }).start();
        }
    }

    private File convertWavToM4a(File wavFile) {
        String baseName = wavFile.getAbsolutePath();
        String m4aPath = baseName.substring(0, baseName.lastIndexOf('.')) + ".m4a";
        File m4aFile = new File(m4aPath);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", wavFile.getAbsolutePath(),
                "-c:a", "aac",
                "-b:a", "64k",
                m4aFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0 && m4aFile.exists() && m4aFile.length() > 0) {
                log.info("[CONVERSION] Successfully converted WAV to M4A: {} (Size: {} bytes)", m4aFile.getName(), m4aFile.length());
                wavFile.delete();
                return m4aFile;
            } else {
                log.error("[CONVERSION] ffmpeg failed with exit code: {}", exitCode);
            }
        } catch (Exception e) {
            log.error("[CONVERSION] Failed to convert WAV to M4A using ffmpeg", e);
        }
        return wavFile;
    }

    private static class SilenceSendHandler implements net.dv8tion.jda.api.audio.AudioSendHandler {
        private static final byte[] SILENCE_FRAME = new byte[] {(byte) 0xF8, (byte) 0xFF, (byte) 0xFE};

        @Override
        public boolean canProvide() {
            return true;
        }

        @Override
        public java.nio.ByteBuffer provide20MsAudio() {
            return java.nio.ByteBuffer.wrap(SILENCE_FRAME);
        }

        @Override
        public boolean isOpus() {
            return true;
        }
    }
}
