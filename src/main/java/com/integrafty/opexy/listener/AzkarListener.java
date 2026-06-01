package com.integrafty.opexy.listener;

import com.integrafty.opexy.service.AzkarService;
import com.integrafty.opexy.service.AzkarService.ZikrItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AzkarListener extends ListenerAdapter {

    private final AzkarService azkarService;

    private static final Set<String> COUNT_CHANNELS = Set.of(
            "1488278004919435335",
            "1487140532965867600",
            "1487142175765430393",
            "1487147783961051156",
            "1487147889980342323",
            "1509895535472152656",
            "1487148105580154890"
    );

    private final ConcurrentHashMap<String, Integer> messageCounters = new ConcurrentHashMap<>();

    // Listeners
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        // Prefix Commands
        String content = event.getMessage().getContentRaw().trim();
        if (content.startsWith("!")) {
            switch (content) {
                case "!ص" -> replyZikrEmbed(event, "سُبْحَانَ اللهِ وَبِحَمْدِهِ، سُبْحَانَ اللهِ الْعَظِيمِ", "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ.");
                case "!س" -> replyZikrEmbed(event, "أَسْتَغْفِرُ اللهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ", "مَنْ لَزِمَ الِاسْتِغْفَارَ جَعَلَ اللَّهُ لَهُ مِنْ كُلِّ ضِيقٍ مَخْرَجًا، وَمِنْ كُلِّ هَمٍّ فَرَجًا، وَرَزَقَهُ مِنْ حَيْثُ لَا يَحْتَسِبُ.");
                case "!ح" -> replyZikrEmbed(event, "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ الْعَلِيِّ الْعَظِيمِ", "كَنْزٌ مِنْ كُنُوزِ الْجَنَّةِ.");
                case "!ت" -> replyZikrEmbed(event, "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", "كَانَتْ لَهُ عَدْلَ عَشْرِ رِقَابٍ، وَكُتِبَتْ لَهُ مِائَةُ حَسَنَةٍ، وَمُحِيَتْ عَنْهُ مِائَةُ سَيِّئَةٍ.");
                case "!ع" -> replyZikrEmbed(event, "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "مَنْ صَلَّى عَلَيَّ صَلَاةً صَلَّى اللَّهُ عَلَيْهِ بِهَا عَشْرًا.");
                case "!الصباح" -> {
                    if (hasNoPermission(event.getMember())) {
                        replyAccessDenied(event);
                    } else {
                        replyMorningPanel(event);
                    }
                }
                case "!المساء" -> {
                    if (hasNoPermission(event.getMember())) {
                        replyAccessDenied(event);
                    } else {
                        replyEveningPanel(event);
                    }
                }
            }
        }

        // Message Counter
        String channelId = event.getChannel().getId();
        if (COUNT_CHANNELS.contains(channelId)) {
            int count = messageCounters.merge(channelId, 1, Integer::sum);
            if (count >= 170) {
                messageCounters.put(channelId, 0);
                sendRandomZikr(event);
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // Navigation Panel
        if (id.equals("azkar_start_morning")) {
            showPage(event, "morning", 0, true);
        } else if (id.equals("azkar_start_evening")) {
            showPage(event, "evening", 0, true);
        } else if (id.startsWith("azkar_nav_morning_next_")) {
            int idx = Integer.parseInt(id.substring("azkar_nav_morning_next_".length()));
            showPage(event, "morning", idx + 1, false);
        } else if (id.startsWith("azkar_nav_morning_prev_")) {
            int idx = Integer.parseInt(id.substring("azkar_nav_morning_prev_".length()));
            showPage(event, "morning", idx - 1, false);
        } else if (id.startsWith("azkar_nav_evening_next_")) {
            int idx = Integer.parseInt(id.substring("azkar_nav_evening_next_".length()));
            showPage(event, "evening", idx + 1, false);
        } else if (id.startsWith("azkar_nav_evening_prev_")) {
            int idx = Integer.parseInt(id.substring("azkar_nav_evening_prev_".length()));
            showPage(event, "evening", idx - 1, false);
        } else if (id.equals("azkar_close")) {
            closeSession(event);
        }

        // Repetition Counter
        if (id.startsWith("azkar_init_rep_")) {
            int target = Integer.parseInt(id.substring("azkar_init_rep_".length()));
            showRepetitionCounter(event, 0, target, true);
        } else if (id.startsWith("azkar_rep_add_")) {
            String[] parts = id.split("_");
            int current = Integer.parseInt(parts[3]);
            int target = Integer.parseInt(parts[4]);
            showRepetitionCounter(event, current + 1, target, false);
        } else if (id.startsWith("azkar_rep_sub_")) {
            String[] parts = id.split("_");
            int current = Integer.parseInt(parts[3]);
            int target = Integer.parseInt(parts[4]);
            showRepetitionCounter(event, current - 1, target, false);
        }
    }

    // Helpers
    private void sendRandomZikr(MessageReceivedEvent event) {
        ZikrItem item = azkarService.getRandomHourlyZikrItem();
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### " + item.getText()));
        if (item.getBenefit() != null && !item.getBenefit().isEmpty()) {
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.add(TextDisplay.of(item.getBenefit()));
        }
        Container container = Container.of(layout);
        event.getChannel().sendMessageComponents(container).useComponentsV2(true).queue();
    }

    private void showPage(ButtonInteractionEvent event, String type, int index, boolean isInitial) {
        List<ZikrItem> list = type.equals("morning") ? azkarService.getMorningAzkar() : azkarService.getEveningAzkar();
        if (list.isEmpty() || index < 0 || index >= list.size()) {
            event.reply(new MessageCreateBuilder().setComponents(com.integrafty.opexy.utils.EmbedUtil.error("خطأ", "حدث خطأ أثناء تحميل الذكر.")).useComponentsV2(true).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
            return;
        }

        ZikrItem item = list.get(index);
        Container container = buildPageContainer(type, index, list.size(), item);

        if (!isInitial) {
            MessageEditBuilder editBuilder = new MessageEditBuilder();
            editBuilder.setComponents(container);
            editBuilder.useComponentsV2(true);
            event.editMessage(editBuilder.build()).useComponentsV2(true).queue();
        } else {
            event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
        }
    }

    private Container buildPageContainer(String type, int index, int total, ZikrItem item) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        String title = type.equals("morning") ? "🌅 أذكار الصباح" : "🌇 أذكار المساء";
        layout.add(TextDisplay.of("### " + title + " (" + (index + 1) + " / " + total + ")\n\n" + item.getText()));

        if (item.getBenefit() != null && !item.getBenefit().isEmpty()) {
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.add(TextDisplay.of(item.getBenefit()));
        }

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("azkar_nav_" + type + "_prev_" + index, "السابق").withDisabled(index == 0));

        if (item.getCount() > 1) {
            buttons.add(Button.secondary("azkar_init_rep_" + item.getCount(), "التكرار"));
        }

        buttons.add(Button.secondary("azkar_nav_" + type + "_next_" + index, "التالي").withDisabled(index == total - 1));
        buttons.add(Button.danger("azkar_close", "إغلاق"));

        layout.add(Separator.createDivider(Spacing.SMALL));
        layout.add(ActionRow.of(buttons));

        return Container.of(layout);
    }

    private void closeSession(ButtonInteractionEvent event) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### 📿 تم إغلاق الأذكار\n\nتقبل الله طاعتكم وصالح أعمالكم."));
        Container container = Container.of(layout);

        MessageEditBuilder editBuilder = new MessageEditBuilder();
        editBuilder.setComponents(container);
        editBuilder.useComponentsV2(true);
        event.editMessage(editBuilder.build()).useComponentsV2(true).queue();
    }

    private void showRepetitionCounter(ButtonInteractionEvent event, int current, int target, boolean isInitial) {
        int finalCurrent = Math.max(0, Math.min(target, current));
        List<ContainerChildComponent> layout = new ArrayList<>();

        if (finalCurrent == target) {
            layout.add(TextDisplay.of("### 📿 عدّاد الأذكار والتكرار\n\nالعداد الحالي: **" + finalCurrent + "** من **" + target + "**\n\n✅ أحسنت! تم إكمال التكرار المطلوب بنجاح. تقبل الله منك."));
        } else {
            layout.add(TextDisplay.of("### 📿 عدّاد الأذكار والتكرار\n\nالعداد الحالي: **" + finalCurrent + "** من **" + target + "**"));
        }

        layout.add(Separator.createDivider(Spacing.SMALL));

        ActionRow row = ActionRow.of(
                Button.primary("azkar_rep_add_" + finalCurrent + "_" + target, "+1").withDisabled(finalCurrent == target),
                Button.danger("azkar_rep_sub_" + finalCurrent + "_" + target, "-1").withDisabled(finalCurrent == 0)
        );
        layout.add(row);
        Container container = Container.of(layout);

        if (!isInitial) {
            MessageEditBuilder editBuilder = new MessageEditBuilder();
            editBuilder.setComponents(container);
            editBuilder.useComponentsV2(true);
            event.editMessage(editBuilder.build()).useComponentsV2(true).queue();
        } else {
            event.reply(new MessageCreateBuilder().setComponents(container).useComponentsV2(true).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
        }
    }

    private void replyZikrEmbed(MessageReceivedEvent event, String zikr, String benefit) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### " + zikr));
        if (benefit != null && !benefit.isEmpty()) {
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.add(TextDisplay.of(benefit));
        }
        Container container = Container.of(layout);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);
        event.getMessage().reply(builder.build()).useComponentsV2(true).queue();
    }

    private void replyMorningPanel(MessageReceivedEvent event) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### 🌅 أذكار الصباح\n\n﴿**وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ**﴾\n\nاضغط على الزر أدناه لبدء قراءة أذكار الصباح تفاعلياً وكسب الأجر الجزيـل."));
        layout.add(Separator.createDivider(Spacing.SMALL));
        layout.add(ActionRow.of(Button.success("azkar_start_morning", "قراءة أذكار الصباح")));
        Container container = Container.of(layout);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);
        event.getMessage().reply(builder.build()).useComponentsV2(true).queue();
    }

    private void replyEveningPanel(MessageReceivedEvent event) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### 🌇 أذكار المساء\n\n\"**وَسَبِّحْ بِحَمْدِ رَبِّكَ قَبْلَ طُلُوعِ الشَّمْسِ وَقَبْلَ الْغُرُوبِ**\"\n\nاضغط على الزر أدناه لبدء قراءة أذكار المساء تفاعلياً وكسب الأجر الجزيـل."));
        layout.add(Separator.createDivider(Spacing.SMALL));
        layout.add(ActionRow.of(Button.success("azkar_start_evening", "قراءة أذكار المساء")));
        Container container = Container.of(layout);
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(container);
        builder.useComponentsV2(true);
        event.getMessage().reply(builder.build()).useComponentsV2(true).queue();
    }

    private boolean hasNoPermission(net.dv8tion.jda.api.entities.Member member) {
        if (member == null) return true;
        return member.getRoles().stream().noneMatch(role -> role.getId().equals("1487195816220430406"));
    }

    private void replyAccessDenied(MessageReceivedEvent event) {
        MessageCreateBuilder builder = new MessageCreateBuilder();
        builder.setComponents(com.integrafty.opexy.utils.EmbedUtil.accessDenied());
        builder.useComponentsV2(true);
        event.getMessage().reply(builder.build()).useComponentsV2(true).queue();
    }
}
