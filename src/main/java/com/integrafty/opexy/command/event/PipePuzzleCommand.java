package com.integrafty.opexy.command.event;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.service.event.PipePuzzleManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.Random;

@Component
public class PipePuzzleCommand implements MultiSlashCommand {

    private final PipePuzzleManager pipePuzzleManager;
    private final Random random = new Random();

    @Value("${opexy.roles.hype-manager}")
    private String hypeManagerId;

    @Value("${opexy.roles.hype-events}")
    private String hypeEventsId;

    public PipePuzzleCommand(PipePuzzleManager pipePuzzleManager) {
        this.pipePuzzleManager = pipePuzzleManager;
    }

    @Override
    public List<SlashCommandData> getCommandDataList() {
        return List.of(Commands.slash("pipes", "بدء لغز أنابيب التوصيل (Connect the Pipes)")
                .addOptions(new OptionData(OptionType.STRING, "difficulty", "الصعوبة", true)
                        .addChoice("سهل (3x3)", "easy")
                        .addChoice("متوسط (4x4)", "medium")
                        .addChoice("صعب (5x5)", "hard")));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pipes")) return;

        String difficulty = event.getOption("difficulty") != null ? event.getOption("difficulty").getAsString() : "easy";
        int size = difficulty.equals("easy") ? 3 : difficulty.equals("medium") ? 4 : 5;
        
        String renderedGrid = pipePuzzleManager.startNewGame(event.getUser().getIdLong(), size, event.getGuild());

        event.reply(EmbedUtil.createBrandedMessage(com.integrafty.opexy.utils.EmbedUtil.containerBranded("ENGINEERING", "لغز الأنابيب — صعوبة: " + difficulty, "استخدم الأسهم للتحرك والزر الدائري لتدوير الأنبوب!\n\n" + renderedGrid, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_up", "🔼"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_down", "🔽"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_left", "◀️"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_right", "▶️"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("pipe_rotate", "🔄")
                        ),
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.success("pipe_submit", "تحقق من الحل ✅")
                        ))).build())
                .useComponentsV2(true).queue();
    }

}
