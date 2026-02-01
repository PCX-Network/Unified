/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.example.minigame.arena;

import sh.pcx.unified.region.Region;
import sh.pcx.unified.world.UnifiedLocation;
import sh.pcx.unified.world.UnifiedWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a minigame arena with spawn points and region-based boundaries.
 *
 * <p>Arenas are defined by a cuboid region that determines the playable area.
 * Players inside the region are considered to be in the arena. Each arena
 * has multiple spawn points that are used when starting a game.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArenaManager
 * @see ArenaState
 */
public class Arena {

    private final String name;
    private final UUID id;
    private final UnifiedWorld world;
    private final List<UnifiedLocation> spawnPoints;

    private Region region;
    private UnifiedLocation lobbySpawn;
    private UnifiedLocation spectatorSpawn;
    private ArenaState state;
    private boolean enabled;
    private int minPlayers;
    private int maxPlayers;
    private String displayName;
    private String description;

    /**
     * Creates a new arena with the specified name.
     *
     * @param name  the unique name of the arena
     * @param world the world the arena is in
     */
    public Arena(@NotNull String name, @NotNull UnifiedWorld world) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.world = world;
        this.spawnPoints = new ArrayList<>();
        this.state = ArenaState.DISABLED;
        this.enabled = false;
        this.minPlayers = 2;
        this.maxPlayers = 16;
        this.displayName = name;
        this.description = "";
    }

    /**
     * Creates a new arena with a specific UUID (for loading from storage).
     */
    public Arena(@NotNull String name, @NotNull UUID id, @NotNull UnifiedWorld world) {
        this.name = name;
        this.id = id;
        this.world = world;
        this.spawnPoints = new ArrayList<>();
        this.state = ArenaState.DISABLED;
        this.enabled = false;
        this.minPlayers = 2;
        this.maxPlayers = 16;
        this.displayName = name;
        this.description = "";
    }

    // ==================== Region and Location Methods ====================

    public boolean contains(@NotNull UnifiedLocation location) {
        if (region == null) {
            return false;
        }
        return region.contains(location);
    }

    @Nullable
    public UnifiedLocation getCenter() {
        if (region == null) {
            return null;
        }
        return region.getCenter();
    }

    public void addSpawnPoint(@NotNull UnifiedLocation location) {
        spawnPoints.add(location);
    }

    public boolean removeSpawnPoint(int index) {
        if (index >= 0 && index < spawnPoints.size()) {
            spawnPoints.remove(index);
            return true;
        }
        return false;
    }

    public void clearSpawnPoints() {
        spawnPoints.clear();
    }

    @Nullable
    public UnifiedLocation getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) {
            return null;
        }
        int index = (int) (Math.random() * spawnPoints.size());
        return spawnPoints.get(index);
    }

    @NotNull
    public List<UnifiedLocation> getShuffledSpawnPoints() {
        List<UnifiedLocation> shuffled = new ArrayList<>(spawnPoints);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    // ==================== State Methods ====================

    public boolean isAvailable() {
        return enabled && state == ArenaState.WAITING && region != null && !spawnPoints.isEmpty();
    }

    public boolean isInGame() {
        return state == ArenaState.STARTING || state == ArenaState.IN_GAME;
    }

    public boolean isConfigured() {
        return region != null && !spawnPoints.isEmpty() && lobbySpawn != null;
    }

    // ==================== Getters ====================

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public UnifiedWorld getWorld() {
        return world;
    }

    @Nullable
    public Region getRegion() {
        return region;
    }

    @NotNull
    public List<UnifiedLocation> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    public int getSpawnPointCount() {
        return spawnPoints.size();
    }

    @Nullable
    public UnifiedLocation getLobbySpawn() {
        return lobbySpawn;
    }

    @Nullable
    public UnifiedLocation getSpectatorSpawn() {
        return spectatorSpawn;
    }

    @NotNull
    public ArenaState getState() {
        return state;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    // ==================== Setters ====================

    public void setRegion(@Nullable Region region) {
        this.region = region;
    }

    public void setLobbySpawn(@Nullable UnifiedLocation lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public void setSpectatorSpawn(@Nullable UnifiedLocation spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public void setState(@NotNull ArenaState state) {
        this.state = state;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.state = ArenaState.DISABLED;
        } else if (this.state == ArenaState.DISABLED) {
            this.state = ArenaState.WAITING;
        }
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = Math.max(1, minPlayers);
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(this.minPlayers, maxPlayers);
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Arena other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Arena{name='" + name + "', state=" + state + ", enabled=" + enabled + "}";
    }
}
