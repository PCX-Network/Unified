/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import sh.pcx.unified.server.UnifiedServer;
import sh.pcx.unified.service.ServiceRegistry;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for converting between Paper/Bukkit types and Unified API types.
 *
 * <p>This class provides static methods for common type conversions used throughout
 * the Paper platform implementation. It handles differences between Paper's native
 * Adventure support and Spigot's legacy string-based APIs.
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class are thread-safe and stateless.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class PaperConversions {

    private static volatile PaperPlatformProvider providerInstance;
    private static volatile PaperUnifiedServer serverInstance;
    private static volatile ServiceRegistry serviceRegistryInstance;

    /**
     * Private constructor to prevent instantiation.
     */
    private PaperConversions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sets the platform provider instance for conversions.
     *
     * <p>This should be called during API initialization.
     *
     * @param provider the platform provider
     */
    public static void setProvider(@NotNull PaperPlatformProvider provider) {
        providerInstance = provider;
    }

    /**
     * Sets the unified server instance.
     *
     * @param server the server instance
     */
    public static void setServer(@NotNull PaperUnifiedServer server) {
        serverInstance = server;
    }

    /**
     * Sets the service registry instance.
     *
     * @param registry the service registry
     */
    public static void setServiceRegistry(@NotNull ServiceRegistry registry) {
        serviceRegistryInstance = registry;
    }

    /**
     * Gets the unified server instance.
     *
     * @return the server instance
     */
    @NotNull
    public static UnifiedServer getUnifiedServer() {
        if (serverInstance == null) {
            throw new IllegalStateException("Server not initialized. Ensure PaperPlatformProvider is initialized.");
        }
        return serverInstance;
    }

    /**
     * Gets the service registry instance.
     *
     * @return the service registry
     */
    @NotNull
    public static ServiceRegistry getServiceRegistry() {
        if (serviceRegistryInstance == null) {
            throw new IllegalStateException("ServiceRegistry not initialized.");
        }
        return serviceRegistryInstance;
    }

    /**
     * Converts a Bukkit Location to a UnifiedLocation.
     *
     * @param location the Bukkit location
     * @param provider the platform provider for world wrapping
     * @return the unified location
     */
    @NotNull
    public static UnifiedLocation toUnifiedLocation(@NotNull Location location,
                                                     @NotNull PaperPlatformProvider provider) {
        UnifiedWorld world = null;
        World bukkitWorld = location.getWorld();
        if (bukkitWorld != null) {
            world = provider.getOrCreateWorld(bukkitWorld);
        }

        return new UnifiedLocation(
                world,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    /**
     * Converts a UnifiedLocation to a Bukkit Location.
     *
     * @param location the unified location
     * @return the Bukkit location
     * @throws IllegalStateException if the world is not available
     */
    @NotNull
    public static Location toBukkitLocation(@NotNull UnifiedLocation location) {
        World world = null;
        if (location.world() != null) {
            world = location.world().getHandle();
        }

        return new Location(
                world,
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch()
        );
    }

    /**
     * Converts a UnifiedLocation to a Bukkit Location, using a fallback world if needed.
     *
     * @param location      the unified location
     * @param fallbackWorld the fallback world if location's world is null
     * @return the Bukkit location
     */
    @NotNull
    public static Location toBukkitLocation(@NotNull UnifiedLocation location,
                                            @Nullable World fallbackWorld) {
        World world = null;
        if (location.world() != null) {
            world = location.world().getHandle();
        } else {
            world = fallbackWorld;
        }

        return new Location(
                world,
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch()
        );
    }

    /**
     * Converts an Adventure Component to a legacy string.
     *
     * <p>This is used for Spigot compatibility where Adventure is not natively supported.
     *
     * @param component the Adventure component
     * @return the legacy string representation
     */
    @NotNull
    public static String toLegacy(@NotNull Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Converts a legacy string to an Adventure Component.
     *
     * @param legacy the legacy string with section color codes
     * @return the Adventure component
     */
    @NotNull
    public static Component fromLegacy(@NotNull String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    /**
     * Converts a legacy ampersand-formatted string to an Adventure Component.
     *
     * @param legacy the legacy string with ampersand color codes
     * @return the Adventure component
     */
    @NotNull
    public static Component fromLegacyAmpersand(@NotNull String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }

    /**
     * Checks if the server supports native Adventure.
     *
     * @return true if Paper's native Adventure support is available
     */
    public static boolean hasNativeAdventure() {
        try {
            Class.forName("io.papermc.paper.adventure.PaperAdventure");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the server is running Folia.
     *
     * @return true if Folia is detected
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the server is running Paper.
     *
     * @return true if Paper is detected
     */
    public static boolean isPaper() {
        try {
            Class.forName("io.papermc.paper.configuration.Configuration");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    /**
     * Gets the current platform provider.
     *
     * @return the platform provider, or null if not initialized
     */
    @Nullable
    public static PaperPlatformProvider getProvider() {
        return providerInstance;
    }

    /**
     * Converts a world difficulty to the unified enum.
     *
     * @param difficulty the Bukkit difficulty
     * @return the unified difficulty
     */
    @NotNull
    public static UnifiedWorld.Difficulty toUnifiedDifficulty(@NotNull org.bukkit.Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> UnifiedWorld.Difficulty.PEACEFUL;
            case EASY -> UnifiedWorld.Difficulty.EASY;
            case NORMAL -> UnifiedWorld.Difficulty.NORMAL;
            case HARD -> UnifiedWorld.Difficulty.HARD;
        };
    }

    /**
     * Converts a unified difficulty to Bukkit enum.
     *
     * @param difficulty the unified difficulty
     * @return the Bukkit difficulty
     */
    @NotNull
    public static org.bukkit.Difficulty toBukkitDifficulty(@NotNull UnifiedWorld.Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> org.bukkit.Difficulty.PEACEFUL;
            case EASY -> org.bukkit.Difficulty.EASY;
            case NORMAL -> org.bukkit.Difficulty.NORMAL;
            case HARD -> org.bukkit.Difficulty.HARD;
        };
    }

    /**
     * Converts a world environment to the unified enum.
     *
     * @param environment the Bukkit environment
     * @return the unified environment
     */
    @NotNull
    public static UnifiedWorld.Environment toUnifiedEnvironment(@NotNull World.Environment environment) {
        return switch (environment) {
            case NORMAL -> UnifiedWorld.Environment.NORMAL;
            case NETHER -> UnifiedWorld.Environment.NETHER;
            case THE_END -> UnifiedWorld.Environment.THE_END;
            case CUSTOM -> UnifiedWorld.Environment.CUSTOM;
        };
    }

    /**
     * Converts a unified environment to Bukkit enum.
     *
     * @param environment the unified environment
     * @return the Bukkit environment
     */
    @NotNull
    public static World.Environment toBukkitEnvironment(@NotNull UnifiedWorld.Environment environment) {
        return switch (environment) {
            case NORMAL -> World.Environment.NORMAL;
            case NETHER -> World.Environment.NETHER;
            case THE_END -> World.Environment.THE_END;
            case CUSTOM -> World.Environment.CUSTOM;
        };
    }
}
