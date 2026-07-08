package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.event.AchievementService;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShopCommand extends ListenerAdapter implements SlashCommand {

    private final EconomyService economyService;
    private final AchievementService achievementService;

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("shop", "فتح متجر أوبكس لشراء ميزات الألعاب");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String body = """
                مرحباً بك في متجر أوبكس التفاعلي
                هنا يمكنك شراء ميزات خاصة تساعدك في الفعاليات والألعاب.
                <divider>
                **درع المافيا (Mafia Shield)**
                يحميك من هجوم المافيا لمرة واحدة.
                **السعر:** 5,000 opex
                
                **مضاعف الجوائز (2x Reward)**
                يضاعف جائزتك القادمة في أي فعالية تفوز بها.
                **السعر:** 15,000 opex
                
                **جاوب جوابين (Double Answer)**
                يمنحك محاولة إضافية في جولة.
                **السعر:** 2,000 opex
                
                **الحفرة (The Pit)**
                تخصم نقاط من الفريق الآخر في جولة.
                **السعر:** 3,500 opex
                
                **اعكس الدور (Reverse Turn)**
                تغيير دور الفريق في جولة.
                **السعر:** 1,500 opex
                
                **السؤال الذهبي (Golden Question)**
                مضاعفة نقاط السؤال الحالي في جولة.
                **السعر:** 4,000 opex
                """;

        event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("SHOP", "Opexy Shop", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(
                                Button.primary("buy_shield", "درع المافيا"),
                                Button.success("buy_double", "مضاعف الجوائز"),
                                Button.secondary("buy_j_double", "جاوب جوابين")
                        ),
                        ActionRow.of(
                                Button.secondary("buy_j_pit", "الحفرة"),
                                Button.secondary("buy_j_reverse", "عكس الدور"),
                                Button.secondary("buy_j_golden", "السؤال الذهبي")
                        )))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("buy_")) return;

        long userId = event.getUser().getIdLong();
        String guildId = event.getGuild().getId();
        long balance = economyService.getBalance(String.valueOf(userId), guildId);

        if (id.equals("buy_shield")) {
            handlePurchase(event, userId, guildId, balance, 5000, "درع المافيا", stats -> stats.setShieldCount(stats.getShieldCount() + 1));
        } 
        else if (id.equals("buy_double")) {
            if (balance < 15000) {
                event.reply("❌ رصيدك غير كافٍ! تحتاج إلى 15,000 opex.").setEphemeral(true).queue();
                return;
            }
            achievementService.updateStats(userId, event.getGuild(), stats -> {
                if (stats.isDoubleRewardActive()) {
                    event.reply("⚠️ لديك مضاعف نشط بالفعل!").setEphemeral(true).queue();
                    return;
                }
                economyService.subtractBalance(String.valueOf(userId), guildId, 15000);
                stats.setDoubleRewardActive(true);
                event.reply("✅ تم شراء **مضاعف الجوائز** بنجاح!").setEphemeral(true).queue();
            });
        }
        else if (id.equals("buy_j_double")) {
            handlePurchase(event, userId, guildId, balance, 2000, "جاوب جوابين", stats -> stats.setJawlahDoubleAnswer(stats.getJawlahDoubleAnswer() + 1));
        }
        else if (id.equals("buy_j_pit")) {
            handlePurchase(event, userId, guildId, balance, 3500, "الحفرة", stats -> stats.setJawlahPit(stats.getJawlahPit() + 1));
        }
        else if (id.equals("buy_j_reverse")) {
            handlePurchase(event, userId, guildId, balance, 1500, "اعكس الدور", stats -> stats.setJawlahReverse(stats.getJawlahReverse() + 1));
        }
        else if (id.equals("buy_j_golden")) {
            handlePurchase(event, userId, guildId, balance, 4000, "السؤال الذهبي", stats -> stats.setJawlahGolden(stats.getJawlahGolden() + 1));
        }
    }

    private void handlePurchase(ButtonInteractionEvent event, long userId, String guildId, long balance, int price, String itemName, java.util.function.Consumer<com.integrafty.opexy.entity.UserStats> update) {
        if (balance < price) {
            event.reply("❌ رصيدك غير كافٍ! تحتاج إلى " + price + " opex.").setEphemeral(true).queue();
            return;
        }
        economyService.subtractBalance(String.valueOf(userId), guildId, price);
        achievementService.updateStats(userId, event.getGuild(), update);
        event.reply("✅ تم شراء **" + itemName + "** بنجاح!").setEphemeral(true).queue();
    }
}
