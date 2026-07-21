package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    public String translate(String text, String targetLang) {
        try {
            // Normalize language codes
            String langCode = targetLang.toLowerCase();
            if (langCode.equals("eng"))
                langCode = "en";
            if (langCode.equals("ara"))
                langCode = "ar";
            if (langCode.length() > 2)
                langCode = langCode.substring(0, 2);

            // Detect source language
            String sourceLang = isArabic(text) ? "ar" : "en";

            // If translating to the same language, flip source
            if (sourceLang.equals(langCode)) {
                sourceLang = langCode.equals("ar") ? "en" : "ar";
            }

            String langPair = sourceLang + "|" + langCode;

            // Using MyMemory API with explicit language pair
            String urlStr = String.format("https://api.mymemory.translated.net/get?q=%s&langpair=%s",
                    URLEncoder.encode(text, StandardCharsets.UTF_8.toString()), langPair);

            log.info("Translating {} via MyMemory: {}", langPair, urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            if (response.isEmpty())
                return "Error: No response from translation engine.";

            JSONObject json = new JSONObject(response);
            if (json.has("responseData")) {
                String result = json.getJSONObject("responseData").getString("translatedText");
                // Fix potential HTML entities
                return result.replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
            }

            return "Error: Could not parse translation response.";
        } catch (Exception e) {
            log.error("Translation failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private boolean isArabic(String text) {
        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.ARABIC ||
                    block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                    block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
                    block == Character.UnicodeBlock.ARABIC_SUPPLEMENT) {
                return true;
            }
        }
        return false;
    }
}
