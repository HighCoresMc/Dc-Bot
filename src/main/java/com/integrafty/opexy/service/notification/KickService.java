package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class KickService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DEFAULT_FLARESOLVERR_URL = "http://localhost:8191/v1";

    public Optional<JsonObject> getStreamStatus(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        
        String cleanUsername = username.trim();
        String flaresolverrUrl = System.getenv().getOrDefault("FLARESOLVERR_URL", DEFAULT_FLARESOLVERR_URL);
        
        try {
            String targetUrl = "https://kick.com/api/v1/channels/" + cleanUsername;
            
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("cmd", "request.get");
            requestBody.addProperty("url", targetUrl);
            requestBody.addProperty("maxTimeout", 60000);

            log.info("Checking Kick API: {} via FlareSolverr", cleanUsername);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(flaresolverrUrl, entity, String.class);
            String body = response.getBody();

            if (body != null && response.getStatusCode().is2xxSuccessful()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("solution")) {
                    String html = json.getAsJsonObject("solution").get("response").getAsString();
                    
                    String rawJson = html;
                    Matcher m = Pattern.compile("<pre[^>]*>(.*?)</pre>", Pattern.DOTALL).matcher(html);
                    if (m.find()) {
                        rawJson = m.group(1);
                    } else {
                        rawJson = html.replaceAll("<[^>]+>", "").trim();
                    }
                    
                    try {
                        JsonObject channelData = JsonParser.parseString(rawJson).getAsJsonObject();
                        if (channelData.has("livestream") && !channelData.get("livestream").isJsonNull()) {
                            JsonObject livestream = channelData.getAsJsonObject("livestream");
                            if (livestream.has("is_live") && livestream.get("is_live").getAsBoolean()) {
                                log.info("Kick: {} is LIVE (API match)", cleanUsername);
                                return Optional.of(livestream);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Kick API Parse Error for {}: {}", cleanUsername, e.getMessage());
                        if (rawJson.contains("\"is_live\":true") || rawJson.contains("\"is_live\": true")) {
                            log.info("Kick: {} is LIVE (API Text Fallback match)", cleanUsername);
                            JsonObject dummyLive = new JsonObject();
                            dummyLive.addProperty("session_title", "Live on KICK!");
                            
                            Pattern pId = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
                            Matcher mId = pId.matcher(rawJson);
                            if (mId.find()) {
                                dummyLive.addProperty("id", mId.group(1));
                            } else {
                                dummyLive.addProperty("id", cleanUsername + "_live_" + System.currentTimeMillis() / 3600000);
                            }
                            return Optional.of(dummyLive);
                        }
                    }
                    
                    log.info("Kick: {} is currently offline", cleanUsername);
                } else {
                    log.warn("FlareSolverr response missing 'solution' for {}", cleanUsername);
                }
            } else {
                log.warn("FlareSolverr failed with status {}: {}", response.getStatusCode(), body);
            }
        } catch (Exception e) {
            log.error("FlareSolverr Scraping Error for {}: {}", cleanUsername, e.getMessage());
        }
        return Optional.empty();
    }
}
