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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class YouTubeAudioService {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    public YouTubeAudioService() {
        this.playerManager = new DefaultAudioPlayerManager();
        
        String poToken = System.getProperty("PO_TOKEN");
        if (poToken == null || poToken.isEmpty()) {
            poToken = System.getenv("PO_TOKEN");
        }
        String visitorData = System.getProperty("VISITOR_DATA");
        if (visitorData == null || visitorData.isEmpty()) {
            visitorData = System.getenv("VISITOR_DATA");
        }
        if (poToken != null && !poToken.isEmpty() && visitorData != null && !visitorData.isEmpty()) {
            dev.lavalink.youtube.YoutubeSource.setPoTokenAndVisitorData(poToken, visitorData);
        }
        
        dev.lavalink.youtube.YoutubeSourceOptions options = new dev.lavalink.youtube.YoutubeSourceOptions();
        options.setAllowSearch(true);
        options.setAllowDirectVideoIds(true);
        options.setAllowDirectPlaylistIds(true);
        
        String cipherUrl = System.getProperty("YOUTUBE_CIPHER_URL");
        if (cipherUrl == null || cipherUrl.isEmpty()) {
            cipherUrl = System.getenv("YOUTUBE_CIPHER_URL");
        }
        if (cipherUrl == null || cipherUrl.isEmpty()) {
            cipherUrl = "https://cipher.kikkia.dev/";
        }
        
        if (!"none".equalsIgnoreCase(cipherUrl)) {
            String cipherPassword = System.getProperty("YOUTUBE_CIPHER_PASSWORD");
            if (cipherPassword == null || cipherPassword.isEmpty()) {
                cipherPassword = System.getenv("YOUTUBE_CIPHER_PASSWORD");
            }
            if (cipherPassword == null) {
                cipherPassword = "";
            }
            String cipherUserAgent = System.getProperty("YOUTUBE_CIPHER_USER_AGENT");
            if (cipherUserAgent == null || cipherUserAgent.isEmpty()) {
                cipherUserAgent = System.getenv("YOUTUBE_CIPHER_USER_AGENT");
            }
            if (cipherUserAgent == null || cipherUserAgent.isEmpty()) {
                cipherUserAgent = "opexy-bot";
            }
            options.setRemoteCipher(cipherUrl, cipherPassword, cipherUserAgent);
        }
        
        dev.lavalink.youtube.YoutubeAudioSourceManager youtube = new dev.lavalink.youtube.YoutubeAudioSourceManager(
            options,
            new dev.lavalink.youtube.clients.Music(),
            new dev.lavalink.youtube.clients.Web(),
            new dev.lavalink.youtube.clients.Android(),
            new dev.lavalink.youtube.clients.Ios(),
            new dev.lavalink.youtube.clients.TvHtml5Simply(),
            new dev.lavalink.youtube.clients.AndroidMusic(),
            new dev.lavalink.youtube.clients.Tv(),
            new dev.lavalink.youtube.clients.WebEmbedded()
        );
        
        String refreshToken = System.getProperty("YOUTUBE_REFRESH_TOKEN");
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = System.getenv("YOUTUBE_REFRESH_TOKEN");
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            youtube.useOauth2(refreshToken, true);
        } else {
            youtube.useOauth2(null, false);
        }
        
        this.playerManager.registerSourceManager(youtube);
        com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
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