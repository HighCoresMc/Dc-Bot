package com.integrafty.opexy.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.nio.ByteBuffer;

// Section: Audio Sender
public class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;
    private final ByteBuffer[] buffers;
    private int bufferIndex = 0;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocateDirect(4096);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
        this.buffers = new ByteBuffer[20];
        for (int i = 0; i < 20; i++) {
            this.buffers[i] = ByteBuffer.allocateDirect(4096);
        }
    }

    @Override
    public boolean canProvide() {
        buffer.clear();
        return audioPlayer.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        int len = frame.getDataLength();
        ByteBuffer target = buffers[bufferIndex];
        target.clear();
        buffer.position(0);
        buffer.limit(len);
        target.put(buffer);
        target.flip();
        bufferIndex = (bufferIndex + 1) % buffers.length;
        return target;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}

