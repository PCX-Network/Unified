/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.bossbar;

import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.bossbar.BossBarDisplay;
import sh.pcx.unified.visual.bossbar.BossBarService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract base implementation of {@link BossBarService}.
 *
 * <p>Provides common boss bar management functionality. Subclasses
 * implement platform-specific boss bar creation.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractBossBarService implements BossBarService {

    protected final Map<UUID, BossBarDisplay> bossBars = new ConcurrentHashMap<>();

    @Override
    public @NotNull Optional<BossBarDisplay> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        return Optional.ofNullable(bossBars.get(id));
    }

    @Override
    public @NotNull Collection<BossBarDisplay> getAll() {
        return List.copyOf(bossBars.values());
    }

    @Override
    public @NotNull Collection<BossBarDisplay> getBarsFor(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        return bossBars.values().stream()
                .filter(bar -> bar.hasPlayer(player))
                .collect(Collectors.toList());
    }

    @Override
    public int getCount() {
        return bossBars.size();
    }

    @Override
    public boolean remove(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        BossBarDisplay bar = bossBars.remove(id);
        if (bar != null) {
            bar.remove();
            return true;
        }
        return false;
    }

    @Override
    public void removeAll() {
        List<BossBarDisplay> toRemove = new ArrayList<>(bossBars.values());
        bossBars.clear();
        toRemove.forEach(BossBarDisplay::remove);
    }

    @Override
    public void removeAllFor(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        List<BossBarDisplay> playerBars = bossBars.values().stream()
                .filter(bar -> bar.hasPlayer(player))
                .toList();
        playerBars.forEach(bar -> bar.removePlayer(player));
    }

    /**
     * Registers a boss bar with the service.
     *
     * @param bossBar the boss bar to register
     */
    protected void registerBossBar(@NotNull BossBarDisplay bossBar) {
        bossBars.put(bossBar.getId(), bossBar);
    }

    /**
     * Unregisters a boss bar from the service.
     *
     * @param bossBar the boss bar to unregister
     */
    protected void unregisterBossBar(@NotNull BossBarDisplay bossBar) {
        bossBars.remove(bossBar.getId());
    }

    @Override
    public String getServiceName() {
        return "BossBarService";
    }
}
