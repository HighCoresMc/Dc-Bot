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

    private final String apiUser = "631680776";
    private final String apiSecret = "MkUcMWGyxX3wfUx7nkG9Pwcrzvs5XzHo";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isPornographic(String imageUrl) {
        try {
            log.info("[NSFW Filter] Checking image URL: {}", imageUrl);
            String url = "https://api.sightengine.com/1.0/check.json?models=nudity-2.0"
                    + "&api_user=" + apiUser
                    + "&api_secret=" + apiSecret
                    + "&url=" + java.net.URLEncoder.encode(imageUrl, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[NSFW Filter] Status code: {}, Response: {}", response.statusCode(), response.body());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                if ("success".equals(root.path("status").asText())) {
                    JsonNode nudity = root.path("nudity");
                    if (!nudity.isMissingNode()) {
                        double sexualActivity = nudity.path("sexual_activity").asDouble(0);
                        double sexualDisplay = nudity.path("sexual_display").asDouble(0);
                        double erotica = nudity.path("erotica").asDouble(0);
                        double suggestive = nudity.path("suggestive").asDouble(0);

                        boolean isNsfw = sexualActivity > 0.1 || sexualDisplay > 0.1 || erotica > 0.1 || suggestive > 0.1;
                        log.info("[NSFW Filter] Evaluation result for {}: {}", imageUrl, isNsfw);
                        return isNsfw;
                    }
                } else {
                    log.warn("[NSFW Filter] Sightengine API error: {}", root.path("error").path("message").asText());
                }
            }
        } catch (Exception e) {
            log.warn("[NSFW Filter] Failed to moderate image URL {}: {}", imageUrl, e.getMessage());
        }
        return false;
    }
}
