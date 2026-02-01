/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Paper/Spigot implementation of {@link UnifiedBlock}.
 *
 * <p>This class wraps a Bukkit {@link Block} and provides a unified API for
 * block operations including type checking, modification, and state access.
 *
 * <h2>Block Data</h2>
 * <p>Block properties are accessed through Bukkit's BlockData system. Property
 * names should match Minecraft's block state property names (e.g., "facing",
 * "waterlogged", "half").
 *
 * <h2>Thread Safety</h2>
 * <p>Block modifications should be performed on the main server thread or the
 * appropriate region thread for Folia. Read operations are generally safe from
 * any thread but may return stale data.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedBlock
 * @see Block
 */
public final class PaperUnifiedBlock implements UnifiedBlock {

    private final Block block;
    private final PaperPlatformProvider provider;

    /**
     * Creates a new PaperUnifiedBlock wrapping the given Bukkit block.
     *
     * @param block    the Bukkit block to wrap
     * @param provider the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperUnifiedBlock(@NotNull Block block, @NotNull PaperPlatformProvider provider) {
        this.block = Objects.requireNonNull(block, "block");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * Returns the underlying Bukkit block.
     *
     * @return the Bukkit block
     */
    @NotNull
    public Block getBukkitBlock() {
        return block;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getLocation() {
        return PaperConversions.toUnifiedLocation(block.getLocation(), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedWorld getWorld() {
        return provider.getOrCreateWorld(block.getWorld());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getX() {
        return block.getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getY() {
        return block.getY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getZ() {
        return block.getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getType() {
        return block.getType().getKey().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(@NotNull String type) {
        setType(type, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setType(@NotNull String type, boolean physics) {
        Material material = parseMaterial(type);
        if (material != null) {
            block.setType(material, physics);
        }
    }

    /**
     * Parses a namespaced material ID to a Bukkit Material.
     *
     * @param type the material type ID
     * @return the Material, or null if not found
     */
    @Nullable
    private Material parseMaterial(@NotNull String type) {
        // Remove minecraft: prefix if present
        String name = type.replace("minecraft:", "").toUpperCase();

        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Try using NamespacedKey
            try {
                NamespacedKey key = NamespacedKey.fromString(type);
                if (key != null) {
                    return Registry.MATERIAL.get(key);
                }
            } catch (Exception ex) {
                // Failed to parse
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return block.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLiquid() {
        return block.isLiquid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSolid() {
        return block.getType().isSolid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTransparent() {
        return !block.getType().isOccluding();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPassable() {
        return block.isPassable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBurnable() {
        return block.getType().isBurnable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReplaceable() {
        try {
            return block.isReplaceable();
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            Material type = block.getType();
            return type.isAir() || type == Material.WATER || type == Material.LAVA ||
                   type.name().contains("GRASS") || type.name().contains("FERN") ||
                   type.name().contains("FLOWER") || type.name().contains("SNOW");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightLevel() {
        return block.getLightLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightFromSky() {
        return block.getLightFromSky();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLightFromBlocks() {
        return block.getLightFromBlocks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getRelative(@NotNull BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getRelative(int dx, int dy, int dz) {
        return new PaperUnifiedBlock(block.getRelative(dx, dy, dz), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getProperty(@NotNull String property) {
        BlockData blockData = block.getBlockData();
        String dataString = blockData.getAsString();

        // Parse property from block data string: minecraft:block[property=value]
        int start = dataString.indexOf('[');
        int end = dataString.indexOf(']');
        if (start >= 0 && end > start) {
            String properties = dataString.substring(start + 1, end);
            for (String prop : properties.split(",")) {
                String[] parts = prop.split("=");
                if (parts.length == 2 && parts[0].trim().equals(property)) {
                    return Optional.of(parts[1].trim());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setProperty(@NotNull String property, @NotNull String value) {
        try {
            BlockData currentData = block.getBlockData();
            String dataString = currentData.getAsString();

            // Check if property exists
            if (!getProperty(property).isPresent() && !dataString.contains(property)) {
                return false;
            }

            // Modify the block data string
            String baseName = dataString.contains("[")
                    ? dataString.substring(0, dataString.indexOf('['))
                    : dataString;

            // Build new data string
            String newDataString;
            if (dataString.contains("[")) {
                // Replace existing property or add new one
                String properties = dataString.substring(dataString.indexOf('[') + 1, dataString.indexOf(']'));
                StringBuilder newProperties = new StringBuilder();
                boolean found = false;

                for (String prop : properties.split(",")) {
                    if (!newProperties.isEmpty()) {
                        newProperties.append(",");
                    }
                    String[] parts = prop.split("=");
                    if (parts.length == 2 && parts[0].trim().equals(property)) {
                        newProperties.append(property).append("=").append(value);
                        found = true;
                    } else {
                        newProperties.append(prop);
                    }
                }

                if (!found) {
                    if (!newProperties.isEmpty()) {
                        newProperties.append(",");
                    }
                    newProperties.append(property).append("=").append(value);
                }

                newDataString = baseName + "[" + newProperties + "]";
            } else {
                newDataString = baseName + "[" + property + "=" + value + "]";
            }

            BlockData newData = block.getBlockData().getMaterial().createBlockData(newDataString);
            block.setBlockData(newData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<String> getPropertyNames() {
        BlockData blockData = block.getBlockData();
        String dataString = blockData.getAsString();

        int start = dataString.indexOf('[');
        int end = dataString.indexOf(']');
        if (start >= 0 && end > start) {
            String properties = dataString.substring(start + 1, end);
            return java.util.Arrays.stream(properties.split(","))
                    .map(prop -> prop.split("=")[0].trim())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasProperty(@NotNull String property) {
        return getProperty(property).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedChunk getChunk() {
        return new PaperUnifiedChunk(block.getChunk(), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean breakNaturally() {
        return block.breakNaturally();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean breakNaturally(@Nullable UnifiedItemStack tool) {
        if (tool == null) {
            return breakNaturally();
        }
        ItemStack bukkitItem = tool.getHandle();
        return block.breakNaturally(bukkitItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops() {
        return block.getDrops().stream()
                .map(PaperUnifiedItemStack::new)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedItemStack> getDrops(@NotNull UnifiedItemStack tool) {
        ItemStack bukkitItem = tool.getHandle();
        return block.getDrops(bukkitItem).stream()
                .map(PaperUnifiedItemStack::new)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowered() {
        return block.isBlockPowered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIndirectlyPowered() {
        return block.isBlockIndirectlyPowered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBlockPower() {
        return block.getBlockPower();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) block;
    }

    /**
     * Checks equality based on block location.
     *
     * @param o the object to compare
     * @return true if the other object represents the same block
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperUnifiedBlock that)) return false;
        return block.getX() == that.block.getX() &&
               block.getY() == that.block.getY() &&
               block.getZ() == that.block.getZ() &&
               block.getWorld().equals(that.block.getWorld());
    }

    /**
     * Returns a hash code based on block location.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * Returns a string representation of this block.
     *
     * @return a string containing the block's type and location
     */
    @Override
    public String toString() {
        return "PaperUnifiedBlock{" +
                "type=" + block.getType() +
                ", x=" + block.getX() +
                ", y=" + block.getY() +
                ", z=" + block.getZ() +
                ", world=" + block.getWorld().getName() +
                '}';
    }
}
