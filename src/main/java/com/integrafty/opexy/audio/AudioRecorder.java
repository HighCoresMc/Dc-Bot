package com.integrafty.opexy.audio;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// Section: Audio Recording
public class AudioRecorder implements AudioReceiveHandler {
    private File tempFile;
    private BufferedOutputStream os;
    private boolean recording = false;
    private long totalBytes = 0;

    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private final Thread writerThread;
    private volatile boolean running = true;

    public AudioRecorder() throws IOException {
        this.tempFile = File.createTempFile("opexy_rec_", ".raw");
        this.os = new BufferedOutputStream(new FileOutputStream(tempFile), 65536);
        
        this.writerThread = new Thread(() -> {
            while (running || !queue.isEmpty()) {
                try {
                    byte[] data = queue.poll(50, TimeUnit.MILLISECONDS);
                    if (data != null) {
                        synchronized (this) {
                            os.write(data);
                            totalBytes += data.length;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                }
            }
            try {
                synchronized (this) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
            }
        }, "opexy-audio-writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isRecording() {
        return recording;
    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (!recording) return;
        byte[] data = combinedAudio.getAudioData(1.0);
        if (data != null && data.length > 0) {
            queue.offer(data);
        }
    }

    public synchronized File getTempFile() {
        return tempFile;
    }

    public synchronized long getTotalBytes() {
        while (!queue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
        while (!queue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
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
        running = false;
        try {
            writerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
            int leftover = 0;
            
            while (true) {
                int bytesRead = in.read(buffer, leftover, buffer.length - leftover);
                if (bytesRead <= 0) break;
                
                int totalLen = leftover + bytesRead;
                int processLen = totalLen - (totalLen % 2);
                
                for (int i = 0; i < processLen; i += 2) {
                    byte b1 = buffer[i];
                    byte b2 = buffer[i + 1];
                    buffer[i] = b2;
                    buffer[i + 1] = b1;
                }
                
                out.write(buffer, 0, processLen);
                
                leftover = totalLen - processLen;
                if (leftover > 0) {
                    buffer[0] = buffer[processLen];
                }
            }
            
            if (leftover > 0) {
                out.write(buffer, 0, leftover);
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
        header.putShort((short) 1);
        header.putShort((short) 2);
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
