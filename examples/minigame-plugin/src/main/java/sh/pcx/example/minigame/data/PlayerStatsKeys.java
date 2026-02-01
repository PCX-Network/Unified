/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.data;

import sh.pcx.unified.data.player.DataKey;
import sh.pcx.unified.data.player.PersistentDataKey;

/**
 * Data keys for player statistics.
 *
 * <p>This class demonstrates how to define typed data keys for use with
 * the UnifiedPlugin PlayerDataService. Each key represents a piece of
 * player data that can be stored, queried, and synchronized.
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link PersistentDataKey} - Persisted to database and synced across servers</li>
 *   <li>{@link DataKey} - In-memory only, not persisted</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get a value
 * int kills = profile.getData(PlayerStatsKeys.KILLS);
 *
 * // Set a value
 * profile.setData(PlayerStatsKeys.KILLS, kills + 1);
 *
 * // Increment
 * profile.incrementData(PlayerStatsKeys.KILLS, 1);
 *
 * // Query
 * playerData.query()
 *     .orderBy(PlayerStatsKeys.KILLS, Order.DESC)
 *     .limit(10)
 *     .execute();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public final class PlayerStatsKeys {

    /**
     * Namespace for all minigame data keys.
     */
    public static final String NAMESPACE = "minigame";

    // ==================== Game Statistics ====================

    /**
     * Total games played.
     */
    public static final DataKey<Integer> GAMES_PLAYED =
            DataKey.ofInt(NAMESPACE, "games_played", 0);

    /**
     * Total game wins.
     */
    public static final DataKey<Integer> WINS =
            DataKey.ofInt(NAMESPACE, "wins", 0);

    /**
     * Total game losses.
     */
    public static final DataKey<Integer> LOSSES =
            DataKey.ofInt(NAMESPACE, "losses", 0);

    /**
     * Total kills across all games.
     */
    public static final DataKey<Integer> KILLS =
            DataKey.ofInt(NAMESPACE, "kills", 0);

    /**
     * Total deaths across all games.
     */
    public static final DataKey<Integer> DEATHS =
            DataKey.ofInt(NAMESPACE, "deaths", 0);

    /**
     * Total assists across all games.
     */
    public static final DataKey<Integer> ASSISTS =
            DataKey.ofInt(NAMESPACE, "assists", 0);

    /**
     * Total damage dealt.
     */
    public static final DataKey<Integer> DAMAGE_DEALT =
            DataKey.ofInt(NAMESPACE, "damage_dealt", 0);

    /**
     * Total damage taken.
     */
    public static final DataKey<Integer> DAMAGE_TAKEN =
            DataKey.ofInt(NAMESPACE, "damage_taken", 0);

    /**
     * Best kill streak ever achieved.
     */
    public static final DataKey<Integer> BEST_KILL_STREAK =
            DataKey.ofInt(NAMESPACE, "best_kill_streak", 0);

    /**
     * Total play time in seconds.
     */
    public static final DataKey<Integer> PLAY_TIME_SECONDS =
            DataKey.ofInt(NAMESPACE, "play_time_seconds", 0);

    // ==================== Currency ====================

    /**
     * Coin balance.
     */
    public static final DataKey<Long> COINS =
            DataKey.ofLong(NAMESPACE, "coins", 0L);

    // ==================== Timestamps ====================

    /**
     * First time playing the minigame (epoch millis).
     */
    public static final DataKey<Long> FIRST_PLAYED =
            DataKey.ofLong(NAMESPACE, "first_played", 0L);

    /**
     * Last time playing the minigame (epoch millis).
     */
    public static final DataKey<Long> LAST_PLAYED =
            DataKey.ofLong(NAMESPACE, "last_played", 0L);

    // ==================== Computed Keys ====================

    /**
     * Kill/death ratio (computed from kills and deaths).
     */
    public static final DataKey<Double> KD_RATIO =
            DataKey.computed(NAMESPACE, "kd_ratio", profile -> {
                int kills = profile.getData(KILLS);
                int deaths = profile.getData(DEATHS);
                if (deaths == 0) return (double) kills;
                return (double) kills / deaths;
            });

    /**
     * Win rate percentage (computed from wins and games played).
     */
    public static final DataKey<Double> WIN_RATE =
            DataKey.computed(NAMESPACE, "win_rate", profile -> {
                int wins = profile.getData(WINS);
                int games = profile.getData(GAMES_PLAYED);
                if (games == 0) return 0.0;
                return (double) wins / games * 100.0;
            });

    // ==================== Utility Methods ====================

    /**
     * Returns all statistic keys.
     *
     * @return array of all stat keys
     */
    public static DataKey<?>[] getAllKeys() {
        return new DataKey<?>[] {
                GAMES_PLAYED, WINS, LOSSES, KILLS, DEATHS, ASSISTS,
                DAMAGE_DEALT, DAMAGE_TAKEN, BEST_KILL_STREAK, PLAY_TIME_SECONDS,
                COINS, FIRST_PLAYED, LAST_PLAYED
        };
    }

    /**
     * Returns keys that can be used for leaderboards.
     *
     * @return array of leaderboard keys
     */
    public static DataKey<?>[] getLeaderboardKeys() {
        return new DataKey<?>[] {
                KILLS, WINS, KD_RATIO, GAMES_PLAYED, COINS
        };
    }

    // Private constructor to prevent instantiation
    private PlayerStatsKeys() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
