/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Context object that provides access to the current GUI and player.
 *
 * <p>GUIContext encapsulates the state needed for GUI operations,
 * including the viewing player and the current GUI instance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * GUIContext context = GUIContext.of(player, gui);
 *
 * // Access the player
 * UnifiedPlayer viewer = context.getPlayer();
 *
 * // Access the GUI
 * context.getGui().ifPresent(gui -> gui.refresh());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickContext
 * @see AbstractGUI
 */
public final class GUIContext {

    private final UnifiedPlayer player;
    private final AbstractGUI gui;

    /**
     * Creates a new GUIContext.
     *
     * @param player the player viewing the GUI
     * @param gui    the current GUI (may be null)
     */
    private GUIContext(@NotNull UnifiedPlayer player, AbstractGUI gui) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.gui = gui;
    }

    /**
     * Creates a new GUIContext with a player and GUI.
     *
     * @param player the player viewing the GUI
     * @param gui    the current GUI
     * @return the new context
     */
    @NotNull
    public static GUIContext of(@NotNull UnifiedPlayer player, @NotNull AbstractGUI gui) {
        return new GUIContext(player, gui);
    }

    /**
     * Creates a new GUIContext with only a player.
     *
     * @param player the player
     * @return the new context
     */
    @NotNull
    public static GUIContext of(@NotNull UnifiedPlayer player) {
        return new GUIContext(player, null);
    }

    /**
     * Returns the player associated with this context.
     *
     * @return the player
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the current GUI, if any.
     *
     * @return an Optional containing the GUI, or empty if no GUI
     */
    @NotNull
    public Optional<AbstractGUI> getGui() {
        return Optional.ofNullable(gui);
    }

    /**
     * Creates a new context with a different GUI.
     *
     * @param newGui the new GUI
     * @return a new context with the specified GUI
     */
    @NotNull
    public GUIContext withGui(@NotNull AbstractGUI newGui) {
        return new GUIContext(player, newGui);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GUIContext that = (GUIContext) o;
        return Objects.equals(player, that.player) && Objects.equals(gui, that.gui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, gui);
    }

    @Override
    public String toString() {
        return "GUIContext{player=" + player.getName() + ", gui=" + (gui != null ? gui.getClass().getSimpleName() : "null") + '}';
    }
}
