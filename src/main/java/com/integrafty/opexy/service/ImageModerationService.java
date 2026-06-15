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

    @Value("${openai.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isPornographic(String imageUrl) {
        try {
            log.info("[NSFW Filter] Checking image URL: {}", imageUrl);
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("[NSFW Filter] OpenAI API key is missing. Moderation check skipped.");
                return false;
            }
            
            java.util.Map<String, Object> imageMap = java.util.Map.of("url", imageUrl);
            java.util.Map<String, Object> inputItem = java.util.Map.of("type", "image_url", "image_url", imageMap);
            java.util.Map<String, Object> requestBody = java.util.Map.of(
                    "model", "omni-moderation-latest",
                    "input", java.util.List.of(inputItem)
            );
            
            String jsonPayload = mapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/moderations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[NSFW Filter] Status code: {}, Response: {}", response.statusCode(), response.body());
            
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode results = root.path("results");
                if (results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    boolean flagged = firstResult.path("flagged").asBoolean();
                    
                    JsonNode scores = firstResult.path("category_scores");
                    double sexual = scores.path("sexual").asDouble(0);
                    double sexualMinors = scores.path("sexual/minors").asDouble(0);
                    
                    log.info("[NSFW Filter] OpenAI evaluation - flagged: {}, sexual score: {}, sexual/minors score: {}", flagged, sexual, sexualMinors);
                    
                    boolean isNsfw = flagged || sexual > 0.1 || sexualMinors > 0.1;
                    log.info("[NSFW Filter] Final result: {}", isNsfw);
                    return isNsfw;
                }
            } else {
                log.warn("[NSFW Filter] OpenAI API error status: {}, response: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("[NSFW Filter] Failed to moderate image URL {}: {}", imageUrl, e.getMessage());
        }
        return false;
    }
}
