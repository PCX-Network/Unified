/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl.hologram;

import sh.pcx.unified.visual.hologram.Hologram;
import sh.pcx.unified.visual.hologram.HologramBuilder;
import sh.pcx.unified.visual.hologram.HologramService;
import sh.pcx.unified.visual.hologram.ItemHologramBuilder;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract base implementation of {@link HologramService}.
 *
 * <p>Provides common hologram management functionality. Subclasses
 * implement platform-specific hologram creation and rendering.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class AbstractHologramService implements HologramService {

    protected final Map<UUID, Hologram> hologramsById = new ConcurrentHashMap<>();
    protected final Map<String, Hologram> hologramsByName = new ConcurrentHashMap<>();

    @Override
    public @NotNull Optional<Hologram> getById(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        return Optional.ofNullable(hologramsById.get(id));
    }

    @Override
    public @NotNull Optional<Hologram> getByName(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        return Optional.ofNullable(hologramsByName.get(name));
    }

    @Override
    public @NotNull Collection<Hologram> getAll() {
        return List.copyOf(hologramsById.values());
    }

    @Override
    public @NotNull Collection<Hologram> getNear(@NotNull UnifiedLocation location, double radius) {
        Objects.requireNonNull(location, "location cannot be null");
        double radiusSquared = radius * radius;
        return hologramsById.values().stream()
                .filter(h -> h.getLocation().sameWorld(location))
                .filter(h -> h.getLocation().distanceSquared(location) <= radiusSquared)
                .collect(Collectors.toList());
    }

    @Override
    public int getCount() {
        return hologramsById.size();
    }

    @Override
    public boolean remove(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");
        Hologram hologram = hologramsById.remove(id);
        if (hologram != null) {
            hologram.getName().ifPresent(hologramsByName::remove);
            hologram.remove();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeByName(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        Hologram hologram = hologramsByName.remove(name);
        if (hologram != null) {
            hologramsById.remove(hologram.getId());
            hologram.remove();
            return true;
        }
        return false;
    }

    @Override
    public void removeAll() {
        List<Hologram> toRemove = new ArrayList<>(hologramsById.values());
        hologramsById.clear();
        hologramsByName.clear();
        toRemove.forEach(Hologram::remove);
    }

    @Override
    public void removeNonPersistent() {
        List<Hologram> toRemove = hologramsById.values().stream()
                .filter(h -> !h.isPersistent())
                .toList();
        toRemove.forEach(h -> {
            hologramsById.remove(h.getId());
            h.getName().ifPresent(hologramsByName::remove);
            h.remove();
        });
    }

    /**
     * Registers a hologram with the service.
     *
     * @param hologram the hologram to register
     */
    protected void registerHologram(@NotNull Hologram hologram) {
        hologramsById.put(hologram.getId(), hologram);
        hologram.getName().ifPresent(name -> hologramsByName.put(name, hologram));
    }

    /**
     * Unregisters a hologram from the service.
     *
     * @param hologram the hologram to unregister
     */
    protected void unregisterHologram(@NotNull Hologram hologram) {
        hologramsById.remove(hologram.getId());
        hologram.getName().ifPresent(hologramsByName::remove);
    }

    @Override
    public String getServiceName() {
        return "HologramService";
    }
}
