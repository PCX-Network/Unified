/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.config;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Configuration for the minigame plugin.
 *
 * <p>In production, this class would use {@code @ConfigSerializable} annotation
 * from the UnifiedPlugin configuration framework for automatic serialization.
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class MinigameConfig {

    // General settings
    private boolean debug = false;
    private String prefix = "&6[Minigame] &r";

    // Game settings
    private int minPlayers = 2;
    private int maxPlayers = 16;
    private int countdownSeconds = 30;
    private int gameDurationMinutes = 10;
    private int respawnDelaySeconds = 5;
    private boolean allowLatejoin = true;
    private int latejoinGracePeriodSeconds = 60;

    // Arena settings
    private int arenaDetectionRadius = 50;
    private boolean autoSaveArenas = true;
    private int saveIntervalMinutes = 5;

    // Scoreboard settings
    private boolean scoreboardEnabled = true;
    private int scoreboardUpdateTicks = 20;
    private String scoreboardTitle = "&6&lMinigame";

    // Boss bar settings
    private boolean bossBarEnabled = true;
    private String bossBarColor = "YELLOW";
    private String bossBarStyle = "SOLID";

    // Hologram settings
    private boolean leaderboardsEnabled = true;
    private int leaderboardTopCount = 10;
    private int leaderboardUpdateMinutes = 5;

    // Rewards
    private int killReward = 10;
    private int winReward = 100;
    private int participationReward = 25;

    // ==================== Getters ====================

    public boolean isDebug() {
        return debug;
    }

    @NotNull
    public String getPrefix() {
        return prefix;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    @NotNull
    public Duration getCountdownDuration() {
        return Duration.ofSeconds(countdownSeconds);
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    @NotNull
    public Duration getGameDuration() {
        return Duration.ofMinutes(gameDurationMinutes);
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public boolean isAllowLatejoin() {
        return allowLatejoin;
    }

    public int getLatejoinGracePeriodSeconds() {
        return latejoinGracePeriodSeconds;
    }

    public int getArenaDetectionRadius() {
        return arenaDetectionRadius;
    }

    public boolean isAutoSaveArenas() {
        return autoSaveArenas;
    }

    public int getSaveIntervalMinutes() {
        return saveIntervalMinutes;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public int getScoreboardUpdateTicks() {
        return scoreboardUpdateTicks;
    }

    @NotNull
    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    @NotNull
    public String getBossBarColor() {
        return bossBarColor;
    }

    @NotNull
    public String getBossBarStyle() {
        return bossBarStyle;
    }

    public boolean isLeaderboardsEnabled() {
        return leaderboardsEnabled;
    }

    public int getLeaderboardTopCount() {
        return leaderboardTopCount;
    }

    public int getLeaderboardUpdateMinutes() {
        return leaderboardUpdateMinutes;
    }

    public int getKillReward() {
        return killReward;
    }

    public int getWinReward() {
        return winReward;
    }

    public int getParticipationReward() {
        return participationReward;
    }

    // ==================== Setters ====================

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public void setScoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
    }

    public void setBossBarEnabled(boolean bossBarEnabled) {
        this.bossBarEnabled = bossBarEnabled;
    }

    public void setLeaderboardsEnabled(boolean leaderboardsEnabled) {
        this.leaderboardsEnabled = leaderboardsEnabled;
    }
}
