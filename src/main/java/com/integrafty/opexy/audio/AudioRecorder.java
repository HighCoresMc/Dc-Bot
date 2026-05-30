package com.integrafty.opexy.audio;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Section: Audio Recording
 * Human-generated recorder for Opexy Bot (Ported from Highcore Bot)
 */
public class AudioRecorder implements AudioReceiveHandler {
    private final File tempFile;
    private final BufferedOutputStream os;
    private boolean recording = false;
    private long totalBytes = 0;

    public AudioRecorder() throws IOException {
        this.tempFile = File.createTempFile("opexy_rec_", ".raw");
        // Increased buffer to 64KB for better performance in long meetings
        this.os = new BufferedOutputStream(new FileOutputStream(tempFile), 65536);
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isRecording() {
        return recording;
    }

    @Override
    public boolean canReceiveCombined() {
        return true; // We always receive, but handle CombinedAudio based on 'recording' flag
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (!recording) return;
        try {
            byte[] data = combinedAudio.getAudioData(1.0);
            os.write(data);
            totalBytes += data.length;
        } catch (IOException e) {
            // Error handled silently per section policy
        }
    }

    public synchronized File getTempFile() {
        return tempFile;
    }

    public synchronized long getTotalBytes() {
        return totalBytes;
    }

    public static class SplitResult {
        public final File tempFile;
        public final long totalBytes;
        public SplitResult(File tempFile, long totalBytes) {
            this.tempFile = tempFile;
            this.totalBytes = totalBytes;
        }
    }

    public synchronized SplitResult split() throws IOException {
        os.flush();
        os.close();
        
        File oldTemp = this.tempFile;
        long oldBytes = this.totalBytes;
        
        this.tempFile = File.createTempFile("opexy_rec_", ".raw");
        this.os = new BufferedOutputStream(new FileOutputStream(this.tempFile), 65536);
        this.totalBytes = 0;
        
        return new SplitResult(oldTemp, oldBytes);
    }

    public void stop() {
        recording = false;
        try {
            os.close();
        } catch (IOException e) {
            // Error handled silently
        }
    }

    public void saveAsWav(File wavFile) throws IOException {
        saveAsWav(this.tempFile, this.totalBytes, wavFile);
    }

    public static void saveAsWav(File tempFile, long totalBytes, File wavFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(wavFile);
             FileInputStream in = new FileInputStream(tempFile)) {
            
            writeWavHeader(out, totalBytes);
            
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                // Swap bytes from Big Endian (JDA) to Little Endian (WAV)
                for (int i = 0; i < len; i += 2) {
                    if (i + 1 < len) {
                        byte b1 = buffer[i];
                        byte b2 = buffer[i + 1];
                        buffer[i] = b2;
                        buffer[i + 1] = b1;
                    }
                }
                out.write(buffer, 0, len);
            }
        }
    }

    private static void writeWavHeader(FileOutputStream out, long rawLength) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);
        
        header.put("RIFF".getBytes());
        header.putInt((int) (rawLength + 36));
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1); // PCM
        header.putShort((short) 2); // Stereo
        header.putInt(48000);
        header.putInt(48000 * 2 * 2);
        header.putShort((short) 4);
        header.putShort((short) 16);
        header.put("data".getBytes());
        header.putInt((int) rawLength);
        
        out.write(header.array());
    }

    public void cleanup() {
        cleanup(this.tempFile);
    }

    public static void cleanup(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}
