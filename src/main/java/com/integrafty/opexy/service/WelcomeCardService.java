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
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                
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
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            avatar = ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            log.warn("Failed to load user avatar: {}. Using generic fallback.", e.getMessage());
            avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gAv = avatar.createGraphics();
            gAv.setColor(new Color(212, 175, 55));
            gAv.fillOval(0, 0, 256, 256);
            gAv.dispose();
        }

        // --- THE DESIGNER'S BLUEPRINT ---
        int avatarX = 755; 
        int avatarY = 59;
        int avatarW = 405;
        int avatarH = 405;

        // The template is now clean, so we just draw the masked avatar directly on it!
        // Dynamically erase the yellow placeholder from the template behind the avatar by cloning the texture from the left.
        for (int y = avatarY - 15; y < avatarY + avatarH + 15; y++) {
            for (int x = avatarX - 15; x < avatarX + avatarW + 15; x++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int rgb = combined.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int gCol = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // Target the yellow placeholder more accurately
                    if (r > 100 && gCol > 100 && b < 80) {
                        int texX = Math.max(x - 450, 0); // clone from the dark blue left side
                        combined.setRGB(x, y, combined.getRGB(texX, y));
                    }
                }
            }
        }

        // Apply pixelated mask to avatar
        try {
            BufferedImage mask = ImageIO.read(WelcomeCardService.class.getResourceAsStream("/images/avatar_mask.png"));
            
            // Scale mask to ensure it matches avatar dimensions
            BufferedImage scaledMask = new BufferedImage(avatarW, avatarH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D maskG2d = scaledMask.createGraphics();
            maskG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            maskG2d.drawImage(mask, 0, 0, avatarW, avatarH, null);
            maskG2d.dispose();

            BufferedImage scaledAvatar = new BufferedImage(avatarW, avatarH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = scaledAvatar.createGraphics();
            
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            sg.drawImage(avatar, 0, 0, avatarW, avatarH, null);
            sg.dispose();

            for (int y = 0; y < avatarH; y++) {
                for (int x = 0; x < avatarW; x++) {
                    int maskPixel = scaledMask.getRGB(x, y);
                    int maskAlpha = (maskPixel >> 24) & 0xFF;
                    int maskR = (maskPixel >> 16) & 0xFF;
                    int maskG = (maskPixel >> 8) & 0xFF;
                    int maskB = maskPixel & 0xFF;
                    
                    // If mask uses alpha, use it. Otherwise use brightness (white = opaque, black = transparent)
                    int finalAlpha = maskAlpha;
                    if (maskAlpha == 255) {
                        finalAlpha = (maskR + maskG + maskB) / 3;
                    } else if (finalAlpha > 0) {
                        // If it's a transparent mask but also has brightness, let alpha dictate
                    }

                    int avatarPixel = scaledAvatar.getRGB(x, y);
                    int avatarR = (avatarPixel >> 16) & 0xFF;
                    int avatarG = (avatarPixel >> 8) & 0xFF;
                    int avatarB = avatarPixel & 0xFF;
                    
                    scaledAvatar.setRGB(x, y, (finalAlpha << 24) | (avatarR << 16) | (avatarG << 8) | avatarB);
                }
            }
            g.drawImage(scaledAvatar, avatarX, avatarY, null);
        } catch (Exception e) {
            log.warn("Failed to apply avatar mask", e);
            g.drawImage(avatar, avatarX, avatarY, avatarW, avatarH, null);
        }

        // 2. Member Identity Engine
        String name = member.getEffectiveName();
        if (name.length() > 25) name = name.substring(0, 23) + "..";

        int boxW = 1797 - 1348;
        int fontSize = 47; 
        Font baseFont = new Font("SansSerif", Font.BOLD, fontSize);
        try {
            baseFont = Font.createFont(Font.TRUETYPE_FONT, WelcomeCardService.class.getResourceAsStream("/minecraft_arabic.ttf"));
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
        int nameY = 234 + (boxH - metrics.getHeight()) / 2 + metrics.getAscent() + 15; // +15 offset to move the name down a bit

        g.setColor(new Color(5, 18, 59));
        g.drawString(name, nameX, nameY);

        

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
