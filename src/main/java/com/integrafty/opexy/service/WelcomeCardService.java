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
        int avatarW = 403;
        int avatarH = 401;

        // Apply pixelated mask to avatar
        try {
            BufferedImage mask = ImageIO.read(WelcomeCardService.class.getResourceAsStream("/images/avatar_mask.png"));
            BufferedImage scaledAvatar = new BufferedImage(avatarW, avatarH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sg = scaledAvatar.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            sg.drawImage(avatar, 0, 0, avatarW, avatarH, null);
            sg.dispose();

            for (int y = 0; y < avatarH; y++) {
                for (int x = 0; x < avatarW; x++) {
                    int maskPixel = mask.getRGB(x, y);
                    if ((maskPixel & 0xFFFFFF) == 0) { // If mask is black (not yellow originally)
                        scaledAvatar.setRGB(x, y, 0x00000000); // Make transparent
                    }
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

        int fontSize = 47; 
        Font pixelFont = new Font("SansSerif", Font.BOLD, fontSize);
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, WelcomeCardService.class.getResourceAsStream("/minecraft_arabic.ttf")).deriveFont((float)fontSize);
        } catch (Exception e) {
            log.warn("Failed to load minecraft_arabic.ttf");
        }
        g.setFont(pixelFont);

        FontMetrics metrics = g.getFontMetrics();
        int nameWidth = metrics.stringWidth(name);
        int boxW = 1797 - 1348;
        int boxH = 47;
        int nameX = 1348 + (boxW - nameWidth) / 2; 
        int nameY = 234 + (boxH - metrics.getHeight()) / 2 + metrics.getAscent();

        g.setColor(new Color(5, 18, 59));
        g.drawString(name, nameX, nameY);

        

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
