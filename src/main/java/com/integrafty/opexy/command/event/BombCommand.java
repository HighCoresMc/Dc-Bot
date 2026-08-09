package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.service.event.BombManager;
import com.integrafty.opexy.service.event.EventManager;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BombCommand implements MultiSlashCommand {

        private final EventManager eventManager;
        private final BombManager bombManager;

        public BombCommand(EventManager eventManager, BombManager bombManager) {
                this.eventManager = eventManager;
                this.bombManager = bombManager;
        }

        @Override
        public List<SlashCommandData> getCommandDataList() {
                return List.of(Commands.slash("bomb", "بدء لعبة تفكيك القنبلة (فردية)")
                                .addOptions(new OptionData(OptionType.STRING, "difficulty", "درجة الصعوبة", true)
                                                .addChoice("سهل (15 ثانية - 10 opex)", "EASY")
                                                .addChoice("وسط (10 ثواني - 15 opex)", "MEDIUM")
                                                .addChoice("صعب (5 ثواني - 20 opex)", "HARD")));
        }

        @Override
        public void execute(SlashCommandInteractionEvent event) {
                if (!event.getName().equals("bomb"))
                        return;

                String diffStr = event.getOption("difficulty").getAsString();
                BombManager.Difficulty difficulty = BombManager.Difficulty.valueOf(diffStr);
                long userId = event.getUser().getIdLong();

                String sessionId = event.getId();
                String hint = bombManager.startBomb(sessionId, userId, difficulty, event.getGuild(), event.getMember());

                String body = String.format("🆘 **تحذير!** تم زرع قنبلة موقوتة خاصة بك يا %s!\n" +
                                "يجب قطع سلك واحد لإبطال مفعولها. هناك سلك واحد صحيح والبقية ستفجر المكان!\n\n" +
                                "🔍 **التلميح:** `%s`\n\n" +
                                "💰 الجائزة: **%d opex**", event.getUser().getAsMention(), hint, difficulty.reward);

                String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, difficulty.seconds);
                String fullBody = timerFormat + "\n\n" + body;

                event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("DEFUSAL", "⚠️ قنبلة الوقت!", fullBody,
                                                EmbedUtil.BANNER_MAIN,
                                                ActionRow.of(
                                                                Button.danger("wire_red_" + sessionId, "السلك الأحمر")
                                                                                .withEmoji(Emoji.fromUnicode("🔴")),
                                                                Button.primary("wire_blue_" + sessionId, "السلك الأزرق")
                                                                                .withEmoji(Emoji.fromUnicode("🔵")),
                                                                Button.success("wire_green_" + sessionId, "السلك الأخضر")
                                                                                .withEmoji(Emoji.fromUnicode("🟢")),
                                                                Button.secondary("wire_yellow_" + sessionId,
                                                                                "السلك الأصفر")
                                                                                .withEmoji(Emoji.fromUnicode("🟡")),
                                                                Button.secondary("wire_purple_" + sessionId,
                                                                                "السلك البنفسجي")
                                                                                .withEmoji(Emoji.fromUnicode("🟣"))))).build()).queue(hook -> bombManager.initTimer(sessionId, difficulty, event));
        }
}
