package com.integrafty.opexy.service.event;

import com.integrafty.opexy.utils.EmbedUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class JawlahManager extends ListenerAdapter {

    private final JDA jda;
    private final EventManager eventManager;
    private final com.integrafty.opexy.service.LogManager logManager;
    private final AchievementService achievementService;
    private final com.integrafty.opexy.service.EconomyService economyService;

    private final Map<Long, JawlahGame> activeGames = new ConcurrentHashMap<>();
    private final Map<String, List<JawlahQuestion>> questionBank = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Long, ScheduledFuture<?>> sessionTimers = new HashMap<>();

    private static final Map<String, String> CAT_AR = Map.of(
            "football", "كرة قدم ⚽",
            "guess", "خمن 🔍",
            "world", "حول العالم 🌍",
            "general", "عام 📚",
            "islamic", "إسلاميات 🌙",
            "art", "فن 🎨");

    private static final Map<String, String> SUB_CAT_AR = Map.ofEntries(
            Map.entry("player_eye", "عين اللاعب 👀"),
            Map.entry("who_player", "من اللاعب 👤"),
            Map.entry("young_players", "لاعبين صغار 👶"),
            Map.entry("club_logos", "شعارات أندية 🛡️"),
            Map.entry("player_name", "اسم اللاعب 📛"),
            Map.entry("guess_food", "خمن الأكل 🍔"),
            Map.entry("guess_name", "خمن الاسم 👤"),
            Map.entry("guess_img", "خمن الصورة 🖼️"),
            Map.entry("guess_player", "خمن اللاعب ⚽"),
            Map.entry("langs", "لغات 🗣️"),
            Map.entry("flags", "أعلام 🚩"),
            Map.entry("banknotes", "عملات ورقية 💵"),
            Map.entry("capitals", "عواصم 🏛️"),
            Map.entry("flag_colors", "لون العلم 🎨"),
            Map.entry("animals", "عالم الحيوان 🦁"),
            Map.entry("old_gen", "جيل الطيبين 📺"),
            Map.entry("no_word", "ولا كلمة 🙊"),
            Map.entry("general_info", "معلومات عامة 💡"),
            Map.entry("complete_proverb", "أكمل المثل 📝"),
            Map.entry("what_food", "وش الأكلة 🥘"),
            Map.entry("correct_logo", "الشعار الصحيح ✅"),
            Map.entry("codenames", "كودنيمز 🧩"),
            Map.entry("puzzle_imgs", "صور ألغاز 🧩"),
            Map.entry("logos", "شعارات 🏷️"),
            Map.entry("letters", "حروف 🅰️"),
            Map.entry("car_logo", "شعار السيارة 🚗"),
            Map.entry("blur", "تغبيشة 🌫️"),
            Map.entry("three_imgs", "٣ صور 🖼️"),
            Map.entry("quran", "القرآن 📖"),
            Map.entry("tabarak", "جزء تبارك 📑"),
            Map.entry("islamic_general", "إسلامي 🕌"),
            Map.entry("amma", "جزء عم 📜"),
            Map.entry("prophets", "قصص أنبياء ✨"),
            Map.entry("one_piece", "One Piece 👒"),
            Map.entry("got", "Game of Thrones ⚔️"),
            Map.entry("cartoons", "شخصيات كرتونية 🐭"),
            Map.entry("breaking_bad", "Breaking Bad 🧪"));

    @PostConstruct
    public void init() {
        loadQuestions();
    }

    private void loadQuestions() {
        // --- FOOTBALL ---
        addQ("football_who_player", new JawlahQuestion("من هو اللاعب الملقب بـ 'البرغوث'؟", "ميسي", null));
        addQ("football_who_player",
                new JawlahQuestion("لاعب سجل 5 أهداف في 9 دقائق مع بايرن ميونخ؟", "ليفاندوفسكي", null));
        addQ("football_who_player",
                new JawlahQuestion("من هو اللاعب الوحيد الذي فاز بكأس العالم 3 مرات؟", "بيليه", null));
        addQ("football_who_player",
                new JawlahQuestion("لاعب برازيلي فاز بالكرة الذهبية 2005 ولعب لبرشلونة وميلان؟", "رونالدينيو", null));
        addQ("football_who_player", new JawlahQuestion("أول حارس مرمى يفوز بجائزة الكرة الذهبية؟", "ياشين", null));
        addQ("football_who_player",
                new JawlahQuestion("من هو الهداف التاريخي لمنتخب ألمانيا؟", "ميروسلاف كلوزه", null));
        addQ("football_who_player", new JawlahQuestion("من هو اللاعب الملقب بـ 'المدفعجي'؟", "روبيرتو كارلوس", null));
        addQ("football_who_player", new JawlahQuestion("لاعب كرواتي فاز بالكرة الذهبية عام 2018؟", "مودريتش", null));
        addQ("football_who_player",
                new JawlahQuestion("من هو صاحب أسرع هاتريك في تاريخ الدوري الإنجليزي؟", "ساديو ماني", null));
        addQ("football_who_player", new JawlahQuestion("لاعب فرنسي يلقب بـ 'الزيزو'؟", "زيدان", null));

        addQ("football_player_eye",
                new JawlahQuestion("من صاحب هذه العين؟ (لاعب برتغالي)", "رونالدو", "https://i.imgur.com/r6TqB7C.png"));
        addQ("football_player_eye",
                new JawlahQuestion("من صاحب هذه العين؟ (لاعب أرجنتيني)", "ميسي", "https://i.imgur.com/8mY2PZ0.png"));
        addQ("football_player_eye",
                new JawlahQuestion("من صاحب هذه العين؟ (لاعب مصري)", "صلاح", "https://i.imgur.com/xO7v8GZ.png"));
        addQ("football_player_eye",
                new JawlahQuestion("من صاحب هذه العين؟ (حارس بلجيكي)", "كورتوا", "https://i.imgur.com/L1X7ZqV.png"));
        addQ("football_player_eye",
                new JawlahQuestion("من صاحب هذه العين؟ (مهاجم فرنسي)", "مبابي", "https://i.imgur.com/zV5hBvY.png"));

        addQ("football_club_logos",
                new JawlahQuestion("نادي يلقب بـ 'الملكي' وشعاره يحتوي على تاج؟", "ريال مدريد", null));
        addQ("football_club_logos", new JawlahQuestion("نادي إيطالي يلقب بـ 'السيدة العجوز'؟", "يوفنتوس", null));
        addQ("football_club_logos",
                new JawlahQuestion("نادي ألماني يلقب بـ 'أسود الفيستفاليا'؟", "بروسيا دورتموند", null));
        addQ("football_club_logos",
                new JawlahQuestion("نادي إنجليزي يلقب بـ 'الشياطين الحمر'؟", "مانشستر يونايتد", null));
        addQ("football_club_logos", new JawlahQuestion("نادي إنجليزي يلقب بـ 'الغانرز'؟", "أرسنال", null));
        addQ("football_club_logos", new JawlahQuestion("نادي يلقب بـ 'الخفافيش' في الدوري الإسباني؟", "فالنسيا", null));
        addQ("football_club_logos", new JawlahQuestion("نادي إنجليزي يلقب بـ 'السيتيزنز'؟", "مانشستر سيتي", null));

        addQ("football_player_name", new JawlahQuestion("هداف الدوري الإنجليزي التاريخي؟", "آلان شيرر", null));
        addQ("football_player_name", new JawlahQuestion("أول لاعب عربي فاز بالدوري الإنجليزي؟", "رياض محرز", null));
        addQ("football_player_name", new JawlahQuestion("لاعب يلقب بـ 'الظاهرة'؟", "رونالدو", null));
        addQ("football_player_name", new JawlahQuestion("من هو اللاعب الملقب بـ 'الأباتشي'؟", "تيفيز", null));
        addQ("football_player_name", new JawlahQuestion("من هو اللاعب الملقب بـ 'الرسام'؟", "إنييستا", null));

        // --- WORLD ---
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة اليابان؟", "طوكيو", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة فرنسا؟", "باريس", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة إسبانيا؟", "مدريد", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة مصر؟", "القاهرة", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة الأرجنتين؟", "بوينس آيرس", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة روسيا؟", "موسكو", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة إيطاليا؟", "روما", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة كوريا الجنوبية؟", "سول", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة تركيا؟", "أنقرة", null));
        addQ("world_capitals", new JawlahQuestion("ما هي عاصمة العراق؟", "بغداد", null));

        addQ("world_flags", new JawlahQuestion("ما هي الدولة التي يظهر علمها في الصورة؟", "السعودية",
                "https://i.imgur.com/S8Wn9Z2.png"));
        addQ("world_flags", new JawlahQuestion("ما هي الدولة التي يظهر علمها في الصورة؟", "البرازيل",
                "https://i.imgur.com/4qK9p9C.png"));
        addQ("world_flags", new JawlahQuestion("ما هي الدولة التي يظهر علمها في الصورة؟", "كندا",
                "https://i.imgur.com/O6L8xM9.png"));
        addQ("world_flags", new JawlahQuestion("دولة علمها يحتوي على شمس في المنتصف؟", "الأرجنتين", null));
        addQ("world_flags",
                new JawlahQuestion("دولة علمها عبارة عن ثلاث ألوان طولية: أخضر، أبيض، أحمر؟", "إيطاليا", null));

        addQ("world_langs", new JawlahQuestion("ما هي اللغة الرسمية في البرازيل؟", "البرتغالية", null));
        addQ("world_langs", new JawlahQuestion("ما هي اللغة الرسمية في النمسا؟", "الألمانية", null));
        addQ("world_langs", new JawlahQuestion("ما هي اللغة الرسمية في المكسيك؟", "الإسبانية", null));
        addQ("world_langs", new JawlahQuestion("ما هي اللغة الرسمية في الأرجنتين؟", "الإسبانية", null));

        // --- GENERAL ---
        addQ("general_animals", new JawlahQuestion("ما هو أسرع حيوان بري؟", "الفهد", null));
        addQ("general_animals", new JawlahQuestion("ما هو أضخم حيوان على كوكب الأرض؟", "الحوت الأزرق", null));
        addQ("general_animals", new JawlahQuestion("ما هو الحيوان الذي يلقب بـ 'سفينة الصحراء'؟", "الجمل", null));
        addQ("general_animals", new JawlahQuestion("كم قلباً للأخطبوط؟", "3", null));
        addQ("general_animals", new JawlahQuestion("ما هو الحيوان الذي لا ينام أبداً؟", "القرش", null));

        addQ("general_general_info", new JawlahQuestion("كم عدد كواكب المجموعة الشمسية؟", "8", null));
        addQ("general_general_info", new JawlahQuestion("من هو مكتشف الجاذبية؟", "نيوتن", null));
        addQ("general_general_info", new JawlahQuestion("ما هو أقرب كوكب إلى الشمس؟", "عطارد", null));
        addQ("general_general_info", new JawlahQuestion("ما هي الوحدة المستخدمة لقياس الكهرباء؟", "الأمبير", null));
        addQ("general_general_info",
                new JawlahQuestion("ما هو الغاز الذي يسمى بـ 'غاز الضحك'؟", "أكسيد النيتروز", null));
        addQ("general_general_info",
                new JawlahQuestion("ما هو العنصر الكيميائي الذي يرمز له بالرمز Au؟", "الذهب", null));

        addQ("general_complete_proverb", new JawlahQuestion("أكمل المثل: الوقت كالسيف إن لم تقطعه...", "قطعك", null));
        addQ("general_complete_proverb", new JawlahQuestion("أكمل المثل: من حفر حفرة لأخيه...", "وقع فيها", null));
        addQ("general_complete_proverb", new JawlahQuestion("أكمل المثل: اطلبوا العلم ولو في...", "الصين", null));
        addQ("general_complete_proverb", new JawlahQuestion("أكمل المثل: اتقِ شر من أحسنت...", "إليه", null));

        // --- ISLAMIC ---
        addQ("islamic_prophets", new JawlahQuestion("من هو النبي الملقب بكليم الله؟", "موسى", null));
        addQ("islamic_prophets", new JawlahQuestion("من هو النبي الذي ابتلعه الحوت؟", "يونس", null));
        addQ("islamic_prophets",
                new JawlahQuestion("من هو النبي الذي أُعطي ملكاً لا ينبغي لأحد من بعده؟", "سليمان", null));
        addQ("islamic_prophets", new JawlahQuestion("من هو النبي الملقب بـ 'أبو البشر'؟", "آدم", null));

        addQ("islamic_quran", new JawlahQuestion("ما هي أطول سورة في القرآن الكريم؟", "البقرة", null));
        addQ("islamic_quran", new JawlahQuestion("ما هي أقصر سورة في القرآن الكريم؟", "الكوثر", null));
        addQ("islamic_quran", new JawlahQuestion("ما هي السورة التي تسمى 'عروس القرآن'؟", "الرحمن", null));
        addQ("islamic_quran", new JawlahQuestion("سورة تنتهي جميع آياتها بحرف السين؟", "الناس", null));

        addQ("islamic_islamic_general", new JawlahQuestion("من هو أول مؤذن في الإسلام؟", "بلال بن رباح", null));
        addQ("islamic_islamic_general",
                new JawlahQuestion("من هو الصحابي الملقب بـ 'سيف الله المسلول'؟", "خالد بن الوليد", null));
        addQ("islamic_islamic_general", new JawlahQuestion("كم عدد ركعات صلاة الفجر؟", "2", null));

        // --- ART ---
        addQ("art_one_piece", new JawlahQuestion("من هو بطل قصة ون بيس؟", "لوفي", null));
        addQ("art_one_piece", new JawlahQuestion("ما هو اسم السفينة الأولى لطاقم قبعة القش؟", "غويني ميري", null));
        addQ("art_one_piece", new JawlahQuestion("من هو صائد القراصنة في طاقم لوفي؟", "زورو", null));
        addQ("art_one_piece", new JawlahQuestion("من هو والد لوفي؟", "دراجون", null));

        addQ("art_breaking_bad", new JawlahQuestion("ما هو الاسم المستعار لوالتر وايت؟", "هايزنبرغ", null));
        addQ("art_breaking_bad", new JawlahQuestion("ما هي المادة التي كان يطبخها والتر وايت؟", "الميث", null));

        addQ("art_cartoons", new JawlahQuestion("من هو الفار الذي يسكن في منزل جيري؟", "جيري", null));
        addQ("art_cartoons", new JawlahQuestion("ما اسم الصديق المقرب لسبونج بوب؟", "بسيط", null));

        // --- GUESS ---
        addQ("guess_guess_food",
                new JawlahQuestion("خمن الأكلة: تتكون من أرز ولحم وتشتهر بها السعودية؟", "كبسة", null));
        addQ("guess_guess_food", new JawlahQuestion("خمن الأكلة: عجينة وعليها صلصة طماطم وجبن؟", "بيتزا", null));
        addQ("guess_guess_player", new JawlahQuestion("خمن اللاعب: هداف ليفربول الحالي؟", "صلاح", null));

    }

    private void addQ(String key, JawlahQuestion q) {
        questionBank.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
    }

    public void initiateSetup(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        Modal modal = Modal.create("jawlah_setup_modal", "إعـــدادات لـــعـــبـــة جـــولـــة")
                .addComponents(
                        Label.of("اســـم الـــلـــعـــبـــة",
                                TextInput.create("game_name", TextInputStyle.SHORT).setRequired(true)
                                        .setPlaceholder("مـــثـــال: تـــحـــدي الـــعـــمـــالـــقـــة").build()),
                        Label.of("اســـم الـــفـــريـــق الأول",
                                TextInput.create("team_a_name", TextInputStyle.SHORT).setRequired(true)
                                        .setPlaceholder("مـــثـــال: فـــريـــق الـــصـــقـــور").build()),
                        Label.of("اســـم الـــفـــريـــق الـــثـــانـــي",
                                TextInput.create("team_b_name", TextInputStyle.SHORT).setRequired(true)
                                        .setPlaceholder("مـــثـــال: فـــريـــق الـــنـــســـور").build()),
                        Label.of("عـدد الـلاعـبـيـن لـكـل فـريـق", TextInput.create("max_players", TextInputStyle.SHORT)
                                .setRequired(true).setPlaceholder("مـــثـــال: 3").build()))
                .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("jawlah_setup_modal"))
            return;

        String gameName = event.getValue("game_name").getAsString();
        String teamA = event.getValue("team_a_name").getAsString();
        String teamB = event.getValue("team_b_name").getAsString();
        int maxPlayers = 5;
        try {
            maxPlayers = Integer.parseInt(event.getValue("max_players").getAsString());
        } catch (Exception ignored) {
        }

        JawlahGame game = new JawlahGame(event.getChannel().getIdLong(), event.getUser().getIdLong());
        game.setGameName(gameName);
        game.setTeamAName(teamA);
        game.setTeamBName(teamB);
        game.setMaxPlayersPerTeam(maxPlayers);
        activeGames.put(event.getChannel().getIdLong(), game);
        sendHelpingHandsSelection(event);

        String logDetails = String.format("### 🏆 فعالية جولة: بدء\n▫️ **المنظم:** %s\n▫️ **القناة:** <#%d>",
                event.getMember().getAsMention(), event.getChannel().getIdLong());
        logManager.logEmbed(event.getGuild(), com.integrafty.opexy.service.LogManager.LOG_GAMES,
                EmbedUtil.createOldLogEmbed("jawlah", logDetails, event.getMember(), null, null, EmbedUtil.INFO));
    }

    private void sendHelpingHandsSelection(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        String enabledStr = game.getEnabledHelpers().isEmpty() ? "*لا يوجد*"
                : String.join(" - ", game.getEnabledHelpers());

        StringBuilder playersList = new StringBuilder();
        playersList.append(String.format("🔵 **%s** (%d/%d):\n", game.teamAName, game.teamAPlayers.size(),
                game.maxPlayersPerTeam));
        if (game.teamAPlayers.isEmpty())
            playersList.append("*في انتظار اللاعبين...*\n");
        else
            game.teamAPlayers.forEach(id -> playersList.append("<@").append(id).append("> "));

        playersList.append(String.format("\n\n🔴 **%s** (%d/%d):\n", game.teamBName, game.teamBPlayers.size(),
                game.maxPlayersPerTeam));
        if (game.teamBPlayers.isEmpty())
            playersList.append("*في انتظار اللاعبين...*\n");
        else
            game.teamBPlayers.forEach(id -> playersList.append("<@").append(id).append("> "));

        String body = "### 🛠️ إعـــدادات الـــلـــعـــبـــة\n" +
                playersList.toString() + "\n\n" +
                "**الـــمـــســـاعـــدات الـــمـــفـــعـــلـــه:**\n" +
                enabledStr + "\n\n" +
                "اخـــتـــر فـــريـــقـــك لـــلـــدخول، والـــمـــنـــظـــم يـــمـــكـــنـــه تـــفـــعـــيـــل الـــمـــســـاعـــدات وبـــدء الـــلـــعب.";

        Button joinA = Button.primary("jawlah_join_a", "انضمام لـ " + game.teamAName + " 🔵")
                .withDisabled(game.teamAPlayers.size() >= game.maxPlayersPerTeam);
        Button joinB = Button.danger("jawlah_join_b", "انضمام لـ " + game.teamBName + " 🔴")
                .withDisabled(game.teamBPlayers.size() >= game.maxPlayersPerTeam);

        Button h1 = game.usedHelpers.contains("جاوب جوابين ✌️")
                ? Button.secondary("jawlah_help_1", "جاوب جوابين ✌️").asDisabled()
                : (game.getEnabledHelpers().contains("جاوب جوابين ✌️")
                        ? Button.success("jawlah_help_1", "جاوب جوابين ✌️")
                        : Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"));
        Button h3 = game.usedHelpers.contains("الحفرة ⛳") ? Button.secondary("jawlah_help_3", "الحفرة ⛳").asDisabled()
                : (game.getEnabledHelpers().contains("الحفرة ⛳") ? Button.success("jawlah_help_3", "الحفرة ⛳")
                        : Button.secondary("jawlah_help_3", "الحفرة ⛳"));
        Button h4 = game.usedHelpers.contains("اعكس الدور 🔄")
                ? Button.secondary("jawlah_help_4", "اعكس الدور 🔄").asDisabled()
                : (game.getEnabledHelpers().contains("اعكس الدور 🔄") ? Button.success("jawlah_help_4", "اعكس الدور 🔄")
                        : Button.secondary("jawlah_help_4", "اعكس الدور 🔄"));
        Button h5 = game.usedHelpers.contains("السؤال الذهبي 🏆")
                ? Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆").asDisabled()
                : (game.getEnabledHelpers().contains("السؤال الذهبي 🏆")
                        ? Button.success("jawlah_help_5", "السؤال الذهبي 🏆")
                        : Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆"));

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("SETUP",
                        "تـــجـــهـــيـــز الـــفـــرق والـــمـــســـاعـــدات", body, EmbedUtil.BANNER_MAIN,
                        ActionRow.of(joinA, joinB),
                        ActionRow.of(h1, h3, h4, h5),
                        ActionRow.of(Button.success("jawlah_start_confirm", "بـــدء الـــلـــعـــب 🚀"))))
                .useComponentsV2(true);

        if (event instanceof ButtonInteractionEvent bie)
            bie.editMessage(edit.build()).queue();
        else if (event instanceof ModalInteractionEvent mie) {
            MessageCreateBuilder cb = new MessageCreateBuilder().setComponents(edit.getComponents())
                    .useComponentsV2(true);
            mie.reply(cb.build()).useComponentsV2(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("jawlah_"))
            return;

        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null)
            return;

        if (event.getUser().getIdLong() != game.organizerId) {
            event.reply("❌ عذراً، المنظم فقط يمكنه التحكم في اللعبة.").setEphemeral(true).queue();
            return;
        }

        if (id.equals("jawlah_category")) {
            game.setSelectedCategory(event.getValues().get(0));
            sendSubCategoryMenu(event, game);
        } else if (id.equals("jawlah_subcategory")) {
            game.setSelectedSubCategory(event.getValues().get(0));
            String arSub = SUB_CAT_AR.getOrDefault(game.getSelectedSubCategory(), game.getSelectedSubCategory());
            event.reply("تم اختيار الفئة: **" + arSub + "**. الآن اختر القيمة من اللوحة الرئيسية.").setEphemeral(true)
                    .queue();
        } else if (id.equals("jawlah_value")) {
            int val = Integer.parseInt(event.getValues().get(0));
            String key = game.getSelectedCategory() + "_" + val;
            if (game.getUsedQuestions().contains(key)) {
                event.reply("⚠️ هذا السؤال تم استخدامه بالفعل! اختر قيمة أخرى.").setEphemeral(true).queue();
                return;
            }
            game.setSelectedValue(val);
            game.getUsedQuestions().add(key);

            // Fix repeating questions: pick one that wasn't shown yet
            String qKey = game.selectedCategory + "_" + game.selectedSubCategory;
            List<JawlahQuestion> qs = questionBank.getOrDefault(qKey, new ArrayList<>());
            List<JawlahQuestion> available = qs.stream().filter(que -> !game.displayedQuestionTexts.contains(que.text))
                    .toList();
            if (available.isEmpty()) {
                event.reply("⚠️ لا يوجد أسئلة متبقية في هذه الفئة! الرجاء اختيار فئة أو قيمة أخرى.").setEphemeral(true).queue();
                game.getUsedQuestions().remove(key); // Revert the value usage
                return;
            }
            JawlahQuestion q = available.get(new Random().nextInt(available.size()));
            game.setCurrentQuestion(q);
            game.displayedQuestionTexts.add(q.text);

            showQuestionPrompt(event, game);
        }
    }

    private void sendSubCategoryMenu(StringSelectInteractionEvent event, JawlahGame game) {
        String cat = game.getSelectedCategory();
        StringSelectMenu.Builder menu = StringSelectMenu.create("jawlah_subcategory")
                .setPlaceholder("اخـتـر الـفـئـة الـفـرعـيـة...");

        switch (cat) {
            case "football" -> {
                menu.addOption("عين اللاعب 👀", "player_eye");
                menu.addOption("من اللاعب 👤", "who_player");
                menu.addOption("لاعبين صغار 👶", "young_players");
                menu.addOption("شعارات أندية 🛡️", "club_logos");
                menu.addOption("اسم اللاعب 📛", "player_name");
            }
            case "guess" -> {
                menu.addOption("خمن الأكل 🍔", "guess_food");
                menu.addOption("خمن الاسم 👤", "guess_name");
                menu.addOption("خمن الصورة 🖼️", "guess_img");
                menu.addOption("خمن اللاعب ⚽", "guess_player");
            }
            case "world" -> {
                menu.addOption("لغات 🗣️", "langs");
                menu.addOption("أعلام 🚩", "flags");
                menu.addOption("عملات ورقية 💵", "banknotes");
                menu.addOption("عواصم 🏛️", "capitals");
            }
            case "general" -> {
                menu.addOption("عالم الحيوان 🦁", "animals");
                menu.addOption("معلومات عامة 💡", "general_info");
                menu.addOption("أكمل المثل 📝", "complete_proverb");
            }
            case "islamic" -> {
                menu.addOption("القرآن 📖", "quran");
                menu.addOption("قصص أنبياء ✨", "prophets");
                menu.addOption("إسلامي 🕌", "islamic_general");
            }
            case "art" -> {
                menu.addOption("One Piece 👒", "one_piece");
                menu.addOption("Breaking Bad 🧪", "breaking_bad");
                menu.addOption("شخصيات كرتونية 🐭", "cartoons");
            }
        }

        event.editMessage(new MessageEditBuilder()
                .setComponents(ActionRow.of(menu.build()),
                        ActionRow.of(Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️")))
                .useComponentsV2(true).build()).useComponentsV2(true).queue();
    }

    private void showQuestionPrompt(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, JawlahGame game) {
        JawlahQuestion q = game.getCurrentQuestion();
        game.setAttemptsLeft(game.getEnabledHelpers().contains("جاوب جوابين ✌️") ? 2 : 1);

        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "")
                + (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");
        String arCat = CAT_AR.getOrDefault(game.selectedCategory, game.selectedCategory);
        String arSub = SUB_CAT_AR.getOrDefault(game.selectedSubCategory, game.selectedSubCategory);

        int seconds = (game.selectedValue == 300) ? 20 : (game.selectedValue == 600) ? 15 : 10;
        String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, seconds);

        String body = String.format(
                "### ❓ ســـؤال الـــتـــحـــدي\n\n%s\n\n**الـفـئـة:** `%s` -> `%s`\n**الـقـيـمـة:** `%d` نـقـطـة\n**الـدور لـفـريـق:** %s\n\n📢 **الـسـؤال:** %s\n\n%sيـرجـى كـتـابـة الإجـابـة فـي الـشـات الآن!",
                timerFormat, arCat, arSub, game.selectedValue,
                game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName, q.text, modifiers);

        Button h1 = game.usedHelpers.contains("جاوب جوابين ✌️")
                ? Button.secondary("jawlah_help_1", "جاوب جوابين ✌️").asDisabled()
                : (game.getEnabledHelpers().contains("جاوب جوابين ✌️")
                        ? Button.success("jawlah_help_1", "جاوب جوابين ✌️")
                        : Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"));
        Button h4 = game.usedHelpers.contains("اعكس الدور 🔄")
                ? Button.secondary("jawlah_help_4", "اعكس الدور 🔄").asDisabled()
                : (game.getEnabledHelpers().contains("اعكس الدور 🔄") ? Button.success("jawlah_help_4", "اعكس الدور 🔄")
                        : Button.secondary("jawlah_help_4", "اعكس الدور 🔄"));
        Button h5 = game.usedHelpers.contains("السؤال الذهبي 🏆")
                ? Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆").asDisabled()
                : (game.getEnabledHelpers().contains("السؤال الذهبي 🏆")
                        ? Button.success("jawlah_help_5", "السؤال الذهبي 🏆")
                        : Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆"));

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("QUESTION", "🔍 جـــاري الـــتـــحـــدي...", body,
                        q.imageUrl != null ? q.imageUrl : EmbedUtil.BANNER_MAIN,
                        ActionRow.of(h1, h4, h5),
                        ActionRow.of(Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️"))))
                .useComponentsV2(true);

        if (event instanceof ButtonInteractionEvent bie) {
            bie.editMessage(edit.build()).queue(msg -> {
                game.lastQuestionMessageId = msg.getIdLong();
                initTimer(game.channelId, seconds);
            });
        } else if (event instanceof StringSelectInteractionEvent ssie) {
            ssie.editMessage(edit.build()).queue(msg -> {
                game.lastQuestionMessageId = msg.getIdLong();
                initTimer(game.channelId, seconds);
            });
        }
    }

    private void initTimer(long channelId, int seconds) {
        cancelTimer(channelId);
        final int[] timeLeft = { seconds };
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                JawlahGame game = activeGames.get(channelId);
                if (game == null || game.getCurrentQuestion() == null) {
                    cancelTimer(channelId);
                    return;
                }
                timeLeft[0]--;

                // Update timer in message
                if (timeLeft[0] > 0 && timeLeft[0] % 5 == 0) { // Update every 5 seconds to reduce rate limits
                    TextChannel channel = jda.getTextChannelById(channelId);
                    if (channel != null && game.lastQuestionMessageId != 0) {
                        channel.retrieveMessageById(game.lastQuestionMessageId).queue(msg -> {
                            JawlahQuestion q = game.getCurrentQuestion();
                            if (q == null) return;

                            String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, timeLeft[0]);
                            String arCat = CAT_AR.getOrDefault(game.selectedCategory, game.selectedCategory);
                            String arSub = SUB_CAT_AR.getOrDefault(game.selectedSubCategory, game.selectedSubCategory);
                            String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعلـة**\n" : "") + (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");
                            
                            String body = String.format("### ❓ ســـؤال الـــتـــحـــدي\n\n%s\n\n**الـفـئـة:** `%s` -> `%s`\n**الـقـيـمـة:** `%d` نـقـطـة\n**الـدور لـفـريـق:** %s\n\n📢 **الـسـؤال:** %s\n\n%sيـرجـى كـتـابـة الإجـابـة فـي الـشـات الآن!",
                                    timerFormat, arCat, arSub, game.selectedValue, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName, q.text, modifiers);

                            Button h1 = game.usedHelpers.contains("جاوب جوابين ✌️") ? Button.secondary("jawlah_help_1", "جاوب جوابين ✌️").asDisabled() : (game.getEnabledHelpers().contains("جاوب جوابين ✌️") ? Button.success("jawlah_help_1", "جاوب جوابين ✌️") : Button.secondary("jawlah_help_1", "جاوب جوابين ✌️"));
                            Button h4 = game.usedHelpers.contains("اعكس الدور 🔄") ? Button.secondary("jawlah_help_4", "اعكس الدور 🔄").asDisabled() : (game.getEnabledHelpers().contains("اعكس الدور 🔄") ? Button.success("jawlah_help_4", "اعكس الدور 🔄") : Button.secondary("jawlah_help_4", "اعكس الدور 🔄"));
                            Button h5 = game.usedHelpers.contains("السؤال الذهبي 🏆") ? Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆").asDisabled() : (game.getEnabledHelpers().contains("السؤال الذهبي 🏆") ? Button.success("jawlah_help_5", "السؤال الذهبي 🏆") : Button.secondary("jawlah_help_5", "السؤال الذهبي 🏆"));

                            msg.editMessage(new MessageEditBuilder()
                                    .setComponents(EmbedUtil.containerBranded("QUESTION", "🔍 جـــاري الـــتـــحـــدي...", body, q.imageUrl != null ? q.imageUrl : EmbedUtil.BANNER_MAIN,
                                            ActionRow.of(h1, h4, h5),
                                            ActionRow.of(Button.secondary("jawlah_back", "الـعـودة لـلـوحـة ⬅️"))
                                    )).useComponentsV2(true).build()).queue();
                        }, err -> {});
                    }
                }

                if (timeLeft[0] <= 0) {
                    cancelTimer(channelId);
                    TextChannel channel = jda.getTextChannelById(channelId);
                    if (channel != null) {
                        channel.sendMessage("⏰ **انتهى الوقت!** تم تخطي الدور لعدم الإجابة.").queue();
                        game.setCurrentQuestion(null);
                        game.setGoldenQuestion(false);
                        game.setPitActive(false);
                        game.getEnabledHelpers().clear();
                        game.setTurnA(!game.turnA);
                        sendBoardAfterDelay(channelId);
                    }
                }
            } catch (Exception e) {
                cancelTimer(channelId);
            }
        }, 1, 1, TimeUnit.SECONDS);
        sessionTimers.put(channelId, future);
    }

    private void cancelTimer(long channelId) {
        if (sessionTimers.containsKey(channelId)) {
            sessionTimers.get(channelId).cancel(true);
            sessionTimers.remove(channelId);
        }
    }

    public void stopGame(long channelId) {
        JawlahGame game = activeGames.get(channelId);
        if (game != null) {
            cancelTimer(channelId);
            distributePrizes(game, jda.getTextChannelById(channelId).getGuild());
            activeGames.remove(channelId);
            eventManager.endGroupEvent();
        }
    }

    public void stopAllGames() {
        activeGames.keySet().forEach(id -> stopGame(id));
    }

    private void distributePrizes(JawlahGame game, net.dv8tion.jda.api.entities.Guild guild) {
        if (guild == null)
            return;
        if (game.scoreA > 0 && !game.teamAPlayers.isEmpty()) {
            long perPerson = game.scoreA / game.teamAPlayers.size();
            game.teamAPlayers.forEach(id -> {
                try {
                    economyService.addBalance(String.valueOf(id), guild.getId(), perPerson);
                } catch (Exception e) {
                }
            });
        }
        if (game.scoreB > 0 && !game.teamBPlayers.isEmpty()) {
            long perPerson = game.scoreB / game.teamBPlayers.size();
            game.teamBPlayers.forEach(id -> {
                try {
                    economyService.addBalance(String.valueOf(id), guild.getId(), perPerson);
                } catch (Exception e) {
                }
            });
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("jawlah_"))
            return;
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null)
            return;

        switch (id) {
            case "jawlah_back" -> sendBoard(event);
            case "jawlah_start_confirm" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه بدء اللعبة.").setEphemeral(true).queue();
                    return;
                }
                if (game.teamAPlayers.isEmpty() || game.teamBPlayers.isEmpty()) {
                    event.reply("⚠️ يجب أن يكون هناك لاعب واحد على الأقل في كل فريق للبدء!").setEphemeral(true).queue();
                    return;
                }
                sendBoard(event);
            }
            case "jawlah_stop" -> {
                if (event.getUser().getIdLong() != game.organizerId) {
                    event.reply("❌ عذراً، المنظم فقط يمكنه إنهاء اللعبة.").setEphemeral(true).queue();
                    return;
                }
                distributePrizes(game, event.getGuild());
                activeGames.remove(event.getChannel().getIdLong());
                cancelTimer(event.getChannel().getIdLong());
                eventManager.endGroupEvent();
                String summary = String.format(
                        "🛑 **تم إنهاء اللعبة وتوزيع الجوائز!**\n\n🔵 **%s**: %d نقطة\n🔴 **%s**: %d نقطة",
                        game.teamAName, game.scoreA, game.teamBName, game.scoreB);
                event.reply(new MessageCreateBuilder().setComponents(EmbedUtil.success("GAME OVER", summary))
                        .useComponentsV2(true).build()).useComponentsV2(true).queue();
            }
            case "jawlah_join_a" -> {
                long uid = event.getUser().getIdLong();
                if (game.teamAPlayers.contains(uid))
                    return;
                game.teamBPlayers.remove(uid);
                if (game.teamAPlayers.size() < game.maxPlayersPerTeam) {
                    game.teamAPlayers.add(uid);
                    sendHelpingHandsSelection(event);
                }
            }
            case "jawlah_join_b" -> {
                long uid = event.getUser().getIdLong();
                if (game.teamBPlayers.contains(uid))
                    return;
                game.teamAPlayers.remove(uid);
                if (game.teamBPlayers.size() < game.maxPlayersPerTeam) {
                    game.teamBPlayers.add(uid);
                    sendHelpingHandsSelection(event);
                }
            }
            case "jawlah_help_1" -> {
                if (event.getUser().getIdLong() != game.organizerId)
                    return;
                String h = "جاوب جوابين ✌️";
                if (game.usedHelpers.contains(h)) {
                    event.reply("⚠️ تم استخدام هذه المساعدة بالفعل في هذه اللعبة!").setEphemeral(true).queue();
                    return;
                }

                if (game.getEnabledHelpers().contains(h))
                    game.getEnabledHelpers().remove(h);
                else {
                    game.getEnabledHelpers().add(h);
                    if (game.selectedValue > 0)
                        game.usedHelpers.add(h); // Mark as used if activated during a question
                }

                if (game.selectedValue > 0)
                    showQuestionPrompt(event, game);
                else
                    sendHelpingHandsSelection(event);
            }
            case "jawlah_help_3" -> {
                if (event.getUser().getIdLong() != game.organizerId)
                    return;
                String h = "الحفرة ⛳";
                if (game.usedHelpers.contains(h)) {
                    event.reply("⚠️ تم استخدام هذه المساعدة بالفعل في هذه اللعبة!").setEphemeral(true).queue();
                    return;
                }

                if (game.getEnabledHelpers().contains(h)) {
                    game.getEnabledHelpers().remove(h);
                    game.setPitActive(false);
                } else {
                    game.getEnabledHelpers().add(h);
                    game.setPitActive(true);
                    if (game.selectedValue > 0)
                        game.usedHelpers.add(h);
                }

                if (game.selectedValue > 0)
                    showQuestionPrompt(event, game);
                else
                    sendHelpingHandsSelection(event);
            }
            case "jawlah_help_4" -> {
                if (event.getUser().getIdLong() != game.organizerId)
                    return;
                String h = "اعكس الدور 🔄";
                if (game.usedHelpers.contains(h)) {
                    event.reply("⚠️ تم استخدام هذه المساعدة بالفعل في هذه اللعبة!").setEphemeral(true).queue();
                    return;
                }

                if (game.getEnabledHelpers().contains(h)) {
                    game.getEnabledHelpers().remove(h);
                    game.setTurnA(!game.turnA);
                } else {
                    game.getEnabledHelpers().add(h);
                    game.setTurnA(!game.turnA);
                    if (game.selectedValue > 0)
                        game.usedHelpers.add(h);
                }

                if (game.selectedValue > 0)
                    showQuestionPrompt(event, game);
                else
                    sendHelpingHandsSelection(event);
            }
            case "jawlah_help_5" -> {
                if (event.getUser().getIdLong() != game.organizerId)
                    return;
                String h = "السؤال الذهبي 🏆";
                if (game.usedHelpers.contains(h)) {
                    event.reply("⚠️ تم استخدام هذه المساعدة بالفعل في هذه اللعبة!").setEphemeral(true).queue();
                    return;
                }

                if (game.getEnabledHelpers().contains(h)) {
                    game.getEnabledHelpers().remove(h);
                    game.setGoldenQuestion(false);
                } else {
                    game.getEnabledHelpers().add(h);
                    game.setGoldenQuestion(true);
                    if (game.selectedValue > 0)
                        game.usedHelpers.add(h);
                }

                if (game.selectedValue > 0)
                    showQuestionPrompt(event, game);
                else
                    sendHelpingHandsSelection(event);
            }
        }
    }

    private boolean useItem(long userId, net.dv8tion.jda.api.entities.Guild guild,
            java.util.function.Function<com.integrafty.opexy.entity.UserStats, Integer> getter,
            java.util.function.BiConsumer<com.integrafty.opexy.entity.UserStats, Integer> setter) {
        com.integrafty.opexy.entity.UserStats stats = achievementService.getStats(userId);
        int count = getter.apply(stats);
        if (count > 0) {
            setter.accept(stats, count - 1);
            achievementService.updateStats(userId, guild, s -> {
            });
            return true;
        }
        return false;
    }

    private void sendBoard(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        game.setSelectedValue(0);
        int totalQuestions = 6 * 3;
        int usedCount = game.getUsedQuestions().size();
        int remaining = totalQuestions - usedCount;
        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعـلـة**\n" : "")
                + (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");
        String body = String.format(
                "### 🏆 %s\n\n**الـنـتـيـجـة:**\n🔵 **%s**: `%d` نـقـطـة\n🔴 **%s**: `%d` نـقـطـة\n\n📊 **الأســـئـــلـــة الـــمـــتـــبـــقـــيـــة:** `%d / %d`\n\n%s**الـدور لـفـريـق:** %s\n\nيـرجـى اخـتـيـار الـفـئـة والـقـيـمـة مـن الـقـوائـم أدناه.",
                game.gameName, game.teamAName, game.scoreA, game.teamBName, game.scoreB, remaining, totalQuestions,
                modifiers, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName);

        StringSelectMenu categoryMenu = StringSelectMenu.create("jawlah_category").setPlaceholder("اخـتـر الـفـئـة...")
                .addOption("كرة قدم ⚽", "football").addOption("خمن 🔍", "guess").addOption("حول العالم 🌍", "world")
                .addOption("عام 📚", "general").addOption("إسلاميات 🌙", "islamic").addOption("فن 🎨", "art").build();

        StringSelectMenu valueMenu = StringSelectMenu.create("jawlah_value").setPlaceholder("اخـتـر الـقـيـمـة...")
                .addOption("300 نـقـطـة", "300").addOption("600 نـقـطـة", "600").addOption("900 نـقـطـة", "900")
                .build();

        MessageEditBuilder edit = new MessageEditBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body,
                        EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu), ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))))
                .useComponentsV2(true);

        if (event instanceof ButtonInteractionEvent bie)
            bie.editMessage(edit.build()).queue();
        else if (event instanceof StringSelectInteractionEvent ssie)
            ssie.editMessage(edit.build()).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        JawlahGame game = activeGames.get(event.getChannel().getIdLong());
        if (game == null || game.getCurrentQuestion() == null)
            return;
        long uid = event.getAuthor().getIdLong();
        boolean isA = game.teamAPlayers.contains(uid);
        boolean isB = game.teamBPlayers.contains(uid);
        if (!isA && !isB && uid != game.organizerId)
            return;
        boolean isHisTurn = (game.turnA && isA) || (!game.turnA && isB);
        if (!isHisTurn && uid != game.organizerId) {
            event.getMessage().reply("⚠️ ليس دور فريقك!").queue();
            return;
        }

        String content = event.getMessage().getContentRaw().trim();
        String answer = game.getCurrentQuestion().answer;
        if (isSimilar(content, answer)) {
            int points = game.selectedValue;
            if (game.isGoldenQuestion())
                points *= 2;
            if (game.turnA) {
                game.scoreA += points;
                if (game.isPitActive())
                    game.scoreB = Math.max(0, game.scoreB - points);
            } else {
                game.scoreB += points;
                if (game.isPitActive())
                    game.scoreA = Math.max(0, game.scoreA - points);
            }
            event.getMessage().reply("✅ إجابة صحيحة يا " + event.getAuthor().getAsMention() + "!").queue();
            game.setCurrentQuestion(null);
            game.setGoldenQuestion(false);
            game.setPitActive(false);
            game.getEnabledHelpers().clear();
            game.setTurnA(!game.turnA);
            cancelTimer(event.getChannel().getIdLong());
            sendBoardAfterDelay(event.getChannel().getIdLong());
        } else {
            game.setAttemptsLeft(game.getAttemptsLeft() - 1);
            if (game.attemptsLeft <= 0) {
                event.getMessage().reply("❌ خطأ! الإجابة: **" + answer + "**").queue();
                game.setCurrentQuestion(null);
                game.setGoldenQuestion(false);
                game.setPitActive(false);
                game.getEnabledHelpers().clear();
                game.setTurnA(!game.turnA);
                cancelTimer(event.getChannel().getIdLong());
                sendBoardAfterDelay(event.getChannel().getIdLong());
            } else
                event.getMessage().reply("❌ خطأ! محاولة أخرى...").queue();
        }
    }

    private boolean isSimilar(String input, String target) {
        if (input == null || target == null)
            return false;
        String s1 = normalize(input);
        String s2 = normalize(target);
        return s1.equals(s2);
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[أإآ]", "ا")
                .replaceAll("ة", "ه")
                .replaceAll("ى", "ي")
                .replaceAll("\\s+", "")
                .replaceAll("[^\\w\\u0600-\\u06FF]", "");
    }

    private void sendBoardAfterDelay(long channelId) {
        JawlahGame game = activeGames.get(channelId);
        if (game == null)
            return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;
        int totalQuestions = 6 * 3;
        int usedCount = game.getUsedQuestions().size();
        int remaining = totalQuestions - usedCount;
        String modifiers = (game.isPitActive() ? "⛳ **الـحـفـرة مـفـعلـة**\n" : "")
                + (game.isGoldenQuestion() ? "🏆 **الـسـؤال الـذهـبـي مـفـعـل**\n" : "");
        String body = String.format(
                "### 🏆 %s\n\n**الـنـتـيـجـة:**\n🔵 **%s**: `%d` نـقـطـة\n🔴 **%s**: `%d` نـقـطـة\n\n📊 **الأســـئـــلـــة الـــمـــتـــبـــقـــيـــة:** `%d / %d`\n\n%s**الـدور لـفـريـق:** %s\n\nيـرجـى اخـتـيـار الـفـئـة والـقـيـمـة مـن الـقـوائـم أدناه.",
                game.gameName, game.teamAName, game.scoreA, game.teamBName, game.scoreB, remaining, totalQuestions,
                modifiers, game.turnA ? "🔵 " + game.teamAName : "🔴 " + game.teamBName);

        StringSelectMenu categoryMenu = StringSelectMenu.create("jawlah_category").setPlaceholder("اخـتـر الـفـئـة...")
                .addOption("كرة قدم ⚽", "football").addOption("خمن 🔍", "guess").addOption("حول العالم 🌍", "world")
                .addOption("عام 📚", "general").addOption("إسلاميات 🌙", "islamic").addOption("فن 🎨", "art").build();
        StringSelectMenu valueMenu = StringSelectMenu.create("jawlah_value").setPlaceholder("اخـتـر الـقـيـمـة...")
                .addOption("300 نـقـطـة", "300").addOption("600 نـقـطـة", "600").addOption("900 نـقـطـة", "900")
                .build();

        channel.sendMessage(new MessageCreateBuilder()
                .setComponents(EmbedUtil.containerBranded("GAME", "🎮 لوحـة الـتـحـدي — Jawlah Board", body,
                        EmbedUtil.BANNER_MAIN,
                        ActionRow.of(categoryMenu), ActionRow.of(valueMenu),
                        ActionRow.of(Button.danger("jawlah_stop", "إنـهـاء الـلـعـبـة 🛑"))))
                .useComponentsV2(true).build()).queue();
    }

    @Getter
    @Setter
    private static class JawlahQuestion {
        private final String text;
        private final String answer;
        private final String imageUrl;

        public JawlahQuestion(String text, String answer, String imageUrl) {
            this.text = text;
            this.answer = answer;
            this.imageUrl = imageUrl;
        }
    }

    @Getter
    @Setter
    private static class JawlahGame {
        private final long channelId;
        private final long organizerId;
        private String gameName, teamAName, teamBName;
        private int scoreA = 0, scoreB = 0;
        private boolean turnA = true;
        private String selectedCategory, selectedSubCategory;
        private int selectedValue;
        private boolean pitActive = false, goldenQuestion = false;
        private int maxPlayersPerTeam = 5;
        private final Set<Long> teamAPlayers = new LinkedHashSet<>(), teamBPlayers = new LinkedHashSet<>();
        private final Set<String> usedQuestions = new HashSet<>(), enabledHelpers = new LinkedHashSet<>();
        private JawlahQuestion currentQuestion;
        private int attemptsLeft = 1;

        public JawlahGame(long channelId, long organizerId) {
            this.channelId = channelId;
            this.organizerId = organizerId;
        }

        private final Set<String> usedHelpers = new HashSet<>();
        private long lastQuestionMessageId;
        private final Set<String> displayedQuestionTexts = new HashSet<>();
    }
}
