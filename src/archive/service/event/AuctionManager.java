package com.integrafty.opexy.service.event;

import com.integrafty.opexy.entity.UserStats;
import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AuctionManager extends ListenerAdapter {

    private final AchievementService achievementService;
    private final EventManager eventManager;
    private final EconomyService economyService;
    private final LogManager logManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AuctionManager(AchievementService achievementService, EventManager eventManager, EconomyService economyService, LogManager logManager) {
        this.achievementService = achievementService;
        this.eventManager = eventManager;
        this.economyService = economyService;
        this.logManager = logManager;
    }

    private long currentHighestBid = 0;
    private long highestBidderId = 0;
    private String currentPrize = "📦 صندوق عشوائي";
    private int durationSeconds = 30;
    private long targetPrice = 0;
    private ScheduledFuture<?> endTask = null;
    private String activeMessageId = null;
    private String guildId = null;
    private String targetRoleId = null;

    public void startAuction(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, net.dv8tion.jda.api.entities.Guild guild, net.dv8tion.jda.api.entities.Member organizer, String prize, String targetRoleId, int duration, long targetPrice) {
        this.currentPrize = prize;
        this.targetRoleId = targetRoleId;
        this.durationSeconds = duration > 0 ? duration : 30;
        this.targetPrice = targetPrice;
        this.currentHighestBid = 0;
        this.highestBidderId = 0;
        this.guildId = guild.getId();

        log.info("[Auction] Starting auction in channel {} for {} seconds. Prize: {}", channel.getName(), durationSeconds, currentPrize);
        
        // LOGGING
        String logDetails = String.format("### 🏆 فعالية المزاد: بدء المزاد\n▫️ **المنظم:** %s\n▫️ **الجائزة:** %s\n▫️ **المدة:** %d ثانية", 
                organizer.getAsMention(), prize, durationSeconds);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("auction", logDetails, organizer, null, null, EmbedUtil.INFO));

        if (endTask != null) endTask.cancel(false);
        endTask = scheduler.schedule(() -> {
            log.info("[Auction] Timer expired for auction in channel {}", channel.getName());
            finishAuction(channel);
        }, durationSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("bid_")) return;

        long userId = event.getUser().getIdLong();
        long bidAmount = 0;

        String id = event.getComponentId();
        if (id.equals("bid_10")) bidAmount = 10;
        else if (id.equals("bid_50")) bidAmount = 50;
        else if (id.equals("bid_100")) bidAmount = 100;
        else if (id.equals("bid_custom")) {
            TextInput bidInput = TextInput.create("bid_val", TextInputStyle.SHORT)
                    .setPlaceholder("أدخل المبلغ هنا (مثال: 500)")
                    .setRequired(true)
                    .build();

            event.replyModal(Modal.create("auction_custom_modal", "مزايدة مخصصة")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("سعر المزايدة", bidInput))
                    .build()).queue();
            return;
        }

        processBid(event, userId, bidAmount);
    }

    private void processBid(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, long userId, long bidAmount) {
        long newTotal = currentHighestBid + bidAmount;
        String currentGuildId = event.getGuild().getId();
        
        // 1. Check Money
        long balance = economyService.getBalance(String.valueOf(userId), currentGuildId);
        if (balance < newTotal) {
            event.reply("❌ عذراً، رصيدك غير كافٍ للمزايدة! (رصيدك: " + balance + " opex)").setEphemeral(true).queue();
            return;
        }

        // 2. Refund Previous Bidder (if exists)
        if (highestBidderId != 0 && currentHighestBid > 0) {
            economyService.addBalance(String.valueOf(highestBidderId), currentGuildId, currentHighestBid);
        }

        // 3. Deduct New Bidder
        economyService.subtractBalance(String.valueOf(userId), currentGuildId, newTotal);

        currentHighestBid = newTotal;
        highestBidderId = userId;
        this.guildId = currentGuildId;

        // Update stats
        achievementService.updateStats(userId, event.getGuild(), stats -> {
            if (newTotal > stats.getMaxBid()) stats.setMaxBid(newTotal);
        });

        // 2. Check Target Price
        if (targetPrice > 0 && newTotal >= targetPrice) {
            if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback mec) mec.deferEdit().queue();
            finishAuction(event.getMessageChannel());
            return;
        }

        resetTimer(event.getMessageChannel());

        // Update Message
        String body = "تم بدء مزاد على **جائزة غامضة**! 📦\n\n**القوانين:**\n• المزايدة تبدأ بـ 10 opex.\n• المزايدة الأعلى تفوز بالمحتوى.\n\n" +
                      "👤 المزايد الحالي: <@" + userId + ">\n💰 أعلى سعر: **" + newTotal + " opex**";

        net.dv8tion.jda.api.utils.messages.MessageEditBuilder reply = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏆 المزاد الأعمى — Blind Auction", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_10", "+10"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_50", "+50"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("bid_100", "+100"),
                                net.dv8tion.jda.api.components.buttons.Button.success("bid_custom", "سعر مخصص ✏️")
                        )))
                .useComponentsV2(true);

        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback mec) {
            mec.editMessage(reply.build()).useComponentsV2(true).queue();
        }
    }

    @Override
    public void onModalInteraction(net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
        if (!event.getModalId().equals("auction_custom_modal")) return;
        
        try {
            long bidAmount = Long.parseLong(event.getValue("bid_val").getAsString());
            if (bidAmount <= 0) {
                event.reply("❌ يجب أن يكون المبلغ أكبر من 0!").setEphemeral(true).queue();
                return;
            }
            processBid(event, event.getUser().getIdLong(), bidAmount);
        } catch (NumberFormatException e) {
            event.reply("❌ الرجاء إدخال رقم صحيح.").setEphemeral(true).queue();
        }
    }

    private void resetTimer(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
        if (endTask != null) endTask.cancel(false);
        
        endTask = scheduler.schedule(() -> {
            finishAuction(channel);
        }, durationSeconds, TimeUnit.SECONDS);
    }

    private void finishAuction(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
        log.info("[Auction] Finishing auction for {}...", channel.getName());
        try {
            eventManager.endGroupEvent();
            
            if (highestBidderId != 0 && guildId != null) {
                if (channel instanceof net.dv8tion.jda.api.entities.channel.middleman.GuildChannel gc) {
                    // Update stats
                    achievementService.updateStats(highestBidderId, gc.getGuild(), stats -> {
                        stats.setSuccessBids(stats.getSuccessBids() + 1);
                    });

                    // GIVE ROLE IF SET
                    if (targetRoleId != null) {
                        net.dv8tion.jda.api.entities.Role role = gc.getGuild().getRoleById(targetRoleId);
                        if (role != null) {
                            gc.getGuild().addRoleToMember(net.dv8tion.jda.api.entities.User.fromId(highestBidderId), role).queue();
                        }
                    }

                    // GIVE CURRENCY IF PRIZE IS OPEX
                    try {
                        String cleanPrize = currentPrize.toLowerCase().replace(",", "");
                        if (cleanPrize.contains("opex")) {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)(\\s*k)?").matcher(cleanPrize);
                            if (m.find()) {
                                long amount = Long.parseLong(m.group(1));
                                if (m.group(2) != null) amount *= 1000;
                                economyService.addBalance(String.valueOf(highestBidderId), guildId, amount);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[Auction] Failed to parse currency prize: {}", currentPrize);
                    }
                }
            }

            String displayedPrize = currentPrize;
            String prizeLower = currentPrize.toLowerCase();
            if (prizeLower.contains("صندوق عشوائي") || prizeLower.contains("صندوق غامض") || prizeLower.contains("random box") || prizeLower.contains("mystery box")) {
                String[] randomItems = {
                    "<:sand:1500879926696607846> (1x Sand)", 
                    "<:stick:1500879473992794212> (1x Stick)", 
                    "<:coble_stone:1500879838041608243> (1x Cobblestone)", 
                    "<:feather:1500880285431238807> (1x Feather)",
                    "<:Minecraft_Gunpowder_pngremovebgp:1500879430367707366> (1x Gunpowder)",
                    "<:string:1500880235510497360> (1x String)",
                    "<:leather:1500880346206568498> (1x Leather)",
                    "<:water_empty_bottle:1500880709315723428> (1x Glass Bottle)"
                };
                displayedPrize = "📦 صندوق عشوائي ➔ (" + randomItems[new java.util.Random().nextInt(randomItems.length)] + ")";
            }

            String body = highestBidderId != 0 ? 
                "الفائز هو <@" + highestBidderId + "> بسعر **" + currentHighestBid + " opex**!\n\n**الجائزة:** " + displayedPrize + "\n\nمبروك للفائز!" :
                "انتهى المزاد دون وجود أي مزايدات.";
            
            channel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("AUCTION", "🏁 انتهى المزاد!", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                    .useComponentsV2(true).build())
                    .useComponentsV2(true).queue();

            // LOGGING
            String winnerMention = highestBidderId != 0 ? "<@" + highestBidderId + ">" : "لا يوجد";
            String logDetails = String.format("### 🏆 فعالية المزاد: انتهى المزاد\n▫️ **الفائز:** %s\n▫️ **السعر النهائي:** %d opex\n▫️ **الجائزة:** %s", 
                    winnerMention, currentHighestBid, currentPrize);
            logManager.logEmbed(channel instanceof net.dv8tion.jda.api.entities.channel.middleman.GuildChannel ? ((net.dv8tion.jda.api.entities.channel.middleman.GuildChannel)channel).getGuild() : null, 
                    LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("auction", logDetails, null, null, null, EmbedUtil.SUCCESS));

        } catch (Exception e) {
            log.error("[Auction] Error finishing auction: ", e);
        } finally {
            // Reset state
            currentHighestBid = 0;
            highestBidderId = 0;
            guildId = null;
            targetRoleId = null;
            if (endTask != null) endTask.cancel(false);
            log.info("[Auction] Auction state reset.");
        }
    }

    public void stopAuction() {
        if (endTask != null) endTask.cancel(true);
        
        // REFUND CURRENT BIDDER
        if (highestBidderId != 0 && guildId != null && currentHighestBid > 0) {
            economyService.addBalance(String.valueOf(highestBidderId), guildId, currentHighestBid);
        }

        eventManager.endGroupEvent();
        this.currentHighestBid = 0;
        this.highestBidderId = 0;
        this.activeMessageId = null;
        this.guildId = null;
    }
}
