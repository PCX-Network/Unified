/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.data;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent statistics for a player in the minigame.
 *
 * <p>This class demonstrates the data model for player statistics stored
 * using the UnifiedPlugin PlayerDataService. Statistics are automatically
 * persisted and synchronized across servers on networks.
 *
 * <h2>Storage</h2>
 * <p>In production, this would be stored using data keys:
 * <pre>{@code
 * PlayerProfile profile = playerData.getProfile(playerId);
 * int kills = profile.getData(PlayerStatsKeys.KILLS);
 * profile.setData(PlayerStatsKeys.KILLS, kills + 1);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see PlayerStatsKeys
 */
public class PlayerStats {

    private final UUID playerId;

    // Lifetime statistics
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int assists;
    private int damageDealt;
    private int damageTaken;
    private int bestKillStreak;
    private int totalPlayTimeSeconds;

    // Currency
    private long coins;

    // Timestamps
    private Instant firstPlayed;
    private Instant lastPlayed;

    /**
     * Creates a new PlayerStats for a player.
     *
     * @param playerId the player's UUID
     */
    public PlayerStats(@NotNull UUID playerId) {
        this.playerId = playerId;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.assists = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        this.bestKillStreak = 0;
        this.totalPlayTimeSeconds = 0;
        this.coins = 0;
        this.firstPlayed = Instant.now();
        this.lastPlayed = Instant.now();
    }

    // ==================== Stat Calculations ====================

    /**
     * Calculates the kill/death ratio.
     *
     * @return the K/D ratio (kills if no deaths)
     */
    public double getKDRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    /**
     * Calculates the win rate as a percentage.
     *
     * @return the win rate (0-100)
     */
    public double getWinRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return (double) wins / gamesPlayed * 100.0;
    }

    /**
     * Calculates average kills per game.
     *
     * @return the average kills
     */
    public double getAverageKills() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return (double) kills / gamesPlayed;
    }

    /**
     * Calculates the total play time in hours.
     *
     * @return the play time in hours
     */
    public double getPlayTimeHours() {
        return totalPlayTimeSeconds / 3600.0;
    }

    // ==================== Stat Modifiers ====================

    /**
     * Records a game completion.
     *
     * @param won           whether the player won
     * @param kills         kills in the game
     * @param deaths        deaths in the game
     * @param assists       assists in the game
     * @param damageDealt   damage dealt in the game
     * @param damageTaken   damage taken in the game
     * @param bestStreak    best kill streak in the game
     * @param playTimeSeconds time spent in the game
     * @param coinsEarned   coins earned
     */
    public void recordGame(boolean won, int kills, int deaths, int assists,
                           int damageDealt, int damageTaken, int bestStreak,
                           int playTimeSeconds, long coinsEarned) {
        this.gamesPlayed++;
        if (won) {
            this.wins++;
        } else {
            this.losses++;
        }
        this.kills += kills;
        this.deaths += deaths;
        this.assists += assists;
        this.damageDealt += damageDealt;
        this.damageTaken += damageTaken;
        if (bestStreak > this.bestKillStreak) {
            this.bestKillStreak = bestStreak;
        }
        this.totalPlayTimeSeconds += playTimeSeconds;
        this.coins += coinsEarned;
        this.lastPlayed = Instant.now();
    }

    /**
     * Adds coins to the player's balance.
     *
     * @param amount the amount to add
     */
    public void addCoins(long amount) {
        this.coins += amount;
    }

    /**
     * Removes coins from the player's balance.
     *
     * @param amount the amount to remove
     * @return true if the player had enough coins
     */
    public boolean removeCoins(long amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    // ==================== Getters ====================

    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
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

    public int getBestKillStreak() {
        return bestKillStreak;
    }

    public int getTotalPlayTimeSeconds() {
        return totalPlayTimeSeconds;
    }

    public long getCoins() {
        return coins;
    }

    @NotNull
    public Instant getFirstPlayed() {
        return firstPlayed;
    }

    @NotNull
    public Instant getLastPlayed() {
        return lastPlayed;
    }

    // ==================== Setters (for loading from storage) ====================

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public void setDamageDealt(int damageDealt) {
        this.damageDealt = damageDealt;
    }

    public void setDamageTaken(int damageTaken) {
        this.damageTaken = damageTaken;
    }

    public void setBestKillStreak(int bestKillStreak) {
        this.bestKillStreak = bestKillStreak;
    }

    public void setTotalPlayTimeSeconds(int totalPlayTimeSeconds) {
        this.totalPlayTimeSeconds = totalPlayTimeSeconds;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }

    public void setFirstPlayed(@NotNull Instant firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public void setLastPlayed(@NotNull Instant lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    @Override
    public String toString() {
        return "PlayerStats{" +
                "playerId=" + playerId +
                ", games=" + gamesPlayed +
                ", wins=" + wins +
                ", kills=" + kills +
                ", deaths=" + deaths +
                ", coins=" + coins +
                '}';
    }
}
