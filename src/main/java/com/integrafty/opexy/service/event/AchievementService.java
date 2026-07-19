package com.integrafty.opexy.service.event;

import com.integrafty.opexy.entity.UserStats;
import com.integrafty.opexy.repository.UserStatsRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
public class AchievementService {

    private final UserStatsRepository statsRepository;
    private final com.integrafty.opexy.service.LogManager logManager;

    public AchievementService(UserStatsRepository statsRepository, com.integrafty.opexy.service.LogManager logManager) {
        this.statsRepository = statsRepository;
        this.logManager = logManager;
    }

    @Value("${opexy.roles.achievements.jinxed-bidder}")
    private String jinxedBidderId;

    @Value("${opexy.roles.achievements.golden-gavel}")
    private String goldenGavelId;

    @Value("${opexy.roles.achievements.high-roller}")
    private String highRollerId;

    @Value("${opexy.roles.achievements.contract-killer}")
    private String contractKillerId;

    @Value("${opexy.roles.achievements.eternal-civilian}")
    private String eternalCivilianId;

    @Value("${opexy.roles.achievements.public-enemy}")
    private String publicEnemyId;

    @Value("${opexy.roles.achievements.eagle-eye}")
    private String eagleEyeId;

    @Value("${opexy.roles.achievements.guardian-angel}")
    private String guardianAngelId;

    @Value("${opexy.roles.achievements.master-plumber}")
    private String masterPlumberId;

    @Value("${opexy.roles.achievements.sonic-fingers}")
    private String sonicFingersId;

    public UserStats getStats(long userId) {
        return statsRepository.findByUserId(userId)
                .orElseGet(() -> statsRepository.save(new UserStats(userId)));
    }

    @Transactional
    public void updateStats(long userId, Guild guild, Consumer<UserStats> updateAction) {
        UserStats stats = getStats(userId);
        updateAction.accept(stats);
        statsRepository.save(stats);
        checkAchievements(userId, guild, stats);
    }

    private void checkAchievements(long userId, Guild guild, UserStats stats) {
        Member member = guild.getMemberById(userId);
        if (member == null) return;

        // Minigame Achievements
        checkAndGrant(member, masterPlumberId, stats.getPipeWins() >= 4);
        checkAndGrant(member, sonicFingersId, stats.getSpeedWins() >= 1);
    }

    private void checkAndGrant(Member member, String roleId, boolean condition) {
        if (condition && roleId != null && !roleId.isEmpty()) {
            Role role = member.getGuild().getRoleById(roleId);
            if (role != null && !member.getRoles().contains(role)) {
                member.getGuild().addRoleToMember(member, role).queue(v -> {
                    // Public Notification
                    String time = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now());
                    String logBody = String.format("""
                            ### 🏆 إنجاز جـــديـــد | NEW ACHIEVEMENT
                            
                            ▫️ **الـعـضـو:** %s
                            ▫️ **الـرتـبـة:** %s
                            ▫️ **الـوقـت:** `%s`
                            
                            تـم مـنـح الـرتـبـة تـلـقـائـيـاً لـتـحـقـيـق شـروط الإنجاز!
                            """, member.getAsMention(), role.getAsMention(), time);
                            
                    logManager.logEmbed(member.getGuild(), com.integrafty.opexy.service.LogManager.LOG_GAMES,
                            com.integrafty.opexy.utils.EmbedUtil.createOldLogEmbed("achievement", logBody, member, null, null, com.integrafty.opexy.utils.EmbedUtil.SUCCESS));
                });
            }
        }
    }
}
