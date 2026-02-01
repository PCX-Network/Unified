/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Region-aware block wrapper for Folia.
 *
 * <p>This class wraps a Bukkit Block object and provides region-aware
 * access to block data. Block modifications must be performed on the
 * thread that owns the region containing this block.
 *
 * <h2>Thread Safety</h2>
 * <p>Reading block state is generally safe from any thread. However,
 * modifying block state (setType, setBlockData) must be done on the
 * correct region thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedBlock
 * @see FoliaRegionScheduler
 */
public final class FoliaUnifiedBlock implements UnifiedBlock {

    private static final Logger LOGGER = Logger.getLogger(FoliaUnifiedBlock.class.getName());

    /**
     * The underlying Bukkit Block object.
     */
    private final Object bukkitBlock;

    /**
     * The world this block belongs to.
     */
    private final FoliaUnifiedWorld world;

    /**
     * The platform provider.
     */
    private final FoliaPlatformProvider provider;

    /**
     * Cached coordinates.
     */
    private final int x, y, z;

    /**
     * Constructs a new FoliaUnifiedBlock.
     *
     * @param bukkitBlock the Bukkit Block object
     * @param world the world this block belongs to
     * @param provider the platform provider
     * @since 1.0.0
     */
    public FoliaUnifiedBlock(@NotNull Object bukkitBlock,
                              @NotNull FoliaUnifiedWorld world,
                              @NotNull FoliaPlatformProvider provider) {
        this.bukkitBlock = bukkitBlock;
        this.world = world;
        this.provider = provider;

        try {
            this.x = (int) bukkitBlock.getClass().getMethod("getX").invoke(bukkitBlock);
            this.y = (int) bukkitBlock.getClass().getMethod("getY").invoke(bukkitBlock);
            this.z = (int) bukkitBlock.getClass().getMethod("getZ").invoke(bukkitBlock);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Bukkit Block object", e);
        }
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return new UnifiedLocation(world, x, y, z);
    }

    @Override
    @NotNull
    public UnifiedChunk getChunk() {
        try {
            Object chunk = bukkitBlock.getClass().getMethod("getChunk").invoke(bukkitBlock);
            return new FoliaUnifiedChunk(chunk, world, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chunk for block", e);
        }
    }

    @Override
    @NotNull
    public String getType() {
        try {
            Object material = bukkitBlock.getClass().getMethod("getType").invoke(bukkitBlock);
            Object key = material.getClass().getMethod("getKey").invoke(material);
            return key.toString();
        } catch (Exception e) {
            return "minecraft:air";
        }
    }

    @Override
    public void setType(@NotNull String type) {
        try {
            Class<?> materialClass = Class.forName("org.bukkit.Material");
            Object material = materialClass.getMethod("matchMaterial", String.class).invoke(null, type);
            if (material != null) {
                bukkitBlock.getClass().getMethod("setType", materialClass).invoke(bukkitBlock, material);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set block type to: " + type, e);
        }
    }

    @Override
    public void setType(@NotNull String type, boolean applyPhysics) {
        try {
            Class<?> materialClass = Class.forName("org.bukkit.Material");
            Object material = materialClass.getMethod("matchMaterial", String.class).invoke(null, type);
            if (material != null) {
                bukkitBlock.getClass().getMethod("setType", materialClass, boolean.class)
                        .invoke(bukkitBlock, material, applyPhysics);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set block type", e);
        }
    }

    /**
     * Returns the block data string representation.
     *
     * @return the block data as a string
     * @since 1.0.0
     */
    @NotNull
    public String getBlockData() {
        try {
            Object blockData = bukkitBlock.getClass().getMethod("getBlockData").invoke(bukkitBlock);
            return (String) blockData.getClass().getMethod("getAsString").invoke(blockData);
        } catch (Exception e) {
            return getType();
        }
    }

    /**
     * Sets the block data from a string representation.
     *
     * @param data the block data string
     * @since 1.0.0
     */
    public void setBlockData(@NotNull String data) {
        setBlockData(data, true);
    }

    /**
     * Sets the block data from a string representation with optional physics.
     *
     * @param data the block data string
     * @param applyPhysics whether to apply physics updates
     * @since 1.0.0
     */
    public void setBlockData(@NotNull String data, boolean applyPhysics) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object blockData = bukkitClass.getMethod("createBlockData", String.class).invoke(null, data);

            bukkitBlock.getClass().getMethod("setBlockData",
                    Class.forName("org.bukkit.block.data.BlockData"), boolean.class)
                    .invoke(bukkitBlock, blockData, applyPhysics);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set block data: " + data, e);
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("isEmpty").invoke(bukkitBlock);
        } catch (Exception e) {
            return getType().endsWith(":air");
        }
    }

    @Override
    public boolean isLiquid() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("isLiquid").invoke(bukkitBlock);
        } catch (Exception e) {
            String type = getType();
            return type.contains("water") || type.contains("lava");
        }
    }

    @Override
    public boolean isSolid() {
        try {
            Object material = bukkitBlock.getClass().getMethod("getType").invoke(bukkitBlock);
            return (boolean) material.getClass().getMethod("isSolid").invoke(material);
        } catch (Exception e) {
            return !isEmpty() && !isLiquid();
        }
    }

    @Override
    public boolean isPassable() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("isPassable").invoke(bukkitBlock);
        } catch (Exception e) {
            return isEmpty() || isLiquid();
        }
    }

    @Override
    public int getLightLevel() {
        try {
            return (byte) bukkitBlock.getClass().getMethod("getLightLevel").invoke(bukkitBlock);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getLightFromSky() {
        try {
            return (byte) bukkitBlock.getClass().getMethod("getLightFromSky").invoke(bukkitBlock);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getLightFromBlocks() {
        try {
            return (byte) bukkitBlock.getClass().getMethod("getLightFromBlocks").invoke(bukkitBlock);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @NotNull
    public UnifiedBlock getRelative(@NotNull BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    @NotNull
    public UnifiedBlock getRelative(int modX, int modY, int modZ) {
        try {
            Object relative = bukkitBlock.getClass().getMethod("getRelative", int.class, int.class, int.class)
                    .invoke(bukkitBlock, modX, modY, modZ);
            return new FoliaUnifiedBlock(relative, world, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get relative block", e);
        }
    }

    @Override
    public boolean isTransparent() {
        try {
            Object material = bukkitBlock.getClass().getMethod("getType").invoke(bukkitBlock);
            return (boolean) material.getClass().getMethod("isTransparent").invoke(material);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isBurnable() {
        try {
            Object material = bukkitBlock.getClass().getMethod("getType").invoke(bukkitBlock);
            return (boolean) material.getClass().getMethod("isBurnable").invoke(material);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isReplaceable() {
        try {
            // Paper/modern Bukkit API
            Object blockData = bukkitBlock.getClass().getMethod("getBlockData").invoke(bukkitBlock);
            Object material = blockData.getClass().getMethod("getMaterial").invoke(blockData);
            return (boolean) material.getClass().getMethod("isAir").invoke(material) ||
                    getType().contains("grass") || getType().contains("flower");
        } catch (Exception e) {
            return isEmpty();
        }
    }

    @Override
    @NotNull
    public Optional<String> getProperty(@NotNull String property) {
        try {
            Object blockData = bukkitBlock.getClass().getMethod("getBlockData").invoke(bukkitBlock);
            String dataString = (String) blockData.getClass().getMethod("getAsString").invoke(blockData);

            // Parse property from data string like "minecraft:oak_stairs[facing=north,half=bottom]"
            int startBracket = dataString.indexOf('[');
            if (startBracket == -1) {
                return Optional.empty();
            }

            String properties = dataString.substring(startBracket + 1, dataString.length() - 1);
            for (String prop : properties.split(",")) {
                String[] parts = prop.split("=");
                if (parts.length == 2 && parts[0].equals(property)) {
                    return Optional.of(parts[1]);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to get property: " + property, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean setProperty(@NotNull String property, @NotNull String value) {
        try {
            String currentData = getBlockData();
            String newData;

            if (currentData.contains("[")) {
                // Has properties, modify them
                if (currentData.contains(property + "=")) {
                    // Replace existing property
                    newData = currentData.replaceAll(property + "=[^,\\]]+", property + "=" + value);
                } else {
                    // Add new property
                    newData = currentData.replace("]", "," + property + "=" + value + "]");
                }
            } else {
                // No properties, add them
                newData = currentData + "[" + property + "=" + value + "]";
            }

            setBlockData(newData);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set property", e);
            return false;
        }
    }

    @Override
    @NotNull
    public Collection<String> getPropertyNames() {
        try {
            String dataString = getBlockData();
            int startBracket = dataString.indexOf('[');
            if (startBracket == -1) {
                return Collections.emptyList();
            }

            String properties = dataString.substring(startBracket + 1, dataString.length() - 1);
            java.util.List<String> names = new java.util.ArrayList<>();
            for (String prop : properties.split(",")) {
                String[] parts = prop.split("=");
                if (parts.length >= 1) {
                    names.add(parts[0]);
                }
            }
            return names;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasProperty(@NotNull String property) {
        return getProperty(property).isPresent();
    }

    @Override
    public boolean breakNaturally() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("breakNaturally").invoke(bukkitBlock);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to break block", e);
            return false;
        }
    }

    @Override
    public boolean breakNaturally(@Nullable UnifiedItemStack tool) {
        try {
            if (tool == null) {
                return breakNaturally();
            }
            Object itemStack = tool.getHandle();
            return (boolean) bukkitBlock.getClass()
                    .getMethod("breakNaturally", Class.forName("org.bukkit.inventory.ItemStack"))
                    .invoke(bukkitBlock, itemStack);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to break block with tool", e);
            return false;
        }
    }

    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops() {
        // TODO: Implement with UnifiedItemStack wrapper
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops(@NotNull UnifiedItemStack tool) {
        // TODO: Implement with UnifiedItemStack wrapper
        return Collections.emptyList();
    }

    @Override
    public boolean isPowered() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("isBlockPowered").invoke(bukkitBlock);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isIndirectlyPowered() {
        try {
            return (boolean) bukkitBlock.getClass().getMethod("isBlockIndirectlyPowered").invoke(bukkitBlock);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getBlockPower() {
        try {
            return (int) bukkitBlock.getClass().getMethod("getBlockPower").invoke(bukkitBlock);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitBlock;
    }

    /**
     * Returns the region context for this block.
     *
     * @return the region context
     * @since 1.0.0
     */
    @NotNull
    public RegionContext getRegionContext() {
        return RegionContext.ofBlock(world, x, z);
    }

    /**
     * Checks if the current thread owns this block's region.
     *
     * @return true if safe to modify this block on current thread
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread() {
        return getRegionContext().isOwnedByCurrentThread();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FoliaUnifiedBlock other)) return false;
        return x == other.x && y == other.y && z == other.z
                && world.equals(other.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return String.format("FoliaUnifiedBlock[world=%s, x=%d, y=%d, z=%d, type=%s]",
                world.getName(), x, y, z, getType());
    }
}
