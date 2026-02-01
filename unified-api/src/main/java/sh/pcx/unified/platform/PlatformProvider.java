/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform;

import sh.pcx.unified.player.OfflineUnifiedPlayer;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

/**
 * Service Provider Interface (SPI) for platform implementations.
 *
 * <p>This interface defines the contract that platform-specific implementations
 * must fulfill to integrate with the UnifiedPlugin API. Each supported platform
 * (Paper, Sponge, etc.) provides an implementation of this interface.
 *
 * <h2>Implementation Guidelines</h2>
 * <ul>
 *   <li>Implementations must be thread-safe</li>
 *   <li>All wrapper methods must handle null platform objects gracefully</li>
 *   <li>Implementations should cache wrapper objects when appropriate</li>
 *   <li>Register via {@code META-INF/services/sh.pcx.unified.platform.PlatformProvider}</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PaperPlatformProvider implements PlatformProvider {
 *
 *     @Override
 *     public PlatformType getPlatformType() {
 *         return PlatformType.BUKKIT;
 *     }
 *
 *     @Override
 *     public UnifiedPlayer wrapPlayer(Object platformPlayer) {
 *         if (platformPlayer instanceof Player player) {
 *             return new PaperUnifiedPlayer(player);
 *         }
 *         throw new IllegalArgumentException("Not a Bukkit Player");
 *     }
 *
 *     // ... other methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Platform
 * @see PlatformType
 */
public interface PlatformProvider {

    /**
     * Loads the platform provider using ServiceLoader.
     *
     * @return the loaded provider
     * @throws IllegalStateException if no provider is found
     * @since 1.0.0
     */
    @NotNull
    static PlatformProvider load() {
        ServiceLoader<PlatformProvider> loader = ServiceLoader.load(PlatformProvider.class);
        return loader.findFirst().orElseThrow(() ->
                new IllegalStateException("No PlatformProvider implementation found. " +
                        "Ensure a platform module (unified-paper, unified-sponge) is on the classpath.")
        );
    }

    /**
     * Returns the platform type this provider implements.
     *
     * @return the platform type
     * @since 1.0.0
     */
    @NotNull
    PlatformType getPlatformType();

    /**
     * Returns the priority of this provider.
     *
     * <p>Higher priority providers are preferred when multiple are available.
     * Default priority is 0.
     *
     * @return the provider priority
     * @since 1.0.0
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Checks if this provider can handle the current environment.
     *
     * @return true if this provider is compatible with the current platform
     * @since 1.0.0
     */
    boolean isCompatible();

    /**
     * Initializes the platform provider.
     *
     * <p>Called once during API startup. Implementations should perform
     * any necessary setup here.
     *
     * @throws Exception if initialization fails
     * @since 1.0.0
     */
    void initialize() throws Exception;

    /**
     * Shuts down the platform provider.
     *
     * <p>Called during API shutdown. Implementations should cleanup
     * any resources here.
     *
     * @since 1.0.0
     */
    void shutdown();

    /**
     * Creates the Platform instance for this provider.
     *
     * @return the platform instance
     * @since 1.0.0
     */
    @NotNull
    Platform createPlatform();

    /**
     * Creates the UnifiedServer instance for this provider.
     *
     * @return the server instance
     * @since 1.0.0
     */
    @NotNull
    UnifiedServer createServer();

    /**
     * Wraps a platform-specific player object into a UnifiedPlayer.
     *
     * @param platformPlayer the platform player object
     * @return the wrapped player
     * @throws IllegalArgumentException if the object is not a valid player
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer wrapPlayer(@NotNull Object platformPlayer);

    /**
     * Wraps a platform-specific offline player object.
     *
     * @param platformOfflinePlayer the platform offline player object
     * @return the wrapped offline player
     * @throws IllegalArgumentException if the object is not a valid offline player
     * @since 1.0.0
     */
    @NotNull
    OfflineUnifiedPlayer wrapOfflinePlayer(@NotNull Object platformOfflinePlayer);

    /**
     * Wraps a platform-specific world object.
     *
     * @param platformWorld the platform world object
     * @return the wrapped world
     * @throws IllegalArgumentException if the object is not a valid world
     * @since 1.0.0
     */
    @NotNull
    UnifiedWorld wrapWorld(@NotNull Object platformWorld);

    /**
     * Gets a player by UUID using the platform's player lookup.
     *
     * @param uuid the player's UUID
     * @return an Optional containing the player if online
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer(@NotNull UUID uuid);

    /**
     * Gets a player by name using the platform's player lookup.
     *
     * @param name the player's name
     * @return an Optional containing the player if online
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedPlayer> getPlayer(@NotNull String name);

    /**
     * Gets a world by name using the platform's world lookup.
     *
     * @param name the world name
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull String name);

    /**
     * Gets a world by UUID using the platform's world lookup.
     *
     * @param uuid the world's UUID
     * @return an Optional containing the world if loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<UnifiedWorld> getWorld(@NotNull UUID uuid);

    /**
     * Executes a task on the main/primary thread.
     *
     * @param task the task to execute
     * @since 1.0.0
     */
    void runOnMainThread(@NotNull Runnable task);

    /**
     * Executes a task asynchronously.
     *
     * @param task the task to execute
     * @since 1.0.0
     */
    void runAsync(@NotNull Runnable task);

    /**
     * Checks if the current thread is the main thread.
     *
     * @return true if on the main thread
     * @since 1.0.0
     */
    boolean isMainThread();
}
