package com.integrafty.opexy.command;

import com.integrafty.opexy.command.base.MultiSlashCommand;
import com.integrafty.opexy.entity.UserEntity;
import com.integrafty.opexy.repository.UserRepository;
import com.integrafty.opexy.utils.EmbedUtil;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventPointCommands implements MultiSlashCommand {

    private final UserRepository userRepository;

    @Override
    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> list = new ArrayList<>();

        list.add(Commands.slash("add-point", "إضافة نقاط فعاليات لعضو")
                .addOption(OptionType.INTEGER, "amount", "عدد النقاط", true)
                .addOption(OptionType.USER, "user", "العضو المستهدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("remove-point", "سحب نقاط فعاليات من عضو")
                .addOption(OptionType.INTEGER, "amount", "عدد النقاط", true)
                .addOption(OptionType.USER, "user", "العضو المستهدف", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        list.add(Commands.slash("points", "عرض نقاط الفعاليات لك أو للجميع")
                .addOption(OptionType.USER, "user", "العضو (اختياري)", false));

        list.add(Commands.slash("rest-points", "تصفير نقاط الفعاليات للجميع أو لعضو معين")
                .addOption(OptionType.USER, "user", "العضو (اختياري)", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS)));

        return list;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String name = event.getName();

        switch (name) {
            case "add-point" -> handleAddPoint(event);
            case "remove-point" -> handleRemovePoint(event);
            case "points" -> handlePoints(event);
            case "rest-points" -> handleResetPoints(event);
        }
    }

    private void handleAddPoint(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;

        UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());
        user.setEventPoints(user.getEventPoints() + amount);
        userRepository.save(user);

        String body = String.format("### ✅ تـــم إضـــافـــة نـــقـــاط\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــمـــبـــلـــغ:** `%d` نـــقـــطـــة\n▫️ **الـــرصـــيـــد الـــحـــالـــي:** `%d`",
                target.getAsMention(), amount, user.getEventPoints());
        
        event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("EVENTS", "Points Update", body, EmbedUtil.BANNER_MAIN)).build()).queue();
    }

    private void handleRemovePoint(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        Member target = event.getOption("user").getAsMember();
        if (target == null) return;

        UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());
        user.setEventPoints(Math.max(0, user.getEventPoints() - amount));
        userRepository.save(user);

        String body = String.format("### ❌ تـــم ســـحـــب نـــقـــاط\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــمـــبـــلـــغ:** `%d` نـــقـــطـــة\n▫️ **الـــرصـــيـــد الـــحـــالـــي:** `%d`",
                target.getAsMention(), amount, user.getEventPoints());
        
        event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("EVENTS", "Points Update", body, EmbedUtil.BANNER_MAIN)).build()).queue();
    }

    private void handlePoints(SlashCommandInteractionEvent event) {
        OptionMapping userOpt = event.getOption("user");
        
        if (userOpt == null) {
            // Show Leaderboard (List everyone with > 0 points)
            List<UserEntity> top = userRepository.findTop10ByGuildIdOrderByEventPointsDesc(event.getGuild().getId());
            
            StringBuilder sb = new StringBuilder();
            sb.append("### 🏆 مـــتـــصـــدرو نـــقـــاط الـــفـــعـــالـــيـــات\n\n");
            
            boolean found = false;
            for (int i = 0; i < top.size(); i++) {
                UserEntity u = top.get(i);
                if (u.getEventPoints() > 0) {
                    sb.append(String.format("`#%d` <@%s> — **%d** نـــقـــطـــة\n", i + 1, u.getUserId(), u.getEventPoints()));
                    found = true;
                }
            }

            if (!found) {
                sb.append("*لا يـــوجـــد أعـــضـــاء لـــديـــهـــم نـــقـــاط حـــالـــيـــاً.*");
            }

            event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("EVENTS", "Leaderboard", sb.toString(), EmbedUtil.BANNER_MAIN)).build()).queue();
        } else {
            // Show specific user
            Member target = userOpt.getAsMember();
            if (target == null) return;

            UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());

            String body = String.format("### 📊 نـــقـــاط الـــفـــعـــالـــيـــات\n▫️ **الـــعـــضـــو:** %s\n▫️ **الـــنـــقـــاط الـــحـــالـــيـــة:** `%d`",
                    target.getAsMention(), user.getEventPoints());

            event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.containerBranded("EVENTS", "Points Status", body, EmbedUtil.BANNER_MAIN)).build()).queue();
        }
    }

    private void handleResetPoints(SlashCommandInteractionEvent event) {
        OptionMapping userOpt = event.getOption("user");

        if (userOpt == null) {
            // Reset Everyone
            userRepository.resetAllEventPointsByGuildId(event.getGuild().getId());
            event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.success("EVENTS RESET", "✅ تـــم تـــصـــفـــيـــر نـــقـــاط الـــفـــعـــالـــيـــات لـــجـــمـــيـــع الأعـــضـــاء بـــنـــجـــاح!")).build()).queue();
        } else {
            // Reset specific user
            Member target = userOpt.getAsMember();
            if (target == null) return;

            UserEntity user = getOrCreateUser(target.getId(), event.getGuild().getId());
            user.setEventPoints(0);
            userRepository.save(user);

            event.reply(EmbedUtil.createBrandedMessage(EmbedUtil.success("POINTS RESET", "✅ تـــم تـــصـــفـــيـــر نـــقـــاط الـــعـــضـــو " + target.getAsMention() + " بـــنـــجـــاح!")).build()).queue();
        }
    }

    private UserEntity getOrCreateUser(String userId, String guildId) {
        return userRepository.findByUserIdAndGuildId(userId, guildId)
                .orElse(new UserEntity(userId, guildId, 0, 0, false, null, null, 0, 0));
    }
}
