package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.JDA;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class CraftManager extends ListenerAdapter {

        private final JDA jda;
        private final AchievementService achievementService;
        private final EconomyService economyService;
        private final LogManager logManager;

        private final Map<String, Recipe> sessionActiveRecipes = new HashMap<>();
        private final Map<String, Long> sessionRewards = new HashMap<>();
        private final Map<String, Difficulty> sessionDifficulty = new HashMap<>();
        private final Map<String, String> sessionMentions = new HashMap<>();
        private final Map<String, Long> sessionGuilds = new HashMap<>();
        private final Map<String, String> sessionGrids = new HashMap<>();
        private final Map<String, Long> sessionUserIds = new HashMap<>();
        private final Map<String, net.dv8tion.jda.api.interactions.InteractionHook> sessionHooks = new HashMap<>();
        private final Map<String, java.util.concurrent.ScheduledFuture<?>> sessionTimers = new HashMap<>();
        private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
                        .newScheduledThreadPool(10);

        @PostConstruct
        public void init() {
        }

        public enum Difficulty {
                EASY(10, 15, "سهل"),
                MEDIUM(15, 12, "وسط"),
                HARD(20, 10, "صعب");

                public final int reward;
                public final int seconds;
                public final String displayName;

                Difficulty(int r, int s, String d) {
                        this.reward = r;
                        this.seconds = s;
                        this.displayName = d;
                }
        }

        private static final Map<String, String> ITEMS = Map.ofEntries(
                        Map.entry("W", "<:oak_planks:1500879889119707266>"), // Wood/Planks
                        Map.entry("S", "<:stick:1500879473992794212>"), // Stick
                        Map.entry("I", "<:Minecraft_Iron_Ingot:1500878789402693744>"), // Iron Ingot
                        Map.entry("G", "<:gold_ingot:1500878971410055330>"), // Gold Ingot
                        Map.entry("D", "<:dimoand:1500878887662518334>"), // Diamond
                        Map.entry("P", "<:Minecraft_Sugar_Cane_Item__HD_Pn:1500879626824585426>"), // Sugar Cane (Paper)
                        Map.entry("B", "<:coble_stone:1500879838041608243>"), // Cobblestone
                        Map.entry("C", "<:Coal_JE4_BE3:1500973041805557954>"), // Coal
                        Map.entry("E", "🔳"), // Empty
                        Map.entry("R", "<:red_stone:1500879139010383913>"), // Redstone
                        Map.entry("L", "<:string:1500880235510497360>"), // String
                        Map.entry("F", "<:Minecraft_Gunpowder_pngremovebgp:1500879430367707366>"), // Gunpowder
                        Map.entry("Q", "<:quartz:1500880509960589404>"), // Quartz
                        Map.entry("X", "<:glass:1500880071466942586>"), // Glass Block
                        Map.entry("O", "<:obsadian:1500879689659584643>"), // Obsidian
                        Map.entry("T", "<:torch:1500879281654464652>"), // Torch
                        Map.entry("H", "<:Enchanted_Book__Minecraft_Plugin:1500879162246828203>"), // Book
                        Map.entry("A", "<:red_apple:1500880776655540285>"), // Apple
                        Map.entry("N", "<:ender_eye:1500880557641568347>"), // Ender Eye
                        Map.entry("K", "<:neather_star:1500880657986097172>"), // Nether Star
                        Map.entry("U", "<:leather:1500880346206568498>"), // Leather
                        Map.entry("Z", "<:sand:1500879926696607846>"), // Sand
                        Map.entry("V", "<:water_empty_bottle:1500880709315723428>"), // Glass Bottle
                        Map.entry("M", "<:emraled_ingot:1500879037868806237>"), // Emerald
                        Map.entry("Y", "<:hay_bale:1500880117302562856>"), // Hay Bale Block
                        Map.entry("WH", "<:wheat_minecraft_itemsremovebgpre:1500879408032911492>"), // Wheat
                        Map.entry("J", "<:feather:1500880285431238807>") // Feather
        );

        @RequiredArgsConstructor
        private static class Recipe {
                final String[][] grid;
                final List<String> possibleNames;
                final String displayName;
                final Difficulty difficulty;
        }

        private static final List<Recipe> RECIPES = List.of(
                        // EASY
                        new Recipe(new String[][] { { "W", "W", "E" }, { "W", "W", "E" }, { "E", "E", "E" } },
                                        List.of("ورك بينش", "طاولة صنع", "crafting table", "workbench", "طاوله صنع",
                                                        "طاولة الصناعة",
                                                        "كرفتنق تيبل", "كرافتنق تيبل", "طاولة الكرافتنق",
                                                        "كرافتنج تيبل"),
                                        "طاولة صنع", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "E", "E" }, { "W", "E", "E" }, { "E", "E", "E" } },
                                        List.of("عصا", "stick", "عصاي", "العصا", "ستيك", "عصيان", "خشب عصا", "عصاية"),
                                        "عصا", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "W", "W" }, { "W", "W", "W" } },
                                        List.of("بلوك خشب", "wood block", "خشب", "الخشب", "بلوك الخشب", "planks",
                                                        "wood", "خشب محلل", "بلوكة خشب", "بلوكه خشب"),
                                        "بلوك خشب",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "C", "E", "E" }, { "S", "E", "E" }, { "E", "E", "E" } },
                                        List.of("شمعة", "torch", "شعلة", "شمعه", "شعلة نار", "تورتش", "تورچ", "الشمعه",
                                                        "الشمعة"),
                                        "شمعة", Difficulty.EASY),
                        new Recipe(new String[][] { { "S", "E", "S" }, { "S", "S", "S" }, { "S", "E", "S" } },
                                        List.of("سلم", "ladder", "سلم خشب", "السلم", "لادر", "درج", "سلالم", "درج خشب"),
                                        "سلم خشب", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "E" }, { "W", "W", "E" }, { "W", "W", "E" } },
                                        List.of("باب", "door", "باب خشب", "الباب", "دور", "باب خشبي"), "باب خشب",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("سلاب", "slab", "بلاطة", "بلاطة خشب", "بلاطه", "نصف بلوكة", "نص بلوكه",
                                                        "نص بلوكة", "نصف بلوكه خشب"),
                                        "بلاطة خشب", Difficulty.EASY),

                        // MEDIUM
                        new Recipe(new String[][] { { "E", "D", "E" }, { "E", "D", "E" }, { "E", "S", "E" } },
                                        List.of("سيف", "sword", "سيف دايموند", "سيف الدايموند", "السيف", "دايموند سورد",
                                                        "سيف الماس", "سيف الماسي", "دايموند سيف"),
                                        "سيف دايموند",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("بيكاكس", "pickaxe", "فأس", "بيكاكس دايموند", "بيكاكس الدايموند",
                                                        "الفأس",
                                                        "دايموند بيكاكس", "معول", "معول دايموند", "معول الماس",
                                                        "بيكاكس الماسي"),
                                        "بيكاكس دايموند", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "E", "W" }, { "W", "W", "W" } },
                                        List.of("صندوق", "chest", "تشيست", "الصندوق", "صندوق خشب", "چيست", "چست",
                                                        "تشست", "صندوق عادي"),
                                        "صندوق", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "S", "L" }, { "S", "E", "L" }, { "E", "S", "L" } },
                                        List.of("قوس", "bow", "سهم", "القوس", "سهم وقوس", "بوو", "بو", "قوس رماية",
                                                        "قوس وسهم"),
                                        "قوس", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "E", "S" }, { "E", "S", "L" }, { "S", "E", "L" } },
                                        List.of("صنارة", "fishing rod", "صنارة صيد", "الصنارة", "صناره", "فيشنق رود",
                                                        "سنارة", "سنارة صيد"),
                                        "صنارة صيد",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "I", "E", "I" }, { "E", "E", "E" } },
                                        List.of("خوذة", "helmet", "خوذة حديد", "خوذه", "الخوذة", "هيلت", "طاقية حديد",
                                                        "خوذة ايرون", "ايرون هلمت", "ايرون هيلمت", "هلمت ايرون"),
                                        "خوذة حديد", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "I", "E" }, { "I", "R", "I" }, { "E", "I", "E" } },
                                        List.of("بوصلة", "compass", "البوصلة", "بوصله", "كمباس", "كومباس", "مؤشر",
                                                        "كومپاس"),
                                        "بوصلة", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "G", "E" }, { "G", "R", "G" }, { "E", "G", "E" } },
                                        List.of("ساعة", "clock", "ساعه", "الساعة", "كلوك", "ساعة وقت"), "ساعة",
                                        Difficulty.MEDIUM),

                        // HARD
                        new Recipe(new String[][] { { "I", "I", "I" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("بيكاكس حديد", "iron pickaxe", "بيكاكس الحديد", "ايرون بيكاكس",
                                                        "معول حديد", "معول ايرون"),
                                        "بيكاكس حديد",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("بيكاكس ذهب", "gold pickaxe", "بيكاكس الذهب", "قولد بيكاكس", "معول ذهب",
                                                        "معول قولد", "بيكاكس قولد", "جولد بيكاكس"),
                                        "بيكاكس ذهب",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "P", "P", "P" }, { "P", "R", "P" }, { "P", "P", "P" } },
                                        List.of("خريطة", "map", "ماب", "خريطة فارغة", "الخريطة", "ورقة خريطة", "خريطه",
                                                        "خريطه فارغه"),
                                        "خريطة فارغة", Difficulty.HARD),
                        new Recipe(new String[][] { { "B", "B", "B" }, { "B", "E", "B" }, { "B", "B", "B" } },
                                        List.of("فرن", "furnace", "الفرن", "فرنيس", "فرن حجر", "فورناس"), "فرن حجري",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "E", "H", "E" }, { "D", "O", "D" }, { "O", "O", "O" } },
                                        List.of("طاولة تطوير", "enchantment table", "تطوير", "طاوله تطوير",
                                                        "طاولة التطوير",
                                                        "انشانتمنت تيبل", "انشانتمنت", "انشانت تيبل", "طاولة سحر",
                                                        "طاوله سحر"),
                                        "طاولة تطوير", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "E", "I", "E" }, { "I", "I", "I" } },
                                        List.of("سندان", "anvil", "السندان", "انفيل", "انفل", "آنفيل", "طاولة تصليح"),
                                        "سندان", Difficulty.HARD),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "R", "W" }, { "W", "W", "W" } },
                                        List.of("نوت بلوك", "note block", "موسيقى", "النوت بلوك", "بلوكة موسيقى",
                                                        "نوت بلوكه", "بلوك موسيقي"),
                                        "نوت بلوك", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "E", "I" }, { "I", "I", "I" }, { "I", "I", "I" } },
                                        List.of("درع", "chestplate", "درع حديد", "الدرع", "درع الحديد", "تشيست بليت",
                                                        "ايرون تشيست بليت", "ايرون شستبل بليت", "تشست بليت ايرون",
                                                        "ايرون تشست بليت", "شست بليت ايرون", "ايرون شست بليت"),
                                        "درع حديد",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "G", "A", "G" }, { "G", "G", "G" } },
                                        List.of("تفاحة ذهبية", "golden apple", "تفاحة ذهب", "قولدن ابل", "جولدن ابل",
                                                        "تفاحه ذهبيه", "تفاحه ذهب", "قولدن آبل", "جولدن آبل"),
                                        "تفاحة ذهبية",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "O", "O", "O" }, { "O", "N", "O" }, { "O", "O", "O" } },
                                        List.of("صندوق اندر", "ender chest", "اندر تشيست", "اندر چست", "صندوق الاند",
                                                        "اندر چيست"),
                                        "صندوق اندر", Difficulty.HARD),
                        new Recipe(new String[][] { { "X", "X", "X" }, { "X", "K", "X" }, { "O", "O", "O" } },
                                        List.of("بيكون", "beacon", "منارة", "بيكن", "ليزر", "البيكون", "مناره"),
                                        "بيكون", Difficulty.HARD),
                        new Recipe(new String[][] { { "F", "Z", "F" }, { "Z", "F", "Z" }, { "F", "Z", "F" } },
                                        List.of("تي ان تي", "tnt", "متفجرات", "قنبلة", "قنبله", "تي إن تي", "ديناميت"),
                                        "TNT", Difficulty.HARD),
                        new Recipe(new String[][] { { "E", "I", "E" }, { "E", "S", "E" }, { "E", "J", "E" } },
                                        List.of("سهم", "arrow", "سهام", "سهم رماية", "سهم قتال"), "سهم",
                                        Difficulty.HARD),

                        // TOOLS (DIAMOND)
                        new Recipe(new String[][] { { "D", "D", "E" }, { "D", "S", "E" }, { "E", "S", "E" } },
                                        List.of("فأس دايموند", "diamond axe", "فأس", "الفأس", "اكس دايموند",
                                                        "دايموند اكس", "فأس الماس"),
                                        "فأس دايموند", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "D", "E" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("مجرفة دايموند", "diamond shovel", "مجرفة", "مجرفه", "شوفل دايموند",
                                                        "دايموند شوفل", "شفل دايموند", "مجرفة الماس", "دايموند شفل"),
                                        "مجرفة دايموند", Difficulty.EASY),
                        new Recipe(new String[][] { { "D", "D", "E" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("فأس زراعة", "diamond hoe", "محراث", "هو دايموند", "دايموند هو",
                                                        "محراث دايموند", "فأس زراعه دايموند"),
                                        "فأس زراعة دايموند", Difficulty.MEDIUM),

                        // ARMOR (DIAMOND)
                        new Recipe(new String[][] { { "D", "E", "D" }, { "D", "E", "D" }, { "E", "E", "E" } },
                                        List.of("حذاء دايموند", "diamond boots", "بوت", "حذاء", "بوتس دايموند",
                                                        "دايموند بوت", "دايموند بوتس", "شوز دايموند", "حذاء الماس"),
                                        "حذاء دايموند", Difficulty.EASY),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "D", "E", "D" }, { "D", "E", "D" } },
                                        List.of("سروال دايموند", "diamond leggings", "بنطلون", "سروال", "لقينز دايموند",
                                                        "دايموند لقينز", "لغينز دايموند", "دايموند لغينز",
                                                        "بنطلون دايموند", "سروال الماس"),
                                        "سروال دايموند", Difficulty.HARD),

                        // UTILITY
                        new Recipe(new String[][] { { "I", "E", "E" }, { "E", "I", "E" }, { "E", "E", "E" } },
                                        List.of("مقص", "shears", "المقص", "شيرز", "شير", "مقص خرفان"), "مقص",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "I", "E", "I" }, { "E", "I", "E" }, { "E", "E", "E" } },
                                        List.of("سطل", "bucket", "سطل حديد", "جردل", "بوكيت", "بكت", "سطل فاضي"),
                                        "سطل حديد", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "E", "W" }, { "E", "W", "E" }, { "E", "E", "E" } },
                                        List.of("وعاء", "bowl", "صحن", "بادية", "بول", "صحن خشب", "وعاء خشب"),
                                        "وعاء خشبي", Difficulty.EASY),
                        new Recipe(new String[][] { { "P", "P", "P" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ورق", "paper", "ورقة", "اوراق", "ورقه", "بيبر"), "ورق",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "WH", "WH", "WH" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("خبز", "bread", "الخبز", "خبزة", "خبزه", "بريد"), "خبز",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "WH", "WH", "WH" }, { "WH", "WH", "WH" }, { "WH", "WH", "WH" } },
                                        List.of("بلوك قش", "hay bale", "بلوك القش", "قش", "هيبيل", "هي بيل", "بلوكة قش",
                                                        "بلوكه قش"),
                                        "بلوك قش", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "S", "S", "S" }, { "S", "U", "S" }, { "S", "S", "S" } },
                                        List.of("لوحة", "painting", "لوحه", "صورة", "صوره", "بينتنق", "لوحة رسم"),
                                        "لوحة فنية", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "U", "U", "U" }, { "W", "W", "W" }, { "E", "E", "E" } },
                                        List.of("سرير", "bed", "السرير", "فراش", "بيد"), "سرير", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "S", "W" }, { "W", "S", "W" }, { "E", "E", "E" } },
                                        List.of("سياج", "fence", "سور", "فينس", "حاجز خشب", "سياج خشب"), "سياج خشبي",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "S", "W", "S" }, { "S", "W", "S" }, { "E", "E", "E" } },
                                        List.of("بوابة سياج", "fence gate", "بوابة", "بوابه", "فينس قيت", "فينس جيت",
                                                        "باب سياج", "بوابة سور"),
                                        "بوابة سياج", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "B", "B", "B" }, { "B", "L", "B" }, { "B", "R", "B" } },
                                        List.of("ديسبنسر", "dispenser", "موزع", "دسبنسر", "رامي"), "ديسبنسر",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "X", "X", "X" }, { "Q", "Q", "Q" }, { "W", "W", "W" } },
                                        List.of("حساس ضوء", "daylight sensor", "ديلايت سنسر", "سنسور", "حساس شمس",
                                                        "حساس ضوئي"),
                                        "حساس ضوء الشمس", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "I", "I", "I" }, { "I", "I", "I" } },
                                        List.of("بلوك حديد", "iron block", "ايرون بلوك", "بلوكة حديد", "بلوكة ايرون",
                                                        "بلوكه حديد"),
                                        "بلوك حديد", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "G", "G", "G" }, { "G", "G", "G" } },
                                        List.of("بلوك ذهب", "gold block", "قولد بلوك", "جولد بلوك", "بلوكة ذهب",
                                                        "بلوكة قولد", "بلوكه ذهب"),
                                        "بلوك ذهب", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "D", "D", "D" }, { "D", "D", "D" } },
                                        List.of("بلوك دايموند", "diamond block", "دايموند بلوك", "بلوكة دايموند",
                                                        "بلوكة الماس", "بلوكه دايموند"),
                                        "بلوك دايموند", Difficulty.HARD),
                        new Recipe(new String[][] { { "M", "M", "M" }, { "M", "M", "M" }, { "M", "M", "M" } },
                                        List.of("بلوك زمرد", "emerald block", "بلوك ايمرلد", "ايمرلد بلوك",
                                                        "بلوكة ايمرلد", "بلوكه زمرد"),
                                        "بلوك زمرد", Difficulty.HARD));

        public String startCraft(String sessionId, long userId, Difficulty difficulty, Guild guild, Member organizer) {
                List<Recipe> possible = RECIPES.stream().filter(r -> r.difficulty == difficulty).toList();
                Recipe recipe = possible.get(new Random().nextInt(possible.size()));

                sessionActiveRecipes.put(sessionId, recipe);
                sessionRewards.put(sessionId, (long) difficulty.reward);
                sessionDifficulty.put(sessionId, difficulty);
                sessionMentions.put(sessionId, organizer.getAsMention());
                sessionGuilds.put(sessionId, guild.getIdLong());
                sessionUserIds.put(sessionId, userId);

                StringBuilder sb = new StringBuilder();
                sb.append("      **1**            **2**            **3**\n");
                sb.append("▬▬▬▬▬▬▬▬▬▬▬▬\n");
                for (int i = 0; i < 3; i++) {
                        sb.append("**").append(i + 1).append("**  ");
                        for (int j = 0; j < 3; j++) {
                                String item = ITEMS.get(recipe.grid[i][j]);
                                // If it's a custom emoji, add extra spaces around it to make it look
                                // larger/centered
                                sb.append("   ").append(item).append("   ");
                        }
                        sb.append("\n\n"); // Double newline for row spacing
                }
                sb.append("▬▬▬▬▬▬▬▬▬▬▬▬");

                sessionGrids.put(sessionId, sb.toString());

                // LOGGING SYSTEM
                String logDetails = String.format(
                                "### فعالية الصناعة: بدء (فردية)\n▫️ **اللاعب:** %s\n▫️ **الصعوبة:** %s\n▫️ **الجائزة:** %d opex\n▫️ **ID الجلسة:** `%s`",
                                organizer.getAsMention(), difficulty.displayName, difficulty.reward, sessionId);
                logManager.logEmbed(guild, LogManager.LOG_GAMES,
                                EmbedUtil.createOldLogEmbed("craft", logDetails, organizer, null, null,
                                                EmbedUtil.INFO));

                return sb.toString();
        }

        public void initTimer(String sessionId, Difficulty difficulty,
                        net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
                final int[] timeLeft = { difficulty.seconds };
                sessionHooks.put(sessionId, event.getHook());
                long userId = sessionUserIds.getOrDefault(sessionId, 0L);
                String grid = sessionGrids.get(sessionId);

                log.info("[CraftTimer] Initializing timer for session: {} (User: {})", sessionId, userId);
                java.util.concurrent.ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                        try {
                                if (!sessionActiveRecipes.containsKey(sessionId))
                                        return;
                                log.debug("[CraftTimer] Tick for session: {}", sessionId);
                                timeLeft[0]--;

                                if (timeLeft[0] <= 0) {
                                        Recipe activeRecipe = sessionActiveRecipes.get(sessionId);
                                        if (activeRecipe != null) {
                                                String failMsg = String.format(
                                                                "**انتهى الوقت!** لم تنجح في تخمين الشيء المطلوب.\nالشيء الصحيح هو: **%s**\nحظاً أوفر في المرة القادمة.",
                                                                activeRecipe.displayName);
                                                event.getHook().editOriginal(
                                                                new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                                                                                .setComponents(EmbedUtil.error(
                                                                                                "CRAFTING TIMEOUT",
                                                                                                "`=---------------- 00:00 ----------------=`\n\n"
                                                                                                                + failMsg))
                                                                                .useComponentsV2(true)
                                                                                .build())
                                                                .queue();
                                        }
                                        stopTimer(sessionId);
                                        return;
                                }

                                String body = getCraftBody(sessionMentions.get(sessionId), grid, difficulty.reward,
                                                timeLeft[0]);
                                net.dv8tion.jda.api.interactions.InteractionHook hook = event.getHook();

                                hook.editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                                                .setComponents(
                                                                EmbedUtil.containerBranded("CRAFTING", "ماذا نصنع؟",
                                                                                body, EmbedUtil.BANNER_MAIN))
                                                .useComponentsV2(true)
                                                .build()).queue(null, e -> {
                                                        if (e.getMessage() != null && (e.getMessage()
                                                                        .contains("Unknown Interaction")
                                                                        || e.getMessage().contains("expired"))) {
                                                                stopTimer(sessionId);
                                                        }
                                                });

                                if (timeLeft[0] % 5 == 0) {
                                        log.info("[CraftTimer] Session {} - Time Left: {}", sessionId, timeLeft[0]);
                                }
                        } catch (Exception e) {
                                log.error("[CraftTimer] Fatal error for session {}: ", sessionId, e);
                        }
                }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);

                sessionTimers.put(sessionId, future);
        }

        private String getCraftBody(String mention, String grid, int reward, int seconds) {
                String timerFormat = String.format("`=----------------%02d:%02d----------------=`", 0, seconds);
                return timerFormat + "\n\n" +
                                String.format("أمامك طاولة كرافتنق خاصة بك يا %s... خمن ما هو الشيء الذي يتم صنعه؟\n\n",
                                                mention)
                                +
                                grid + "\n" +
                                "الجائزة: **" + reward + " opex**\n\n" +
                                "اكتب الإجابة مباشرة في الشات!";
        }

        public void stopTimer(String sessionId) {
                if (sessionTimers.containsKey(sessionId)) {
                        sessionTimers.get(sessionId).cancel(true);
                        sessionTimers.remove(sessionId);
                }
                sessionActiveRecipes.remove(sessionId);
                sessionRewards.remove(sessionId);
                sessionDifficulty.remove(sessionId);
                sessionMentions.remove(sessionId);
                sessionGuilds.remove(sessionId);
                sessionGrids.remove(sessionId);
                sessionUserIds.remove(sessionId);
                sessionHooks.remove(sessionId);
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
                if (event.getAuthor().isBot())
                        return;
                long userId = event.getAuthor().getIdLong();

                String content = event.getMessage().getContentRaw().trim().toLowerCase();

                // Check all active sessions for this user
                List<String> sessionsToProcess = new ArrayList<>();
                for (Map.Entry<String, Long> entry : sessionUserIds.entrySet()) {
                        if (entry.getValue() == userId) {
                                sessionsToProcess.add(entry.getKey());
                        }
                }

                for (String sessionId : sessionsToProcess) {
                        Recipe activeRecipe = sessionActiveRecipes.get(sessionId);
                        if (activeRecipe == null)
                                continue;

                        if (activeRecipe.possibleNames.contains(content)) {
                                String itemName = activeRecipe.displayName;
                                long reward = sessionRewards.getOrDefault(sessionId, 0L);
                                net.dv8tion.jda.api.interactions.InteractionHook hook = sessionHooks.get(sessionId);

                                stopTimer(sessionId);

                                economyService.addBalance(event.getAuthor().getId(), event.getGuild().getId(),
                                                (int) reward);
                                achievementService.updateStats(userId, event.getGuild(),
                                                s -> s.setCraftWins(s.getCraftWins() + 1));

                                String successMsg = String.format(
                                                "**كفوو!** إجابة صحيحة يا %s!\nتم صنع: **%s** بنجاح.\nحصلت على **%d opex**",
                                                event.getAuthor().getAsMention(), itemName, reward);

                                if (hook != null) {
                                        hook.editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                                                        .setComponents(EmbedUtil.success("CRAFTING SUCCESS",
                                                                        successMsg))
                                                        .useComponentsV2(true)
                                                        .build())
                                                        .useComponentsV2(true)
                                                        .queue();
                                        event.getMessage().delete().queue(null, e -> {
                                        });
                                } else {
                                        event.getChannel().sendMessage(new MessageCreateBuilder()
                                                        .setComponents(EmbedUtil.success("CRAFTING MASTER", successMsg))
                                                        .useComponentsV2(true)
                                                        .build())
                                                        .useComponentsV2(true)
                                                        .queue();
                                }

                                // LOG WIN
                                String logWin = String.format(
                                                "### فعالية الصناعة: فوز (فردية)\n▫️ **الفائز:** <@%s>\n▫️ **الصعوبة:** %s\n▫️ **الشيء:** %s\n▫️ **ID الجلسة:** `%s`",
                                                event.getAuthor().getId(), activeRecipe.difficulty.displayName,
                                                itemName, sessionId);
                                logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES,
                                                EmbedUtil.createOldLogEmbed("craft_win", logWin, event.getMember(),
                                                                null, null,
                                                                EmbedUtil.SUCCESS));

                                // Break after first win to avoid multiple wins with one message if answers are
                                // same
                                break;
                        }
                }
        }
}
