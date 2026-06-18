package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import net.dv8tion.jda.api.JDA;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

@Service
@RequiredArgsConstructor
public class ScavengerHuntManager extends ListenerAdapter {

    private final JDA jda;
    private final EventManager eventManager;
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;
    private final Random random = new Random();

    private String activeCode = null;
    private String activeCodeBackup = null;
    private long reward = 5000;
    private String codeMessageId = null;
    private net.dv8tion.jda.api.entities.channel.concrete.TextChannel huntChannel = null;
    private java.util.concurrent.ScheduledFuture<?> huntTimer = null;
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
    }

    private String generateCode() {
        return "OP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public String startHunt(long rewardAmount, net.dv8tion.jda.api.entities.Guild guild, net.dv8tion.jda.api.entities.Member organizer, net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel) {
        String code = generateCode();
        this.activeCode = code;
        this.activeCodeBackup = code;
        this.reward = rewardAmount;
        this.huntChannel = channel;
        this.codeMessageId = null;

        MessageCreateAction action = channel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("DISCOVERY", "🔍 لقد وجدت شيئاً!",
                        "لقد عثرت على الكود السري! الكود هو: **" + code + "**\nأسرع واكتبه لتفوز!",
                        com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build());
                
        action.queue(msg -> {
            this.codeMessageId = msg.getId();
        });

        huntTimer = scheduler.schedule(() -> {
            if (activeCode != null) {
                if (huntChannel != null) {
                    huntChannel.sendMessage("⏰ **انتهى الوقت!** لم يقم أحد بكتابة الكود.").queue();
                }
                stopHunt();
            }
        }, 3, TimeUnit.MINUTES);

        String logDetails = String.format("### 🔎 فعالية الصيد: بدء الفعالية\n▫️ **المنظم:** %s\n▫️ **الجائزة:** %d opex\n▫️ **الكود:** `%s`\n▫️ **القناة:** %s", 
                organizer.getAsMention(), rewardAmount, code, channel.getAsMention());
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("scavenger-hunt", logDetails, organizer, null, null, EmbedUtil.INFO));
                
        return activeCode;
    }

    public void stopHunt() {
        this.activeCode = null;
        if (huntTimer != null) {
            huntTimer.cancel(false);
        }
        eventManager.endGroupEvent();
        if (huntChannel != null && codeMessageId != null) {
            huntChannel.deleteMessageById(codeMessageId).queue(null, e -> {});
        }
        this.codeMessageId = null;
        this.huntChannel = null;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || activeCode == null) return;

        String content = event.getMessage().getContentRaw().trim().toUpperCase();
        if (content.equals(activeCode)) {
            long rewardWon = reward; 
            stopHunt(); 

            event.getMessage().reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("WINNER", "🏆 فائز بفعالية الصيد!", 
                            "مبروك <@" + event.getAuthor().getId() + ">! لقد عثرت على الكود السري أولاً وربحت **" + rewardWon + " opex**!", 
                            com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                    .useComponentsV2(true).build())
                    .queue();

            String logWin = String.format("### 🔎 فعالية الصيد: فوز\n▫️ **الفائز:** <@%s>\n▫️ **الكود:** `%s`\n▫️ **الجائزة:** %d opex", 
                    event.getAuthor().getId(), activeCodeBackup, rewardWon);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("scavenger-hunt", logWin, null, event.getMember(), null, EmbedUtil.SUCCESS));

            achievementService.updateStats(event.getAuthor().getIdLong(), event.getGuild(), stats -> {
            });
        }
    }
}
