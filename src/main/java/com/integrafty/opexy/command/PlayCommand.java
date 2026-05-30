package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.SoundCloudAudioService;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Section: SoundCloud Playback Command
@Component
@lombok.RequiredArgsConstructor
public class PlayCommand extends ListenerAdapter implements SlashCommand {
    private static final Logger log = LoggerFactory.getLogger(PlayCommand.class);
    
    private final SoundCloudAudioService soundCloudAudioService;
    private final com.integrafty.opexy.listener.VoiceRecordingListener voiceRecordingListener;
    
    public static final java.util.Map<Long, ActiveTrackInfo> activeTracks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService embedUpdaterExecutor = java.util.concurrent.Executors.newScheduledThreadPool(4);
    private static final java.util.Map<Long, ClickTracker> clickTrackers = new java.util.concurrent.ConcurrentHashMap<>();

    public static class ClickTracker {
        public int count = 0;
        public long lastClickTime = 0;
    }

    public static class ActiveTrackInfo {
        public final String title;
        public final String uri;
        public final String requesterMention;
        public final long channelId;
        public final long messageId;
        public final java.util.concurrent.ScheduledFuture<?> updateTask;
        public final boolean fixed;
        
        public ActiveTrackInfo(String title, String uri, String requesterMention, long channelId, long messageId, java.util.concurrent.ScheduledFuture<?> updateTask, boolean fixed) {
            this.title = title;
            this.uri = uri;
            this.requesterMention = requesterMention;
            this.channelId = channelId;
            this.messageId = messageId;
            this.updateTask = updateTask;
            this.fixed = fixed;
        }
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("play", "تشغيل المقاطع الصوتية من SoundCloud في الروم الصوتي")
                .addOption(OptionType.STRING, "link", "اسم المقطع أو رابط SoundCloud", true)
                .addOption(OptionType.BOOLEAN, "fixed", "تثبيت البوت في الروم الصوتي حتى لو خرج الجميع", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!hasPlayRole(event.getMember())) {
            event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة لتشغيل هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;

        if (voiceRecordingListener.isRecordingActive(guild.getIdLong())) {
            event.reply("⚠️ عذراً، لا يمكن تشغيل الموسيقى أثناء وجود تسجيل نشط. يرجى إيقاف التسجيل أولاً.").setEphemeral(true).queue();
            return;
        }

        AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null) {
            event.reply("🔊 يرجى الانضمام إلى قناة صوتية أولاً حتى يتمكن البوت من مشاركتك الاستماع.").setEphemeral(true).queue();
            return;
        }

        String link = event.getOption("link").getAsString();
        boolean fixedOpt = false;
        if (event.getOption("fixed") != null) {
            fixedOpt = event.getOption("fixed").getAsBoolean();
        }
        final boolean fixed = fixedOpt;

        if (link.contains("youtube.com") || link.contains("youtu.be")) {
            event.reply("⚠️ عذراً، هذا الرابط غير مدعوم. يرجى استخدام روابط SoundCloud أو البحث باسم المقطع.").setEphemeral(true).queue();
            return;
        }

        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "scsearch:" + link;
        }

        event.deferReply().queue();
        final net.dv8tion.jda.api.interactions.InteractionHook hook = event.getHook();

        soundCloudAudioService.loadTrack(link).thenAccept(track -> {
            soundCloudAudioService.play(guild, channel, track);
            cancelActiveTrackUpdate(guild.getIdLong());
            activeTracks.put(guild.getIdLong(), new ActiveTrackInfo(track.getInfo().title, track.getInfo().uri, event.getUser().getAsMention(), event.getChannel().getIdLong(), 0L, null, fixed));
            
            long totalMs = track.getDuration();
            Container container = buildPlaybackEmbed(track.getInfo().title, 0, totalMs, false);
            
            hook.editOriginal(new MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue(message -> {
                        startEmbedUpdater(event.getJDA(), guild.getIdLong(), message.getChannel().getIdLong(), message.getIdLong());
                    });
        }).exceptionally(ex -> {
            Container errorContainer = EmbedUtil.containerBranded("", "⚠️ تنبيه النظام", "عذراً، واجهنا صعوبة في تحميل المقطع. يرجى التحقق من صحة الرابط أو المحاولة لاحقاً.", EmbedUtil.BANNER_MAIN);
            hook.editOriginal(new MessageEditBuilder()
                    .setComponents(errorContainer)
                    .useComponentsV2(true)
                    .build()).queue();
            return null;
        });
    }

    // Section: Button and Modal Listeners
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("play_")) return;

        if (!hasPlayRole(event.getMember())) {
            event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة للتحكم بالتشغيل.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;

        if (id.equals("play_toggle")) {
            event.deferEdit().queue();
            SoundCloudAudioService.GuildMusicManager musicManager = soundCloudAudioService.getMusicManager(guild);
            if (musicManager != null) {
                com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = musicManager.getPlayer();
                boolean nowPaused = !player.isPaused();
                player.setPaused(nowPaused);
                
                ActiveTrackInfo info = activeTracks.get(guild.getIdLong());
                if (info != null && player.getPlayingTrack() != null) {
                    long currentMs = player.getPlayingTrack().getPosition();
                    long totalMs = player.getPlayingTrack().getDuration();
                    Container container = buildPlaybackEmbed(info.title, currentMs, totalMs, nowPaused);
                    event.getHook().editOriginal(new MessageEditBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build()).queue();
                } else {
                    event.getHook().sendMessage("⚠️ لا يوجد مقطع قيد التشغيل حالياً.").setEphemeral(true).queue();
                }
            }

        } else if (id.equals("play_rewind")) {
            event.deferEdit().queue();
            SoundCloudAudioService.GuildMusicManager musicManager = soundCloudAudioService.getMusicManager(guild);
            if (musicManager != null) {
                com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = musicManager.getPlayer();
                com.sedmelluq.discord.lavaplayer.track.AudioTrack track = player.getPlayingTrack();
                if (track != null) {
                    if (track.isSeekable()) {
                        long currentPos = track.getPosition();
                        long newPos = Math.max(0, currentPos - 10000);
                        track.setPosition(newPos);
                        
                        ActiveTrackInfo info = activeTracks.get(guild.getIdLong());
                        long totalMs = track.getDuration();
                        Container container = buildPlaybackEmbed(info != null ? info.title : track.getInfo().title, newPos, totalMs, player.isPaused());
                        event.getHook().editOriginal(new MessageEditBuilder()
                                .setComponents(container)
                                .useComponentsV2(true)
                                .build()).queue();
                    } else {
                        event.getHook().sendMessage("⚠️ هذا المقطع لا يدعم التقديم والتأخير.").setEphemeral(true).queue();
                    }
                } else {
                    event.getHook().sendMessage("⚠️ لا يوجد مقطع قيد التشغيل حالياً.").setEphemeral(true).queue();
                }
            }

        } else if (id.equals("play_forward")) {
            long guildId = guild.getIdLong();
            long now = System.currentTimeMillis();
            ClickTracker tracker = clickTrackers.computeIfAbsent(guildId, g -> new ClickTracker());
            
            if (now - tracker.lastClickTime < 2000) {
                tracker.count++;
            } else {
                tracker.count = 1;
            }
            tracker.lastClickTime = now;

            if (tracker.count >= 3) {
                tracker.count = 0;
                TextInput timeInput = TextInput.create("play_seek_time", TextInputStyle.SHORT)
                        .setPlaceholder("مثال: 02:30 أو 15:00")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("modal_play_seek", "تقديم إلى وقت محدد")
                        .addComponents(Label.of("أدخل الوقت المستهدف (دقيقة:ثانية أو ثواني)", timeInput))
                        .build();
                event.replyModal(modal).queue();
                return;
            }

            event.deferEdit().queue();
            SoundCloudAudioService.GuildMusicManager musicManager = soundCloudAudioService.getMusicManager(guild);
            if (musicManager != null) {
                com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = musicManager.getPlayer();
                com.sedmelluq.discord.lavaplayer.track.AudioTrack track = player.getPlayingTrack();
                if (track != null) {
                    if (track.isSeekable()) {
                        long currentPos = track.getPosition();
                        long newPos = Math.min(track.getDuration(), currentPos + 10000);
                        track.setPosition(newPos);
                        
                        ActiveTrackInfo info = activeTracks.get(guildId);
                        long totalMs = track.getDuration();
                        Container container = buildPlaybackEmbed(info != null ? info.title : track.getInfo().title, newPos, totalMs, player.isPaused());
                        event.getHook().editOriginal(new MessageEditBuilder()
                                .setComponents(container)
                                .useComponentsV2(true)
                                .build()).queue();
                    } else {
                        event.getHook().sendMessage("⚠️ هذا المقطع لا يدعم التقديم والتأخير.").setEphemeral(true).queue();
                    }
                } else {
                    event.getHook().sendMessage("⚠️ لا يوجد مقطع قيد التشغيل حالياً.").setEphemeral(true).queue();
                }
            }

        } else if (id.equals("play_leave")) {
            event.deferEdit().queue();
            cancelActiveTrackUpdate(guild.getIdLong());
            activeTracks.remove(guild.getIdLong());
            soundCloudAudioService.stop(guild);
            Container stopContainer = EmbedUtil.containerBranded("", "إنهاء الجلسة", "🚪 تم إنهاء الجلسة ومغادرة الروم الصوتي بنجاح. شكراً لاستماعكم!", EmbedUtil.BANNER_MAIN);
            event.getHook().editOriginal(new MessageEditBuilder()
                    .setComponents(stopContainer)
                    .useComponentsV2(true)
                    .build()).queue();

        } else if (id.equals("play_change")) {
            TextInput linkInput = TextInput.create("play_link", TextInputStyle.SHORT)
                    .setPlaceholder("اسم المقطع للبحث أو رابط SoundCloud")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_play_change", "تغيير مقطع التشغيل")
                    .addComponents(Label.of("رابط SoundCloud الجديد أو اسم المقطع", linkInput))
                    .build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("modal_play_change")) {
            if (!hasPlayRole(event.getMember())) {
                event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            Guild guild = event.getGuild();
            if (guild == null) return;

            if (voiceRecordingListener.isRecordingActive(guild.getIdLong())) {
                event.reply("⚠️ عذراً، لا يمكن تغيير مقطع التشغيل أو تشغيل الموسيقى أثناء وجود تسجيل نشط. يرجى إيقاف التسجيل أولاً.").setEphemeral(true).queue();
                return;
            }
            
            String newLink = event.getValue("play_link").getAsString();

            if (newLink.contains("youtube.com") || newLink.contains("youtu.be")) {
                event.reply("⚠️ عذراً، هذا الرابط غير مدعوم. يرجى استخدام روابط SoundCloud أو البحث باسم المقطع.").setEphemeral(true).queue();
                return;
            }

            if (!newLink.startsWith("http://") && !newLink.startsWith("https://")) {
                newLink = "scsearch:" + newLink;
            }

            AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
            if (channel == null) {
                Member member = event.getMember();
                if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                    channel = member.getVoiceState().getChannel();
                }
            }
            
            if (channel == null) {
                event.reply("🔊 يرجى الانضمام إلى قناة صوتية أولاً حتى يتمكن البوت من الدخول والتشغيل.").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();
            
            final AudioChannel finalChannel = channel;
            final net.dv8tion.jda.api.interactions.InteractionHook hook = event.getHook();
            ActiveTrackInfo oldInfo = activeTracks.get(guild.getIdLong());
            final boolean currentFixed = oldInfo != null && oldInfo.fixed;
            
            soundCloudAudioService.loadTrack(newLink).thenAccept(track -> {
                soundCloudAudioService.play(guild, finalChannel, track);
                cancelActiveTrackUpdate(guild.getIdLong());
                activeTracks.put(guild.getIdLong(), new ActiveTrackInfo(track.getInfo().title, track.getInfo().uri, event.getUser().getAsMention(), event.getChannel().getIdLong(), 0L, null, currentFixed));
                
                long totalMs = track.getDuration();
                Container container = buildPlaybackEmbed(track.getInfo().title, 0, totalMs, false);
                
                hook.editOriginal(new MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue(message -> {
                            startEmbedUpdater(event.getJDA(), guild.getIdLong(), message.getChannel().getIdLong(), message.getIdLong());
                        });
            }).exceptionally(ex -> {
                hook.sendMessage("⚠️ | عذراً، واجهنا صعوبة في تحميل المقطع. يرجى التحقق من صحة الرابط أو المحاولة لاحقاً.").setEphemeral(true).queue();
                return null;
            });
        } else if (event.getModalId().equals("modal_play_seek")) {
            if (!hasPlayRole(event.getMember())) {
                event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            Guild guild = event.getGuild();
            if (guild == null) return;

            String timeVal = event.getValue("play_seek_time").getAsString();
            try {
                long targetMs = parseSeekTime(timeVal);
                SoundCloudAudioService.GuildMusicManager musicManager = soundCloudAudioService.getMusicManager(guild);
                if (musicManager != null) {
                    com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = musicManager.getPlayer();
                    com.sedmelluq.discord.lavaplayer.track.AudioTrack track = player.getPlayingTrack();
                    if (track != null) {
                        if (track.isSeekable()) {
                            long finalPos = Math.max(0, Math.min(track.getDuration(), targetMs));
                            track.setPosition(finalPos);
                            
                            ActiveTrackInfo info = activeTracks.get(guild.getIdLong());
                            Container container = buildPlaybackEmbed(info != null ? info.title : track.getInfo().title, finalPos, track.getDuration(), player.isPaused());
                            
                            event.deferEdit().queue();
                            event.getHook().editOriginal(new MessageEditBuilder()
                                    .setComponents(container)
                                    .useComponentsV2(true)
                                    .build()).queue();
                        } else {
                            event.reply("⚠️ هذا المقطع لا يدعم التقديم والتأخير.").setEphemeral(true).queue();
                        }
                    } else {
                        event.reply("⚠️ لا يوجد مقطع قيد التشغيل حالياً.").setEphemeral(true).queue();
                    }
                }
            } catch (Exception e) {
                event.reply("⚠️ صيغة الوقت غير صالحة. يرجى الاستخدام بصيغة (دقيقة:ثانية) مثل: 02:30").setEphemeral(true).queue();
            }
        }
    }

    private static long parseSeekTime(String input) {
        String clean = input.trim();
        if (clean.matches("\\d+")) {
            return Long.parseLong(clean) * 1000;
        }
        
        String[] parts = clean.split(":");
        if (parts.length == 2) {
            long minutes = Long.parseLong(parts[0]);
            long seconds = Long.parseLong(parts[1]);
            return (minutes * 60 + seconds) * 1000;
        } else if (parts.length == 3) {
            long hours = Long.parseLong(parts[0]);
            long minutes = Long.parseLong(parts[1]);
            long seconds = Long.parseLong(parts[2]);
            return (hours * 3600 + minutes * 60 + seconds) * 1000;
        }
        throw new IllegalArgumentException("Invalid format");
    }

    // Section: Permissions Helper
    private boolean hasPlayRole(Member member) {
        if (member == null) return false;
        return member.getRoles().stream().anyMatch(role -> role.getId().equals("1487152572207861870"));
    }

    // Section: UI Helpers
    private Container buildPlaybackEmbed(String title, long currentMs, long totalMs, boolean isPaused) {
        String body = "\n" +
                      "“" + title + "”\n\n" +
                      formatTime(currentMs) + "  " + getProgressBar(currentMs, totalMs) + "  " + formatTime(totalMs) + "\n";
        
        String toggleLabel = isPaused ? "▶" : "❚❚";
        
        ActionRow row = ActionRow.of(
            Button.secondary("play_rewind", "◁"),
            Button.secondary("play_toggle", toggleLabel),
            Button.secondary("play_forward", "▷"),
            Button.primary("play_change", "🔄"),
            Button.danger("play_leave", "🚪")
        );
        
        return EmbedUtil.containerBranded("", "𓆩 𝗡𝗼𝘄 𝗣𝗹𝗮𝘆𝗶𝗻𝗴 𓆪", body, EmbedUtil.BANNER_MAIN, row);
    }

    private static String formatTime(long ms) {
        if (ms == Long.MAX_VALUE || ms < 0) {
            return "00:00";
        }
        long totalSecs = ms / 1000;
        long hours = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private static String getProgressBar(long currentMs, long totalMs) {
        int totalBars = 15;
        if (totalMs <= 0) {
            return "━━━━━━━●──────";
        }
        float percentage = (float) currentMs / totalMs;
        int filledBars = Math.round(percentage * totalBars);
        if (filledBars < 0) filledBars = 0;
        if (filledBars > totalBars) filledBars = totalBars;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) {
                sb.append("●");
            } else if (i < filledBars) {
                sb.append("━");
            } else {
                sb.append("─");
            }
        }
        if (!sb.toString().contains("●")) {
            if (filledBars >= totalBars) {
                sb.setCharAt(totalBars - 1, '●');
            } else {
                sb.setCharAt(0, '●');
            }
        }
        return sb.toString();
    }

    private void startEmbedUpdater(net.dv8tion.jda.api.JDA jda, long guildId, long channelId, long messageId) {
        cancelActiveTrackUpdate(guildId);
        
        ActiveTrackInfo currentInfo = activeTracks.get(guildId);
        if (currentInfo == null) return;
        
        java.util.concurrent.ScheduledFuture<?> task = embedUpdaterExecutor.scheduleAtFixedRate(() -> {
            try {
                ActiveTrackInfo info = activeTracks.get(guildId);
                if (info == null) {
                    cancelActiveTrackUpdate(guildId);
                    return;
                }
                
                SoundCloudAudioService.GuildMusicManager musicManager = soundCloudAudioService.getMusicManager(guildId);
                if (musicManager == null) {
                    cancelActiveTrackUpdate(guildId);
                    activeTracks.remove(guildId);
                    return;
                }
                
                com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = musicManager.getPlayer();
                com.sedmelluq.discord.lavaplayer.track.AudioTrack track = player.getPlayingTrack();
                if (track == null) {
                    cancelActiveTrackUpdate(guildId);
                    activeTracks.remove(guildId);
                    return;
                }
                
                if (player.isPaused()) {
                    return;
                }
                
                long currentMs = track.getPosition();
                long totalMs = track.getDuration();
                
                Container container = buildPlaybackEmbed(info.title, currentMs, totalMs, false);
                
                net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel = jda.getChannelById(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel.class, channelId);
                if (channel != null) {
                    channel.editMessageById(messageId, new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build()).queue(null, ex -> {
                                if (ex instanceof net.dv8tion.jda.api.exceptions.ErrorResponseException ere) {
                                    if (ere.getErrorResponse() == net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE ||
                                        ere.getErrorResponse() == net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_CHANNEL) {
                                        cancelActiveTrackUpdate(guildId);
                                        activeTracks.remove(guildId);
                                    } else {
                                        log.warn("[PLAY] Failed to edit playback message {} in channel {}: {}", messageId, channelId, ere.getMessage());
                                    }
                                } else {
                                    log.warn("[PLAY] Exception in editing playback message {} in channel {}: {}", messageId, channelId, ex.getMessage());
                                }
                            });
                } else {
                    log.warn("[PLAY] Could not find channel {} to edit playback message {}", channelId, messageId);
                }
            } catch (Exception e) {
                log.error("[PLAY] Exception in scheduled embed updater for guild {}", guildId, e);
            }
        }, 3, 3, java.util.concurrent.TimeUnit.SECONDS);
        
        activeTracks.put(guildId, new ActiveTrackInfo(currentInfo.title, currentInfo.uri, currentInfo.requesterMention, channelId, messageId, task, currentInfo.fixed));
    }

    public static void cancelActiveTrackUpdate(long guildId) {
        ActiveTrackInfo oldInfo = activeTracks.get(guildId);
        if (oldInfo != null && oldInfo.updateTask != null) {
            oldInfo.updateTask.cancel(true);
        }
    }
}

