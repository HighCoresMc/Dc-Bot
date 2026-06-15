package com.integrafty.opexy.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class ImageModerationService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private OrtEnvironment env;
    private OrtSession session;

    @org.springframework.context.event.EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    public void onStartup() {
        try {
            File modelFile = new File("nsfw_model.onnx");
            if (!modelFile.exists()) {
                log.info("[NSFW Filter] Local model file not found. Downloading...");
                downloadModel(modelFile);
            }
            log.info("[NSFW Filter] Initializing local ONNX Runtime...");
            this.env = OrtEnvironment.getEnvironment();
            this.session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
            log.info("[NSFW Filter] ONNX session initialized successfully.");
        } catch (Exception e) {
            log.error("[NSFW Filter] Failed to initialize ONNX model", e);
        }
    }

    private void downloadModel(File target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://huggingface.co/onnx-community/nsfw-image-detector-ONNX/resolve/main/onnx/model_quantized.onnx"))
                .GET()
                .build();
        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 200) {
            try (java.io.InputStream is = response.body();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
            log.info("[NSFW Filter] Local model downloaded successfully.");
        } else {
            throw new RuntimeException("HTTP Status: " + response.statusCode());
        }
    }

    @jakarta.annotation.PreDestroy
    public void onDestroy() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
            log.info("[NSFW Filter] ONNX session closed.");
        } catch (Exception e) {
            log.error("[NSFW Filter] Failed to close ONNX session", e);
        }
    }

    public boolean isPornographic(String imageUrl) {
        if (session == null) {
            log.warn("[NSFW Filter] ONNX session is not initialized. Skipping check.");
            return false;
        }
        try {
            log.info("[NSFW Filter] Checking image URL: {}", imageUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                log.warn("[NSFW Filter] Failed to fetch image: status code {}", response.statusCode());
                return false;
            }

            BufferedImage original;
            try (java.io.InputStream is = response.body()) {
                original = ImageIO.read(is);
            }
            if (original == null) {
                log.warn("[NSFW Filter] Failed to decode image from URL");
                return false;
            }

            BufferedImage resized = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, 224, 224, null);
            g.dispose();

            float[] floatValues = new float[3 * 224 * 224];
            int[] rgbValues = new int[224 * 224];
            resized.getRGB(0, 0, 224, 224, rgbValues, 0, 224);

            for (int i = 0; i < rgbValues.length; i++) {
                int rgb = rgbValues[i];
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float gVal = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;

                floatValues[i] = (r - 0.5f) / 0.5f;
                floatValues[224 * 224 + i] = (gVal - 0.5f) / 0.5f;
                floatValues[2 * 224 * 224 + i] = (b - 0.5f) / 0.5f;
            }

            long[] shape = new long[]{1, 3, 224, 224};
            java.nio.FloatBuffer buffer = java.nio.FloatBuffer.wrap(floatValues);
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape)) {
                try (OrtSession.Result results = session.run(java.util.Collections.singletonMap("pixel_values", inputTensor))) {
                    float[][] logits = (float[][]) results.get(0).getValue();
                    float normalLogit = logits[0][0];
                    float nsfwLogit = logits[0][1];

                    double expNormal = Math.exp(normalLogit);
                    double expNsfw = Math.exp(nsfwLogit);
                    double probNsfw = expNsfw / (expNormal + expNsfw);

                    log.info("[NSFW Filter] Local AI normal logit: {}, nsfw logit: {}, nsfw prob: {}", normalLogit, nsfwLogit, probNsfw);
                    boolean isNsfw = probNsfw > 0.45;
                    log.info("[NSFW Filter] Final result: {}", isNsfw);
                    return isNsfw;
                }
            }
        } catch (Exception e) {
            log.warn("[NSFW Filter] Failed to moderate image URL {}: {}", imageUrl, e.getMessage());
        }
        return false;
    }
}
