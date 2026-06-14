package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwitchService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<JsonObject> getStreamStatus(String username) {
        try {
            String url = "https://www.twitch.tv/" + username;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String html = response.getBody();
                
                if (html.contains("\"isLiveBroadcast\":true")) {
                    JsonObject stream = new JsonObject();
                    stream.addProperty("id", username + "_" + (System.currentTimeMillis() / 3600000)); 
                    stream.addProperty("title", extractValue(html, "\"description\":\"(.*?)\""));
                    stream.addProperty("thumbnail_url", "https://static-cdn.jtvnw.net/previews-ttv/live_user_" + username.toLowerCase() + "-1280x720.jpg");
                    return Optional.of(stream);
                }
            }
        } catch (Exception e) {
            log.warn("Twitch Scraper: Could not fetch status for {}: {}", username, e.getMessage());
        }
        return Optional.empty();
    }

    private String extractValue(String html, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Live Stream";
    }
}
