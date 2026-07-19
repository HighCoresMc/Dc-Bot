package com.integrafty.opexy.service.event;

import lombok.Data;
import net.dv8tion.jda.api.entities.Member;
import java.util.*;

@Data
public class MafiaGame {
    public enum Phase { WAITING, DAY_DISCUSSION, DAY_VOTING, NIGHT_MAFIA, NIGHT_DOCTOR, NIGHT_DETECTIVE }
    public enum Role { CITIZEN, MAFIA, DOCTOR, DETECTIVE }

    private final long channelId;
    private Phase phase = Phase.WAITING;
    private final Map<Long, Role> players = new HashMap<>();
    private final Set<Long> alivePlayers = new HashSet<>();
    private final Map<Long, Long> votes = new HashMap<>();
    
    private Long targetToKill = null;
    private Long targetToSave = null;
    private Long targetToInvestigate = null;

    public void addPlayer(Member member) {
        players.put(member.getIdLong(), Role.CITIZEN);
        alivePlayers.add(member.getIdLong());
    }

    public void assignRoles() {
        List<Long> playerIds = new ArrayList<>(players.keySet());
        Collections.shuffle(playerIds);

        // Assign Mafia
        players.put(playerIds.get(0), Role.MAFIA);
        // Assign Doctor
        players.put(playerIds.get(1), Role.DOCTOR);
        // Assign Detective
        players.put(playerIds.get(2), Role.DETECTIVE);
    }
}
