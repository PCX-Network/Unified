/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.execution;

import sh.pcx.unified.commands.annotation.Cooldown;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages command cooldowns for rate limiting.
 *
 * <p>The {@code CooldownManager} tracks when commands were last executed
 * and enforces cooldown periods defined by the {@link Cooldown} annotation.
 * It supports various scoping strategies including per-player, global,
 * and custom scopes.</p>
 *
 * <h2>Cooldown Scopes</h2>
 * <ul>
 *   <li><b>PLAYER</b> - Separate cooldown per player (default)</li>
 *   <li><b>GLOBAL</b> - Shared cooldown across all players</li>
 *   <li><b>WORLD</b> - Separate cooldown per world</li>
 *   <li><b>CUSTOM</b> - User-defined scope using templates</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Checking Cooldown</h3>
 * <pre>{@code
 * CooldownManager cooldowns = commandService.getCooldownManager();
 *
 * // Check if player is on cooldown
 * if (cooldowns.isOnCooldown(player.getUniqueId(), "spawn")) {
 *     long remaining = cooldowns.getRemainingCooldown(player.getUniqueId(), "spawn");
 *     player.sendMessage("Wait " + (remaining / 1000) + " seconds!");
 *     return;
 * }
 *
 * // Execute command and set cooldown
 * cooldowns.setCooldown(player.getUniqueId(), "spawn", Duration.ofSeconds(30));
 * }</pre>
 *
 * <h3>Global Cooldown</h3>
 * <pre>{@code
 * // Set global cooldown (null UUID)
 * cooldowns.setGlobalCooldown("broadcast", Duration.ofMinutes(5));
 *
 * // Check global cooldown
 * if (cooldowns.isOnGlobalCooldown("broadcast")) {
 *     // Cooldown active
 * }
 * }</pre>
 *
 * <h3>Persistent Cooldowns</h3>
 * <pre>{@code
 * // Enable persistence for long cooldowns
 * cooldowns.setPersistent("dailykit", true);
 *
 * // Cooldowns are saved/loaded automatically on restart
 * }</pre>
 *
 * <h3>Bypassing Cooldowns</h3>
 * <pre>{@code
 * // Clear a player's cooldown
 * cooldowns.clearCooldown(player.getUniqueId(), "spawn");
 *
 * // Clear all cooldowns for a player
 * cooldowns.clearAllCooldowns(player.getUniqueId());
 *
 * // Clear all cooldowns for a command
 * cooldowns.clearCommandCooldowns("spawn");
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Cooldown
 */
public interface CooldownManager {

    /**
     * Checks if a player is on cooldown for a command.
     *
     * <pre>{@code
     * if (manager.isOnCooldown(player.getUniqueId(), "spawn")) {
     *     // Cooldown is active
     * }
     * }</pre>
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return {@code true} if on cooldown
     */
    boolean isOnCooldown(@NotNull UUID playerId, @NotNull String command);

    /**
     * Checks if there is a global cooldown active for a command.
     *
     * @param command the command name
     * @return {@code true} if on global cooldown
     */
    boolean isOnGlobalCooldown(@NotNull String command);

    /**
     * Checks cooldown with a custom scope key.
     *
     * <pre>{@code
     * // Per-warp cooldown
     * String scopeKey = playerId + ":" + warpName;
     * if (manager.isOnCooldown(scopeKey, "warp")) {
     *     // Cooldown active for this specific warp
     * }
     * }</pre>
     *
     * @param scopeKey the scope key
     * @param command the command name
     * @return {@code true} if on cooldown
     */
    boolean isOnCooldown(@NotNull String scopeKey, @NotNull String command);

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return remaining time in milliseconds, 0 if not on cooldown
     */
    long getRemainingCooldown(@NotNull UUID playerId, @NotNull String command);

    /**
     * Gets the remaining global cooldown time.
     *
     * @param command the command name
     * @return remaining time in milliseconds, 0 if not on cooldown
     */
    long getRemainingGlobalCooldown(@NotNull String command);

    /**
     * Gets the remaining cooldown for a custom scope.
     *
     * @param scopeKey the scope key
     * @param command the command name
     * @return remaining time in milliseconds, 0 if not on cooldown
     */
    long getRemainingCooldown(@NotNull String scopeKey, @NotNull String command);

    /**
     * Gets the remaining cooldown as a Duration.
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return the remaining duration
     */
    @NotNull
    Duration getRemainingDuration(@NotNull UUID playerId, @NotNull String command);

    /**
     * Sets a cooldown for a player.
     *
     * <pre>{@code
     * manager.setCooldown(player.getUniqueId(), "spawn", Duration.ofSeconds(30));
     * }</pre>
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @param duration the cooldown duration
     */
    void setCooldown(@NotNull UUID playerId, @NotNull String command, @NotNull Duration duration);

    /**
     * Sets a global cooldown.
     *
     * @param command the command name
     * @param duration the cooldown duration
     */
    void setGlobalCooldown(@NotNull String command, @NotNull Duration duration);

    /**
     * Sets a cooldown with a custom scope.
     *
     * @param scopeKey the scope key
     * @param command the command name
     * @param duration the cooldown duration
     */
    void setCooldown(@NotNull String scopeKey, @NotNull String command, @NotNull Duration duration);

    /**
     * Sets a cooldown with an explicit expiry time.
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @param expiresAt when the cooldown expires
     */
    void setCooldown(@NotNull UUID playerId, @NotNull String command, @NotNull Instant expiresAt);

    /**
     * Clears a player's cooldown for a command.
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return {@code true} if a cooldown was cleared
     */
    boolean clearCooldown(@NotNull UUID playerId, @NotNull String command);

    /**
     * Clears the global cooldown for a command.
     *
     * @param command the command name
     * @return {@code true} if a cooldown was cleared
     */
    boolean clearGlobalCooldown(@NotNull String command);

    /**
     * Clears a cooldown for a custom scope.
     *
     * @param scopeKey the scope key
     * @param command the command name
     * @return {@code true} if a cooldown was cleared
     */
    boolean clearCooldown(@NotNull String scopeKey, @NotNull String command);

    /**
     * Clears all cooldowns for a player.
     *
     * @param playerId the player's UUID
     * @return number of cooldowns cleared
     */
    int clearAllCooldowns(@NotNull UUID playerId);

    /**
     * Clears all cooldowns for a command.
     *
     * @param command the command name
     * @return number of cooldowns cleared
     */
    int clearCommandCooldowns(@NotNull String command);

    /**
     * Clears all cooldowns.
     */
    void clearAll();

    /**
     * Gets the time when a cooldown expires.
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return the expiry instant, or {@code null} if not on cooldown
     */
    Instant getExpiryTime(@NotNull UUID playerId, @NotNull String command);

    /**
     * Enables persistence for a command's cooldowns.
     *
     * <p>Persistent cooldowns are saved to disk and restored on server restart.
     * Useful for long cooldowns like daily rewards.</p>
     *
     * @param command the command name
     * @param persistent whether to persist cooldowns
     */
    void setPersistent(@NotNull String command, boolean persistent);

    /**
     * Checks if a command's cooldowns are persistent.
     *
     * @param command the command name
     * @return {@code true} if persistent
     */
    boolean isPersistent(@NotNull String command);

    /**
     * Saves all persistent cooldowns to storage.
     */
    void savePersistent();

    /**
     * Loads persistent cooldowns from storage.
     */
    void loadPersistent();

    /**
     * Gets the formatted remaining time for display.
     *
     * <pre>{@code
     * String remaining = manager.getFormattedRemaining(playerId, "spawn");
     * // Returns: "30 seconds", "1 minute 30 seconds", etc.
     * }</pre>
     *
     * @param playerId the player's UUID
     * @param command the command name
     * @return formatted remaining time
     */
    @NotNull
    String getFormattedRemaining(@NotNull UUID playerId, @NotNull String command);

    /**
     * Cleans up expired cooldowns.
     *
     * <p>Called periodically to remove expired entries from memory.</p>
     *
     * @return number of expired cooldowns removed
     */
    int cleanup();
}
