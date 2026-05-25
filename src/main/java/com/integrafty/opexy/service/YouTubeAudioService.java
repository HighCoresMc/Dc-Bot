package com.integrafty.opexy.service;

import com.integrafty.opexy.audio.AudioPlayerSendHandler;
import dev.arbjerg.lavaplayer.player.AudioLoadResultHandler;
import dev.arbjerg.lavaplayer.player.AudioPlayer;
import dev.arbjerg.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavaplayer.player.DefaultAudioPlayerManager;
import dev.arbjerg.lavaplayer.player.event.AudioEventAdapter;
import dev.arbjerg.lavaplayer.source.AudioSourceManagers;
import dev.arbjerg.lavaplayer.tools.FriendlyException;
import dev.arbjerg.lavaplayer.track.AudioPlaylist;
import dev.arbjerg.lavaplayer.track.AudioTrack;
import dev.arbjerg.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

// Section: YouTube Audio Service
@Service
public class YouTubeAudioService {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public YouTubeAudioService() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        this.musicManagers = new ConcurrentHashMap<>();
    }

    public synchronized GuildMusicManager getMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), guildId -> new GuildMusicManager(playerManager));
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

    public void stop(Guild guild) {
        GuildMusicManager musicManager = musicManagers.remove(guild.getIdLong());
        if (musicManager != null) {
            musicManager.getPlayer().stopTrack();
            musicManager.getPlayer().destroy();
        }
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
        }
    }

    // Section: Guild Music Manager
    public static class GuildMusicManager {
        private final AudioPlayer player;
        private final AudioPlayerSendHandler sendHandler;

        public GuildMusicManager(AudioPlayerManager manager) {
            this.player = manager.createPlayer();
            this.sendHandler = new AudioPlayerSendHandler(player);
            this.player.addListener(new TrackScheduler(player));
        }

        public AudioPlayer getPlayer() {
            return player;
        }

        public AudioPlayerSendHandler getSendHandler() {
            return sendHandler;
        }
    }

    // Section: Track Scheduler
    private static class TrackScheduler extends AudioEventAdapter {
        private final AudioPlayer player;

        public TrackScheduler(AudioPlayer player) {
            this.player = player;
        }

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (endReason.mayStartNext) {
                player.playTrack(track.makeClone());
            }
        }
    }
}
