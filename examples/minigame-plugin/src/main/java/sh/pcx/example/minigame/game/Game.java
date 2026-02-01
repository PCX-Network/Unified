/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.game;

import sh.pcx.example.minigame.arena.Arena;
import sh.pcx.example.minigame.arena.ArenaState;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active minigame session.
 *
 * <p>A Game manages the state of a single minigame match, including:
 * <ul>
 *   <li>Player participation and teams</li>
 *   <li>Kill/death tracking</li>
 *   <li>Game timer and phase management</li>
 *   <li>Win condition checking</li>
 * </ul>
 *
 * <h2>Game Lifecycle</h2>
 * <ol>
 *   <li>WAITING - Waiting for minimum players</li>
 *   <li>STARTING - Countdown phase</li>
 *   <li>RUNNING - Active gameplay</li>
 *   <li>ENDING - Game over, showing results</li>
 * </ol>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see GameManager
 * @see GamePhase
 */
public class Game {

    private final UUID id;
    private final Arena arena;
    private final Map<UUID, GamePlayer> players;
    private final Map<UUID, Integer> killCounts;
    private final Map<UUID, Integer> deathCounts;
    private final List<UUID> spectators;

    private GamePhase phase;
    private Instant startTime;
    private Instant endTime;
    private int countdownSeconds;
    private int gameDurationSeconds;
    private UUID winner;

    /**
     * Creates a new game in the specified arena.
     *
     * @param arena the arena for this game
     */
    public Game(@NotNull Arena arena) {
        this.id = UUID.randomUUID();
        this.arena = arena;
        this.players = new ConcurrentHashMap<>();
        this.killCounts = new ConcurrentHashMap<>();
        this.deathCounts = new ConcurrentHashMap<>();
        this.spectators = Collections.synchronizedList(new ArrayList<>());
        this.phase = GamePhase.WAITING;
        this.countdownSeconds = 30;
        this.gameDurationSeconds = 600; // 10 minutes
    }

    // ==================== Player Management ====================

    /**
     * Adds a player to the game.
     *
     * @param player the player to add
     * @return true if the player was added
     */
    public boolean addPlayer(@NotNull UnifiedPlayer player) {
        if (players.containsKey(player.getUniqueId())) {
            return false;
        }

        if (players.size() >= arena.getMaxPlayers()) {
            return false;
        }

        if (phase == GamePhase.RUNNING || phase == GamePhase.ENDING) {
            return false;
        }

        GamePlayer gamePlayer = new GamePlayer(player.getUniqueId(), player.getName());
        players.put(player.getUniqueId(), gamePlayer);
        killCounts.put(player.getUniqueId(), 0);
        deathCounts.put(player.getUniqueId(), 0);

        return true;
    }

    /**
     * Removes a player from the game.
     *
     * @param playerId the UUID of the player to remove
     * @return true if the player was removed
     */
    public boolean removePlayer(@NotNull UUID playerId) {
        GamePlayer removed = players.remove(playerId);
        if (removed != null) {
            spectators.remove(playerId);
            return true;
        }
        return false;
    }

    /**
     * Checks if a player is in the game.
     *
     * @param playerId the player's UUID
     * @return true if the player is in the game
     */
    public boolean hasPlayer(@NotNull UUID playerId) {
        return players.containsKey(playerId);
    }

    /**
     * Gets the game player data for a player.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the game player data
     */
    @NotNull
    public Optional<GamePlayer> getPlayer(@NotNull UUID playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    /**
     * Adds a spectator to the game.
     *
     * @param playerId the spectator's UUID
     */
    public void addSpectator(@NotNull UUID playerId) {
        if (!spectators.contains(playerId)) {
            spectators.add(playerId);
        }
    }

    /**
     * Removes a spectator from the game.
     *
     * @param playerId the spectator's UUID
     */
    public void removeSpectator(@NotNull UUID playerId) {
        spectators.remove(playerId);
    }

    /**
     * Checks if a player is a spectator.
     *
     * @param playerId the player's UUID
     * @return true if the player is spectating
     */
    public boolean isSpectator(@NotNull UUID playerId) {
        return spectators.contains(playerId);
    }

    // ==================== Kill/Death Tracking ====================

    /**
     * Records a kill for a player.
     *
     * @param killerId the killer's UUID
     * @param victimId the victim's UUID
     */
    public void recordKill(@NotNull UUID killerId, @NotNull UUID victimId) {
        killCounts.compute(killerId, (k, v) -> (v == null ? 0 : v) + 1);
        deathCounts.compute(victimId, (k, v) -> (v == null ? 0 : v) + 1);

        GamePlayer killer = players.get(killerId);
        GamePlayer victim = players.get(victimId);

        if (killer != null) {
            killer.addKill();
        }
        if (victim != null) {
            victim.addDeath();
        }
    }

    /**
     * Gets the kill count for a player.
     *
     * @param playerId the player's UUID
     * @return the kill count
     */
    public int getKills(@NotNull UUID playerId) {
        return killCounts.getOrDefault(playerId, 0);
    }

    /**
     * Gets the death count for a player.
     *
     * @param playerId the player's UUID
     * @return the death count
     */
    public int getDeaths(@NotNull UUID playerId) {
        return deathCounts.getOrDefault(playerId, 0);
    }

    // ==================== Game Phase Management ====================

    /**
     * Starts the countdown phase.
     */
    public void startCountdown() {
        if (phase != GamePhase.WAITING) {
            return;
        }
        phase = GamePhase.STARTING;
        arena.setState(ArenaState.STARTING);
    }

    /**
     * Starts the game.
     */
    public void start() {
        if (phase != GamePhase.STARTING) {
            return;
        }
        phase = GamePhase.RUNNING;
        startTime = Instant.now();
        arena.setState(ArenaState.IN_GAME);
    }

    /**
     * Ends the game.
     *
     * @param winner the winner's UUID, or null for draw/timeout
     */
    public void end(@Nullable UUID winner) {
        if (phase == GamePhase.ENDING) {
            return;
        }
        phase = GamePhase.ENDING;
        endTime = Instant.now();
        this.winner = winner;
    }

    /**
     * Checks if the game can start (has minimum players).
     *
     * @return true if the game has enough players
     */
    public boolean canStart() {
        return players.size() >= arena.getMinPlayers();
    }

    /**
     * Checks if the game is currently active.
     *
     * @return true if the game is running
     */
    public boolean isActive() {
        return phase == GamePhase.STARTING || phase == GamePhase.RUNNING;
    }

    /**
     * Gets the remaining time in seconds.
     *
     * @return the remaining time, or -1 if not running
     */
    public int getRemainingSeconds() {
        if (startTime == null || phase != GamePhase.RUNNING) {
            return -1;
        }
        long elapsed = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        return Math.max(0, gameDurationSeconds - (int) elapsed);
    }

    /**
     * Gets the elapsed time in seconds.
     *
     * @return the elapsed time, or 0 if not started
     */
    public int getElapsedSeconds() {
        if (startTime == null) {
            return 0;
        }
        Instant end = endTime != null ? endTime : Instant.now();
        return (int) (end.getEpochSecond() - startTime.getEpochSecond());
    }

    // ==================== Spawn Points ====================

    /**
     * Gets a spawn location for a player.
     *
     * @param playerId the player's UUID
     * @return a spawn location
     */
    @NotNull
    public UnifiedLocation getSpawnLocation(@NotNull UUID playerId) {
        // Get a spawn point based on player index for fair distribution
        List<UnifiedLocation> spawns = arena.getShuffledSpawnPoints();
        if (spawns.isEmpty()) {
            return arena.getCenter();
        }

        int index = new ArrayList<>(players.keySet()).indexOf(playerId);
        return spawns.get(index % spawns.size());
    }

    // ==================== Leaderboard ====================

    /**
     * Gets the current leaderboard sorted by kills.
     *
     * @return a list of player IDs sorted by kills (descending)
     */
    @NotNull
    public List<UUID> getLeaderboard() {
        return players.keySet().stream()
                .sorted((a, b) -> Integer.compare(getKills(b), getKills(a)))
                .toList();
    }

    /**
     * Gets the current leader.
     *
     * @return an Optional containing the leader's UUID
     */
    @NotNull
    public Optional<UUID> getLeader() {
        return getLeaderboard().stream().findFirst();
    }

    // ==================== Getters ====================

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public Arena getArena() {
        return arena;
    }

    @NotNull
    public GamePhase getPhase() {
        return phase;
    }

    public int getPlayerCount() {
        return players.size();
    }

    @NotNull
    public Collection<GamePlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    @NotNull
    public Set<UUID> getPlayerIds() {
        return Collections.unmodifiableSet(players.keySet());
    }

    @NotNull
    public List<UUID> getSpectators() {
        return Collections.unmodifiableList(spectators);
    }

    @Nullable
    public Instant getStartTime() {
        return startTime;
    }

    @Nullable
    public Instant getEndTime() {
        return endTime;
    }

    @Nullable
    public UUID getWinner() {
        return winner;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public int getGameDurationSeconds() {
        return gameDurationSeconds;
    }

    public void setGameDurationSeconds(int gameDurationSeconds) {
        this.gameDurationSeconds = gameDurationSeconds;
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Game other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Game{id=" + id + ", arena=" + arena.getName() + ", phase=" + phase +
                ", players=" + players.size() + "}";
    }
}
