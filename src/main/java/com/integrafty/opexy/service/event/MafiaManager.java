package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import com.integrafty.opexy.entity.UserStats;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MafiaManager extends ListenerAdapter {

    private final AchievementService achievementService;
    private final EventManager eventManager;
    private final LogManager logManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private MafiaGame currentGame = null;
    private long organizerId = 0;

    public MafiaManager(AchievementService achievementService, EventManager eventManager, LogManager logManager) {
        this.achievementService = achievementService;
        this.eventManager = eventManager;
        this.logManager = logManager;
    }

    public void startNewGame(long channelId, net.dv8tion.jda.api.entities.Guild guild, net.dv8tion.jda.api.entities.Member organizer) {
        this.currentGame = new MafiaGame(channelId);
        this.organizerId = organizer.getIdLong();
        
        String logDetails = String.format("### 🕵️ فعالية المافيا: بدء الفعالية\n▫️ **المنظم:** %s\n▫️ **القناة:** <#%d>", organizer.getAsMention(), channelId);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("mafia", logDetails, organizer, null, null, EmbedUtil.INFO));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (currentGame == null || !event.getComponentId().startsWith("mafia_")) return;

        String id = event.getComponentId();
        long userId = event.getUser().getIdLong();

        if (id.equals("mafia_join")) {
            if (currentGame.getPlayers().containsKey(userId)) {
                event.reply("❌ أنت منضم بالفعل!").setEphemeral(true).queue();
                return;
            }
            currentGame.addPlayer(event.getMember());
            updateLobby(event);
        } else if (id.equals("mafia_start")) {
            boolean isStaff = event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) || 
                             event.getMember().getRoles().stream().anyMatch(r -> r.getName().toLowerCase().contains("hype") || r.getName().toLowerCase().contains("event"));
            
            if (userId != organizerId && !isStaff) {
                 event.reply("❌ هذا الزر للمنظمين فقط.").setEphemeral(true).queue();
                 return;
            }
            if (currentGame.getPlayers().size() < 5) {
                event.reply("يجب وجود 5 لاعبين على الأقل للبدء.").setEphemeral(true).queue();
                return;
            }
            startGame(event);
        } else if (id.startsWith("mafia_action_")) {
            handleAction(event);
        } else if (id.startsWith("mafia_vote_")) {
            handleVote(event);
        }
    }

    private void updateLobby(ButtonInteractionEvent event) {
        StringBuilder players = new StringBuilder();
        for (long pid : currentGame.getPlayers().keySet()) {
            players.append("• <@").append(pid).append(">\n");
        }

        String body = "اللاعبين المنضمين: **" + currentGame.getPlayers().size() + "**\n\n" + players.toString() +
                      "\n**القوانين:**\n• الحد الأدنى للاعبين: 5.\n• النهار للمناقشة، والليل لتنفيذ الأدوار.";

        event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("GAME", "لعبة المافيا — Mafia Game", body, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.primary("mafia_join", "انضمام"),
                                net.dv8tion.jda.api.components.buttons.Button.danger("mafia_start", "بدء اللعبة (المنظم فقط)")
                        )))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    private void startGame(ButtonInteractionEvent event) {
        currentGame.assignRoles();
        currentGame.setPhase(MafiaGame.Phase.NIGHT_MAFIA);
        
        event.editMessage("⏳ بدأت اللعبة! يتم الآن توزيع الأدوار في الخاص...")
                .setComponents(Collections.emptyList())
                .queue();

        for (long pid : currentGame.getPlayers().keySet()) {
            MafiaGame.Role role = currentGame.getPlayers().get(pid);
            String roleName = role == MafiaGame.Role.MAFIA ? "المافيا" : 
                             role == MafiaGame.Role.DOCTOR ? "الطبيب" : 
                             role == MafiaGame.Role.DETECTIVE ? "المحقق" : "مواطن";
            
            event.getGuild().retrieveMemberById(pid).queue(m -> {
                m.getUser().openPrivateChannel().queue(pc -> {
                    pc.sendMessage("دورك في اللعبة هو: **" + roleName + "**").queue();
                });
            });
        }

        startNight(event);
    }

    private void startNight(ButtonInteractionEvent event) {
        currentGame.setPhase(MafiaGame.Phase.NIGHT_MAFIA);
        currentGame.setTargetToKill(null);
        currentGame.setTargetToSave(null);
        currentGame.setTargetToInvestigate(null);

        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("GAME", "حل الليل...", "أغمض الجميع أعينهم... حان دور المافيا والطبيب والمحقق.", com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
        
        List<Button> buttons = new ArrayList<>();
        for (long pid : currentGame.getAlivePlayers()) {
            buttons.add(Button.secondary("mafia_action_" + pid, event.getGuild().getMemberById(pid).getEffectiveName()));
        }

        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setContent("اختيارات الليل (تظهر للجميع لكن لا ينفذها إلا صاحب الدور):")
                .setComponents(com.integrafty.opexy.util.ComponentUtil.splitToRows(buttons))
                .useComponentsV2(true)
                .build())
                .useComponentsV2(true)
                .queue();

        scheduler.schedule(() -> startDay(event), 20, TimeUnit.SECONDS);
    }

    private void handleAction(ButtonInteractionEvent event) {
        long actorId = event.getUser().getIdLong();
        long targetId = Long.parseLong(event.getComponentId().replace("mafia_action_", ""));
        MafiaGame.Role role = currentGame.getPlayers().get(actorId);

        if (!currentGame.getAlivePlayers().contains(actorId)) {
            event.reply("الأموات لا يتحدثون.").setEphemeral(true).queue();
            return;
        }

        if (role == MafiaGame.Role.MAFIA) {
            currentGame.setTargetToKill(targetId);
            event.reply("اخترت قتل <@" + targetId + ">").setEphemeral(true).queue();
        } else if (role == MafiaGame.Role.DOCTOR) {
            currentGame.setTargetToSave(targetId);
            event.reply("اخترت حماية <@" + targetId + ">").setEphemeral(true).queue();
        } else if (role == MafiaGame.Role.DETECTIVE) {
            currentGame.setTargetToInvestigate(targetId);
            boolean isMafia = currentGame.getPlayers().get(targetId) == MafiaGame.Role.MAFIA;
            event.reply("نتيجة التحقيق: الشخص " + (isMafia ? "**مافيا!!**" : "مواطن صالح.")).setEphemeral(true).queue();
            if (isMafia) {
                achievementService.updateStats(actorId, event.getGuild(), s -> s.setDetectiveReveals(s.getDetectiveReveals() + 1));
            }
        } else {
            event.reply("ليس لديك دور ليلي.").setEphemeral(true).queue();
        }
    }

    private void startDay(ButtonInteractionEvent event) {
        currentGame.setPhase(MafiaGame.Phase.DAY_DISCUSSION);
        
        Long killed = currentGame.getTargetToKill();
        Long saved = currentGame.getTargetToSave();
        
        String resultMsg = "طلع النهار!";
        if (killed != null && !killed.equals(saved)) {
            currentGame.getAlivePlayers().remove(killed);
            resultMsg += "\nللأسف، استيقظتم على خبر مقتل <@" + killed + ">!";
        } else {
            resultMsg += "\nليلة هادئة، لم يمت أحد!";
        }

        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("GAME", "بداية يوم جديد", resultMsg + "\n\n**وقت المناقشة (20 ثانية)**", com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();

        if (checkWin(event)) return;

        scheduler.schedule(() -> startVoting(event), 20, TimeUnit.SECONDS);
    }

    private void startVoting(ButtonInteractionEvent event) {
        currentGame.setPhase(MafiaGame.Phase.DAY_VOTING);
        currentGame.getVotes().clear();

        List<Button> buttons = new ArrayList<>();
        for (long pid : currentGame.getAlivePlayers()) {
            buttons.add(Button.danger("mafia_vote_" + pid, event.getGuild().getMemberById(pid).getEffectiveName()));
        }

        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setContent("حان وقت التصويت! من هو المافيا؟")
                .setComponents(com.integrafty.opexy.util.ComponentUtil.splitToRows(buttons))
                .useComponentsV2(true)
                .build())
                .useComponentsV2(true)
                .queue();

        scheduler.schedule(() -> endDay(event), 15, TimeUnit.SECONDS);
    }

    private void handleVote(ButtonInteractionEvent event) {
        long voterId = event.getUser().getIdLong();
        long targetId = Long.parseLong(event.getComponentId().replace("mafia_vote_", ""));

        if (!currentGame.getAlivePlayers().contains(voterId)) {
            event.reply("لا يمكنك التصويت وأنت ميت.").setEphemeral(true).queue();
            return;
        }

        currentGame.getVotes().put(voterId, targetId);
        event.reply("تم تسجيل تصويتك ضد <@" + targetId + ">").setEphemeral(true).queue();
    }

    private void endDay(ButtonInteractionEvent event) {
        Map<Long, Integer> voteCount = new HashMap<>();
        for (long target : currentGame.getVotes().values()) {
            voteCount.put(target, voteCount.getOrDefault(target, 0) + 1);
        }

        Long votedOut = null;
        int maxVotes = 0;
        for (Map.Entry<Long, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                votedOut = entry.getKey();
            }
        }

        if (votedOut != null) {
            currentGame.getAlivePlayers().remove(votedOut);
            boolean wasMafia = currentGame.getPlayers().get(votedOut) == MafiaGame.Role.MAFIA;
            event.getChannel().sendMessage("بقرار الأغلبية، تم إعدام <@" + votedOut + ">! لقد كان: **" + (wasMafia ? "المافيا!" : "مواطن بريء") + "**").queue();
            
            final long fVotedOut = votedOut;
            achievementService.updateStats(votedOut, event.getGuild(), s -> s.setVotesReceived(s.getVotesReceived() + 1));
        } else {
            event.getChannel().sendMessage("تعادل في الأصوات، لم يتم إعدام أحد.").queue();
        }

        if (checkWin(event)) return;

        startNight(event);
    }

    private boolean checkWin(ButtonInteractionEvent event) {
        int mafiaCount = 0;
        int citizenCount = 0;
        for (long pid : currentGame.getAlivePlayers()) {
            if (currentGame.getPlayers().get(pid) == MafiaGame.Role.MAFIA) mafiaCount++;
            else citizenCount++;
        }

        if (mafiaCount == 0) {
            endGame(event, "المواطنين");
            return true;
        } else if (mafiaCount >= citizenCount) {
            endGame(event, "المافيا");
            return true;
        }
        return false;
    }

    private void endGame(ButtonInteractionEvent event, String winner) {
        eventManager.endGroupEvent();
        event.getChannel().sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("GAME", "انتهت اللعبة!", "الفائز هم: **" + winner + "**\n\nشكراً للجميع على اللعب!", com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
        
        for (long pid : currentGame.getPlayers().keySet()) {
            MafiaGame.Role role = currentGame.getPlayers().get(pid);
            achievementService.updateStats(pid, event.getGuild(), s -> {
                if (winner.contains("المافيا") && role == MafiaGame.Role.MAFIA) s.setMafiaWins(s.getMafiaWins() + 1);
                if (role == MafiaGame.Role.CITIZEN) s.setCitizenCount(s.getCitizenCount() + 1);
            });
        }
        String logDetails = String.format("### 🕵️ فعالية المافيا: انتهت اللعبة\n▫️ **الفائز:** %s\n▫️ **عدد اللاعبين:** %d", 
                winner, currentGame.getPlayers().size());
        logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("mafia", logDetails, null, null, null, EmbedUtil.SUCCESS));

        currentGame = null;
    }

    public void stopGame() {
        eventManager.endGroupEvent();
        this.currentGame = null;
    }
}
