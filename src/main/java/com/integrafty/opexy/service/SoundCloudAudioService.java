package com.integrafty.opexy.service;

import com.integrafty.opexy.audio.AudioPlayerSendHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

// Section: SoundCloud Audio Service
@Service
public class SoundCloudAudioService {
    private static final Logger log = LoggerFactory.getLogger(SoundCloudAudioService.class);
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public SoundCloudAudioService() {
        this.playerManager = new DefaultAudioPlayerManager();
        
        log.info("[SOUNDCLOUD] Initializing SoundCloud Audio Service...");
        
        playerManager.setFrameBufferDuration(15000);
        playerManager.getConfiguration().setFrameBufferFactory(com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer::new);
        playerManager.registerSourceManager(com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager.createDefault());
        this.musicManagers = new ConcurrentHashMap<>();
    }

    public synchronized GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), guildId -> new GuildMusicManager(playerManager));
    }

    public synchronized GuildMusicManager getMusicManager(long guildId) {
        return musicManagers.get(guildId);
    }

    public CompletableFuture<AudioTrack> loadTrack(String identifier) {
        CompletableFuture<AudioTrack> future = new CompletableFuture<>();
        playerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    future.complete(playlist.getTracks().get(0));
                } else {
                    future.completeExceptionally(new RuntimeException("Playlist is empty"));
                }
            }

            @Override
            public void noMatches() {
                future.completeExceptionally(new RuntimeException("No matches found"));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public void play(Guild guild, AudioChannel channel, AudioTrack track) {
        GuildMusicManager musicManager = getMusicManager(guild);
        AudioManager audioManager = guild.getAudioManager();
        
        audioManager.setSendingHandler(musicManager.getSendHandler());
        audioManager.openAudioConnection(channel);
        
        musicManager.getPlayer().playTrack(track);
    }

    public void pause(Guild guild) {
        GuildMusicManager musicManager = getMusicManager(guild);
        musicManager.getPlayer().setPaused(true);
    }

    public void resume(Guild guild) {
        GuildMusicManager musicManager = getMusicManager(guild);
        musicManager.getPlayer().setPaused(false);
    }

    public void stop(Guild guild) {
        GuildMusicManager musicManager = musicManagers.remove(guild.getIdLong());
        if (musicManager != null) {
            musicManager.getScheduler().stop();
            musicManager.getPlayer().stopTrack();
            musicManager.getPlayer().destroy();
        }
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
        }
    }

    public void stopWithoutDisconnect(Guild guild) {
        GuildMusicManager musicManager = musicManagers.remove(guild.getIdLong());
        if (musicManager != null) {
            musicManager.getScheduler().stop();
            musicManager.getPlayer().stopTrack();
            musicManager.getPlayer().destroy();
        }
    }

    public static class GuildMusicManager {
        private final AudioPlayer player;
        private final AudioPlayerSendHandler sendHandler;
        private final TrackScheduler scheduler;

        public GuildMusicManager(AudioPlayerManager manager) {
            this.player = manager.createPlayer();
            this.sendHandler = new AudioPlayerSendHandler(player);
            this.scheduler = new TrackScheduler(player);
            this.player.addListener(scheduler);
        }

        public AudioPlayer getPlayer() {
            return player;
        }

        public AudioPlayerSendHandler getSendHandler() {
            return sendHandler;
        }

        public TrackScheduler getScheduler() {
            return scheduler;
        }
    }

    private static class TrackScheduler extends AudioEventAdapter {
        private final AudioPlayer player;
        private volatile boolean stopped = false;

        public TrackScheduler(AudioPlayer player) {
            this.player = player;
        }

        public void stop() {
            this.stopped = true;
        }

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (stopped || endReason == AudioTrackEndReason.REPLACED) {
                return;
            }
            player.playTrack(track.makeClone());
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
            if (stopped) {
                return;
            }
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                if (!stopped) {
                    player.playTrack(track.makeClone());
                }
            }).start();
        }

        @Override
        public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
            if (stopped) {
                return;
            }
            player.playTrack(track.makeClone());
        }
    }
}
