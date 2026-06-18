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
                EASY(10, 15, "ط³ظ‡ظ„"),
                MEDIUM(15, 12, "ظˆط³ط·"),
                HARD(20, 10, "طµط¹ط¨");

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
                        Map.entry("W", "<:oak_planks:1500879889119707266>"),
                        Map.entry("S", "<:stick:1500879473992794212>"),
                        Map.entry("I", "<:Minecraft_Iron_Ingot:1500878789402693744>"),
                        Map.entry("G", "<:gold_ingot:1500878971410055330>"),
                        Map.entry("D", "<:dimoand:1500878887662518334>"),
                        Map.entry("P", "<:Minecraft_Sugar_Cane_Item__HD_Pn:1500879626824585426>"),
                        Map.entry("B", "<:coble_stone:1500879838041608243>"),
                        Map.entry("C", "<:Coal_JE4_BE3:1500973041805557954>"),
                        Map.entry("E", "ًں”³"),
                        Map.entry("R", "<:red_stone:1500879139010383913>"),
                        Map.entry("L", "<:string:1500880235510497360>"),
                        Map.entry("F", "<:Minecraft_Gunpowder_pngremovebgp:1500879430367707366>"),
                        Map.entry("Q", "<:quartz:1500880509960589404>"),
                        Map.entry("X", "<:glass:1500880071466942586>"),
                        Map.entry("O", "<:obsadian:1500879689659584643>"),
                        Map.entry("T", "<:torch:1500879281654464652>"),
                        Map.entry("H", "<:Enchanted_Book__Minecraft_Plugin:1500879162246828203>"),
                        Map.entry("A", "<:red_apple:1500880776655540285>"),
                        Map.entry("N", "<:ender_eye:1500880557641568347>"),
                        Map.entry("K", "<:neather_star:1500880657986097172>"),
                        Map.entry("U", "<:leather:1500880346206568498>"),
                        Map.entry("Z", "<:sand:1500879926696607846>"),
                        Map.entry("V", "<:water_empty_bottle:1500880709315723428>"),
                        Map.entry("M", "<:emraled_ingot:1500879037868806237>"),
                        Map.entry("Y", "<:hay_bale:1500880117302562856>"),
                        Map.entry("WH", "<:wheat_minecraft_itemsremovebgpre:1500879408032911492>"),
                        Map.entry("J", "<:feather:1500880285431238807>")
        );

        @RequiredArgsConstructor
        private static class Recipe {
                final String[][] grid;
                final List<String> possibleNames;
                final String displayName;
                final Difficulty difficulty;
        }

        private static final List<Recipe> RECIPES = List.of(

                        new Recipe(new String[][] { { "W", "W", "E" }, { "W", "W", "E" }, { "E", "E", "E" } },
                                        List.of("ظˆط±ظƒ ط¨ظٹظ†ط´", "ط·ط§ظˆظ„ط© طµظ†ط¹", "crafting table", "workbench", "ط·ط§ظˆظ„ظ‡ طµظ†ط¹",
                                                        "ط·ط§ظˆظ„ط© ط§ظ„طµظ†ط§ط¹ط©",
                                                        "ظƒط±ظپطھظ†ظ‚ طھظٹط¨ظ„", "ظƒط±ط§ظپطھظ†ظ‚ طھظٹط¨ظ„", "ط·ط§ظˆظ„ط© ط§ظ„ظƒط±ط§ظپطھظ†ظ‚",
                                                        "ظƒط±ط§ظپطھظ†ط¬ طھظٹط¨ظ„"),
                                        "ط·ط§ظˆظ„ط© طµظ†ط¹", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "E", "E" }, { "W", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ط¹طµط§", "stick", "ط¹طµط§ظٹ", "ط§ظ„ط¹طµط§", "ط³طھظٹظƒ", "ط¹طµظٹط§ظ†", "ط®ط´ط¨ ط¹طµط§", "ط¹طµط§ظٹط©"),
                                        "ط¹طµط§", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "W", "W" }, { "W", "W", "W" } },
                                        List.of("ط¨ظ„ظˆظƒ ط®ط´ط¨", "wood block", "ط®ط´ط¨", "ط§ظ„ط®ط´ط¨", "ط¨ظ„ظˆظƒ ط§ظ„ط®ط´ط¨", "planks",
                                                        "wood", "ط®ط´ط¨ ظ…ط­ظ„ظ„", "ط¨ظ„ظˆظƒط© ط®ط´ط¨", "ط¨ظ„ظˆظƒظ‡ ط®ط´ط¨"),
                                        "ط¨ظ„ظˆظƒ ط®ط´ط¨",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "C", "E", "E" }, { "S", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ط´ظ…ط¹ط©", "torch", "ط´ط¹ظ„ط©", "ط´ظ…ط¹ظ‡", "ط´ط¹ظ„ط© ظ†ط§ط±", "طھظˆط±طھط´", "طھظˆط±ع†", "ط§ظ„ط´ظ…ط¹ظ‡",
                                                        "ط§ظ„ط´ظ…ط¹ط©"),
                                        "ط´ظ…ط¹ط©", Difficulty.EASY),
                        new Recipe(new String[][] { { "S", "E", "S" }, { "S", "S", "S" }, { "S", "E", "S" } },
                                        List.of("ط³ظ„ظ…", "ladder", "ط³ظ„ظ… ط®ط´ط¨", "ط§ظ„ط³ظ„ظ…", "ظ„ط§ط¯ط±", "ط¯ط±ط¬", "ط³ظ„ط§ظ„ظ…", "ط¯ط±ط¬ ط®ط´ط¨"),
                                        "ط³ظ„ظ… ط®ط´ط¨", Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "E" }, { "W", "W", "E" }, { "W", "W", "E" } },
                                        List.of("ط¨ط§ط¨", "door", "ط¨ط§ط¨ ط®ط´ط¨", "ط§ظ„ط¨ط§ط¨", "ط¯ظˆط±", "ط¨ط§ط¨ ط®ط´ط¨ظٹ"), "ط¨ط§ط¨ ط®ط´ط¨",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ط³ظ„ط§ط¨", "slab", "ط¨ظ„ط§ط·ط©", "ط¨ظ„ط§ط·ط© ط®ط´ط¨", "ط¨ظ„ط§ط·ظ‡", "ظ†طµظپ ط¨ظ„ظˆظƒط©", "ظ†طµ ط¨ظ„ظˆظƒظ‡",
                                                        "ظ†طµ ط¨ظ„ظˆظƒط©", "ظ†طµظپ ط¨ظ„ظˆظƒظ‡ ط®ط´ط¨"),
                                        "ط¨ظ„ط§ط·ط© ط®ط´ط¨", Difficulty.EASY),

                        new Recipe(new String[][] { { "E", "D", "E" }, { "E", "D", "E" }, { "E", "S", "E" } },
                                        List.of("ط³ظٹظپ", "sword", "ط³ظٹظپ ط¯ط§ظٹظ…ظˆظ†ط¯", "ط³ظٹظپ ط§ظ„ط¯ط§ظٹظ…ظˆظ†ط¯", "ط§ظ„ط³ظٹظپ", "ط¯ط§ظٹظ…ظˆظ†ط¯ ط³ظˆط±ط¯",
                                                        "ط³ظٹظپ ط§ظ„ظ…ط§ط³", "ط³ظٹظپ ط§ظ„ظ…ط§ط³ظٹ", "ط¯ط§ظٹظ…ظˆظ†ط¯ ط³ظٹظپ"),
                                        "ط³ظٹظپ ط¯ط§ظٹظ…ظˆظ†ط¯",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ط¨ظٹظƒط§ظƒط³", "pickaxe", "ظپط£ط³", "ط¨ظٹظƒط§ظƒط³ ط¯ط§ظٹظ…ظˆظ†ط¯", "ط¨ظٹظƒط§ظƒط³ ط§ظ„ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط§ظ„ظپط£ط³",
                                                        "ط¯ط§ظٹظ…ظˆظ†ط¯ ط¨ظٹظƒط§ظƒط³", "ظ…ط¹ظˆظ„", "ظ…ط¹ظˆظ„ ط¯ط§ظٹظ…ظˆظ†ط¯", "ظ…ط¹ظˆظ„ ط§ظ„ظ…ط§ط³",
                                                        "ط¨ظٹظƒط§ظƒط³ ط§ظ„ظ…ط§ط³ظٹ"),
                                        "ط¨ظٹظƒط§ظƒط³ ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "E", "W" }, { "W", "W", "W" } },
                                        List.of("طµظ†ط¯ظˆظ‚", "chest", "طھط´ظٹط³طھ", "ط§ظ„طµظ†ط¯ظˆظ‚", "طµظ†ط¯ظˆظ‚ ط®ط´ط¨", "ع†ظٹط³طھ", "ع†ط³طھ",
                                                        "طھط´ط³طھ", "طµظ†ط¯ظˆظ‚ ط¹ط§ط¯ظٹ"),
                                        "طµظ†ط¯ظˆظ‚", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "S", "L" }, { "S", "E", "L" }, { "E", "S", "L" } },
                                        List.of("ظ‚ظˆط³", "bow", "ط³ظ‡ظ…", "ط§ظ„ظ‚ظˆط³", "ط³ظ‡ظ… ظˆظ‚ظˆط³", "ط¨ظˆظˆ", "ط¨ظˆ", "ظ‚ظˆط³ ط±ظ…ط§ظٹط©",
                                                        "ظ‚ظˆط³ ظˆط³ظ‡ظ…"),
                                        "ظ‚ظˆط³", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "E", "S" }, { "E", "S", "L" }, { "S", "E", "L" } },
                                        List.of("طµظ†ط§ط±ط©", "fishing rod", "طµظ†ط§ط±ط© طµظٹط¯", "ط§ظ„طµظ†ط§ط±ط©", "طµظ†ط§ط±ظ‡", "ظپظٹط´ظ†ظ‚ ط±ظˆط¯",
                                                        "ط³ظ†ط§ط±ط©", "ط³ظ†ط§ط±ط© طµظٹط¯"),
                                        "طµظ†ط§ط±ط© طµظٹط¯",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "I", "E", "I" }, { "E", "E", "E" } },
                                        List.of("ط®ظˆط°ط©", "helmet", "ط®ظˆط°ط© ط­ط¯ظٹط¯", "ط®ظˆط°ظ‡", "ط§ظ„ط®ظˆط°ط©", "ظ‡ظٹظ„طھ", "ط·ط§ظ‚ظٹط© ط­ط¯ظٹط¯",
                                                        "ط®ظˆط°ط© ط§ظٹط±ظˆظ†", "ط§ظٹط±ظˆظ† ظ‡ظ„ظ…طھ", "ط§ظٹط±ظˆظ† ظ‡ظٹظ„ظ…طھ", "ظ‡ظ„ظ…طھ ط§ظٹط±ظˆظ†"),
                                        "ط®ظˆط°ط© ط­ط¯ظٹط¯", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "I", "E" }, { "I", "R", "I" }, { "E", "I", "E" } },
                                        List.of("ط¨ظˆطµظ„ط©", "compass", "ط§ظ„ط¨ظˆطµظ„ط©", "ط¨ظˆطµظ„ظ‡", "ظƒظ…ط¨ط§ط³", "ظƒظˆظ…ط¨ط§ط³", "ظ…ط¤ط´ط±",
                                                        "ظƒظˆظ…ظ¾ط§ط³"),
                                        "ط¨ظˆطµظ„ط©", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "G", "E" }, { "G", "R", "G" }, { "E", "G", "E" } },
                                        List.of("ط³ط§ط¹ط©", "clock", "ط³ط§ط¹ظ‡", "ط§ظ„ط³ط§ط¹ط©", "ظƒظ„ظˆظƒ", "ط³ط§ط¹ط© ظˆظ‚طھ"), "ط³ط§ط¹ط©",
                                        Difficulty.MEDIUM),

                        new Recipe(new String[][] { { "I", "I", "I" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ط¨ظٹظƒط§ظƒط³ ط­ط¯ظٹط¯", "iron pickaxe", "ط¨ظٹظƒط§ظƒط³ ط§ظ„ط­ط¯ظٹط¯", "ط§ظٹط±ظˆظ† ط¨ظٹظƒط§ظƒط³",
                                                        "ظ…ط¹ظˆظ„ ط­ط¯ظٹط¯", "ظ…ط¹ظˆظ„ ط§ظٹط±ظˆظ†"),
                                        "ط¨ظٹظƒط§ظƒط³ ط­ط¯ظٹط¯",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ط¨ظٹظƒط§ظƒط³ ط°ظ‡ط¨", "gold pickaxe", "ط¨ظٹظƒط§ظƒط³ ط§ظ„ط°ظ‡ط¨", "ظ‚ظˆظ„ط¯ ط¨ظٹظƒط§ظƒط³", "ظ…ط¹ظˆظ„ ط°ظ‡ط¨",
                                                        "ظ…ط¹ظˆظ„ ظ‚ظˆظ„ط¯", "ط¨ظٹظƒط§ظƒط³ ظ‚ظˆظ„ط¯", "ط¬ظˆظ„ط¯ ط¨ظٹظƒط§ظƒط³"),
                                        "ط¨ظٹظƒط§ظƒط³ ط°ظ‡ط¨",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "P", "P", "P" }, { "P", "R", "P" }, { "P", "P", "P" } },
                                        List.of("ط®ط±ظٹط·ط©", "map", "ظ…ط§ط¨", "ط®ط±ظٹط·ط© ظپط§ط±ط؛ط©", "ط§ظ„ط®ط±ظٹط·ط©", "ظˆط±ظ‚ط© ط®ط±ظٹط·ط©", "ط®ط±ظٹط·ظ‡",
                                                        "ط®ط±ظٹط·ظ‡ ظپط§ط±ط؛ظ‡"),
                                        "ط®ط±ظٹط·ط© ظپط§ط±ط؛ط©", Difficulty.HARD),
                        new Recipe(new String[][] { { "B", "B", "B" }, { "B", "E", "B" }, { "B", "B", "B" } },
                                        List.of("ظپط±ظ†", "furnace", "ط§ظ„ظپط±ظ†", "ظپط±ظ†ظٹط³", "ظپط±ظ† ط­ط¬ط±", "ظپظˆط±ظ†ط§ط³"), "ظپط±ظ† ط­ط¬ط±ظٹ",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "E", "H", "E" }, { "D", "O", "D" }, { "O", "O", "O" } },
                                        List.of("ط·ط§ظˆظ„ط© طھط·ظˆظٹط±", "enchantment table", "طھط·ظˆظٹط±", "ط·ط§ظˆظ„ظ‡ طھط·ظˆظٹط±",
                                                        "ط·ط§ظˆظ„ط© ط§ظ„طھط·ظˆظٹط±",
                                                        "ط§ظ†ط´ط§ظ†طھظ…ظ†طھ طھظٹط¨ظ„", "ط§ظ†ط´ط§ظ†طھظ…ظ†طھ", "ط§ظ†ط´ط§ظ†طھ طھظٹط¨ظ„", "ط·ط§ظˆظ„ط© ط³ط­ط±",
                                                        "ط·ط§ظˆظ„ظ‡ ط³ط­ط±"),
                                        "ط·ط§ظˆظ„ط© طھط·ظˆظٹط±", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "E", "I", "E" }, { "I", "I", "I" } },
                                        List.of("ط³ظ†ط¯ط§ظ†", "anvil", "ط§ظ„ط³ظ†ط¯ط§ظ†", "ط§ظ†ظپظٹظ„", "ط§ظ†ظپظ„", "ط¢ظ†ظپظٹظ„", "ط·ط§ظˆظ„ط© طھطµظ„ظٹط­"),
                                        "ط³ظ†ط¯ط§ظ†", Difficulty.HARD),
                        new Recipe(new String[][] { { "W", "W", "W" }, { "W", "R", "W" }, { "W", "W", "W" } },
                                        List.of("ظ†ظˆطھ ط¨ظ„ظˆظƒ", "note block", "ظ…ظˆط³ظٹظ‚ظ‰", "ط§ظ„ظ†ظˆطھ ط¨ظ„ظˆظƒ", "ط¨ظ„ظˆظƒط© ظ…ظˆط³ظٹظ‚ظ‰",
                                                        "ظ†ظˆطھ ط¨ظ„ظˆظƒظ‡", "ط¨ظ„ظˆظƒ ظ…ظˆط³ظٹظ‚ظٹ"),
                                        "ظ†ظˆطھ ط¨ظ„ظˆظƒ", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "E", "I" }, { "I", "I", "I" }, { "I", "I", "I" } },
                                        List.of("ط¯ط±ط¹", "chestplate", "ط¯ط±ط¹ ط­ط¯ظٹط¯", "ط§ظ„ط¯ط±ط¹", "ط¯ط±ط¹ ط§ظ„ط­ط¯ظٹط¯", "طھط´ظٹط³طھ ط¨ظ„ظٹطھ",
                                                        "ط§ظٹط±ظˆظ† طھط´ظٹط³طھ ط¨ظ„ظٹطھ", "ط§ظٹط±ظˆظ† ط´ط³طھط¨ظ„ ط¨ظ„ظٹطھ", "طھط´ط³طھ ط¨ظ„ظٹطھ ط§ظٹط±ظˆظ†",
                                                        "ط§ظٹط±ظˆظ† طھط´ط³طھ ط¨ظ„ظٹطھ", "ط´ط³طھ ط¨ظ„ظٹطھ ط§ظٹط±ظˆظ†", "ط§ظٹط±ظˆظ† ط´ط³طھ ط¨ظ„ظٹطھ"),
                                        "ط¯ط±ط¹ ط­ط¯ظٹط¯",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "G", "A", "G" }, { "G", "G", "G" } },
                                        List.of("طھظپط§ط­ط© ط°ظ‡ط¨ظٹط©", "golden apple", "طھظپط§ط­ط© ط°ظ‡ط¨", "ظ‚ظˆظ„ط¯ظ† ط§ط¨ظ„", "ط¬ظˆظ„ط¯ظ† ط§ط¨ظ„",
                                                        "طھظپط§ط­ظ‡ ط°ظ‡ط¨ظٹظ‡", "طھظپط§ط­ظ‡ ط°ظ‡ط¨", "ظ‚ظˆظ„ط¯ظ† ط¢ط¨ظ„", "ط¬ظˆظ„ط¯ظ† ط¢ط¨ظ„"),
                                        "طھظپط§ط­ط© ط°ظ‡ط¨ظٹط©",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "O", "O", "O" }, { "O", "N", "O" }, { "O", "O", "O" } },
                                        List.of("طµظ†ط¯ظˆظ‚ ط§ظ†ط¯ط±", "ender chest", "ط§ظ†ط¯ط± طھط´ظٹط³طھ", "ط§ظ†ط¯ط± ع†ط³طھ", "طµظ†ط¯ظˆظ‚ ط§ظ„ط§ظ†ط¯",
                                                        "ط§ظ†ط¯ط± ع†ظٹط³طھ"),
                                        "طµظ†ط¯ظˆظ‚ ط§ظ†ط¯ط±", Difficulty.HARD),
                        new Recipe(new String[][] { { "X", "X", "X" }, { "X", "K", "X" }, { "O", "O", "O" } },
                                        List.of("ط¨ظٹظƒظˆظ†", "beacon", "ظ…ظ†ط§ط±ط©", "ط¨ظٹظƒظ†", "ظ„ظٹط²ط±", "ط§ظ„ط¨ظٹظƒظˆظ†", "ظ…ظ†ط§ط±ظ‡"),
                                        "ط¨ظٹظƒظˆظ†", Difficulty.HARD),
                        new Recipe(new String[][] { { "F", "Z", "F" }, { "Z", "F", "Z" }, { "F", "Z", "F" } },
                                        List.of("طھظٹ ط§ظ† طھظٹ", "tnt", "ظ…طھظپط¬ط±ط§طھ", "ظ‚ظ†ط¨ظ„ط©", "ظ‚ظ†ط¨ظ„ظ‡", "طھظٹ ط¥ظ† طھظٹ", "ط¯ظٹظ†ط§ظ…ظٹطھ"),
                                        "TNT", Difficulty.HARD),
                        new Recipe(new String[][] { { "E", "I", "E" }, { "E", "S", "E" }, { "E", "J", "E" } },
                                        List.of("ط³ظ‡ظ…", "arrow", "ط³ظ‡ط§ظ…", "ط³ظ‡ظ… ط±ظ…ط§ظٹط©", "ط³ظ‡ظ… ظ‚طھط§ظ„"), "ط³ظ‡ظ…",
                                        Difficulty.HARD),

                        new Recipe(new String[][] { { "D", "D", "E" }, { "D", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ظپط£ط³ ط¯ط§ظٹظ…ظˆظ†ط¯", "diamond axe", "ظپط£ط³", "ط§ظ„ظپط£ط³", "ط§ظƒط³ ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط¯ط§ظٹظ…ظˆظ†ط¯ ط§ظƒط³", "ظپط£ط³ ط§ظ„ظ…ط§ط³"),
                                        "ظپط£ط³ ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "E", "D", "E" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ظ…ط¬ط±ظپط© ط¯ط§ظٹظ…ظˆظ†ط¯", "diamond shovel", "ظ…ط¬ط±ظپط©", "ظ…ط¬ط±ظپظ‡", "ط´ظˆظپظ„ ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط¯ط§ظٹظ…ظˆظ†ط¯ ط´ظˆظپظ„", "ط´ظپظ„ ط¯ط§ظٹظ…ظˆظ†ط¯", "ظ…ط¬ط±ظپط© ط§ظ„ظ…ط§ط³", "ط¯ط§ظٹظ…ظˆظ†ط¯ ط´ظپظ„"),
                                        "ظ…ط¬ط±ظپط© ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.EASY),
                        new Recipe(new String[][] { { "D", "D", "E" }, { "E", "S", "E" }, { "E", "S", "E" } },
                                        List.of("ظپط£ط³ ط²ط±ط§ط¹ط©", "diamond hoe", "ظ…ط­ط±ط§ط«", "ظ‡ظˆ ط¯ط§ظٹظ…ظˆظ†ط¯", "ط¯ط§ظٹظ…ظˆظ†ط¯ ظ‡ظˆ",
                                                        "ظ…ط­ط±ط§ط« ط¯ط§ظٹظ…ظˆظ†ط¯", "ظپط£ط³ ط²ط±ط§ط¹ظ‡ ط¯ط§ظٹظ…ظˆظ†ط¯"),
                                        "ظپط£ط³ ط²ط±ط§ط¹ط© ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.MEDIUM),

                        new Recipe(new String[][] { { "D", "E", "D" }, { "D", "E", "D" }, { "E", "E", "E" } },
                                        List.of("ط­ط°ط§ط، ط¯ط§ظٹظ…ظˆظ†ط¯", "diamond boots", "ط¨ظˆطھ", "ط­ط°ط§ط،", "ط¨ظˆطھط³ ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط¯ط§ظٹظ…ظˆظ†ط¯ ط¨ظˆطھ", "ط¯ط§ظٹظ…ظˆظ†ط¯ ط¨ظˆطھط³", "ط´ظˆط² ط¯ط§ظٹظ…ظˆظ†ط¯", "ط­ط°ط§ط، ط§ظ„ظ…ط§ط³"),
                                        "ط­ط°ط§ط، ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.EASY),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "D", "E", "D" }, { "D", "E", "D" } },
                                        List.of("ط³ط±ظˆط§ظ„ ط¯ط§ظٹظ…ظˆظ†ط¯", "diamond leggings", "ط¨ظ†ط·ظ„ظˆظ†", "ط³ط±ظˆط§ظ„", "ظ„ظ‚ظٹظ†ط² ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط¯ط§ظٹظ…ظˆظ†ط¯ ظ„ظ‚ظٹظ†ط²", "ظ„ط؛ظٹظ†ط² ط¯ط§ظٹظ…ظˆظ†ط¯", "ط¯ط§ظٹظ…ظˆظ†ط¯ ظ„ط؛ظٹظ†ط²",
                                                        "ط¨ظ†ط·ظ„ظˆظ† ط¯ط§ظٹظ…ظˆظ†ط¯", "ط³ط±ظˆط§ظ„ ط§ظ„ظ…ط§ط³"),
                                        "ط³ط±ظˆط§ظ„ ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.HARD),

                        new Recipe(new String[][] { { "I", "E", "E" }, { "E", "I", "E" }, { "E", "E", "E" } },
                                        List.of("ظ…ظ‚طµ", "shears", "ط§ظ„ظ…ظ‚طµ", "ط´ظٹط±ط²", "ط´ظٹط±", "ظ…ظ‚طµ ط®ط±ظپط§ظ†"), "ظ…ظ‚طµ",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "I", "E", "I" }, { "E", "I", "E" }, { "E", "E", "E" } },
                                        List.of("ط³ط·ظ„", "bucket", "ط³ط·ظ„ ط­ط¯ظٹط¯", "ط¬ط±ط¯ظ„", "ط¨ظˆظƒظٹطھ", "ط¨ظƒطھ", "ط³ط·ظ„ ظپط§ط¶ظٹ"),
                                        "ط³ط·ظ„ ط­ط¯ظٹط¯", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "E", "W" }, { "E", "W", "E" }, { "E", "E", "E" } },
                                        List.of("ظˆط¹ط§ط،", "bowl", "طµط­ظ†", "ط¨ط§ط¯ظٹط©", "ط¨ظˆظ„", "طµط­ظ† ط®ط´ط¨", "ظˆط¹ط§ط، ط®ط´ط¨"),
                                        "ظˆط¹ط§ط، ط®ط´ط¨ظٹ", Difficulty.EASY),
                        new Recipe(new String[][] { { "P", "P", "P" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ظˆط±ظ‚", "paper", "ظˆط±ظ‚ط©", "ط§ظˆط±ط§ظ‚", "ظˆط±ظ‚ظ‡", "ط¨ظٹط¨ط±"), "ظˆط±ظ‚",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "WH", "WH", "WH" }, { "E", "E", "E" }, { "E", "E", "E" } },
                                        List.of("ط®ط¨ط²", "bread", "ط§ظ„ط®ط¨ط²", "ط®ط¨ط²ط©", "ط®ط¨ط²ظ‡", "ط¨ط±ظٹط¯"), "ط®ط¨ط²",
                                        Difficulty.EASY),
                        new Recipe(new String[][] { { "WH", "WH", "WH" }, { "WH", "WH", "WH" }, { "WH", "WH", "WH" } },
                                        List.of("ط¨ظ„ظˆظƒ ظ‚ط´", "hay bale", "ط¨ظ„ظˆظƒ ط§ظ„ظ‚ط´", "ظ‚ط´", "ظ‡ظٹط¨ظٹظ„", "ظ‡ظٹ ط¨ظٹظ„", "ط¨ظ„ظˆظƒط© ظ‚ط´",
                                                        "ط¨ظ„ظˆظƒظ‡ ظ‚ط´"),
                                        "ط¨ظ„ظˆظƒ ظ‚ط´", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "S", "S", "S" }, { "S", "U", "S" }, { "S", "S", "S" } },
                                        List.of("ظ„ظˆط­ط©", "painting", "ظ„ظˆط­ظ‡", "طµظˆط±ط©", "طµظˆط±ظ‡", "ط¨ظٹظ†طھظ†ظ‚", "ظ„ظˆط­ط© ط±ط³ظ…"),
                                        "ظ„ظˆط­ط© ظپظ†ظٹط©", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "U", "U", "U" }, { "W", "W", "W" }, { "E", "E", "E" } },
                                        List.of("ط³ط±ظٹط±", "bed", "ط§ظ„ط³ط±ظٹط±", "ظپط±ط§ط´", "ط¨ظٹط¯"), "ط³ط±ظٹط±", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "W", "S", "W" }, { "W", "S", "W" }, { "E", "E", "E" } },
                                        List.of("ط³ظٹط§ط¬", "fence", "ط³ظˆط±", "ظپظٹظ†ط³", "ط­ط§ط¬ط² ط®ط´ط¨", "ط³ظٹط§ط¬ ط®ط´ط¨"), "ط³ظٹط§ط¬ ط®ط´ط¨ظٹ",
                                        Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "S", "W", "S" }, { "S", "W", "S" }, { "E", "E", "E" } },
                                        List.of("ط¨ظˆط§ط¨ط© ط³ظٹط§ط¬", "fence gate", "ط¨ظˆط§ط¨ط©", "ط¨ظˆط§ط¨ظ‡", "ظپظٹظ†ط³ ظ‚ظٹطھ", "ظپظٹظ†ط³ ط¬ظٹطھ",
                                                        "ط¨ط§ط¨ ط³ظٹط§ط¬", "ط¨ظˆط§ط¨ط© ط³ظˆط±"),
                                        "ط¨ظˆط§ط¨ط© ط³ظٹط§ط¬", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "B", "B", "B" }, { "B", "L", "B" }, { "B", "R", "B" } },
                                        List.of("ط¯ظٹط³ط¨ظ†ط³ط±", "dispenser", "ظ…ظˆط²ط¹", "ط¯ط³ط¨ظ†ط³ط±", "ط±ط§ظ…ظٹ"), "ط¯ظٹط³ط¨ظ†ط³ط±",
                                        Difficulty.HARD),
                        new Recipe(new String[][] { { "X", "X", "X" }, { "Q", "Q", "Q" }, { "W", "W", "W" } },
                                        List.of("ط­ط³ط§ط³ ط¶ظˆط،", "daylight sensor", "ط¯ظٹظ„ط§ظٹطھ ط³ظ†ط³ط±", "ط³ظ†ط³ظˆط±", "ط­ط³ط§ط³ ط´ظ…ط³",
                                                        "ط­ط³ط§ط³ ط¶ظˆط¦ظٹ"),
                                        "ط­ط³ط§ط³ ط¶ظˆط، ط§ظ„ط´ظ…ط³", Difficulty.HARD),
                        new Recipe(new String[][] { { "I", "I", "I" }, { "I", "I", "I" }, { "I", "I", "I" } },
                                        List.of("ط¨ظ„ظˆظƒ ط­ط¯ظٹط¯", "iron block", "ط§ظٹط±ظˆظ† ط¨ظ„ظˆظƒ", "ط¨ظ„ظˆظƒط© ط­ط¯ظٹط¯", "ط¨ظ„ظˆظƒط© ط§ظٹط±ظˆظ†",
                                                        "ط¨ظ„ظˆظƒظ‡ ط­ط¯ظٹط¯"),
                                        "ط¨ظ„ظˆظƒ ط­ط¯ظٹط¯", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "G", "G", "G" }, { "G", "G", "G" }, { "G", "G", "G" } },
                                        List.of("ط¨ظ„ظˆظƒ ط°ظ‡ط¨", "gold block", "ظ‚ظˆظ„ط¯ ط¨ظ„ظˆظƒ", "ط¬ظˆظ„ط¯ ط¨ظ„ظˆظƒ", "ط¨ظ„ظˆظƒط© ط°ظ‡ط¨",
                                                        "ط¨ظ„ظˆظƒط© ظ‚ظˆظ„ط¯", "ط¨ظ„ظˆظƒظ‡ ط°ظ‡ط¨"),
                                        "ط¨ظ„ظˆظƒ ط°ظ‡ط¨", Difficulty.MEDIUM),
                        new Recipe(new String[][] { { "D", "D", "D" }, { "D", "D", "D" }, { "D", "D", "D" } },
                                        List.of("ط¨ظ„ظˆظƒ ط¯ط§ظٹظ…ظˆظ†ط¯", "diamond block", "ط¯ط§ظٹظ…ظˆظ†ط¯ ط¨ظ„ظˆظƒ", "ط¨ظ„ظˆظƒط© ط¯ط§ظٹظ…ظˆظ†ط¯",
                                                        "ط¨ظ„ظˆظƒط© ط§ظ„ظ…ط§ط³", "ط¨ظ„ظˆظƒظ‡ ط¯ط§ظٹظ…ظˆظ†ط¯"),
                                        "ط¨ظ„ظˆظƒ ط¯ط§ظٹظ…ظˆظ†ط¯", Difficulty.HARD),
                        new Recipe(new String[][] { { "M", "M", "M" }, { "M", "M", "M" }, { "M", "M", "M" } },
                                        List.of("ط¨ظ„ظˆظƒ ط²ظ…ط±ط¯", "emerald block", "ط¨ظ„ظˆظƒ ط§ظٹظ…ط±ظ„ط¯", "ط§ظٹظ…ط±ظ„ط¯ ط¨ظ„ظˆظƒ",
                                                        "ط¨ظ„ظˆظƒط© ط§ظٹظ…ط±ظ„ط¯", "ط¨ظ„ظˆظƒظ‡ ط²ظ…ط±ط¯"),
                                        "ط¨ظ„ظˆظƒ ط²ظ…ط±ط¯", Difficulty.HARD));

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
                sb.append("â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬\n");
                for (int i = 0; i < 3; i++) {
                        sb.append("**").append(i + 1).append("**  ");
                        for (int j = 0; j < 3; j++) {
                                String item = ITEMS.get(recipe.grid[i][j]);


                                sb.append("   ").append(item).append("   ");
                        }
                        sb.append("\n\n");
                }
                sb.append("â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬â–¬");

                sessionGrids.put(sessionId, sb.toString());

                String logDetails = String.format(
                                "### ظپط¹ط§ظ„ظٹط© ط§ظ„طµظ†ط§ط¹ط©: ط¨ط¯ط، (ظپط±ط¯ظٹط©)\nâ–«ï¸ڈ **ط§ظ„ظ„ط§ط¹ط¨:** %s\nâ–«ï¸ڈ **ط§ظ„طµط¹ظˆط¨ط©:** %s\nâ–«ï¸ڈ **ط§ظ„ط¬ط§ط¦ط²ط©:** %d opex\nâ–«ï¸ڈ **ID ط§ظ„ط¬ظ„ط³ط©:** `%s`",
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
                                                                "**ط§ظ†طھظ‡ظ‰ ط§ظ„ظˆظ‚طھ!** ظ„ظ… طھظ†ط¬ط­ ظپظٹ طھط®ظ…ظٹظ† ط§ظ„ط´ظٹط، ط§ظ„ظ…ط·ظ„ظˆط¨.\nط§ظ„ط´ظٹط، ط§ظ„طµط­ظٹط­ ظ‡ظˆ: **%s**\nط­ط¸ط§ظ‹ ط£ظˆظپط± ظپظٹ ط§ظ„ظ…ط±ط© ط§ظ„ظ‚ط§ط¯ظ…ط©.",
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
                                                                EmbedUtil.containerBranded("CRAFTING", "ظ…ط§ط°ط§ ظ†طµظ†ط¹طں",
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
                                String.format("ط£ظ…ط§ظ…ظƒ ط·ط§ظˆظ„ط© ظƒط±ط§ظپطھظ†ظ‚ ط®ط§طµط© ط¨ظƒ ظٹط§ %s... ط®ظ…ظ† ظ…ط§ ظ‡ظˆ ط§ظ„ط´ظٹط، ط§ظ„ط°ظٹ ظٹطھظ… طµظ†ط¹ظ‡طں\n\n",
                                                mention)
                                +
                                grid + "\n" +
                                "ط§ظ„ط¬ط§ط¦ط²ط©: **" + reward + " opex**\n\n" +
                                "ط§ظƒطھط¨ ط§ظ„ط¥ط¬ط§ط¨ط© ظ…ط¨ط§ط´ط±ط© ظپظٹ ط§ظ„ط´ط§طھ!";
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
                                                "**ظƒظپظˆظˆ!** ط¥ط¬ط§ط¨ط© طµط­ظٹط­ط© ظٹط§ %s!\nطھظ… طµظ†ط¹: **%s** ط¨ظ†ط¬ط§ط­.\nط­طµظ„طھ ط¹ظ„ظ‰ **%d opex**",
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

                                String logWin = String.format(
                                                "### ظپط¹ط§ظ„ظٹط© ط§ظ„طµظ†ط§ط¹ط©: ظپظˆط² (ظپط±ط¯ظٹط©)\nâ–«ï¸ڈ **ط§ظ„ظپط§ط¦ط²:** <@%s>\nâ–«ï¸ڈ **ط§ظ„طµط¹ظˆط¨ط©:** %s\nâ–«ï¸ڈ **ط§ظ„ط´ظٹط،:** %s\nâ–«ï¸ڈ **ID ط§ظ„ط¬ظ„ط³ط©:** `%s`",
                                                event.getAuthor().getId(), activeRecipe.difficulty.displayName,
                                                itemName, sessionId);
                                logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES,
                                                EmbedUtil.createOldLogEmbed("craft_win", logWin, event.getMember(),
                                                                null, null,
                                                                EmbedUtil.SUCCESS));


                                break;
                        }
                }
        }
}
