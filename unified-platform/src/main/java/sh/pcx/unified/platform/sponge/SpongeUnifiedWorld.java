/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.Viewer;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.world.difficulty.Difficulties;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.gamerule.GameRule;
import org.spongepowered.api.world.gamerule.GameRules;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.weather.WeatherTypes;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Sponge implementation of the {@link UnifiedWorld} interface.
 *
 * <p>This class wraps Sponge's {@link ServerWorld} object to provide world
 * operations through the unified API.
 *
 * <h2>World Identification</h2>
 * <p>Sponge uses {@link ResourceKey} for world identification. The world name
 * returned by this class is the key's value (e.g., "overworld" for minecraft:overworld).
 *
 * <h2>Game Rules</h2>
 * <p>Sponge uses typed game rules through its registry system. This implementation
 * handles conversion between string-based rules and Sponge's typed system.
 *
 * <h2>Thread Safety</h2>
 * <p>Read operations are thread-safe. Write operations (block changes, explosions)
 * should typically be performed on the main thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UnifiedWorld
 */
public final class SpongeUnifiedWorld implements UnifiedWorld {

    private final ServerWorld world;
    private final SpongePlatformProvider provider;

    /**
     * Creates a new SpongeUnifiedWorld wrapping the given ServerWorld.
     *
     * @param world    the Sponge ServerWorld to wrap
     * @param provider the platform provider
     * @since 1.0.0
     */
    public SpongeUnifiedWorld(@NotNull ServerWorld world, @NotNull SpongePlatformProvider provider) {
        this.world = world;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName() {
        return world.key().value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUniqueId() {
        return world.uniqueId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedLocation getSpawnLocation() {
        Vector3i spawnPos = world.properties().spawnPosition();
        return new UnifiedLocation(this, spawnPos.x(), spawnPos.y(), spawnPos.z());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSpawnLocation(@NotNull UnifiedLocation location) {
        return setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        world.properties().setSpawnPosition(Vector3i.from(x, y, z));
        return true;
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
        ServerLocation location = world.location(x, y, z);
        return new SpongeUnifiedBlock(location, provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UnifiedChunk getChunkAt(int x, int z) {
        return new SpongeUnifiedChunk(world, x, z, provider);
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
        return CompletableFuture.supplyAsync(() -> {
            world.loadChunk(x, 0, z, true);
            return new SpongeUnifiedChunk(world, x, z, provider);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChunkLoaded(int x, int z) {
        return world.isChunkLoaded(x, 0, z, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        Optional<org.spongepowered.api.world.chunk.WorldChunk> chunk = world.loadChunk(x, 0, z, generate);
        return chunk.isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        // Sponge API 8+ uses WorldChunk for unloading
        Optional<org.spongepowered.api.world.chunk.WorldChunk> chunkOpt = world.loadChunk(x, 0, z, false);
        if (chunkOpt.isPresent()) {
            return world.unloadChunk(chunkOpt.get());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Collection<UnifiedPlayer> getPlayers() {
        return world.players().stream()
                .map(provider::getOrCreatePlayer)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTime() {
        return world.properties().dayTime().asTicks().ticks() % 24000;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTime(long time) {
        world.properties().setDayTime(org.spongepowered.api.util.MinecraftDayTime.of(
                Sponge.server(), org.spongepowered.api.util.Ticks.of(time)
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getFullTime() {
        return world.properties().dayTime().asTicks().ticks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFullTime(long time) {
        world.properties().setDayTime(org.spongepowered.api.util.MinecraftDayTime.of(
                Sponge.server(), org.spongepowered.api.util.Ticks.of(time)
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeatherDuration() {
        return (int) world.weather().remainingDuration().ticks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWeatherDuration(int duration) {
        world.setWeather(world.weather().type(), org.spongepowered.api.util.Ticks.of(duration));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasStorm() {
        return world.weather().type().equals(WeatherTypes.RAIN.get()) ||
               world.weather().type().equals(WeatherTypes.THUNDER.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStorm(boolean storm) {
        if (storm) {
            world.setWeather(WeatherTypes.RAIN.get());
        } else {
            world.setWeather(WeatherTypes.CLEAR.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isThundering() {
        return world.weather().type().equals(WeatherTypes.THUNDER.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setThundering(boolean thundering) {
        if (thundering) {
            world.setWeather(WeatherTypes.THUNDER.get());
        } else if (hasStorm()) {
            world.setWeather(WeatherTypes.RAIN.get());
        } else {
            world.setWeather(WeatherTypes.CLEAR.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClearWeatherDuration(int duration) {
        world.setWeather(WeatherTypes.CLEAR.get(), org.spongepowered.api.util.Ticks.of(duration));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Difficulty getDifficulty() {
        org.spongepowered.api.world.difficulty.Difficulty spongeDiff = world.difficulty();

        if (spongeDiff.equals(Difficulties.PEACEFUL.get())) {
            return Difficulty.PEACEFUL;
        } else if (spongeDiff.equals(Difficulties.EASY.get())) {
            return Difficulty.EASY;
        } else if (spongeDiff.equals(Difficulties.NORMAL.get())) {
            return Difficulty.NORMAL;
        } else if (spongeDiff.equals(Difficulties.HARD.get())) {
            return Difficulty.HARD;
        }
        return Difficulty.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {
        org.spongepowered.api.world.difficulty.Difficulty spongeDiff = switch (difficulty) {
            case PEACEFUL -> Difficulties.PEACEFUL.get();
            case EASY -> Difficulties.EASY.get();
            case NORMAL -> Difficulties.NORMAL.get();
            case HARD -> Difficulties.HARD.get();
        };
        world.properties().setDifficulty(spongeDiff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Environment getEnvironment() {
        String dimension = world.worldType().key(org.spongepowered.api.registry.RegistryTypes.WORLD_TYPE).value();

        return switch (dimension) {
            case "overworld" -> Environment.NORMAL;
            case "the_nether" -> Environment.NETHER;
            case "the_end" -> Environment.THE_END;
            default -> Environment.CUSTOM;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSeed() {
        return world.seed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPvPEnabled() {
        return world.properties().pvp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPvPEnabled(boolean pvp) {
        world.properties().setPvp(pvp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinHeight() {
        return world.min().y();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxHeight() {
        return world.max().y();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSeaLevel() {
        return world.seaLevel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getGameRuleValue(@NotNull String rule) {
        ResourceKey ruleKey = ResourceKey.resolve(rule);
        Optional<GameRule<?>> gameRule = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.GAME_RULE).findValue(ruleKey);

        if (gameRule.isEmpty()) {
            return Optional.empty();
        }

        Object value = world.properties().gameRule(gameRule.get());
        return Optional.of(String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean setGameRule(@NotNull String rule, @NotNull String value) {
        ResourceKey ruleKey = ResourceKey.resolve(rule);
        Optional<GameRule<?>> gameRuleOpt = Sponge.game().registry(org.spongepowered.api.registry.RegistryTypes.GAME_RULE).findValue(ruleKey);

        if (gameRuleOpt.isEmpty()) {
            return false;
        }

        GameRule gameRule = gameRuleOpt.get();

        try {
            // Try to parse as boolean first
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                world.properties().setGameRule(gameRule, Boolean.parseBoolean(value));
            } else {
                // Try as integer
                world.properties().setGameRule(gameRule, Integer.parseInt(value));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
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
    public boolean createExplosion(@NotNull UnifiedLocation location, float power, boolean setFire, boolean breakBlocks) {
        Explosion explosion = Explosion.builder()
                .location(world.location(location.x(), location.y(), location.z()))
                .radius(power)
                .shouldPlaySmoke(true)
                .shouldBreakBlocks(breakBlocks)
                .shouldDamageEntities(true)
                .canCauseFire(setFire)
                .build();

        world.triggerExplosion(explosion);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void strikeLightning(@NotNull UnifiedLocation location, boolean effect) {
        Vector3d position = Vector3d.from(location.x(), location.y(), location.z());

        // In Sponge API 12, createEntity returns the entity directly
        org.spongepowered.api.entity.weather.LightningBolt lightning =
                world.createEntity(EntityTypes.LIGHTNING_BOLT.get(), position);
        world.spawnEntity(lightning);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        try {
            world.save();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save world: " + world.key().asString(), e);
        }
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
     * Returns the world's resource key.
     *
     * @return the ResourceKey for this world
     */
    @NotNull
    public ResourceKey getKey() {
        return world.key();
    }

    /**
     * Returns a string representation of this world.
     *
     * @return a descriptive string
     */
    @Override
    public String toString() {
        return "SpongeUnifiedWorld{" +
                "key=" + world.key().asString() +
                ", uuid=" + world.uniqueId() +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SpongeUnifiedWorld other)) return false;
        return world.uniqueId().equals(other.world.uniqueId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return world.uniqueId().hashCode();
    }
}
