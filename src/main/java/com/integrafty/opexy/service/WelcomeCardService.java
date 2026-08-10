package com.integrafty.opexy.service;

import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

@Service
public class WelcomeCardService {
    private static final Logger log = LoggerFactory.getLogger(WelcomeCardService.class);

    public static byte[] generateWelcomeCard(Member member) throws Exception {
        BufferedImage background = null;
        try {
            log.info("Loading background from classpath resources...");
            java.io.InputStream is = WelcomeCardService.class.getResourceAsStream("/welcom.png");

            if (is != null) {
                background = ImageIO.read(is);
            }

            if (background == null) {
                // Fallback URL from Highcore if local resource fails
                String urlStr = "https://i.imgur.com/Lzun3rb.png";
                log.warn("Resource missing. Attempting emergency remote fetch: [{}]", urlStr);

                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

                background = ImageIO.read(connection.getInputStream());
            }
        } catch (Exception e) {
            log.error("Resource pipeline failure: {}", e.getMessage());
            throw new Exception("Branding pipeline failure: " + e.getMessage());
        }

        if (background == null) {
            throw new Exception("Background image is null after read attempt.");
        }

        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Draw Template
        g.drawImage(background, 0, 0, width, height, null);

        // 2. Avatar
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=256";
        BufferedImage avatar = null;
        try {
            java.net.URL url = new java.net.URL(avatarUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            avatar = ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            log.warn("Failed to load user avatar: {}. Using generic fallback.", e.getMessage());
            avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gAv = avatar.createGraphics();
            gAv.setColor(new Color(212, 175, 55));
            gAv.fillOval(0, 0, 256, 256);
            gAv.dispose();
        }

        if (avatar != null) {
            int transparent = 0;
            int black = 0;
            int total = avatar.getWidth() * avatar.getHeight();
        
            for (int y = 0; y < avatar.getHeight(); y++) {
                for (int x = 0; x < avatar.getWidth(); x++) {
                    int pixel = avatar.getRGB(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int gr = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
        
                    if (a < 10) {
                        transparent++;
                    }
                    if (r < 10 && gr < 10 && b < 10) {
                        black++;
                    }
                }
            }
        
            log.info("AVATAR DEBUG -> Transparent: {} / {} ({}%)", transparent, total, (transparent * 100.0 / total));
            log.info("AVATAR DEBUG -> Black: {} / {} ({}%)", black, total, (black * 100.0 / total));
        }

        // --- THE DESIGNER'S BLUEPRINT ---
        int avatarX = 755; 
        int avatarY = 59;
        int avatarW = 403;
        int avatarH = 401;

        try {
            BufferedImage scaledAvatar = new BufferedImage(avatarW, avatarH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = scaledAvatar.createGraphics();
            // Use BILINEAR instead of BICUBIC to prevent ringing artifacts (black dots) on high-contrast manga images!
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            sg.drawImage(avatar, 0, 0, avatarW, avatarH, null);
            sg.dispose();

            // Extract mask dynamically from the Gold Placeholder in the original background!
            for (int y = 0; y < avatarH; y++) {
                for (int x = 0; x < avatarW; x++) {
                    int bgX = avatarX + x;
                    int bgY = avatarY + y;
                    
                    if (bgX >= 0 && bgX < width && bgY >= 0 && bgY < height) {
                        int bgPixel = combined.getRGB(bgX, bgY);
                        int bR = (bgPixel >> 16) & 0xFF;
                        int bG = (bgPixel >> 8) & 0xFF;
                        int bB = bgPixel & 0xFF;
                        
                        // Detect Gold Placeholder: Red and Green are significantly higher than Blue.
                        // This robustly catches the yellow and its anti-aliased edges, but ignores the grey shadow and blue background.
                        if (bR > bB + 15 && bG > bB + 15) {
                            int avatarPixel = scaledAvatar.getRGB(x, y);
                            int aA = (avatarPixel >> 24) & 0xFF;
                            
                            if (aA > 0) { // If the avatar has opacity here
                                int aR = (avatarPixel >> 16) & 0xFF;
                                int aG = (avatarPixel >> 8) & 0xFF;
                                int aB = avatarPixel & 0xFF;
                                
                                // Alpha composite the avatar pixel over the background pixel
                                int outR = (aR * aA + bR * (255 - aA)) / 255;
                                int outG = (aG * aA + bG * (255 - aA)) / 255;
                                int outB = (aB * aA + bB * (255 - aA)) / 255;
                                
                                combined.setRGB(bgX, bgY, (255 << 24) | (outR << 16) | (outG << 8) | outB);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to apply dynamic gold mask", e);
            g.drawImage(avatar, avatarX, avatarY, avatarW, avatarH, null);
        }

        // 2. Member Identity Engine
        String name = member.getEffectiveName();
        if (name.length() > 25)
            name = name.substring(0, 23) + "..";

        int boxW = 1797 - 1348;
        int fontSize = 47;
        Font baseFont = new Font("SansSerif", Font.BOLD, fontSize);
        try {
            baseFont = Font.createFont(Font.TRUETYPE_FONT,
                    WelcomeCardService.class.getResourceAsStream("/minecraft_arabic.ttf"));
        } catch (Exception e) {
            log.warn("Failed to load minecraft_arabic.ttf");
        }

        Font pixelFont;
        FontMetrics metrics;
        while (true) {
            pixelFont = baseFont.deriveFont((float) fontSize);
            g.setFont(pixelFont);
            metrics = g.getFontMetrics();
            if (metrics.stringWidth(name) < boxW - 30 || fontSize <= 18) {
                break;
            }
            fontSize -= 2;
        }

        int nameWidth = metrics.stringWidth(name);
        int boxH = 47;
        int nameX = 1348 + (boxW - nameWidth) / 2;
        int nameY = 234 + (boxH - metrics.getHeight()) / 2 + metrics.getAscent() + 15; // +15 offset to move the name
                                                                                       // down a bit

        g.setColor(new Color(5, 18, 59));
        g.drawString(name, nameX, nameY);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
