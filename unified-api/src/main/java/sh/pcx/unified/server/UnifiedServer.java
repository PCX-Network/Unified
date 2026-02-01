/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.server;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Platform-agnostic interface for server operations.
 *
 * <p>This interface provides access to server-wide functionality including
 * player management, world access, console commands, and server information.
 * It abstracts the differences between Bukkit's Server and Sponge's Server.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the server instance
 * UnifiedServer server = UnifiedAPI.getServer();
 *
 * // Get server information
 * String name = server.getName();
 * MinecraftVersion version = server.getMinecraftVersion();
 * ServerType type = server.getServerType();
 *
 * // Player operations
 * Collection<UnifiedPlayer> players = server.getOnlinePlayers();
 * Optional<UnifiedPlayer> player = server.getPlayer(uuid);
 *
 * // World operations
 * Collection<UnifiedWorld> worlds = server.getWorlds();
 * Optional<UnifiedWorld> world = server.getWorld("world");
 *
 * // Broadcast messages
 * server.broadcast(Component.text("Server restart in 5 minutes!"));
 *
 * // Execute console commands
 * server.executeCommand("say Hello, World!");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Most read operations are thread-safe. Operations that modify server state
 * should be performed on the main thread or appropriate scheduler thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedPlayer
 * @see UnifiedWorld
 */
public interface UnifiedServer {

    /**
     * Returns the server name.
     *
     * @return the server name (e.g., "Paper", "Spigot", "Sponge")
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the server version string.
     *
     * @return the full server version string
     * @since 1.0.0
     */
    @NotNull
    String getVersion();

    /**
     * Returns the Minecraft version running on this server.
     *
     * @return the Minecraft version
     * @since 1.0.0
     */
    @NotNull
    MinecraftVersion getMinecraftVersion();

    /**
     * Returns the server type/platform.
     *
     * @return the server type
     * @since 1.0.0
     */
    @NotNull
    ServerType getServerType();

    /**
     * Returns the server's bind address.
     *
     * @return the server address
     * @since 1.0.0
     */
    @NotNull
    InetSocketAddress getAddress();

    /**
     * Returns the server port.
     *
     * @return the server port
     * @since 1.0.0
     */
    int getPort();

    /**
     * Returns the server's IP address as a string.
     *
     * @return the IP address string
     * @since 1.0.0
     */
    @NotNull
    String getIp();

    /**
     * Returns the maximum number of players allowed on the server.
     *
     * @return the maximum player count
     * @since 1.0.0
     */
    int getMaxPlayers();

    /**
     * Sets the maximum number of players allowed on the server.
     *
     * @param maxPlayers the maximum player count
     * @since 1.0.0
     */
    void setMaxPlayers(int maxPlayers);

    /**
     * Returns the current number of online players.
     *
     * @return the online player count
     * @since 1.0.0
     */
    int getOnlinePlayerCount();

    /**
     * Returns all currently online players.
     *
     * @return a collection of online players
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedPlayer> getOnlinePlayers();

    /**
     * Returns an online player by their UUID.
     *
     * @param uuid the player's UUID
     * @return an Optional containing the player if online
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid);

    /**
     * Returns an online player by their name.
     *
     * <p><strong>Note:</strong> Player names are case-insensitive and may change.
     * Prefer using {@link #getPlayer(UUID)} when possible.
     *
     * @param name the player's name
     * @return an Optional containing the player if online
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer(@NotNull String name);

    /**
     * Returns an offline player by their UUID.
     *
     * <p>This will return a player even if they have never played on the server.
     *
     * @param uuid the player's UUID
     * @return the offline player
     * @since 1.0.0
     */
    @NotNull
    OfflineUnifiedPlayer getOfflinePlayer(@NotNull UUID uuid);

    /**
     * Returns an offline player by their name.
     *
     * @param name the player's name
     * @return the offline player
     * @deprecated Use {@link #getOfflinePlayer(UUID)} instead
     * @since 1.0.0
     */
    @Deprecated
    @NotNull
    OfflineUnifiedPlayer getOfflinePlayer(@NotNull String name);

    /**
     * Returns all players who have ever played on this server.
     *
     * @return a collection of offline players
     * @since 1.0.0
     */
    @NotNull
    Collection<OfflineUnifiedPlayer> getOfflinePlayers();

    /**
     * Checks if a player with the given UUID has ever played on this server.
     *
     * @param uuid the player's UUID
     * @return true if the player has played before
     * @since 1.0.0
     */
    boolean hasPlayedBefore(@NotNull UUID uuid);

    /**
     * Returns all loaded worlds.
     *
     * @return a collection of loaded worlds
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedWorld> getWorlds();

    /**
     * Returns a world by its name.
     *
     * @param name the world name
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull String name);

    /**
     * Returns a world by its UUID.
     *
     * @param uuid the world's UUID
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull UUID uuid);

    /**
     * Returns the default/main world.
     *
     * @return the default world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld getDefaultWorld();

    /**
     * Broadcasts a message to all online players.
     *
     * @param message the message to broadcast
     * @since 1.0.0
     */
    void broadcast(@NotNull Component message);

    /**
     * Broadcasts a message to players with a specific permission.
     *
     * @param message    the message to broadcast
     * @param permission the required permission
     * @since 1.0.0
     */
    void broadcast(@NotNull Component message, @NotNull String permission);

    /**
     * Executes a command as the console.
     *
     * @param command the command to execute (without leading slash)
     * @return true if the command was executed successfully
     * @since 1.0.0
     */
    boolean executeCommand(@NotNull String command);

    /**
     * Executes a command asynchronously as the console.
     *
     * @param command the command to execute
     * @return a future that completes with the result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> executeCommandAsync(@NotNull String command);

    /**
     * Returns the server's data folder.
     *
     * @return the path to the server folder
     * @since 1.0.0
     */
    @NotNull
    Path getServerFolder();

    /**
     * Returns the plugins folder.
     *
     * @return the path to the plugins folder
     * @since 1.0.0
     */
    @NotNull
    Path getPluginsFolder();

    /**
     * Returns the worlds folder.
     *
     * @return the path to the worlds folder
     * @since 1.0.0
     */
    @NotNull
    Path getWorldsFolder();

    /**
     * Returns the current TPS (ticks per second).
     *
     * <p>Ideal TPS is 20. Lower values indicate server lag.
     *
     * @return the current TPS
     * @since 1.0.0
     */
    double getTPS();

    /**
     * Returns the average TPS over the last minute.
     *
     * @return array of TPS averages [1 min, 5 min, 15 min]
     * @since 1.0.0
     */
    double @NotNull [] getAverageTPS();

    /**
     * Returns the current MSPT (milliseconds per tick).
     *
     * <p>Ideal MSPT is under 50ms (20 TPS = 50ms per tick).
     *
     * @return the current MSPT
     * @since 1.0.0
     */
    double getMSPT();

    /**
     * Checks if the server is in online mode (authenticating players).
     *
     * @return true if online mode is enabled
     * @since 1.0.0
     */
    boolean isOnlineMode();

    /**
     * Checks if the whitelist is enabled.
     *
     * @return true if whitelist is enabled
     * @since 1.0.0
     */
    boolean isWhitelistEnabled();

    /**
     * Sets whether the whitelist is enabled.
     *
     * @param enabled true to enable whitelist
     * @since 1.0.0
     */
    void setWhitelistEnabled(boolean enabled);

    /**
     * Reloads the whitelist from disk.
     *
     * @since 1.0.0
     */
    void reloadWhitelist();

    /**
     * Returns the server's MOTD.
     *
     * @return the MOTD as a Component
     * @since 1.0.0
     */
    @NotNull
    Component getMotd();

    /**
     * Sets the server's MOTD.
     *
     * @param motd the new MOTD
     * @since 1.0.0
     */
    void setMotd(@NotNull Component motd);

    /**
     * Shuts down the server.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Restarts the server (if supported).
     *
     * <p>Not all server implementations support restart. Check the return value.
     *
     * @return true if restart was initiated
     * @since 1.0.0
     */
    boolean restart();

    /**
     * Checks if the server is currently stopping.
     *
     * @return true if the server is stopping
     * @since 1.0.0
     */
    boolean isStopping();

    /**
     * Returns the primary thread for this server.
     *
     * <p>For Folia, this returns the global region thread.
     *
     * @return the primary thread
     * @since 1.0.0
     */
    @NotNull
    Thread getPrimaryThread();

    /**
     * Checks if the current thread is the primary thread.
     *
     * @return true if on the primary thread
     * @since 1.0.0
     */
    boolean isPrimaryThread();

    /**
     * Returns the underlying platform-specific server object.
     *
     * @param <T> the expected platform server type
     * @return the platform-specific server object
     * @since 1.0.0
     */
    @NotNull
    <T> T getHandle();
}
