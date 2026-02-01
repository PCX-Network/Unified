/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic interface representing an online player.
 *
 * <p>This interface wraps the platform-specific player object (e.g., Bukkit's Player,
 * Sponge's ServerPlayer) and provides a unified API for common player operations.
 * All implementations are guaranteed to be thread-safe for read operations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get a player from the server
 * Optional<UnifiedPlayer> player = UnifiedAPI.getPlayer(uuid);
 *
 * player.ifPresent(p -> {
 *     // Send a message
 *     p.sendMessage(Component.text("Hello, " + p.getName() + "!"));
 *
 *     // Teleport the player
 *     UnifiedLocation spawn = world.getSpawnLocation();
 *     p.teleport(spawn);
 *
 *     // Check permissions
 *     if (p.hasPermission("myplugin.admin")) {
 *         p.sendMessage(Component.text("You are an admin!"));
 *     }
 *
 *     // Give an item
 *     p.giveItem(itemStack);
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations (getters) are thread-safe. Write operations (setters, actions)
 * may need to be executed on the appropriate thread depending on the platform.
 * Use the scheduler service for thread-safe modifications.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see OfflineUnifiedPlayer
 * @see PlayerSession
 */
public interface UnifiedPlayer extends OfflineUnifiedPlayer, Audience {

    /**
     * Returns the player's display name.
     *
     * <p>The display name may differ from the actual username and can include
     * formatting, prefixes, or nicknames set by other plugins.
     *
     * @return the player's display name as a Component
     * @since 1.0.0
     */
    @NotNull
    Component getDisplayName();

    /**
     * Sets the player's display name.
     *
     * @param displayName the new display name, or null to reset to default
     * @since 1.0.0
     */
    void setDisplayName(@Nullable Component displayName);

    /**
     * Returns the player's current location.
     *
     * @return the player's location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getLocation();

    /**
     * Returns the world the player is currently in.
     *
     * @return the player's current world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getWorld();

    /**
     * Teleports the player to the specified location.
     *
     * @param location the destination location
     * @return a future that completes when the teleport is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> teleport(@NotNull UnifiedLocation location);

    /**
     * Teleports the player to another player's location.
     *
     * @param target the player to teleport to
     * @return a future that completes when the teleport is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> teleport(@NotNull UnifiedPlayer target);

    /**
     * Returns the player's current health.
     *
     * @return the player's health (0.0 to max health)
     * @since 1.0.0
     */
    double getHealth();

    /**
     * Sets the player's health.
     *
     * @param health the new health value (0.0 to max health)
     * @throws IllegalArgumentException if health is negative or exceeds max health
     * @since 1.0.0
     */
    void setHealth(double health);

    /**
     * Returns the player's maximum health.
     *
     * @return the player's maximum health
     * @since 1.0.0
     */
    double getMaxHealth();

    /**
     * Returns the player's food level.
     *
     * @return the food level (0-20)
     * @since 1.0.0
     */
    int getFoodLevel();

    /**
     * Sets the player's food level.
     *
     * @param level the new food level (0-20)
     * @since 1.0.0
     */
    void setFoodLevel(int level);

    /**
     * Returns the player's experience level.
     *
     * @return the experience level
     * @since 1.0.0
     */
    int getLevel();

    /**
     * Sets the player's experience level.
     *
     * @param level the new experience level
     * @since 1.0.0
     */
    void setLevel(int level);

    /**
     * Returns the player's experience progress towards the next level.
     *
     * @return the experience progress (0.0 to 1.0)
     * @since 1.0.0
     */
    float getExp();

    /**
     * Sets the player's experience progress towards the next level.
     *
     * @param exp the experience progress (0.0 to 1.0)
     * @since 1.0.0
     */
    void setExp(float exp);

    /**
     * Returns the player's total experience points.
     *
     * @return the total experience points
     * @since 1.0.0
     */
    int getTotalExperience();

    /**
     * Sets the player's total experience points.
     *
     * @param exp the total experience points
     * @since 1.0.0
     */
    void setTotalExperience(int exp);

    /**
     * Gives experience points to the player.
     *
     * @param amount the amount of experience to give
     * @since 1.0.0
     */
    void giveExp(int amount);

    /**
     * Returns the player's game mode.
     *
     * @return the player's game mode
     * @since 1.0.0
     */
    @NotNull
    GameMode getGameMode();

    /**
     * Sets the player's game mode.
     *
     * @param gameMode the new game mode
     * @since 1.0.0
     */
    void setGameMode(@NotNull GameMode gameMode);

    /**
     * Checks if the player is flying.
     *
     * @return true if the player is flying
     * @since 1.0.0
     */
    boolean isFlying();

    /**
     * Sets whether the player is flying.
     *
     * @param flying true to enable flying
     * @since 1.0.0
     */
    void setFlying(boolean flying);

    /**
     * Checks if the player is allowed to fly.
     *
     * @return true if flight is allowed
     * @since 1.0.0
     */
    boolean getAllowFlight();

    /**
     * Sets whether the player is allowed to fly.
     *
     * @param allow true to allow flight
     * @since 1.0.0
     */
    void setAllowFlight(boolean allow);

    /**
     * Checks if the player is sneaking.
     *
     * @return true if the player is sneaking
     * @since 1.0.0
     */
    boolean isSneaking();

    /**
     * Checks if the player is sprinting.
     *
     * @return true if the player is sprinting
     * @since 1.0.0
     */
    boolean isSprinting();

    /**
     * Checks if the player is currently online.
     *
     * <p>This method always returns true for UnifiedPlayer instances.
     * It is provided for API consistency with {@link OfflineUnifiedPlayer}.
     *
     * @return true (always, since this represents an online player)
     * @since 1.0.0
     */
    @Override
    default boolean isOnline() {
        return true;
    }

    /**
     * Returns the player's locale/language setting.
     *
     * @return the player's locale
     * @since 1.0.0
     */
    @NotNull
    Locale getLocale();

    /**
     * Returns the player's ping in milliseconds.
     *
     * @return the ping in milliseconds
     * @since 1.0.0
     */
    int getPing();

    /**
     * Returns the player's client brand (e.g., "vanilla", "fabric", "forge").
     *
     * @return the client brand, or empty if unknown
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getClientBrand();

    /**
     * Returns the player's IP address.
     *
     * @return the IP address as a string
     * @since 1.0.0
     */
    @NotNull
    String getAddress();

    /**
     * Kicks the player from the server.
     *
     * @param reason the kick reason shown to the player
     * @since 1.0.0
     */
    void kick(@NotNull Component reason);

    /**
     * Kicks the player from the server with a default message.
     *
     * @since 1.0.0
     */
    void kick();

    /**
     * Checks if the player has the specified permission.
     *
     * @param permission the permission node to check
     * @return true if the player has the permission
     * @since 1.0.0
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Checks if the player is an operator.
     *
     * @return true if the player is an operator
     * @since 1.0.0
     */
    boolean isOp();

    /**
     * Sets the player's operator status.
     *
     * @param op true to make the player an operator
     * @since 1.0.0
     */
    void setOp(boolean op);

    /**
     * Returns the item currently in the player's main hand.
     *
     * @return the item in the main hand, or empty item if nothing
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItemInMainHand();

    /**
     * Sets the item in the player's main hand.
     *
     * @param item the item to set, or null for empty
     * @since 1.0.0
     */
    void setItemInMainHand(@Nullable UnifiedItemStack item);

    /**
     * Returns the item currently in the player's off hand.
     *
     * @return the item in the off hand, or empty item if nothing
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItemInOffHand();

    /**
     * Sets the item in the player's off hand.
     *
     * @param item the item to set, or null for empty
     * @since 1.0.0
     */
    void setItemInOffHand(@Nullable UnifiedItemStack item);

    /**
     * Gives an item to the player, adding it to their inventory.
     *
     * <p>If the inventory is full, the item will be dropped at the player's location.
     *
     * @param item the item to give
     * @return true if the item was added to inventory, false if dropped
     * @since 1.0.0
     */
    boolean giveItem(@NotNull UnifiedItemStack item);

    /**
     * Opens the player's inventory GUI (their own inventory).
     *
     * @since 1.0.0
     */
    void openInventory();

    /**
     * Closes any open inventory GUI.
     *
     * @since 1.0.0
     */
    void closeInventory();

    /**
     * Performs a command as this player.
     *
     * @param command the command to perform (without leading slash)
     * @return true if the command was executed successfully
     * @since 1.0.0
     */
    boolean performCommand(@NotNull String command);

    /**
     * Sends a plugin message on the specified channel.
     *
     * @param channel the channel name
     * @param data    the message data
     * @since 1.0.0
     */
    void sendPluginMessage(@NotNull String channel, byte @NotNull [] data);

    /**
     * Plays a sound to this player.
     *
     * @param sound  the sound name/key
     * @param volume the volume (0.0 to 1.0)
     * @param pitch  the pitch (0.5 to 2.0)
     * @since 1.0.0
     */
    void playSound(@NotNull String sound, float volume, float pitch);

    /**
     * Returns the current session for this player.
     *
     * @return the player's session
     * @since 1.0.0
     */
    @NotNull
    PlayerSession getSession();

    /**
     * Returns the underlying platform-specific player object.
     *
     * <p>Use this method when you need to access platform-specific functionality
     * not available through the unified API. The returned object type depends
     * on the current platform:
     * <ul>
     *   <li>Paper/Spigot: {@code org.bukkit.entity.Player}</li>
     *   <li>Sponge: {@code org.spongepowered.api.entity.living.player.server.ServerPlayer}</li>
     * </ul>
     *
     * @param <T> the expected platform player type
     * @return the platform-specific player object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();

    /**
     * Game mode enumeration for players.
     *
     * @since 1.0.0
     */
    enum GameMode {
        /**
         * Survival mode - default gameplay with health, hunger, and resource gathering.
         */
        SURVIVAL,

        /**
         * Creative mode - unlimited resources, flight, and invulnerability.
         */
        CREATIVE,

        /**
         * Adventure mode - similar to survival but cannot break/place blocks without proper tools.
         */
        ADVENTURE,

        /**
         * Spectator mode - invisible, can fly through blocks, and cannot interact with the world.
         */
        SPECTATOR
    }
}
