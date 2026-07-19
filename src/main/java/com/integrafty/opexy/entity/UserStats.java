package com.integrafty.opexy.entity;

import jakarta.persistence.*;

// UserStats
@Entity
@Table(name = "user_stats")
public class UserStats {

    public UserStats() {}
    public UserStats(Long userId) { this.userId = userId; }

    @Id
    @Column(name = "user_id")
    private Long userId;

    // Minigame Achievements
    @Column(name = "pipe_wins", columnDefinition = "int default 0")
    private int pipeWins = 0;

    @Column(name = "speed_wins", columnDefinition = "int default 0")
    private int speedWins = 0;

    @Column(name = "bomb_wins", columnDefinition = "int default 0")
    private int bombWins = 0;

    @Column(name = "craft_wins", columnDefinition = "int default 0")
    private int craftWins = 0;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getPipeWins() { return pipeWins; }
    public void setPipeWins(int pipeWins) { this.pipeWins = pipeWins; }

    public int getSpeedWins() { return speedWins; }
    public void setSpeedWins(int speedWins) { this.speedWins = speedWins; }

    public int getBombWins() { return bombWins; }
    public void setBombWins(int bombWins) { this.bombWins = bombWins; }

    public int getCraftWins() { return craftWins; }
    public void setCraftWins(int craftWins) { this.craftWins = craftWins; }
}
