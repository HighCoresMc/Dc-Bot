package com.integrafty.opexy.service.notification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class YouTubeService {
    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<JsonObject> getLatestVideo(String channelId) {
        try {
            String resolvedId = channelId;
            if (!channelId.startsWith("UC")) {
                resolvedId = resolveChannelId(channelId);
            }

            if (resolvedId == null || !resolvedId.startsWith("UC")) {
                return Optional.empty();
            }

            String url = "https://www.youtube.com/feeds/videos.xml?channel_id=" + resolvedId;
            String xml = restTemplate.getForObject(url, String.class);

            if (xml != null && xml.contains("<entry>")) {
                int entryStart = xml.indexOf("<entry>");
                String latestEntry = xml.substring(entryStart);

                JsonObject video = new JsonObject();
                video.addProperty("videoId", extractValue(latestEntry, "<yt:videoId>(.*?)</yt:videoId>"));
                video.addProperty("title", extractValue(latestEntry, "<title>(.*?)</title>"));
                video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + video.get("videoId").getAsString() + "/hqdefault.jpg");

                return Optional.of(video);
            } else {
                log.info("YouTube RSS: No entry found in XML for {}", resolvedId);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("404 Not Found")) {
                    // Suppress log spam. 404 means the channel has no videos or RSS is disabled.
                    log.debug("YouTube RSS: 404 Not Found for {}", channelId);
                } else if (msg.contains("500 Internal Server Error")) {
                    log.warn("YouTube RSS: Temporary 500 Server Error for {}", channelId);
                } else {
                    // Print only the first line of the error to avoid huge HTML blobs
                    log.warn("YouTube RSS: Failed for {} - {}", channelId, msg.split("\n")[0]);
                }
            } else {
                log.warn("YouTube RSS: Failed for {}", channelId);
            }
        }
        return Optional.empty();
    }

    public String resolveChannelId(String input) {
        // Step 1: Try oembed
        try {
            String handle = input.startsWith("@") ? input : "@" + input;
            String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/" + handle + "&format=json";
            String jsonResponse = restTemplate.getForObject(oembedUrl, String.class);
            if (jsonResponse != null) {
                JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                if (json.has("author_url")) {
                    String authorUrl = json.get("author_url").getAsString();
                    if (authorUrl.contains("/channel/")) {
                        return authorUrl.substring(authorUrl.lastIndexOf("/") + 1);
                    }
                }
            }
        } catch (Exception e) {}

        // Step 2: Scrape HTML for channelId
        try {
            String handle = input.startsWith("@") ? input : "@" + input;
            String url = "https://www.youtube.com/" + handle;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            if (html != null) {
                // Try multiple patterns used by YouTube
                Pattern p = Pattern.compile("\"(?:channelId|browseId|externalId)\":\"(UC[a-zA-Z0-9_-]+)\"");
                Matcher m = p.matcher(html);
                if (m.find()) return m.group(1);
                
                // Fallback: look for canonical link
                Pattern pCanon = Pattern.compile("<link rel=\"canonical\" href=\"https://www.youtube.com/channel/(UC[a-zA-Z0-9_-]+)\"");
                Matcher mCanon = pCanon.matcher(html);
                if (mCanon.find()) return mCanon.group(1);
            }
        } catch (Exception e) {
            log.warn("YouTube Resolver: Failed for {} - {}", input, e.getMessage());
        }
        return null;
    }

    public Optional<JsonObject> scrapeLatestVideo(String channelId) {
        try {
            // Target /videos tab to avoid "Featured Video" on the home page which causes duplication
            String url = "https://www.youtube.com/channel/" + channelId + "/videos";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();
            
            if (html != null) {
                // Focus on gridVideoRenderer which represents the actual video list items
                // This prevents picking up Sidebar/Menu/Featured items
                Pattern pGrid = Pattern.compile("\"gridVideoRenderer\":\\{\"videoId\":\"([a-zA-Z0-9_-]+)\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"");
                Matcher mGrid = pGrid.matcher(html);
                
                if (mGrid.find()) {
                    String videoId = mGrid.group(1);
                    String title = mGrid.group(2);
                    
                    JsonObject video = new JsonObject();
                    video.addProperty("videoId", videoId);
                    video.addProperty("title", title);
                    video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
                    
                    log.info("YouTube Scraper Found: {} ({})", title, videoId);
                    return Optional.of(video);
                }

                // Generic videoRenderer fallback (still looking for video items)
                Pattern pVideo = Pattern.compile("\"videoRenderer\":\\{\"videoId\":\"([a-zA-Z0-9_-]+)\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"(.*?)\"");
                Matcher mVideo = pVideo.matcher(html);
                if (mVideo.find()) {
                    String videoId = mVideo.group(1);
                    String title = mVideo.group(2);
                    
                    JsonObject video = new JsonObject();
                    video.addProperty("videoId", videoId);
                    video.addProperty("title", title);
                    video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
                    return Optional.of(video);
                }
            }
        } catch (Exception e) {
            log.warn("YouTube Scraper: Failed for {} - {}", channelId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<JsonObject> getLiveStream(String channelId) {
        try {
            String url = "https://www.youtube.com/channel/" + channelId + "/live";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();

            if (html != null) {
                String canonicalUrl = extractValue(html, "<link rel=\"canonical\" href=\"(.*?)\"");
                if (canonicalUrl != null && canonicalUrl.contains("watch?v=")) {
                    
                    // EXTRA PROTECTION: Verify the page actually belongs to the target channel
                    String pageChannelId = extractValue(html, "<meta itemprop=\"channelId\" content=\"(.*?)\"");
                    if (pageChannelId.isEmpty()) {
                        pageChannelId = extractValue(html, "\"channelId\":\"(UC[a-zA-Z0-9_-]+)\"");
                    }
                    
                    if (!pageChannelId.isEmpty() && !pageChannelId.equals(channelId)) {
                        log.warn("YouTube Live Check: Blocked false positive. Expected {}, found {}", channelId, pageChannelId);
                        return Optional.empty();
                    }

                    String videoId = extractValue(canonicalUrl, "v=([a-zA-Z0-9_-]+)");
                    
                    if (!videoId.isEmpty() && html.contains("\"isLive\":true")) {
                        String title = extractValue(html, "<meta name=\"title\" content=\"(.*?)\"");
                        if (title.isEmpty()) {
                            title = extractValue(html, "\"title\":\"(.*?)\"");
                        }
                        
                        JsonObject video = new JsonObject();
                        video.addProperty("videoId", videoId);
                        video.addProperty("title", title.isEmpty() ? "Live Stream" : title);
                        video.addProperty("thumbnail", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
                        
                        log.info("YouTube Live Found: {} ({})", title, videoId);
                        return Optional.of(video);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("YouTube Live Check: Failed for {} - {}", channelId, e.getMessage());
        }
        return Optional.empty();
    }

    private String extractValue(String xml, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    public boolean isVideoLiveStream(String videoId) {
        try {
            String url = "https://www.youtube.com/watch?v=" + videoId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String html = response.getBody();

            if (html != null) {
                return html.contains("\"isLive\":true") || html.contains("isLiveBroadcast") || html.contains("\"isUpcoming\":true");
            }
        } catch (Exception e) {
            log.debug("YouTube Live Check for Video {}: Failed - {}", videoId, e.getMessage());
        }
        return false;
    }
}
