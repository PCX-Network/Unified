/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.v1_21_R3;

import sh.pcx.unified.version.api.NMSBridge;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NMS bridge implementation for Minecraft 1.21.4-1.21.10 (v1_21_R3).
 *
 * <p>This implementation provides access to NMS internals for versions
 * 1.21.4 through 1.21.10, which brought the Pale Garden update.
 *
 * <h2>Version Notes</h2>
 * <ul>
 *   <li>Pale Garden biome with Creaking mob</li>
 *   <li>Pale Oak wood type</li>
 *   <li>Resin blocks and items</li>
 *   <li>Hardcore banner pattern</li>
 *   <li>CraftBukkit package: org.bukkit.craftbukkit.v1_21_R3</li>
 *   <li>Still uses camelCase gamerules</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class NMSBridgeImpl implements NMSBridge {

    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE / 2);

    private static final Set<String> SUPPORTED_FEATURES = Set.of(
            "PACKETS",
            "FAKE_ENTITIES",
            "ENTITY_DATA",
            "CHUNK_ACCESS",
            "CONNECTION_ACCESS",
            "DATA_COMPONENTS",
            "NAMESPACED_ATTRIBUTE_MODIFIERS",
            "DATA_DRIVEN_ENCHANTMENTS",
            "TRIAL_CHAMBERS",
            "BUNDLES_IMPROVED",
            "PALE_GARDEN",
            "CREAKING_MOB",
            "PALE_OAK",
            "RESIN_BLOCKS",
            "HARDCORE_BANNER"
    );

    /**
     * Creates a new NMS bridge for v1_21_R3.
     */
    public NMSBridgeImpl() {
        // TODO: Initialize NMS class references for 1.21.4-1.21.10
    }

    @Override
    @NotNull
    public Object getHandle(@NotNull Entity entity) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    @NotNull
    public Object getHandle(@NotNull World world) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    @NotNull
    public Object getHandle(@NotNull ItemStack item) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public void sendPacket(@NotNull Player player, @NotNull Object packet) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public void sendPacket(@NotNull Collection<? extends Player> players, @NotNull Object packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    @Override
    public void sendPacketToWorld(@NotNull World world, @NotNull Object packet) {
        sendPacket(world.getPlayers(), packet);
    }

    @Override
    public void sendPacketNearby(@NotNull Location location, double radius, @NotNull Object packet) {
        World world = location.getWorld();
        if (world == null) return;
        double radiusSquared = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                sendPacket(player, packet);
            }
        }
    }

    @Override
    public int spawnFakeEntity(@NotNull Player player, @NotNull EntityType type,
                               @NotNull Location location) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public int spawnFakeEntity(@NotNull Collection<? extends Player> players,
                               @NotNull EntityType type, @NotNull Location location) {
        int entityId = nextEntityId();
        // TODO: Implementation
        return entityId;
    }

    @Override
    public void destroyFakeEntity(@NotNull Player player, int entityId) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public void destroyFakeEntity(@NotNull Collection<? extends Player> players, int entityId) {
        for (Player player : players) {
            destroyFakeEntity(player, entityId);
        }
    }

    @Override
    public void destroyFakeEntities(@NotNull Player player, int... entityIds) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public <T> void setEntityData(int entityId, @NotNull Player player,
                                  @NotNull EntityData<T> data, @NotNull T value) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public <T> void setEntityData(@NotNull Entity entity, @NotNull Player player,
                                  @NotNull EntityData<T> data, @NotNull T value) {
        setEntityData(entity.getEntityId(), player, data, value);
    }

    @Override
    public void teleportFakeEntity(int entityId, @NotNull Player player, @NotNull Location location) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    @Nullable
    public Object getChunk(@NotNull World world, int chunkX, int chunkZ) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public void saveChunk(@NotNull World world, int chunkX, int chunkZ) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public void refreshChunk(@NotNull Player player, int chunkX, int chunkZ) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    @NotNull
    public Object getConnection(@NotNull Player player) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    @NotNull
    public Object getChannel(@NotNull Player player) {
        // TODO: Implement for v1_21_R3
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R3");
    }

    @Override
    public int getPing(@NotNull Player player) {
        return player.getPing();
    }

    @Override
    public int nextEntityId() {
        return ENTITY_ID_COUNTER.getAndDecrement();
    }

    @Override
    @NotNull
    public Set<String> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public boolean supportsFeature(@NotNull String feature) {
        return SUPPORTED_FEATURES.contains(feature);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
