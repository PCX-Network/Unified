/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a hologram displayed in the world.
 *
 * <p>Holograms are virtual displays that can show text, items, or blocks
 * floating in the world. They support per-player visibility, animations,
 * and interactive click handling.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Update a line
 * hologram.setLine(0, Component.text("Updated Text"));
 *
 * // Add a new line
 * hologram.addLine(Component.text("New Line"));
 *
 * // Move the hologram
 * hologram.teleport(newLocation);
 *
 * // Show to specific player
 * hologram.showTo(player);
 *
 * // Remove when done
 * hologram.remove();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see HologramService
 * @see HologramBuilder
 */
public interface Hologram {

    /**
     * Returns the unique identifier of this hologram.
     *
     * @return the hologram's unique ID
     * @since 1.0.0
     */
    @NotNull
    UUID getId();

    /**
     * Returns the optional name of this hologram.
     *
     * @return an Optional containing the name, or empty if unnamed
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getName();

    /**
     * Returns the current location of this hologram.
     *
     * @return the hologram's location
     * @since 1.0.0
     */
    @NotNull
    UnifiedLocation getLocation();

    /**
     * Teleports this hologram to a new location.
     *
     * @param location the new location
     * @since 1.0.0
     */
    void teleport(@NotNull UnifiedLocation location);

    /**
     * Returns all lines of this hologram.
     *
     * @return an unmodifiable list of text lines
     * @since 1.0.0
     */
    @NotNull
    @Unmodifiable
    List<Component> getLines();

    /**
     * Returns a specific line of this hologram.
     *
     * @param index the line index (0-based)
     * @return the text at the specified line
     * @throws IndexOutOfBoundsException if the index is out of range
     * @since 1.0.0
     */
    @NotNull
    Component getLine(int index);

    /**
     * Sets a specific line of this hologram.
     *
     * @param index the line index (0-based)
     * @param text  the new text for the line
     * @throws IndexOutOfBoundsException if the index is out of range
     * @since 1.0.0
     */
    void setLine(int index, @NotNull Component text);

    /**
     * Adds a new line to the bottom of this hologram.
     *
     * @param text the text to add
     * @since 1.0.0
     */
    void addLine(@NotNull Component text);

    /**
     * Inserts a new line at the specified index.
     *
     * @param index the index to insert at
     * @param text  the text to insert
     * @throws IndexOutOfBoundsException if the index is out of range
     * @since 1.0.0
     */
    void insertLine(int index, @NotNull Component text);

    /**
     * Removes a line from this hologram.
     *
     * @param index the index of the line to remove
     * @throws IndexOutOfBoundsException if the index is out of range
     * @since 1.0.0
     */
    void removeLine(int index);

    /**
     * Clears all lines from this hologram.
     *
     * @since 1.0.0
     */
    void clearLines();

    /**
     * Returns the number of lines in this hologram.
     *
     * @return the line count
     * @since 1.0.0
     */
    int getLineCount();

    /**
     * Returns the billboard mode of this hologram.
     *
     * @return the billboard mode
     * @since 1.0.0
     */
    @NotNull
    Billboard getBillboard();

    /**
     * Sets the billboard mode of this hologram.
     *
     * @param billboard the new billboard mode
     * @since 1.0.0
     */
    void setBillboard(@NotNull Billboard billboard);

    /**
     * Returns whether this hologram is visible globally.
     *
     * <p>If true, the hologram is visible to all players unless explicitly
     * hidden from specific players. If false, the hologram is only visible
     * to players in the viewers list.
     *
     * @return true if visible globally
     * @since 1.0.0
     */
    boolean isGloballyVisible();

    /**
     * Sets whether this hologram is visible globally.
     *
     * @param visible true for global visibility
     * @since 1.0.0
     */
    void setGloballyVisible(boolean visible);

    /**
     * Returns the players who can see this hologram.
     *
     * <p>If globally visible, returns players not in the hidden list.
     * If not globally visible, returns only players in the viewers list.
     *
     * @return a collection of players who can see this hologram
     * @since 1.0.0
     */
    @NotNull
    Collection<UnifiedPlayer> getViewers();

    /**
     * Shows this hologram to a specific player.
     *
     * @param player the player to show the hologram to
     * @since 1.0.0
     */
    void showTo(@NotNull UnifiedPlayer player);

    /**
     * Shows this hologram to multiple players.
     *
     * @param players the players to show the hologram to
     * @since 1.0.0
     */
    void showTo(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Hides this hologram from a specific player.
     *
     * @param player the player to hide the hologram from
     * @since 1.0.0
     */
    void hideFrom(@NotNull UnifiedPlayer player);

    /**
     * Hides this hologram from multiple players.
     *
     * @param players the players to hide the hologram from
     * @since 1.0.0
     */
    void hideFrom(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Checks if a player can see this hologram.
     *
     * @param player the player to check
     * @return true if the player can see this hologram
     * @since 1.0.0
     */
    boolean isVisibleTo(@NotNull UnifiedPlayer player);

    /**
     * Returns whether this hologram persists across server restarts.
     *
     * @return true if persistent
     * @since 1.0.0
     */
    boolean isPersistent();

    /**
     * Sets whether this hologram persists across server restarts.
     *
     * @param persistent true to persist
     * @since 1.0.0
     */
    void setPersistent(boolean persistent);

    /**
     * Returns whether this hologram is currently spawned.
     *
     * @return true if spawned
     * @since 1.0.0
     */
    boolean isSpawned();

    /**
     * Spawns this hologram into the world.
     *
     * <p>If the hologram is already spawned, this method does nothing.
     *
     * @since 1.0.0
     */
    void spawn();

    /**
     * Despawns this hologram from the world without removing it.
     *
     * <p>The hologram can be respawned later with {@link #spawn()}.
     *
     * @since 1.0.0
     */
    void despawn();

    /**
     * Removes this hologram completely.
     *
     * <p>After removal, this hologram instance should not be used.
     * Use {@link HologramService#create(UnifiedLocation)} to create a new one.
     *
     * @since 1.0.0
     */
    void remove();

    /**
     * Returns whether this hologram has been removed.
     *
     * @return true if removed
     * @since 1.0.0
     */
    boolean isRemoved();

    /**
     * Sets a click handler for this hologram.
     *
     * @param handler the click handler, or null to remove
     * @since 1.0.0
     */
    void setClickHandler(@Nullable Consumer<HologramClickEvent> handler);

    /**
     * Returns the click handler for this hologram.
     *
     * @return an Optional containing the click handler
     * @since 1.0.0
     */
    @NotNull
    Optional<Consumer<HologramClickEvent>> getClickHandler();

    /**
     * Starts an animation on this hologram.
     *
     * @param animation the animation to start
     * @since 1.0.0
     */
    void startAnimation(@NotNull HologramAnimation animation);

    /**
     * Stops the specified animation.
     *
     * @param animationType the type of animation to stop
     * @since 1.0.0
     */
    void stopAnimation(@NotNull HologramAnimation.Type animationType);

    /**
     * Stops all animations on this hologram.
     *
     * @since 1.0.0
     */
    void stopAllAnimations();

    /**
     * Returns whether an animation type is currently running.
     *
     * @param animationType the animation type to check
     * @return true if the animation is running
     * @since 1.0.0
     */
    boolean isAnimating(@NotNull HologramAnimation.Type animationType);

    /**
     * Updates this hologram, refreshing it for all viewers.
     *
     * <p>This is useful after making multiple changes to batch the update.
     *
     * @since 1.0.0
     */
    void update();
}
