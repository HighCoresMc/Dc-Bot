package com.integrafty.opexy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class ImageModerationService {

    @Value("${opexy.nsfw.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isPornographic(String imageUrl) {
        String activeKey = apiKey;
        if (activeKey == null || activeKey.trim().isEmpty()) {
            activeKey = "f0cf44a07204dc2b15201c2a0afe0a0d"; // Public free key from homepage
        }
        
        try {
            String url = "https://api.moderatecontent.com/moderate/?key=" + activeKey + "&url=" + java.net.URLEncoder.encode(imageUrl, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if (root.has("rating_label")) {
                    String ratingLabel = root.get("rating_label").asText();
                    // ModerateContent returns "everyone", "teen", or "adult"
                    // We only want to block "adult" (pornographic) content to avoid random false positives.
                    return "adult".equalsIgnoreCase(ratingLabel);
                }
            }
        } catch (Exception e) {
            log.warn("[NSFW Filter] Failed to moderate image URL {}: {}", imageUrl, e.getMessage());
        }
        return false;
    }
}
