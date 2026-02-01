/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Optional;

/**
 * Utility class for type conversions between Sponge and Unified API types.
 *
 * <p>This class provides static methods for converting between platform-specific
 * Sponge types and their unified API equivalents. All methods are null-safe and
 * thread-safe.
 *
 * <h2>Conversion Categories</h2>
 * <ul>
 *   <li><strong>Locations:</strong> {@link ServerLocation} to/from {@link UnifiedLocation}</li>
 *   <li><strong>Blocks:</strong> {@link BlockState} to/from unified block types</li>
 *   <li><strong>Game Modes:</strong> Sponge GameMode to/from unified GameMode</li>
 *   <li><strong>Difficulty:</strong> Sponge Difficulty to/from unified Difficulty</li>
 *   <li><strong>Vectors:</strong> Sponge Vector3d/Vector3i conversions</li>
 * </ul>
 *
 * <h2>Resource Keys</h2>
 * <p>Sponge uses {@link ResourceKey} for identifying game objects. This class
 * provides utilities for parsing and formatting resource keys.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class SpongeConversions {

    /**
     * Private constructor to prevent instantiation.
     */
    private SpongeConversions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Location Conversions ====================

    /**
     * Converts a Sponge ServerLocation to a UnifiedLocation.
     *
     * @param location the Sponge location
     * @param provider the platform provider for world wrapping
     * @return the unified location
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation fromSponge(@NotNull ServerLocation location,
                                             @NotNull SpongePlatformProvider provider) {
        return new UnifiedLocation(
                provider.getOrCreateWorld(location.world()),
                location.x(),
                location.y(),
                location.z()
        );
    }

    /**
     * Converts a Sponge ServerLocation with rotation to a UnifiedLocation.
     *
     * @param location the Sponge location
     * @param rotation the rotation vector (yaw in Y, pitch in X)
     * @param provider the platform provider for world wrapping
     * @return the unified location with rotation
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedLocation fromSponge(@NotNull ServerLocation location,
                                             @NotNull Vector3d rotation,
                                             @NotNull SpongePlatformProvider provider) {
        return new UnifiedLocation(
                provider.getOrCreateWorld(location.world()),
                location.x(),
                location.y(),
                location.z(),
                (float) rotation.y(),  // yaw
                (float) rotation.x()   // pitch
        );
    }

    /**
     * Converts a UnifiedLocation to a Sponge ServerLocation.
     *
     * @param location the unified location
     * @return the Sponge ServerLocation
     * @throws IllegalArgumentException if the world is not a Sponge world
     * @since 1.0.0
     */
    @NotNull
    public static ServerLocation toSponge(@NotNull UnifiedLocation location) {
        if (location.world() == null) {
            throw new IllegalArgumentException("Location must have a world");
        }

        ServerWorld world = location.world().getHandle();
        return ServerLocation.of(world, location.x(), location.y(), location.z());
    }

    /**
     * Extracts a rotation vector from a UnifiedLocation.
     *
     * @param location the unified location
     * @return the rotation as Vector3d (pitch in X, yaw in Y, 0 in Z)
     * @since 1.0.0
     */
    @NotNull
    public static Vector3d toRotation(@NotNull UnifiedLocation location) {
        return Vector3d.from(location.pitch(), location.yaw(), 0);
    }

    // ==================== Game Mode Conversions ====================

    /**
     * Converts a Sponge GameMode to a unified GameMode.
     *
     * @param spongeMode the Sponge game mode
     * @return the unified game mode
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedPlayer.GameMode fromSponge(@NotNull GameMode spongeMode) {
        if (spongeMode.equals(GameModes.SURVIVAL.get())) {
            return UnifiedPlayer.GameMode.SURVIVAL;
        } else if (spongeMode.equals(GameModes.CREATIVE.get())) {
            return UnifiedPlayer.GameMode.CREATIVE;
        } else if (spongeMode.equals(GameModes.ADVENTURE.get())) {
            return UnifiedPlayer.GameMode.ADVENTURE;
        } else if (spongeMode.equals(GameModes.SPECTATOR.get())) {
            return UnifiedPlayer.GameMode.SPECTATOR;
        }
        return UnifiedPlayer.GameMode.SURVIVAL;
    }

    /**
     * Converts a unified GameMode to a Sponge GameMode.
     *
     * @param unifiedMode the unified game mode
     * @return the Sponge game mode
     * @since 1.0.0
     */
    @NotNull
    public static GameMode toSponge(@NotNull UnifiedPlayer.GameMode unifiedMode) {
        return switch (unifiedMode) {
            case SURVIVAL -> GameModes.SURVIVAL.get();
            case CREATIVE -> GameModes.CREATIVE.get();
            case ADVENTURE -> GameModes.ADVENTURE.get();
            case SPECTATOR -> GameModes.SPECTATOR.get();
        };
    }

    // ==================== Difficulty Conversions ====================

    /**
     * Converts a Sponge Difficulty to a unified Difficulty.
     *
     * @param spongeDifficulty the Sponge difficulty
     * @return the unified difficulty
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedWorld.Difficulty fromSponge(@NotNull Difficulty spongeDifficulty) {
        if (spongeDifficulty.equals(Difficulties.PEACEFUL.get())) {
            return UnifiedWorld.Difficulty.PEACEFUL;
        } else if (spongeDifficulty.equals(Difficulties.EASY.get())) {
            return UnifiedWorld.Difficulty.EASY;
        } else if (spongeDifficulty.equals(Difficulties.NORMAL.get())) {
            return UnifiedWorld.Difficulty.NORMAL;
        } else if (spongeDifficulty.equals(Difficulties.HARD.get())) {
            return UnifiedWorld.Difficulty.HARD;
        }
        return UnifiedWorld.Difficulty.NORMAL;
    }

    /**
     * Converts a unified Difficulty to a Sponge Difficulty.
     *
     * @param unifiedDifficulty the unified difficulty
     * @return the Sponge difficulty
     * @since 1.0.0
     */
    @NotNull
    public static Difficulty toSponge(@NotNull UnifiedWorld.Difficulty unifiedDifficulty) {
        return switch (unifiedDifficulty) {
            case PEACEFUL -> Difficulties.PEACEFUL.get();
            case EASY -> Difficulties.EASY.get();
            case NORMAL -> Difficulties.NORMAL.get();
            case HARD -> Difficulties.HARD.get();
        };
    }

    // ==================== Block Face Conversions ====================

    /**
     * Converts a unified BlockFace to a Sponge direction vector.
     *
     * @param face the unified block face
     * @return the direction as Vector3i
     * @since 1.0.0
     */
    @NotNull
    public static Vector3i toSponge(@NotNull UnifiedBlock.BlockFace face) {
        return Vector3i.from(face.getModX(), face.getModY(), face.getModZ());
    }

    /**
     * Converts a direction vector to a unified BlockFace.
     *
     * @param direction the direction vector
     * @return the unified block face, or SELF if no match
     * @since 1.0.0
     */
    @NotNull
    public static UnifiedBlock.BlockFace fromSponge(@NotNull Vector3i direction) {
        for (UnifiedBlock.BlockFace face : UnifiedBlock.BlockFace.values()) {
            if (face.getModX() == direction.x() &&
                face.getModY() == direction.y() &&
                face.getModZ() == direction.z()) {
                return face;
            }
        }
        return UnifiedBlock.BlockFace.SELF;
    }

    // ==================== Resource Key Utilities ====================

    /**
     * Parses a resource key string into a Sponge ResourceKey.
     *
     * @param key the key string (e.g., "minecraft:stone")
     * @return the parsed ResourceKey
     * @since 1.0.0
     */
    @NotNull
    public static ResourceKey parseKey(@NotNull String key) {
        return ResourceKey.resolve(key);
    }

    /**
     * Formats a ResourceKey as a string.
     *
     * @param key the ResourceKey
     * @return the formatted string (e.g., "minecraft:stone")
     * @since 1.0.0
     */
    @NotNull
    public static String formatKey(@NotNull ResourceKey key) {
        return key.asString();
    }

    /**
     * Gets the namespace from a resource key string.
     *
     * @param key the key string
     * @return the namespace (e.g., "minecraft")
     * @since 1.0.0
     */
    @NotNull
    public static String getNamespace(@NotNull String key) {
        int colonIndex = key.indexOf(':');
        if (colonIndex == -1) {
            return "minecraft";
        }
        return key.substring(0, colonIndex);
    }

    /**
     * Gets the value/path from a resource key string.
     *
     * @param key the key string
     * @return the value (e.g., "stone")
     * @since 1.0.0
     */
    @NotNull
    public static String getValue(@NotNull String key) {
        int colonIndex = key.indexOf(':');
        if (colonIndex == -1) {
            return key;
        }
        return key.substring(colonIndex + 1);
    }

    // ==================== Registry Lookups ====================

    /**
     * Looks up a block type by its key.
     *
     * @param key the block type key
     * @return the BlockType, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public static Optional<BlockType> findBlockType(@NotNull String key) {
        ResourceKey resourceKey = ResourceKey.resolve(key);
        return Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.BLOCK_TYPE).findValue(resourceKey);
    }

    /**
     * Looks up an item type by its key.
     *
     * @param key the item type key
     * @return the ItemType, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    public static Optional<ItemType> findItemType(@NotNull String key) {
        ResourceKey resourceKey = ResourceKey.resolve(key);
        return Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.ITEM_TYPE).findValue(resourceKey);
    }

    // ==================== Vector Conversions ====================

    /**
     * Creates a Vector3d from coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the Vector3d
     * @since 1.0.0
     */
    @NotNull
    public static Vector3d toVector3d(double x, double y, double z) {
        return Vector3d.from(x, y, z);
    }

    /**
     * Creates a Vector3i from coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the Vector3i
     * @since 1.0.0
     */
    @NotNull
    public static Vector3i toVector3i(int x, int y, int z) {
        return Vector3i.from(x, y, z);
    }

    /**
     * Extracts coordinates from a UnifiedLocation as a Vector3d.
     *
     * @param location the location
     * @return the position as Vector3d
     * @since 1.0.0
     */
    @NotNull
    public static Vector3d toVector3d(@NotNull UnifiedLocation location) {
        return Vector3d.from(location.x(), location.y(), location.z());
    }

    /**
     * Extracts block coordinates from a UnifiedLocation as a Vector3i.
     *
     * @param location the location
     * @return the block position as Vector3i
     * @since 1.0.0
     */
    @NotNull
    public static Vector3i toVector3i(@NotNull UnifiedLocation location) {
        return Vector3i.from(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // ==================== Null-Safe Helpers ====================

    /**
     * Safely converts a nullable Sponge location to a unified location.
     *
     * @param location the Sponge location, may be null
     * @param provider the platform provider
     * @return the unified location, or null if input is null
     * @since 1.0.0
     */
    @Nullable
    public static UnifiedLocation fromSpongeNullable(@Nullable ServerLocation location,
                                                     @NotNull SpongePlatformProvider provider) {
        if (location == null) {
            return null;
        }
        return fromSponge(location, provider);
    }

    /**
     * Safely converts a nullable unified location to a Sponge location.
     *
     * @param location the unified location, may be null
     * @return the Sponge location, or null if input is null
     * @since 1.0.0
     */
    @Nullable
    public static ServerLocation toSpongeNullable(@Nullable UnifiedLocation location) {
        if (location == null || location.world() == null) {
            return null;
        }
        return toSponge(location);
    }
}
