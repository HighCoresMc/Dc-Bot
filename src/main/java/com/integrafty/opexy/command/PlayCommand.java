package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.SlashCommand;
import com.integrafty.opexy.service.YouTubeAudioService;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

// Section: YouTube Playback Command
@Component
@lombok.RequiredArgsConstructor
public class PlayCommand extends ListenerAdapter implements SlashCommand {
    private final YouTubeAudioService youtubeAudioService;

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("play", "تشغيل مقطع يوتيوب كصوت في الروم الصوتي")
                .addOption(OptionType.STRING, "link", "رابط مقطع اليوتيوب", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!hasPlayRole(event.getMember())) {
            event.reply("❌ لا تملك الصلاحية الكافية لاستخدام هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;

        AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null) {
            event.reply("❌ يجب أن تكون في روم صوتي ليتمكن البوت من الدخول والتشغيل.").setEphemeral(true).queue();
            return;
        }

        String link = event.getOption("link").getAsString();
        event.deferReply().queue();

        youtubeAudioService.loadTrack(link).thenAccept(track -> {
            youtubeAudioService.play(guild, channel, track);
            
            String body = "▶️ **تشغيل مقطوعة:** " + track.getInfo().title + "\n" +
                          "🔗 **الرابط:** " + track.getInfo().uri + "\n" +
                          "👤 **بواسطة:** " + event.getUser().getAsMention();
            
            ActionRow row = ActionRow.of(
                Button.danger("play_stop", "إيقاف التشغيل"),
                Button.primary("play_change", "تغيير المقطع")
            );
            
            Container container = EmbedUtil.containerBranded("MUSIC", "مشغل اليوتيوب", body, EmbedUtil.BANNER_MAIN, row);
            
            event.getHook().editOriginal(new MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();
        }).exceptionally(ex -> {
            Container errorContainer = EmbedUtil.error("خطأ", "فشل تحميل المقطع من يوتيوب. تأكد من صحة الرابط.");
            event.getHook().editOriginal(new MessageEditBuilder()
                    .setComponents(errorContainer)
                    .useComponentsV2(true)
                    .build()).queue();
            return null;
        });
    }

    // Section: Button and Modal Listeners
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("play_stop")) {
            if (!hasPlayRole(event.getMember())) {
                event.reply("❌ لا تملك الصلاحية الكافية للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            Guild guild = event.getGuild();
            if (guild != null) {
                youtubeAudioService.stop(guild);
                Container stopContainer = EmbedUtil.containerBranded("MUSIC", "إيقاف التشغيل", "⏹️ تم إيقاف التشغيل ومغادرة الروم الصوتي.", EmbedUtil.BANNER_MAIN);
                event.editMessage(new MessageEditBuilder()
                        .setComponents(stopContainer)
                        .useComponentsV2(true)
                        .build()).queue();
            }
        } else if (id.equals("play_change")) {
            if (!hasPlayRole(event.getMember())) {
                event.reply("❌ لا تملك الصلاحية الكافية للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            TextInput linkInput = TextInput.create("play_link", TextInputStyle.SHORT)
                    .setPlaceholder("https://www.youtube.com/watch?v=...")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_play_change", "تغيير مقطع التشغيل")
                    .addComponents(Label.of("رابط يوتيوب الجديد", linkInput))
                    .build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("modal_play_change")) {
            if (!hasPlayRole(event.getMember())) {
                event.reply("❌ لا تملك الصلاحية الكافية للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            Guild guild = event.getGuild();
            if (guild == null) return;
            
            String newLink = event.getValue("play_link").getAsString();
            AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
            if (channel == null) {
                Member member = event.getMember();
                if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                    channel = member.getVoiceState().getChannel();
                }
            }
            
            if (channel == null) {
                event.reply("❌ يجب أن تكون في روم صوتي ليتمكن البوت من الدخول والتشغيل.").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();
            
            final AudioChannel finalChannel = channel;
            youtubeAudioService.loadTrack(newLink).thenAccept(track -> {
                youtubeAudioService.play(guild, finalChannel, track);
                
                String body = "▶️ **تشغيل مقطوعة:** " + track.getInfo().title + "\n" +
                              "🔗 **الرابط:** " + track.getInfo().uri + "\n" +
                              "👤 **بواسطة:** " + event.getUser().getAsMention();
                
                ActionRow row = ActionRow.of(
                    Button.danger("play_stop", "إيقاف التشغيل"),
                    Button.primary("play_change", "تغيير المقطع")
                );
                
                Container container = EmbedUtil.containerBranded("MUSIC", "مشغل اليوتيوب", body, EmbedUtil.BANNER_MAIN, row);
                
                event.getHook().editOriginal(new MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            }).exceptionally(ex -> {
                event.getHook().sendMessage("❌ فشل تحميل المقطع من يوتيوب. تأكد من صحة الرابط.").setEphemeral(true).queue();
                return null;
            });
        }
    }

    // Section: Permissions Helper
    private boolean hasPlayRole(Member member) {
        if (member == null) return false;
        return member.getRoles().stream().anyMatch(role -> role.getId().equals("1487152572207861870"));
    }
}
