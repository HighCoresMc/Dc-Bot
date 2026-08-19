package com.integrafty.opexy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class WhitelistSyncService {

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public void syncToSupabase(String discord, String mc, String version, String type) {
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

        String checkSql = "SELECT count(*) FROM public.whitelist WHERE mc = ?";
        Integer count = 0;
        try {
            count = jdbcTemplate.queryForObject(checkSql, Integer.class, mc);
        } catch (Exception e) {
            log.error("Failed to check if whitelist entry exists: {}", e.getMessage());
            return;
        }

        if (count != null && count > 0) {
            log.info("Whitelist entry already exists in Database for user: {}", mc);
            return;
        }

        String insertSql = "INSERT INTO public.whitelist (discord, mc, version, type, team, tag, admin) " +
                     "VALUES (?, ?, ?, ?, 'EMPTY', 'مقبول', 'HighCoreMc Bot')";

        try {
            jdbcTemplate.update(insertSql, discord, mc, mappedVersion, mappedType);
            log.info("Successfully synced whitelist entry to Database for user: {}", mc);
        } catch (Exception e) {
            log.error("Failed to sync to Database: {}", e.getMessage());
        }
    }
}
