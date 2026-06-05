package com.integrafty.opexy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class WhitelistSyncService {

    @Value("${SUPABASE_URL:NOT_SET}")
    private String supabaseUrl;

    @Value("${SUPABASE_KEY:NOT_SET}")
    private String supabaseKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void syncToSupabase(String discord, String mc, String version, String type) {
        if (supabaseUrl.equals("NOT_SET") || supabaseKey.equals("NOT_SET")) {
            log.warn("Supabase credentials not set. Skipping sync.");
            return;
        }

        // Map type values (Original vs Crack)
        String mappedType = type.toLowerCase();

        List<String> originalKeywords = Arrays.asList(
            "perm", "premium", "org", "original", "microsoft", "paid", "اصلية", "أصلية",
            "مايكرو سوفت", "مايكروسوفت", "بريميوم", "بيرم", "مدفوعة", "بفلوس", "حساب مايكروسوفت", "حساب بريميوم"
        );

        List<String> crackKeywords = Arrays.asList(
            "crack", "cracked", "tlauncher", "offline", "تي لانشر", "مكركة", "كراك",
            "كرك", "مو اصلية", "مجانية", "مهكرة", "sklauncher", "titan", "gdlauncher",
            "multimc", "prism", "atlauncher", "shiginima", "hmcl", "polymc",
            "اس كي لانشر", "تايتن لانشر", "جي دي لانشر", "ملتي إم سي", "بريزم لانشر",
            "اي تي لانشر", "شيغينما لانشر", "اتش ام سي ال", "بولي ام سي"
        );

        boolean isOriginal = originalKeywords.stream().anyMatch(mappedType::contains);
        boolean isCrack    = crackKeywords.stream().anyMatch(mappedType::contains);

        if (isOriginal) {
            mappedType = "original ~ أصــلــية";
        } else if (isCrack) {
            mappedType = "krack ~ كــراك";
        } else {
            mappedType = type;
        }

        // Map version values (Java vs Bedrock)
        String mappedVersion = version.toLowerCase();

        List<String> javaKeywords = Arrays.asList(
            "java", "pc", "laptop", "حاسبة", "بيسي", "كمبيوتر", "لابتوب", "جافا", "تي لانشر"
        );

        List<String> bedrockKeywords = Arrays.asList(
            "ps4", "ps5", "playstation", "xbox", "phone", "bedrock", "iphone",
            "جوال", "هاتف", "تلفون", "بلايستايشن", "اكس بوكس", "بيد روك", "بيدروك"
        );

        boolean isJava    = javaKeywords.stream().anyMatch(mappedVersion::contains);
        boolean isBedrock = bedrockKeywords.stream().anyMatch(mappedVersion::contains);

        if (isJava) {
            mappedVersion = "Java ~ جــافــا";
        } else if (isBedrock) {
            mappedVersion = "Bedrock ~ بـيدروك";
        } else {
            mappedVersion = version;
        }

        String now = Instant.now().toString();

        // Build JSON body
        String json = String.format(
            "{\"discord\":\"%s\",\"mc\":\"%s\",\"version\":\"%s\",\"type\":\"%s\"," +
            "\"team\":\"EMPTY\",\"tag\":\"مقبول\",\"admin\":\"HighCoreMc Bot\"," +
            "\"created_at\":\"%s\",\"modified_at\":\"%s\"}",
            escapeJson(discord),
            escapeJson(mc),
            escapeJson(mappedVersion),
            escapeJson(mappedType),
            now,
            now
        );

        String endpoint = supabaseUrl + "/rest/v1/whitelist";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                // ignore-duplicates = ON CONFLICT DO NOTHING
                .header("Prefer", "resolution=ignore-duplicates")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Successfully synced whitelist entry to Supabase for user: {}", mc);
            } else {
                log.error("Supabase returned error {} : {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Failed to sync to Supabase: {}", e.getMessage());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
