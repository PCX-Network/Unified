/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.game;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a player's state within a game session.
 *
 * <p>This tracks per-game statistics like kills, deaths, and respawn state.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class GamePlayer {

    private final UUID playerId;
    private final String playerName;
    private final Instant joinTime;

    private int kills;
    private int deaths;
    private int assists;
    private int damageDealt;
    private int damageTaken;
    private boolean alive;
    private boolean respawning;
    private Instant lastDeathTime;
    private int killStreak;
    private int bestKillStreak;

    /**
     * Creates a new GamePlayer.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     */
    public GamePlayer(@NotNull UUID playerId, @NotNull String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.joinTime = Instant.now();
        this.kills = 0;
        this.deaths = 0;
        this.assists = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        this.alive = true;
        this.respawning = false;
        this.killStreak = 0;
        this.bestKillStreak = 0;
    }

    // ==================== Kill/Death Methods ====================

    public void addKill() {
        kills++;
        killStreak++;
        if (killStreak > bestKillStreak) {
            bestKillStreak = killStreak;
        }
    }

    public void addDeath() {
        deaths++;
        killStreak = 0;
        alive = false;
        lastDeathTime = Instant.now();
    }

    public void addAssist() {
        assists++;
    }

    public void addDamageDealt(int damage) {
        damageDealt += damage;
    }

    public void addDamageTaken(int damage) {
        damageTaken += damage;
    }

    public void respawn() {
        alive = true;
        respawning = false;
    }

    public void setRespawning() {
        respawning = true;
    }

    // ==================== Stat Calculations ====================

    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public int getScore() {
        return (kills * 100) + (assists * 25) - (deaths * 10);
    }

    public int getSecondsSinceDeath() {
        if (lastDeathTime == null) {
            return -1;
        }
        return (int) (Instant.now().getEpochSecond() - lastDeathTime.getEpochSecond());
    }

    // ==================== Getters ====================

    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    @NotNull
    public Instant getJoinTime() {
        return joinTime;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getAssists() {
        return assists;
    }

    public int getDamageDealt() {
        return damageDealt;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isRespawning() {
        return respawning;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public int getBestKillStreak() {
        return bestKillStreak;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GamePlayer other)) return false;
        return playerId.equals(other.playerId);
    }

    @Override
    public int hashCode() {
        return playerId.hashCode();
    }

    @Override
    public String toString() {
        return "GamePlayer{name='" + playerName + "', kills=" + kills + ", deaths=" + deaths + "}";
    }
}
