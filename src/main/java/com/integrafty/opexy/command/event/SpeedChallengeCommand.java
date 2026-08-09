package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.service.event.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class SpeedChallengeCommand implements MultiSlashCommand {

    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final LogManager logManager;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    private static final List<String> MINECRAFT_WORDS = List.of(
            "دايموند", "نذررايت", "كريبر", "أندر مان", "بلوكة طين", "سيف حديدي", 
            "درع ذهبي", "خشب محلل", "بوابة النذر", "تنين الاندر", "قرية القرويين",
            "صندوق مخفي", "بيوم الغابة", "كهف عميق", "منجم قديم", "خيوط عنكبوت"
    );
    public SpeedChallengeCommand(EventManager eventManager, AchievementService achievementService, LogManager logManager) {
        this.eventManager = eventManager;
        this.achievementService = achievementService;
        this.logManager = logManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("speed", "بدء تحدي الـ 7 ثواني (Minecraft Edition)")
                .addOptions(new OptionData(OptionType.STRING, "difficulty", "الصعوبة", true)
                        .addChoice("سهل (Easy)", "easy")
                        .addChoice("متوسط (Medium)", "medium")
                        .addChoice("صعب (Hard)", "hard"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));
    }

    private byte[] generateSpeedImage(String text) throws Exception {
        BufferedImage bg = ImageIO.read(SpeedChallengeCommand.class.getResourceAsStream("/type.png"));
        Graphics2D g = bg.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        Font pixelFont;
        try {
            pixelFont = Font.createFont(Font.TRUETYPE_FONT, SpeedChallengeCommand.class.getResourceAsStream("/minecraft_arabic.ttf")).deriveFont(60f);
        } catch (Exception e) {
            pixelFont = new Font("Arial", Font.BOLD, 60);
        }
        g.setFont(pixelFont);
        g.setColor(new Color(5, 18, 59)); // Deep dark blue as requested in welcome card, or default black/white depending on image. Let's use #05123B
        
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        int boxX = 946;
        int boxY = 513;
        int boxW = 1532 - 946;
        int boxH = 641 - 513;
        
        int x = boxX + (boxW - textWidth) / 2;
        int y = boxY + ((boxH - textHeight) / 2) + fm.getAscent();
        
        g.drawString(text, x, y);
        g.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bg, "png", baos);
        return baos.toByteArray();
    }

    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("speed")) return;

        String difficulty = event.getOption("difficulty") != null ? event.getOption("difficulty").getAsString() : "easy";
        int reward = difficulty.equals("easy") ? 35 : difficulty.equals("medium") ? 55 : 70;

        String word = MINECRAFT_WORDS.get(random.nextInt(MINECRAFT_WORDS.size()));
        String body = "أسرع شخص يكتب الكلمة التالية يربح **" + reward + " opex**!\n\nالكلمة هي:\n**" + word + "**";

        // LOGGING
        String logDetails = String.format("### ⚡ تحدي السرعة: بدء التحدي\n▫️ **المنظم:** %s\n▫️ **الكلمة:** %s\n▫️ **الجائزة:** %d opex", 
                event.getMember().getAsMention(), word, reward);
        logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("speed", logDetails, event.getMember(), null, null, EmbedUtil.INFO));

        
        byte[] imgBytes = null;
        try {
            imgBytes = generateSpeedImage(word);
        } catch (Exception e) {
            e.printStackTrace();
        }

        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setEmbeds(
            new EmbedBuilder().setImage("attachment://speed_7_sec.png").build(),
            new EmbedBuilder()
                .setTitle("SPEED")
                .setDescription("تحدي الـ 7 ثواني!\n\n" + body)
                .setColor(EmbedUtil.INFO)
                .setImage("attachment://type.png")
                .build()
        );
        if (imgBytes != null) {
            builder.setFiles(
                FileUpload.fromData(SpeedChallengeCommand.class.getResourceAsStream("/images/speed_7_sec.png"), "speed_7_sec.png"),
                FileUpload.fromData(imgBytes, "type.png")
            );
        }

        event.reply(builder.build()).queue(hook -> {

            
            long startTime = System.currentTimeMillis();
            java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
            
            net.dv8tion.jda.api.hooks.ListenerAdapter listener = new net.dv8tion.jda.api.hooks.ListenerAdapter() {
                @Override
                public void onMessageReceived(net.dv8tion.jda.api.events.message.MessageReceivedEvent msgEvent) {
                    if (finished.get() || msgEvent.getAuthor().isBot() || !msgEvent.getChannel().equals(event.getChannel())) return;
                    
                    String content = msgEvent.getMessage().getContentRaw().trim();
                    if (content.equalsIgnoreCase(word) || content.replace("أ", "ا").replace("ة", "ه").equalsIgnoreCase(word.replace("أ", "ا").replace("ة", "ه"))) {
                        long timeTaken = System.currentTimeMillis() - startTime;
                        if (timeTaken <= 7000) {
                            finished.set(true);
                            msgEvent.getMessage().reply(EmbedUtil.createBrandedMessage(com.integrafty.opexy.utils.EmbedUtil.containerBranded("SPEED", "فائز بالتحدي!", 
                                            "مبروك <@" + msgEvent.getAuthor().getId() + ">! لقد كتبت الكلمة بسرعة خارقة وربحت **" + reward + " opex**!\n\nالوقت: **" + (timeTaken / 1000.0) + " ثانية**", 
                                            com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN)).build())
                                    .useComponentsV2(true).queue();
                            
                            achievementService.updateStats(msgEvent.getAuthor().getIdLong(), event.getGuild(), stats -> {
                                stats.setSpeedWins(stats.getSpeedWins() + 1);
                            });

                            String logWin = String.format("### فعالية السرعة: فوز\n▫️ **الفائز:** <@%s>\n▫️ **الكلمة:** `%s`\n▫️ **الجائزة:** %d opex",
                                    msgEvent.getAuthor().getId(), word, reward);
                            logManager.logEmbed(event.getGuild(), com.integrafty.opexy.service.LogManager.LOG_GAMES, 
                                    com.integrafty.opexy.utils.EmbedUtil.createOldLogEmbed("speed", logWin, null, msgEvent.getMember(), null, com.integrafty.opexy.utils.EmbedUtil.SUCCESS));

                            event.getJDA().removeEventListener(this);
                        }
                    }
                }
            };

            event.getJDA().addEventListener(listener);

            event.getChannel().sendMessage("...").queueAfter(7, TimeUnit.SECONDS, msg -> {
                event.getJDA().removeEventListener(listener);
                msg.delete().queue();
                if (!finished.get()) {
                    event.getChannel().sendMessage(EmbedUtil.createBrandedMessage(com.integrafty.opexy.utils.EmbedUtil.containerBranded("SPEED", "انتهى الوقت!", 
                                    "للأسف، لم يتمكن أحد من كتابة الكلمة في الوقت المحدد. حظاً أوفر في المرة القادمة!", 
                                    com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN)).build())
                            .useComponentsV2(true).queue();

                    String logTimeout = String.format("### ⚡ تحدي السرعة: انتهى الوقت\n▫️ **الكلمة:** %s\n▫️ لم يفز أحد.", word);
                    logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                            EmbedUtil.createOldLogEmbed("speed", logTimeout, null, null, null, EmbedUtil.DANGER));
                }
            });
        });
    }
}
