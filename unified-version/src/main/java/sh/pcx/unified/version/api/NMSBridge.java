/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import sh.pcx.unified.service.Service;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Bridge interface for version-safe NMS (net.minecraft.server) operations.
 *
 * <p>This service abstracts away version-specific NMS code, allowing plugins to
 * perform low-level server operations without worrying about version differences.
 * All NMS-related operations should go through this interface.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private NMSBridge nms;
 *
 * public void doNmsOperations(Player player) {
 *     // Get NMS entity handle
 *     Object nmsPlayer = nms.getHandle(player);
 *
 *     // Send custom packets
 *     nms.sendPacket(player, createCustomPacket());
 *
 *     // Spawn fake entity (client-side only)
 *     int entityId = nms.spawnFakeEntity(player, EntityType.ARMOR_STAND, location);
 *
 *     // Update entity data
 *     nms.setEntityData(entityId, player, EntityData.INVISIBLE, true);
 *
 *     // Destroy fake entity
 *     nms.destroyFakeEntity(player, entityId);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Most methods should only be called from the main server thread unless
 * explicitly documented as thread-safe. Packet sending may be called from
 * any thread on Paper servers.
 *
 * <h2>Version Support</h2>
 * <p>This interface provides consistent behavior across Minecraft 1.20.5 - 1.21.11+.
 * Some operations may be no-ops on certain versions if the underlying functionality
 * doesn't exist.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see NBTService
 * @see ComponentBridge
 */
public interface NMSBridge extends Service {

    // ===== Entity Handle Access =====

    /**
     * Gets the NMS entity handle for a Bukkit entity.
     *
     * <p>The returned object is the underlying NMS entity instance.
     * Cast appropriately based on your NMS knowledge.
     *
     * @param entity the Bukkit entity
     * @return the NMS entity handle
     * @since 1.0.0
     */
    @NotNull
    Object getHandle(@NotNull Entity entity);

    /**
     * Gets the NMS world handle for a Bukkit world.
     *
     * @param world the Bukkit world
     * @return the NMS world handle (ServerLevel)
     * @since 1.0.0
     */
    @NotNull
    Object getHandle(@NotNull World world);

    /**
     * Gets the NMS item stack for a Bukkit item.
     *
     * @param item the Bukkit item stack
     * @return the NMS item stack handle
     * @since 1.0.0
     */
    @NotNull
    Object getHandle(@NotNull ItemStack item);

    // ===== Packet Operations =====

    /**
     * Sends a packet to a player.
     *
     * <p>The packet object must be a valid NMS packet instance.
     * This method is thread-safe on Paper servers.
     *
     * @param player the player to send the packet to
     * @param packet the NMS packet object
     * @since 1.0.0
     */
    void sendPacket(@NotNull Player player, @NotNull Object packet);

    /**
     * Sends a packet to multiple players.
     *
     * @param players the players to send the packet to
     * @param packet  the NMS packet object
     * @since 1.0.0
     */
    void sendPacket(@NotNull Collection<? extends Player> players, @NotNull Object packet);

    /**
     * Sends a packet to all players in a world.
     *
     * @param world  the world
     * @param packet the NMS packet object
     * @since 1.0.0
     */
    void sendPacketToWorld(@NotNull World world, @NotNull Object packet);

    /**
     * Sends a packet to all players near a location.
     *
     * @param location the center location
     * @param radius   the radius in blocks
     * @param packet   the NMS packet object
     * @since 1.0.0
     */
    void sendPacketNearby(@NotNull Location location, double radius, @NotNull Object packet);

    // ===== Fake Entity Operations =====

    /**
     * Spawns a client-side entity visible only to the specified player.
     *
     * <p>The entity only exists on the client and has no server-side presence.
     * Use this for holograms, markers, or other visual elements.
     *
     * @param player   the player to show the entity to
     * @param type     the entity type
     * @param location the spawn location
     * @return the fake entity ID (for later modification/removal)
     * @since 1.0.0
     */
    int spawnFakeEntity(@NotNull Player player, @NotNull EntityType type, @NotNull Location location);

    /**
     * Spawns a client-side entity visible to multiple players.
     *
     * @param players  the players to show the entity to
     * @param type     the entity type
     * @param location the spawn location
     * @return the fake entity ID
     * @since 1.0.0
     */
    int spawnFakeEntity(@NotNull Collection<? extends Player> players, @NotNull EntityType type,
                        @NotNull Location location);

    /**
     * Destroys a fake entity for a player.
     *
     * @param player   the player
     * @param entityId the fake entity ID
     * @since 1.0.0
     */
    void destroyFakeEntity(@NotNull Player player, int entityId);

    /**
     * Destroys a fake entity for multiple players.
     *
     * @param players  the players
     * @param entityId the fake entity ID
     * @since 1.0.0
     */
    void destroyFakeEntity(@NotNull Collection<? extends Player> players, int entityId);

    /**
     * Destroys multiple fake entities for a player.
     *
     * @param player    the player
     * @param entityIds the fake entity IDs
     * @since 1.0.0
     */
    void destroyFakeEntities(@NotNull Player player, int... entityIds);

    // ===== Entity Data Operations =====

    /**
     * Sets entity metadata for a fake entity.
     *
     * @param <T>      the data type
     * @param entityId the fake entity ID
     * @param player   the player to send the update to
     * @param data     the entity data key
     * @param value    the value to set
     * @since 1.0.0
     */
    <T> void setEntityData(int entityId, @NotNull Player player,
                           @NotNull EntityData<T> data, @NotNull T value);

    /**
     * Sets entity metadata for a real entity.
     *
     * <p>This sends a metadata update packet to the player without
     * actually modifying the server-side entity.
     *
     * @param <T>    the data type
     * @param entity the entity
     * @param player the player to send the update to
     * @param data   the entity data key
     * @param value  the value to set
     * @since 1.0.0
     */
    <T> void setEntityData(@NotNull Entity entity, @NotNull Player player,
                           @NotNull EntityData<T> data, @NotNull T value);

    /**
     * Updates the position of a fake entity.
     *
     * @param entityId the fake entity ID
     * @param player   the player
     * @param location the new location
     * @since 1.0.0
     */
    void teleportFakeEntity(int entityId, @NotNull Player player, @NotNull Location location);

    // ===== Chunk Operations =====

    /**
     * Gets the NMS chunk at the specified coordinates.
     *
     * @param world  the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the NMS chunk handle, or null if not loaded
     * @since 1.0.0
     */
    @Nullable
    Object getChunk(@NotNull World world, int chunkX, int chunkZ);

    /**
     * Forces a chunk to be saved immediately.
     *
     * @param world  the world
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @since 1.0.0
     */
    void saveChunk(@NotNull World world, int chunkX, int chunkZ);

    /**
     * Sends a chunk update to a player.
     *
     * @param player the player
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @since 1.0.0
     */
    void refreshChunk(@NotNull Player player, int chunkX, int chunkZ);

    // ===== Connection Operations =====

    /**
     * Gets the player's network connection object.
     *
     * @param player the player
     * @return the NMS player connection
     * @since 1.0.0
     */
    @NotNull
    Object getConnection(@NotNull Player player);

    /**
     * Gets the player's network channel.
     *
     * @param player the player
     * @return the Netty channel
     * @since 1.0.0
     */
    @NotNull
    Object getChannel(@NotNull Player player);

    /**
     * Gets the player's ping in milliseconds.
     *
     * @param player the player
     * @return the ping in milliseconds
     * @since 1.0.0
     */
    int getPing(@NotNull Player player);

    // ===== Utility Operations =====

    /**
     * Gets the next available entity ID for fake entities.
     *
     * <p>Entity IDs are unique across the server and should not conflict
     * with real entities.
     *
     * @return a new unique entity ID
     * @since 1.0.0
     */
    int nextEntityId();

    /**
     * Gets the set of NMS features supported by this implementation.
     *
     * @return immutable set of supported NMS features
     * @since 1.0.0
     */
    @NotNull
    Set<String> getSupportedFeatures();

    /**
     * Checks if a specific NMS feature is supported.
     *
     * @param feature the feature name
     * @return true if supported
     * @since 1.0.0
     */
    boolean supportsFeature(@NotNull String feature);

    @Override
    default String getServiceName() {
        return "NMSBridge";
    }

    /**
     * Entity data keys for metadata operations.
     *
     * @param <T> the data type
     * @since 1.0.0
     */
    interface EntityData<T> {
        /** Entity flags (invisible, glowing, etc.). */
        EntityData<Byte> FLAGS = EntityData.of("flags", Byte.class);

        /** Custom name component. */
        EntityData<Object> CUSTOM_NAME = EntityData.of("custom_name", Object.class);

        /** Custom name visibility. */
        EntityData<Boolean> CUSTOM_NAME_VISIBLE = EntityData.of("custom_name_visible", Boolean.class);

        /** Silent flag. */
        EntityData<Boolean> SILENT = EntityData.of("silent", Boolean.class);

        /** No gravity flag. */
        EntityData<Boolean> NO_GRAVITY = EntityData.of("no_gravity", Boolean.class);

        /** Armor stand flags. */
        EntityData<Byte> ARMOR_STAND_FLAGS = EntityData.of("armor_stand_flags", Byte.class);

        /** Item display item. */
        EntityData<Object> DISPLAY_ITEM = EntityData.of("display_item", Object.class);

        /** Text display text. */
        EntityData<Object> DISPLAY_TEXT = EntityData.of("display_text", Object.class);

        /**
         * Creates an entity data key.
         *
         * @param <T>  the data type
         * @param name the key name
         * @param type the value type
         * @return the entity data key
         */
        static <T> EntityData<T> of(String name, Class<T> type) {
            return new EntityData<>() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Class<T> getType() {
                    return type;
                }
            };
        }

        /**
         * Returns the data key name.
         *
         * @return the key name
         */
        String getName();

        /**
         * Returns the value type.
         *
         * @return the value class
         */
        Class<T> getType();
    }
}
