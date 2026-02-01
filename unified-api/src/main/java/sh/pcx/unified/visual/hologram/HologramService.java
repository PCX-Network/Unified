/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import sh.pcx.unified.service.Service;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating and managing holograms.
 *
 * <p>The HologramService provides methods to create text and item holograms,
 * with support for display entities (1.19.4+) and legacy armor stand fallback.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Text holograms with multi-line support</li>
 *   <li>Item holograms with spinning and bobbing</li>
 *   <li>Per-player visibility control</li>
 *   <li>Persistent holograms that survive restarts</li>
 *   <li>Animations (color cycling, text scrolling, etc.)</li>
 *   <li>Click detection</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private HologramService holograms;
 *
 * // Create text hologram
 * Hologram hologram = holograms.create(location)
 *     .addLine(Component.text("Welcome!", NamedTextColor.GOLD))
 *     .addLine(Component.text("Server Status: Online"))
 *     .billboard(Billboard.CENTER)
 *     .persistent(true)
 *     .build();
 *
 * // Create item hologram
 * ItemHologram item = holograms.createItem(location)
 *     .item(new ItemStack(Material.DIAMOND))
 *     .spin(true, 2.0f)
 *     .bob(true)
 *     .build();
 *
 * // Get by ID
 * holograms.getById(uuid).ifPresent(h -> h.teleport(newLocation));
 *
 * // Remove all
 * holograms.removeAll();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Hologram
 * @see ItemHologram
 * @see HologramBuilder
 */
public interface HologramService extends Service {

    /**
     * Creates a builder for a text hologram at the specified location.
     *
     * @param location the location to create the hologram at
     * @return a hologram builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder create(@NotNull UnifiedLocation location);

    /**
     * Creates a builder for an item hologram at the specified location.
     *
     * @param location the location to create the hologram at
     * @return an item hologram builder
     * @since 1.0.0
     */
    @NotNull
    ItemHologramBuilder createItem(@NotNull UnifiedLocation location);

    /**
     * Returns a hologram by its unique ID.
     *
     * @param id the hologram's unique ID
     * @return an Optional containing the hologram if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Hologram> getById(@NotNull UUID id);

    /**
     * Returns a hologram by its name.
     *
     * @param name the hologram's name
     * @return an Optional containing the hologram if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Hologram> getByName(@NotNull String name);

    /**
     * Returns all registered holograms.
     *
     * @return an unmodifiable collection of all holograms
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    Collection<Hologram> getAll();

    /**
     * Returns all holograms near a location.
     *
     * @param location the center location
     * @param radius   the search radius in blocks
     * @return a collection of holograms within the radius
     * @since 1.0.0
     */
    @NotNull
    Collection<Hologram> getNear(@NotNull UnifiedLocation location, double radius);

    /**
     * Returns the number of registered holograms.
     *
     * @return the hologram count
     * @since 1.0.0
     */
    int getCount();

    /**
     * Removes a hologram by its unique ID.
     *
     * @param id the hologram's unique ID
     * @return true if the hologram was removed
     * @since 1.0.0
     */
    boolean remove(@NotNull UUID id);

    /**
     * Removes a hologram by its name.
     *
     * @param name the hologram's name
     * @return true if the hologram was removed
     * @since 1.0.0
     */
    boolean removeByName(@NotNull String name);

    /**
     * Removes all holograms.
     *
     * @since 1.0.0
     */
    void removeAll();

    /**
     * Removes all non-persistent holograms.
     *
     * @since 1.0.0
     */
    void removeNonPersistent();

    /**
     * Returns whether display entities are supported on this server.
     *
     * <p>Display entities require Minecraft 1.19.4 or later. On older versions,
     * the service falls back to armor stand-based holograms.
     *
     * @return true if display entities are supported
     * @since 1.0.0
     */
    boolean supportsDisplayEntities();

    /**
     * Saves all persistent holograms to storage.
     *
     * <p>This is called automatically on server shutdown but can be called
     * manually to force a save.
     *
     * @since 1.0.0
     */
    void savePersistent();

    /**
     * Loads all persistent holograms from storage.
     *
     * <p>This is called automatically on service initialization.
     *
     * @since 1.0.0
     */
    void loadPersistent();
}
