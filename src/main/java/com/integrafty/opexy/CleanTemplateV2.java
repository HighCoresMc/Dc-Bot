package com.integrafty.opexy;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Color;

public class CleanTemplateV2 {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("src/main/resources/welcom.png"));
        
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Detect yellow pixels
                if (r > 100 && g > 100 && b < r - 50 && b < g - 50) {
                    // Copy texture from 350 pixels to the right
                    int sourceX = Math.min(x + 350, img.getWidth() - 1);
                    int replacementColor = img.getRGB(sourceX, y);
                    img.setRGB(x, y, replacementColor);
                }
            }
        }
        ImageIO.write(img, "png", new File("src/main/resources/welcome_clean.png"));
        System.out.println("Cleaned!");
    }
}
