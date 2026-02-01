/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.folia;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Region-aware world wrapper for Folia.
 *
 * <p>This class wraps a Bukkit World object and provides thread-safe access
 * in Folia's multi-threaded environment. Operations that modify world state
 * should be scheduled on the appropriate region thread.
 *
 * <h2>Chunk Operations</h2>
 * <p>Chunk loading and unloading in Folia must respect region ownership.
 * This wrapper provides async methods that handle region scheduling.
 *
 * <h2>Block Access</h2>
 * <p>Block modifications must be performed on the region thread that owns
 * the chunk containing the block. Use {@link FoliaRegionScheduler} for
 * safe block operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedWorld
 * @see FoliaUnifiedChunk
 */
public final class FoliaUnifiedWorld implements UnifiedWorld {

    private static final Logger LOGGER = Logger.getLogger(FoliaUnifiedWorld.class.getName());

    /**
     * The underlying Bukkit World object.
     */
    private final Object bukkitWorld;

    /**
     * The platform provider for creating other wrappers.
     */
    private final FoliaPlatformProvider provider;

    /**
     * Cached world UUID.
     */
    private final UUID uuid;

    /**
     * Cached world name.
     */
    private final String name;

    /**
     * Constructs a new FoliaUnifiedWorld.
     *
     * @param bukkitWorld the Bukkit World object
     * @param provider the platform provider
     * @since 1.0.0
     */
    public FoliaUnifiedWorld(@NotNull Object bukkitWorld, @NotNull FoliaPlatformProvider provider) {
        this.bukkitWorld = bukkitWorld;
        this.provider = provider;

        try {
            this.uuid = (UUID) bukkitWorld.getClass().getMethod("getUID").invoke(bukkitWorld);
            this.name = (String) bukkitWorld.getClass().getMethod("getName").invoke(bukkitWorld);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Bukkit World object", e);
        }
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    @NotNull
    public UnifiedLocation getSpawnLocation() {
        try {
            Object location = bukkitWorld.getClass().getMethod("getSpawnLocation").invoke(bukkitWorld);
            return convertLocation(location);
        } catch (Exception e) {
            return new UnifiedLocation(this, 0, 64, 0);
        }
    }

    @Override
    public boolean setSpawnLocation(@NotNull UnifiedLocation location) {
        return setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        try {
            Method setSpawnMethod = bukkitWorld.getClass().getMethod("setSpawnLocation", int.class, int.class, int.class);
            return (boolean) setSpawnMethod.invoke(bukkitWorld, x, y, z);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set spawn location", e);
            return false;
        }
    }

    @Override
    @NotNull
    public UnifiedBlock getBlockAt(@NotNull UnifiedLocation location) {
        return getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    @NotNull
    public UnifiedBlock getBlockAt(int x, int y, int z) {
        try {
            Object block = bukkitWorld.getClass().getMethod("getBlockAt", int.class, int.class, int.class)
                    .invoke(bukkitWorld, x, y, z);
            return new FoliaUnifiedBlock(block, this, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get block at " + x + ", " + y + ", " + z, e);
        }
    }

    @Override
    @NotNull
    public UnifiedChunk getChunkAt(int x, int z) {
        try {
            Object chunk = bukkitWorld.getClass().getMethod("getChunkAt", int.class, int.class)
                    .invoke(bukkitWorld, x, z);
            return new FoliaUnifiedChunk(chunk, this, provider);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chunk at " + x + ", " + z, e);
        }
    }

    @Override
    @NotNull
    public UnifiedChunk getChunkAt(@NotNull UnifiedLocation location) {
        return getChunkAt(location.getChunkX(), location.getChunkZ());
    }

    @Override
    @NotNull
    public CompletableFuture<UnifiedChunk> getChunkAtAsync(int x, int z) {
        try {
            // Use Paper/Folia's async chunk loading
            Method getChunkAtAsyncMethod = bukkitWorld.getClass().getMethod("getChunkAtAsync", int.class, int.class);
            CompletableFuture<Object> future = (CompletableFuture<Object>) getChunkAtAsyncMethod.invoke(bukkitWorld, x, z);

            return future.thenApply(chunk -> new FoliaUnifiedChunk(chunk, this, provider));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("isChunkLoaded", int.class, int.class)
                    .invoke(bukkitWorld, x, z);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("loadChunk", int.class, int.class, boolean.class)
                    .invoke(bukkitWorld, x, z, generate);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load chunk", e);
            return false;
        }
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("unloadChunk", int.class, int.class, boolean.class)
                    .invoke(bukkitWorld, x, z, save);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to unload chunk", e);
            return false;
        }
    }

    @Override
    @NotNull
    public Collection<UnifiedPlayer> getPlayers() {
        try {
            Collection<?> players = (Collection<?>) bukkitWorld.getClass().getMethod("getPlayers").invoke(bukkitWorld);
            Collection<UnifiedPlayer> result = new ArrayList<>();
            for (Object player : players) {
                result.add(provider.wrapPlayer(player));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public long getTime() {
        try {
            return (long) bukkitWorld.getClass().getMethod("getTime").invoke(bukkitWorld);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setTime(long time) {
        try {
            bukkitWorld.getClass().getMethod("setTime", long.class).invoke(bukkitWorld, time);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set time", e);
        }
    }

    @Override
    public long getFullTime() {
        try {
            return (long) bukkitWorld.getClass().getMethod("getFullTime").invoke(bukkitWorld);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setFullTime(long time) {
        try {
            bukkitWorld.getClass().getMethod("setFullTime", long.class).invoke(bukkitWorld, time);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set full time", e);
        }
    }

    @Override
    public int getWeatherDuration() {
        try {
            return (int) bukkitWorld.getClass().getMethod("getWeatherDuration").invoke(bukkitWorld);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setWeatherDuration(int duration) {
        try {
            bukkitWorld.getClass().getMethod("setWeatherDuration", int.class).invoke(bukkitWorld, duration);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set weather duration", e);
        }
    }

    @Override
    public boolean hasStorm() {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("hasStorm").invoke(bukkitWorld);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setStorm(boolean storm) {
        try {
            bukkitWorld.getClass().getMethod("setStorm", boolean.class).invoke(bukkitWorld, storm);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set storm", e);
        }
    }

    @Override
    public boolean isThundering() {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("isThundering").invoke(bukkitWorld);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setThundering(boolean thundering) {
        try {
            bukkitWorld.getClass().getMethod("setThundering", boolean.class).invoke(bukkitWorld, thundering);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set thundering", e);
        }
    }

    @Override
    public void setClearWeatherDuration(int duration) {
        try {
            bukkitWorld.getClass().getMethod("setClearWeatherDuration", int.class).invoke(bukkitWorld, duration);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set clear weather duration", e);
        }
    }

    @Override
    @NotNull
    public Difficulty getDifficulty() {
        try {
            Object difficulty = bukkitWorld.getClass().getMethod("getDifficulty").invoke(bukkitWorld);
            String name = difficulty.toString();
            return switch (name) {
                case "PEACEFUL" -> Difficulty.PEACEFUL;
                case "EASY" -> Difficulty.EASY;
                case "HARD" -> Difficulty.HARD;
                default -> Difficulty.NORMAL;
            };
        } catch (Exception e) {
            return Difficulty.NORMAL;
        }
    }

    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {
        try {
            Class<?> difficultyClass = Class.forName("org.bukkit.Difficulty");
            Object bukkitDifficulty = difficultyClass.getField(difficulty.name()).get(null);
            bukkitWorld.getClass().getMethod("setDifficulty", difficultyClass)
                    .invoke(bukkitWorld, bukkitDifficulty);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set difficulty", e);
        }
    }

    @Override
    @NotNull
    public Environment getEnvironment() {
        try {
            Object environment = bukkitWorld.getClass().getMethod("getEnvironment").invoke(bukkitWorld);
            String name = environment.toString();
            return switch (name) {
                case "NETHER" -> Environment.NETHER;
                case "THE_END" -> Environment.THE_END;
                case "CUSTOM" -> Environment.CUSTOM;
                default -> Environment.NORMAL;
            };
        } catch (Exception e) {
            return Environment.NORMAL;
        }
    }

    @Override
    public long getSeed() {
        try {
            return (long) bukkitWorld.getClass().getMethod("getSeed").invoke(bukkitWorld);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isPvPEnabled() {
        try {
            return (boolean) bukkitWorld.getClass().getMethod("getPVP").invoke(bukkitWorld);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void setPvPEnabled(boolean pvp) {
        try {
            bukkitWorld.getClass().getMethod("setPVP", boolean.class).invoke(bukkitWorld, pvp);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set PvP", e);
        }
    }

    @Override
    public int getMinHeight() {
        try {
            return (int) bukkitWorld.getClass().getMethod("getMinHeight").invoke(bukkitWorld);
        } catch (Exception e) {
            return -64;
        }
    }

    @Override
    public int getMaxHeight() {
        try {
            return (int) bukkitWorld.getClass().getMethod("getMaxHeight").invoke(bukkitWorld);
        } catch (Exception e) {
            return 320;
        }
    }

    @Override
    public int getSeaLevel() {
        try {
            return (int) bukkitWorld.getClass().getMethod("getSeaLevel").invoke(bukkitWorld);
        } catch (Exception e) {
            return 63;
        }
    }

    @Override
    @NotNull
    public Optional<String> getGameRuleValue(@NotNull String rule) {
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            Method getByNameMethod = gameRuleClass.getMethod("getByName", String.class);
            Object gameRule = getByNameMethod.invoke(null, rule);

            if (gameRule != null) {
                Method getGameRuleValueMethod = bukkitWorld.getClass().getMethod("getGameRuleValue", gameRuleClass);
                Object value = getGameRuleValueMethod.invoke(bukkitWorld, gameRule);
                return Optional.ofNullable(value != null ? value.toString() : null);
            }
        } catch (Exception e) {
            // Fallback to string method
            try {
                Method method = bukkitWorld.getClass().getMethod("getGameRuleValue", String.class);
                String value = (String) method.invoke(bukkitWorld, rule);
                return Optional.ofNullable(value);
            } catch (Exception ex) {
                // Ignore
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean setGameRule(@NotNull String rule, @NotNull String value) {
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            Method getByNameMethod = gameRuleClass.getMethod("getByName", String.class);
            Object gameRule = getByNameMethod.invoke(null, rule);

            if (gameRule != null) {
                // Determine value type and convert
                Class<?> ruleType = (Class<?>) gameRuleClass.getMethod("getType").invoke(gameRule);

                Object convertedValue;
                if (ruleType == Boolean.class) {
                    convertedValue = Boolean.parseBoolean(value);
                } else if (ruleType == Integer.class) {
                    convertedValue = Integer.parseInt(value);
                } else {
                    convertedValue = value;
                }

                Method setGameRuleValueMethod = bukkitWorld.getClass().getMethod("setGameRule", gameRuleClass, Object.class);
                return (boolean) setGameRuleValueMethod.invoke(bukkitWorld, gameRule, convertedValue);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set game rule: " + rule, e);
        }
        return false;
    }

    @Override
    public boolean getGameRuleBoolean(@NotNull String rule, boolean defaultValue) {
        return getGameRuleValue(rule)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    @Override
    public int getGameRuleInt(@NotNull String rule, int defaultValue) {
        return getGameRuleValue(rule)
                .map(Integer::parseInt)
                .orElse(defaultValue);
    }

    @Override
    public boolean createExplosion(@NotNull UnifiedLocation location, float power, boolean setFire, boolean breakBlocks) {
        try {
            Object bukkitLocation = toBukkitLocation(location);
            return (boolean) bukkitWorld.getClass()
                    .getMethod("createExplosion", Class.forName("org.bukkit.Location"),
                            float.class, boolean.class, boolean.class)
                    .invoke(bukkitWorld, bukkitLocation, power, setFire, breakBlocks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create explosion", e);
            return false;
        }
    }

    @Override
    public void strikeLightning(@NotNull UnifiedLocation location, boolean effect) {
        try {
            Object bukkitLocation = toBukkitLocation(location);
            String methodName = effect ? "strikeLightningEffect" : "strikeLightning";
            bukkitWorld.getClass().getMethod(methodName, Class.forName("org.bukkit.Location"))
                    .invoke(bukkitWorld, bukkitLocation);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to strike lightning", e);
        }
    }

    @Override
    public void save() {
        try {
            bukkitWorld.getClass().getMethod("save").invoke(bukkitWorld);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save world", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T getHandle() {
        return (T) bukkitWorld;
    }

    /**
     * Creates a region context for a location in this world.
     *
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @return the region context
     * @since 1.0.0
     */
    @NotNull
    public RegionContext getRegionContext(int x, int z) {
        return RegionContext.ofBlock(this, x, z);
    }

    /**
     * Checks if the current thread owns the region at the specified coordinates.
     *
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @return true if the current thread owns this region
     * @since 1.0.0
     */
    public boolean isOwnedByCurrentThread(int x, int z) {
        return getRegionContext(x, z).isOwnedByCurrentThread();
    }

    /**
     * Converts a Bukkit Location to UnifiedLocation.
     */
    private UnifiedLocation convertLocation(Object bukkitLocation) throws Exception {
        double x = (double) bukkitLocation.getClass().getMethod("getX").invoke(bukkitLocation);
        double y = (double) bukkitLocation.getClass().getMethod("getY").invoke(bukkitLocation);
        double z = (double) bukkitLocation.getClass().getMethod("getZ").invoke(bukkitLocation);
        float yaw = (float) bukkitLocation.getClass().getMethod("getYaw").invoke(bukkitLocation);
        float pitch = (float) bukkitLocation.getClass().getMethod("getPitch").invoke(bukkitLocation);

        return new UnifiedLocation(this, x, y, z, yaw, pitch);
    }

    /**
     * Converts UnifiedLocation to Bukkit Location.
     */
    private Object toBukkitLocation(UnifiedLocation location) throws Exception {
        Class<?> locationClass = Class.forName("org.bukkit.Location");
        Class<?> worldClass = Class.forName("org.bukkit.World");

        return locationClass.getConstructor(worldClass, double.class, double.class, double.class)
                .newInstance(bukkitWorld, location.x(), location.y(), location.z());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FoliaUnifiedWorld other)) return false;
        return uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "FoliaUnifiedWorld[name=" + name + ", uuid=" + uuid + "]";
    }
}
