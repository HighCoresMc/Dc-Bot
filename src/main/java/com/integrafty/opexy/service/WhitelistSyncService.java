package com.integrafty.opexy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

@Service
@Slf4j
public class WhitelistSyncService {

    @Value("${supabase.url:jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require}")
    private String dbUrl;

    @Value("${supabase.user:postgres.ungifmcwoxnpeduzxxbr}")
    private String dbUser;

    @Value("${SUPABASE_PASSWORD:[YOUR-PASSWORD]}")
    private String dbPassword;

    public void syncToSupabase(String discord, String mc, String version, String type) {
        if (dbPassword.equals("[YOUR-PASSWORD]")) {
            log.warn("Supabase password not set. Skipping sync.");
            return;
        }

        // Map type values (Original vs Crack)
        String mappedType = type.toLowerCase();
        
        java.util.List<String> originalKeywords = java.util.Arrays.asList(
            "perm", "premium", "org", "original", "microsoft", "paid", "اصلية", "أصلية", 
            "مايكرو سوفت", "مايكروسوفت", "بريميوم", "بيرم", "مدفوعة", "بفلوس", "حساب مايكروسوفت", "حساب بريميوم"
        );
        
        java.util.List<String> crackKeywords = java.util.Arrays.asList(
            "crack", "cracked", "tlauncher", "offline", "تي لانشر", "مكركة", "كراك", 
            "كرك", "مو اصلية", "مجانية", "مهكرة", "sklauncher", "titan", "gdlauncher", 
            "multimc", "prism", "atlauncher", "shiginima", "hmcl", "polymc",
            "اس كي لانشر", "تايتن لانشر", "جي دي لانشر", "ملتي إم سي", "بريزم لانشر", 
            "اي تي لانشر", "شيغينما لانشر", "اتش ام سي ال", "بولي ام سي"
        );

        boolean isOriginal = originalKeywords.stream().anyMatch(mappedType::contains);
        boolean isCrack = crackKeywords.stream().anyMatch(mappedType::contains);

        if (isOriginal) {
            mappedType = "original ~ أصــلــية";
        } else if (isCrack) {
            mappedType = "krack ~ كــراك";
        } else {
            mappedType = type; // Fallback to original input
        }

        // Map version values (Java vs Bedrock)
        String mappedVersion = version.toLowerCase();
        
        java.util.List<String> javaKeywords = java.util.Arrays.asList(
            "java", "pc", "laptop", "حاسبة", "بيسي", "كمبيوتر", "لابتوب", "جافا", "تي لانشر"
        );
        
        java.util.List<String> bedrockKeywords = java.util.Arrays.asList(
            "ps4", "ps5", "playstation", "xbox", "phone", "bedrock", "iphone", 
            "جوال", "هاتف", "تلفون", "بلايستايشن", "اكس بوكس", "بيد روك", "بيدروك"
        );

        boolean isJava = javaKeywords.stream().anyMatch(mappedVersion::contains);
        boolean isBedrock = bedrockKeywords.stream().anyMatch(mappedVersion::contains);

        if (isJava) {
            mappedVersion = "Java ~ جــافــا";
        } else if (isBedrock) {
            mappedVersion = "Bedrock ~ بـيدروك";
        } else {
            mappedVersion = version; // Fallback
        }

        String sql = "INSERT INTO whitelist (discord, mc, version, type, team, tag, admin, created_at, modified_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (mc) DO NOTHING";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, discord);
            pstmt.setString(2, mc);
            pstmt.setString(3, mappedVersion);
            pstmt.setString(4, mappedType);
            pstmt.setString(5, "EMPTY");
            pstmt.setString(6, "مقبول");
            pstmt.setString(7, "HighCoreMc Bot"); // admin column
            pstmt.setTimestamp(8, Timestamp.from(Instant.now()));
            pstmt.setTimestamp(9, Timestamp.from(Instant.now()));

            pstmt.executeUpdate();
            log.info("Successfully synced whitelist entry to Supabase for user: {}", mc);

        } catch (Exception e) {
            log.error("Failed to sync to Supabase: {}", e.getMessage());
        }
    }
}
