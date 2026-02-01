/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.world;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.testing.player.MockPlayer;
import sh.pcx.unified.testing.server.MockServer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedChunk;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of a Minecraft world for testing purposes.
 *
 * <p>MockWorld provides a complete simulation of a Minecraft world,
 * including block storage, entity tracking, and world properties.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Block placement and retrieval</li>
 *   <li>Chunk loading simulation</li>
 *   <li>Entity spawning and tracking</li>
 *   <li>Weather and time control</li>
 *   <li>Game rule management</li>
 *   <li>Explosion simulation</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockWorld world = server.getWorld("world");
 *
 * // Set blocks
 * UnifiedLocation loc = new UnifiedLocation(world, 10, 64, 10);
 * world.setBlock(loc, "minecraft:diamond_block");
 *
 * // Verify block
 * assertThat(world.getBlockAt(loc).getType()).isEqualTo("minecraft:diamond_block");
 *
 * // Spawn entity
 * MockEntity zombie = world.spawnEntity(loc, "minecraft:zombie");
 *
 * // Create explosion
 * world.createExplosion(loc, 4.0f, false, true);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MockBlock
 * @see MockChunk
 * @see MockEntity
 */
public final class MockWorld implements UnifiedWorld {

    private final MockServer server;
    private final String name;
    private final UUID uuid;
    private final Environment environment;

    // Block storage: key = "x,y,z"
    private final Map<String, MockBlock> blocks = new ConcurrentHashMap<>();
    private final Map<String, MockChunk> chunks = new ConcurrentHashMap<>();
    private final Map<UUID, MockEntity> entities = new ConcurrentHashMap<>();

    // World properties
    private UnifiedLocation spawnLocation;
    private long time = 0;
    private long fullTime = 0;
    private int weatherDuration = 0;
    private boolean storm = false;
    private boolean thundering = false;
    private int clearWeatherDuration = 0;
    private Difficulty difficulty = Difficulty.NORMAL;
    private long seed = System.currentTimeMillis();
    private boolean pvpEnabled = true;
    private int minHeight = -64;
    private int maxHeight = 320;
    private int seaLevel = 63;

    // Game rules
    private final Map<String, String> gameRules = new ConcurrentHashMap<>();

    /**
     * Creates a new mock world.
     *
     * @param server      the mock server
     * @param name        the world name
     * @param environment the world environment
     */
    public MockWorld(
        @NotNull MockServer server,
        @NotNull String name,
        @NotNull Environment environment
    ) {
        this.server = Objects.requireNonNull(server, "server cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.environment = Objects.requireNonNull(environment, "environment cannot be null");
        this.uuid = UUID.nameUUIDFromBytes(("MockWorld:" + name).getBytes());
        this.spawnLocation = new UnifiedLocation(this, 0, 64, 0, 0, 0);

        // Set default game rules
        initializeGameRules();
    }

    private void initializeGameRules() {
        gameRules.put("doDaylightCycle", "true");
        gameRules.put("doMobSpawning", "true");
        gameRules.put("doWeatherCycle", "true");
        gameRules.put("keepInventory", "false");
        gameRules.put("mobGriefing", "true");
        gameRules.put("pvp", "true");
        gameRules.put("naturalRegeneration", "true");
        gameRules.put("doFireTick", "true");
        gameRules.put("fallDamage", "true");
        gameRules.put("drowningDamage", "true");
        gameRules.put("fireDamage", "true");
        gameRules.put("freezeDamage", "true");
    }

    /**
     * Resets the world to its initial state.
     */
    public void reset() {
        blocks.clear();
        entities.clear();
        time = 0;
        fullTime = 0;
        storm = false;
        thundering = false;
        initializeGameRules();
    }

    // ==================== Block Operations ====================

    /**
     * Sets a block at the specified location.
     *
     * @param location the location
     * @param type     the block type (e.g., "minecraft:stone")
     */
    public void setBlock(@NotNull UnifiedLocation location, @NotNull String type) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        String key = blockKey(location);
        MockBlock block = new MockBlock(this, location, type);
        blocks.put(key, block);
    }

    /**
     * Sets a block at the specified coordinates.
     *
     * @param x    the x coordinate
     * @param y    the y coordinate
     * @param z    the z coordinate
     * @param type the block type
     */
    public void setBlock(int x, int y, int z, @NotNull String type) {
        setBlock(new UnifiedLocation(this, x, y, z), type);
    }

    @Override
    @NotNull
    public UnifiedBlock getBlockAt(@NotNull UnifiedLocation location) {
        String key = blockKey(location);
        return blocks.computeIfAbsent(key, k -> new MockBlock(this, location, "minecraft:air"));
    }

    @Override
    @NotNull
    public UnifiedBlock getBlockAt(int x, int y, int z) {
        return getBlockAt(new UnifiedLocation(this, x, y, z));
    }

    /**
     * Gets a block as a MockBlock.
     *
     * @param location the location
     * @return the mock block
     */
    @NotNull
    public MockBlock getMockBlockAt(@NotNull UnifiedLocation location) {
        return (MockBlock) getBlockAt(location);
    }

    private String blockKey(UnifiedLocation location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    // ==================== Chunk Operations ====================

    @Override
    @NotNull
    public UnifiedChunk getChunkAt(int x, int z) {
        String key = x + "," + z;
        return chunks.computeIfAbsent(key, k -> new MockChunk(this, x, z));
    }

    @Override
    @NotNull
    public UnifiedChunk getChunkAt(@NotNull UnifiedLocation location) {
        return getChunkAt(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    @Override
    @NotNull
    public CompletableFuture<UnifiedChunk> getChunkAtAsync(int x, int z) {
        return CompletableFuture.completedFuture(getChunkAt(x, z));
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        String key = x + "," + z;
        MockChunk chunk = chunks.get(key);
        return chunk != null && chunk.isLoaded();
    }

    @Override
    public boolean loadChunk(int x, int z, boolean generate) {
        String key = x + "," + z;
        MockChunk chunk = chunks.computeIfAbsent(key, k -> new MockChunk(this, x, z));
        chunk.setLoaded(true);
        return true;
    }

    @Override
    public boolean unloadChunk(int x, int z, boolean save) {
        String key = x + "," + z;
        MockChunk chunk = chunks.get(key);
        if (chunk != null) {
            chunk.setLoaded(false);
            return true;
        }
        return false;
    }

    // ==================== Entity Operations ====================

    /**
     * Spawns an entity at the specified location.
     *
     * @param location the spawn location
     * @param type     the entity type (e.g., "minecraft:zombie")
     * @return the spawned entity
     */
    @NotNull
    public MockEntity spawnEntity(@NotNull UnifiedLocation location, @NotNull String type) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        MockEntity entity = new MockEntity(this, type, location);
        entities.put(entity.getUniqueId(), entity);
        return entity;
    }

    /**
     * Removes an entity from this world.
     *
     * @param entity the entity to remove
     */
    public void removeEntity(@NotNull MockEntity entity) {
        entities.remove(entity.getUniqueId());
    }

    /**
     * Gets all entities in this world.
     *
     * @return the collection of entities
     */
    @NotNull
    public Collection<MockEntity> getEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Gets an entity by UUID.
     *
     * @param uuid the entity UUID
     * @return an Optional containing the entity
     */
    @NotNull
    public Optional<MockEntity> getEntity(@NotNull UUID uuid) {
        return Optional.ofNullable(entities.get(uuid));
    }

    // ==================== Player Access ====================

    @Override
    @NotNull
    public Collection<UnifiedPlayer> getPlayers() {
        return server.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().equals(this))
            .toList();
    }

    /**
     * Gets all mock players in this world.
     *
     * @return the collection of mock players
     */
    @NotNull
    public Collection<MockPlayer> getMockPlayers() {
        return server.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().equals(this))
            .map(p -> (MockPlayer) p)
            .toList();
    }

    // ==================== World Properties ====================

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
        return spawnLocation;
    }

    @Override
    public boolean setSpawnLocation(@NotNull UnifiedLocation location) {
        this.spawnLocation = Objects.requireNonNull(location);
        return true;
    }

    @Override
    public boolean setSpawnLocation(int x, int y, int z) {
        this.spawnLocation = new UnifiedLocation(this, x, y, z);
        return true;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time % 24000;
    }

    @Override
    public long getFullTime() {
        return fullTime;
    }

    @Override
    public void setFullTime(long time) {
        this.fullTime = time;
        this.time = time % 24000;
    }

    @Override
    public int getWeatherDuration() {
        return weatherDuration;
    }

    @Override
    public void setWeatherDuration(int duration) {
        this.weatherDuration = duration;
    }

    @Override
    public boolean hasStorm() {
        return storm;
    }

    @Override
    public void setStorm(boolean storm) {
        this.storm = storm;
    }

    @Override
    public boolean isThundering() {
        return thundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    @Override
    public void setClearWeatherDuration(int duration) {
        this.clearWeatherDuration = duration;
        this.storm = false;
        this.thundering = false;
    }

    @Override
    @NotNull
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public void setDifficulty(@NotNull Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty);
    }

    @Override
    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    /**
     * Sets the world seed.
     *
     * @param seed the seed
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    @Override
    public void setPvPEnabled(boolean pvp) {
        this.pvpEnabled = pvp;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    /**
     * Sets the minimum height.
     *
     * @param minHeight the minimum height
     */
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Sets the maximum height.
     *
     * @param maxHeight the maximum height
     */
    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    /**
     * Sets the sea level.
     *
     * @param seaLevel the sea level
     */
    public void setSeaLevel(int seaLevel) {
        this.seaLevel = seaLevel;
    }

    // ==================== Game Rules ====================

    @Override
    @NotNull
    public Optional<String> getGameRuleValue(@NotNull String rule) {
        return Optional.ofNullable(gameRules.get(rule));
    }

    @Override
    public boolean setGameRule(@NotNull String rule, @NotNull String value) {
        gameRules.put(rule, value);
        return true;
    }

    @Override
    public boolean getGameRuleBoolean(@NotNull String rule, boolean defaultValue) {
        String value = gameRules.get(rule);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Override
    public int getGameRuleInt(@NotNull String rule, int defaultValue) {
        String value = gameRules.get(rule);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // ==================== Effects ====================

    @Override
    public boolean createExplosion(
        @NotNull UnifiedLocation location,
        float power,
        boolean setFire,
        boolean breakBlocks
    ) {
        Objects.requireNonNull(location, "location cannot be null");

        if (breakBlocks) {
            // Calculate affected blocks based on power
            int radius = (int) Math.ceil(power);
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance <= power) {
                            // Random chance to destroy block based on distance
                            if (Math.random() > distance / power) {
                                setBlock(x + dx, y + dy, z + dz, "minecraft:air");
                            }
                        }
                    }
                }
            }
        }

        // Fire explosion event
        server.getPluginManager().fireExplosionEvent(this, location, power);

        return true;
    }

    @Override
    public void strikeLightning(@NotNull UnifiedLocation location, boolean effect) {
        Objects.requireNonNull(location, "location cannot be null");
        server.getPluginManager().fireLightningStrikeEvent(this, location, effect);
    }

    @Override
    public void save() {
        // No-op for mock - worlds are in-memory only
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getHandle() {
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockWorld mockWorld = (MockWorld) o;
        return Objects.equals(uuid, mockWorld.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "MockWorld{" +
            "name='" + name + '\'' +
            ", environment=" + environment +
            '}';
    }
}
