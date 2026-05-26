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
    private final com.integrafty.opexy.listener.VoiceRecordingListener voiceRecordingListener;
    public static final java.util.Map<Long, ActiveTrackInfo> activeTracks = new java.util.concurrent.ConcurrentHashMap<>();

    public static class ActiveTrackInfo {
        public final String title;
        public final String uri;
        public final String requesterMention;
        public ActiveTrackInfo(String title, String uri, String requesterMention) {
            this.title = title;
            this.uri = uri;
            this.requesterMention = requesterMention;
        }
    }

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
            event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة لتشغيل هذا الأمر.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;

        if (voiceRecordingListener.isRecordingActive(guild.getIdLong())) {
            event.reply("⚠️ عذراً، لا يمكن تشغيل الموسيقى أثناء وجود تسجيل نشط. يرجى إيقاف التسجيل أولاً.").setEphemeral(true).queue();
            return;
        }

        AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (channel == null) {
            event.reply("🔊 يرجى الانضمام إلى قناة صوتية أولاً حتى يتمكن البوت من مشاركتك الاستماع.").setEphemeral(true).queue();
            return;
        }

        String link = event.getOption("link").getAsString();
        event.deferReply().queue();

        youtubeAudioService.loadTrack(link).thenAccept(track -> {
            youtubeAudioService.play(guild, channel, track);
            activeTracks.put(guild.getIdLong(), new ActiveTrackInfo(track.getInfo().title, track.getInfo().uri, event.getUser().getAsMention()));
            
            String body = "🎶 **المشغل الصوتي النشط**\n\n" +
                          "📌 **العنوان:** " + track.getInfo().title + "\n" +
                          "👤 **طلب بواسطة:** " + event.getUser().getAsMention() + "\n" +
                          "🔗 **الرابط المباشر:** [اضغط هنا للمشاهدة](" + track.getInfo().uri + ")";
            
            ActionRow row = ActionRow.of(
                Button.danger("play_stop", "إيقاف مؤقت ⏸️"),
                Button.primary("play_change", "تغيير المقطع 🔄"),
                Button.secondary("play_leave", "مغادرة الروم 🚪")
            );
            
            Container container = EmbedUtil.containerBranded("MUSIC", "مشغل اليوتيوب", body, EmbedUtil.BANNER_MAIN, row);
            
            event.getHook().editOriginal(new MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();
        }).exceptionally(ex -> {
            Container errorContainer = EmbedUtil.containerBranded("SYSTEM", "⚠️ تنبيه النظام", "عذراً، واجهنا صعوبة في تحميل المقطع من يوتيوب. يرجى التحقق من صحة الرابط أو المحاولة لاحقاً.", EmbedUtil.BANNER_MAIN);
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
        if (!id.startsWith("play_")) return;

        if (!hasPlayRole(event.getMember())) {
            event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة للتحكم بالتشغيل.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;

        if (id.equals("play_stop")) {
            youtubeAudioService.pause(guild);
            ActiveTrackInfo info = activeTracks.get(guild.getIdLong());
            String title = (info != null) ? info.title : "مقطع غير معروف";
            String requester = (info != null) ? info.requesterMention : "غير معروف";
            
            String body = "⏸️ **حالة التشغيل: موقوف مؤقتاً**\n\n" +
                          "📌 **العنوان:** " + title + "\n" +
                          "👤 **طلب بواسطة:** " + requester + "\n\n" +
                          "يمكنك استئناف الاستماع، تغيير المقطع الحالي، أو إخراج البوت من الروم الصوتي.";

            ActionRow row = ActionRow.of(
                Button.success("play_resume", "استئناف التشغيل ▶️"),
                Button.primary("play_change", "تغيير المقطع 🔄"),
                Button.danger("play_leave", "مغادرة الروم 🚪")
            );

            Container container = EmbedUtil.containerBranded("MUSIC", "تم إيقاف التشغيل مؤقتاً", body, EmbedUtil.BANNER_MAIN, row);
            event.editMessage(new MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();

        } else if (id.equals("play_resume")) {
            if (voiceRecordingListener.isRecordingActive(guild.getIdLong())) {
                event.reply("⚠️ عذراً، لا يمكن استئناف تشغيل الموسيقى أثناء وجود تسجيل نشط. يرجى إيقاف التسجيل أولاً.").setEphemeral(true).queue();
                return;
            }
            youtubeAudioService.resume(guild);
            ActiveTrackInfo info = activeTracks.get(guild.getIdLong());
            String title = (info != null) ? info.title : "مقطع غير معروف";
            String uri = (info != null) ? info.uri : "#";
            String requester = (info != null) ? info.requesterMention : "غير معروف";

            String body = "🎶 **المشغل الصوتي النشط**\n\n" +
                          "📌 **العنوان:** " + title + "\n" +
                          "👤 **طلب بواسطة:** " + requester + "\n" +
                          "🔗 **الرابط المباشر:** [اضغط هنا للمشاهدة](" + uri + ")";

            ActionRow row = ActionRow.of(
                Button.danger("play_stop", "إيقاف مؤقت ⏸️"),
                Button.primary("play_change", "تغيير المقطع 🔄"),
                Button.secondary("play_leave", "مغادرة الروم 🚪")
            );

            Container container = EmbedUtil.containerBranded("MUSIC", "مشغل اليوتيوب", body, EmbedUtil.BANNER_MAIN, row);
            event.editMessage(new MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();

        } else if (id.equals("play_leave")) {
            youtubeAudioService.stop(guild);
            activeTracks.remove(guild.getIdLong());
            Container stopContainer = EmbedUtil.containerBranded("MUSIC", "إنهاء الجلسة", "🚪 تم إنهاء الجلسة ومغادرة الروم الصوتي بنجاح. شكراً لاستماعكم!", EmbedUtil.BANNER_MAIN);
            event.editMessage(new MessageEditBuilder()
                    .setComponents(stopContainer)
                    .useComponentsV2(true)
                    .build()).queue();

        } else if (id.equals("play_change")) {
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
                event.reply("⚠️ عذراً، لا تملك الصلاحيات اللازمة للتحكم بالتشغيل.").setEphemeral(true).queue();
                return;
            }
            Guild guild = event.getGuild();
            if (guild == null) return;

            if (voiceRecordingListener.isRecordingActive(guild.getIdLong())) {
                event.reply("⚠️ عذراً، لا يمكن تغيير مقطع التشغيل أو تشغيل الموسيقى أثناء وجود تسجيل نشط. يرجى إيقاف التسجيل أولاً.").setEphemeral(true).queue();
                return;
            }
            
            String newLink = event.getValue("play_link").getAsString();
            AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
            if (channel == null) {
                Member member = event.getMember();
                if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
                    channel = member.getVoiceState().getChannel();
                }
            }
            
            if (channel == null) {
                event.reply("🔊 يرجى الانضمام إلى قناة صوتية أولاً حتى يتمكن البوت من الدخول والتشغيل.").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();
            
            final AudioChannel finalChannel = channel;
            youtubeAudioService.loadTrack(newLink).thenAccept(track -> {
                youtubeAudioService.play(guild, finalChannel, track);
                activeTracks.put(guild.getIdLong(), new ActiveTrackInfo(track.getInfo().title, track.getInfo().uri, event.getUser().getAsMention()));
                
                String body = "🎶 **المشغل الصوتي النشط**\n\n" +
                              "📌 **العنوان:** " + track.getInfo().title + "\n" +
                              "👤 **طلب بواسطة:** " + event.getUser().getAsMention() + "\n" +
                              "🔗 **الرابط المباشر:** [اضغط هنا للمشاهدة](" + track.getInfo().uri + ")";
                
                ActionRow row = ActionRow.of(
                    Button.danger("play_stop", "إيقاف مؤقت ⏸️"),
                    Button.primary("play_change", "تغيير المقطع 🔄"),
                    Button.secondary("play_leave", "مغادرة الروم 🚪")
                );
                
                Container container = EmbedUtil.containerBranded("MUSIC", "مشغل اليوتيوب", body, EmbedUtil.BANNER_MAIN, row);
                
                event.getHook().editOriginal(new MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            }).exceptionally(ex -> {
                event.getHook().sendMessage("⚠️ | عذراً، واجهنا صعوبة في تحميل المقطع من يوتيوب. يرجى التحقق من صحة الرابط أو المحاولة لاحقاً.").setEphemeral(true).queue();
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
