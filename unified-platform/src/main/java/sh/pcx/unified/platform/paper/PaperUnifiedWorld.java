/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.paper;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Paper/Spigot implementation of {@link UnifiedWorld}.
 *
 * <p>This class wraps a Bukkit {@link World} and provides a unified API for
 * world operations including block access, chunk management, weather, and time.
 *
 * <h2>Async Chunk Loading</h2>
 * <p>On Paper servers, async chunk loading methods use Paper's async chunk API.
 * On Spigot servers, a fallback synchronous implementation is used.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are generally thread-safe. Write operations (setting blocks,
 * time, weather) should be performed on the main server thread or appropriate
 * region thread for Folia.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedWorld
 * @see World
 */
public final class PaperUnifiedWorld implements UnifiedWorld {

    private final World world;
    private final PaperPlatformProvider provider;

    /**
     * Creates a new PaperUnifiedWorld wrapping the given Bukkit world.
     *
     * @param world    the Bukkit world to wrap
     * @param provider the platform provider for creating related wrappers
     * @since 1.0.0
     */
    public PaperUnifiedWorld(@NotNull World world, @NotNull PaperPlatformProvider provider) {
        this.world = Objects.requireNonNull(world, "world");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /**
     * Returns the underlying Bukkit world.
     *
     * @return the Bukkit world
     */
    @NotNull
    public World getBukkitWorld() {
        return world;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return world.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUniqueId() {
        return world.getUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getSpawnLocation() {
        return PaperConversions.toUnifiedLocation(world.getSpawnLocation(), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSpawnLocation(@NotNull UnifiedLocation location) {
        return world.setSpawnLocation(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        return world.setSpawnLocation(x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getBlockAt(@NotNull UnifiedLocation location) {
        return getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedBlock getBlockAt(int x, int y, int z) {
        return new PaperUnifiedBlock(world.getBlockAt(x, y, z), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedChunk getChunkAt(int x, int z) {
        return new PaperUnifiedChunk(world.getChunkAt(x, z), provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedChunk getChunkAt(@NotNull UnifiedLocation location) {
        return getChunkAt(location.getChunkX(), location.getChunkZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<UnifiedChunk> getChunkAtAsync(int x, int z) {
        try {
            // Paper supports async chunk loading
            return world.getChunkAtAsync(x, z)
                    .thenApply(chunk -> new PaperUnifiedChunk(chunk, provider));
        } catch (NoSuchMethodError e) {
            // Fallback for Spigot
            CompletableFuture<UnifiedChunk> future = new CompletableFuture<>();
            Chunk chunk = world.getChunkAt(x, z);
            future.complete(new PaperUnifiedChunk(chunk, provider));
            return future;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChunkLoaded(int x, int z) {
        return world.isChunkLoaded(x, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        return world.loadChunk(x, z, generate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        return world.unloadChunk(x, z, save);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedPlayer> getPlayers() {
        return world.getPlayers().stream()
                .map(provider::getOrCreatePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTime() {
        return world.getTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTime(long time) {
        world.setTime(time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFullTime() {
        return world.getFullTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFullTime(long time) {
        world.setFullTime(time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeatherDuration() {
        return world.getWeatherDuration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWeatherDuration(int duration) {
        world.setWeatherDuration(duration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStorm() {
        return world.hasStorm();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStorm(boolean storm) {
        world.setStorm(storm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isThundering() {
        return world.isThundering();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThundering(boolean thundering) {
        world.setThundering(thundering);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClearWeatherDuration(int duration) {
        world.setClearWeatherDuration(duration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Difficulty getDifficulty() {
        return switch (world.getDifficulty()) {
            case PEACEFUL -> Difficulty.PEACEFUL;
            case EASY -> Difficulty.EASY;
            case NORMAL -> Difficulty.NORMAL;
            case HARD -> Difficulty.HARD;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {
        org.bukkit.Difficulty bukkitDifficulty = switch (difficulty) {
            case PEACEFUL -> org.bukkit.Difficulty.PEACEFUL;
            case EASY -> org.bukkit.Difficulty.EASY;
            case NORMAL -> org.bukkit.Difficulty.NORMAL;
            case HARD -> org.bukkit.Difficulty.HARD;
        };
        world.setDifficulty(bukkitDifficulty);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Environment getEnvironment() {
        return switch (world.getEnvironment()) {
            case NORMAL -> Environment.NORMAL;
            case NETHER -> Environment.NETHER;
            case THE_END -> Environment.THE_END;
            case CUSTOM -> Environment.CUSTOM;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSeed() {
        return world.getSeed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPvPEnabled() {
        return world.getPVP();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPvPEnabled(boolean pvp) {
        world.setPVP(pvp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinHeight() {
        return world.getMinHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxHeight() {
        return world.getMaxHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSeaLevel() {
        return world.getSeaLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getGameRuleValue(@NotNull String rule) {
        try {
            // Try to find the game rule
            GameRule<?> gameRule = GameRule.getByName(rule);
            if (gameRule != null) {
                Object value = world.getGameRuleValue(gameRule);
                return Optional.ofNullable(value != null ? value.toString() : null);
            }
        } catch (Exception e) {
            // Game rule not found or error
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean setGameRule(@NotNull String rule, @NotNull String value) {
        try {
            GameRule<?> gameRule = GameRule.getByName(rule);
            if (gameRule == null) {
                return false;
            }

            // Determine the type and set appropriately
            if (gameRule.getType() == Boolean.class) {
                return world.setGameRule((GameRule<Boolean>) gameRule, Boolean.parseBoolean(value));
            } else if (gameRule.getType() == Integer.class) {
                return world.setGameRule((GameRule<Integer>) gameRule, Integer.parseInt(value));
            }
        } catch (Exception e) {
            // Failed to set game rule
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getGameRuleBoolean(@NotNull String rule, boolean defaultValue) {
        return getGameRuleValue(rule)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGameRuleInt(@NotNull String rule, int defaultValue) {
        return getGameRuleValue(rule)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createExplosion(@NotNull UnifiedLocation location, float power,
                                   boolean setFire, boolean breakBlocks) {
        return world.createExplosion(
                location.x(),
                location.y(),
                location.z(),
                power,
                setFire,
                breakBlocks
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void strikeLightning(@NotNull UnifiedLocation location, boolean effect) {
        Location bukkitLocation = PaperConversions.toBukkitLocation(location);
        if (effect) {
            world.strikeLightningEffect(bukkitLocation);
        } else {
            world.strikeLightning(bukkitLocation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        world.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) world;
    }

    /**
     * Checks equality based on world UUID.
     *
     * @param o the object to compare
     * @return true if the other object represents the same world
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperUnifiedWorld that)) return false;
        return world.getUID().equals(that.world.getUID());
    }

    /**
     * Returns a hash code based on the world UUID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return world.getUID().hashCode();
    }

    /**
     * Returns a string representation of this world.
     *
     * @return a string containing the world's name and environment
     */
    @Override
    public String toString() {
        return "PaperUnifiedWorld{" +
                "name='" + world.getName() + '\'' +
                ", environment=" + world.getEnvironment() +
                ", uuid=" + world.getUID() +
                '}';
    }
}
