/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.component;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all GUI components.
 *
 * <p>A GUIComponent represents any element that can be placed in a GUI slot.
 * Components can be interactive (like buttons) or decorative (like borders).
 *
 * <h2>Component Lifecycle</h2>
 * <ol>
 *   <li>Creation via builder or factory method</li>
 *   <li>Placement in a GUI via slot assignment</li>
 *   <li>Rendering when the GUI is displayed</li>
 *   <li>Event handling when interacted with</li>
 *   <li>Cleanup when the GUI is closed</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Button
 * @see Border
 * @see Filler
 */
public interface GUIComponent {

    /**
     * Returns the item to display for this component.
     *
     * <p>This item will be placed in the GUI slot where this component
     * is assigned. The item may change based on component state.
     *
     * @return the display item
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItem();

    /**
     * Returns whether this component is interactive.
     *
     * <p>Interactive components respond to click events, while non-interactive
     * components are purely decorative and ignore clicks.
     *
     * @return true if this component handles click events
     * @since 1.0.0
     */
    default boolean isInteractive() {
        return this instanceof Button;
    }

    /**
     * Returns whether this component should prevent item pickup.
     *
     * <p>By default, all GUI components cancel the click event to prevent
     * players from taking items. Override this to allow item pickup.
     *
     * @return true if clicks should be cancelled (default: true)
     * @since 1.0.0
     */
    default boolean cancelClick() {
        return true;
    }

    /**
     * Called when this component is added to a GUI.
     *
     * <p>Override to perform initialization when the component is placed.
     *
     * @param slot the slot where the component was placed
     * @since 1.0.0
     */
    default void onAdd(int slot) {
        // Default: no-op
    }

    /**
     * Called when this component is removed from a GUI.
     *
     * <p>Override to perform cleanup when the component is removed.
     *
     * @param slot the slot where the component was placed
     * @since 1.0.0
     */
    default void onRemove(int slot) {
        // Default: no-op
    }

    /**
     * Called each tick while the GUI is open.
     *
     * <p>Override to perform periodic updates. Note that frequent updates
     * may impact performance.
     *
     * @param tickCount the number of ticks since the GUI was opened
     * @since 1.0.0
     */
    default void onTick(long tickCount) {
        // Default: no-op
    }

    /**
     * Returns whether this component needs tick updates.
     *
     * <p>Return true only if the component needs to update periodically.
     * This allows GUIs to optimize by skipping tick calls for static components.
     *
     * @return true if {@link #onTick(long)} should be called
     * @since 1.0.0
     */
    default boolean needsTick() {
        return false;
    }
}
