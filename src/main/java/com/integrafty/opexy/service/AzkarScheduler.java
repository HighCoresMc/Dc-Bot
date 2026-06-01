package com.integrafty.opexy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzkarScheduler {

    private final JDA jda;
    private final AzkarService azkarService;

    private static final String AZKAR_CHANNEL_ID = "1511014781081882805";

    // Periodic Task
    @Scheduled(cron = "0 */30 * * * *")
    public void sendPeriodicZikr() {
        try {
            TextChannel channel = jda.getTextChannelById(AZKAR_CHANNEL_ID);
            if (channel == null) {
                log.warn("Azkar channel {} not found.", AZKAR_CHANNEL_ID);
                return;
            }

            String zikr = azkarService.getRandomHourlyZikr();
            List<ContainerChildComponent> layout = new ArrayList<>();
            layout.add(TextDisplay.of("## 📿 ذِكْـرٌ وَتَـسْـبِـيحٌ\n\n" + zikr));
            Container container = Container.of(layout);

            channel.sendMessageComponents(container).useComponentsV2(true).queue();
            log.info("Successfully sent periodic zikr.");
        } catch (Exception e) {
            log.error("Failed to send periodic zikr", e);
        }
    }

    // Morning Task
    @Scheduled(cron = "0 0 6 * * *")
    public void sendMorningPanel() {
        try {
            TextChannel channel = jda.getTextChannelById(AZKAR_CHANNEL_ID);
            if (channel == null) {
                log.warn("Azkar channel {} not found.", AZKAR_CHANNEL_ID);
                return;
            }

            List<ContainerChildComponent> layout = new ArrayList<>();
            layout.add(TextDisplay.of("### 🌅 أذكار الصباح\n\n**﴿وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ﴾**\n\nحان الآن موعد أذكار الصباح. اضغط على الزر أدناه لبدء قراءة الأذكار تفاعلياً وكسب الأجر الجزيـل."));
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.add(ActionRow.of(Button.success("azkar_start_morning", "قراءة أذكار الصباح")));
            Container container = Container.of(layout);

            channel.sendMessageComponents(container).useComponentsV2(true).queue();
            log.info("Successfully sent morning azkar panel.");
        } catch (Exception e) {
            log.error("Failed to send morning azkar panel", e);
        }
    }

    // Evening Task
    @Scheduled(cron = "0 0 17 * * *")
    public void sendEveningPanel() {
        try {
            TextChannel channel = jda.getTextChannelById(AZKAR_CHANNEL_ID);
            if (channel == null) {
                log.warn("Azkar channel {} not found.", AZKAR_CHANNEL_ID);
                return;
            }

            List<ContainerChildComponent> layout = new ArrayList<>();
            layout.add(TextDisplay.of("### 🌇 أذكار المساء\n\n**﴿وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ غُرُوبِ الشَّمْسِ﴾**\n\nحان الآن موعد أذكار المساء. اضغط على الزر أدناه لبدء قراءة الأذكار تفاعلياً وكسب الأجر الجزيـل."));
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.add(ActionRow.of(Button.success("azkar_start_evening", "قراءة أذكار المساء")));
            Container container = Container.of(layout);

            channel.sendMessageComponents(container).useComponentsV2(true).queue();
            log.info("Successfully sent evening azkar panel.");
        } catch (Exception e) {
            log.error("Failed to send evening azkar panel", e);
        }
    }
}
