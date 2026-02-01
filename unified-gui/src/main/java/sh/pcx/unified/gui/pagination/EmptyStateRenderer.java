/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Functional interface for rendering empty states in paginated GUIs.
 *
 * <p>EmptyStateRenderer allows customization of how the empty state is
 * displayed when a paginated GUI has no items to show.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Center the empty state icon
 * EmptyStateRenderer centered = (state, slots, setSlot) -> {
 *     int[] slotArray = slots.getSlots();
 *     if (slotArray.length > 0) {
 *         int centerSlot = slotArray[slotArray.length / 2];
 *         setSlot.accept(centerSlot, state.toItemStack());
 *     }
 * };
 *
 * // Fill all slots with a pattern
 * EmptyStateRenderer filled = (state, slots, setSlot) -> {
 *     UnifiedItemStack item = state.toItemStack();
 *     for (int slot : slots.getSlots()) {
 *         setSlot.accept(slot, item);
 *     }
 * };
 *
 * // Display in a specific row
 * EmptyStateRenderer rowBased = (state, slots, setSlot) -> {
 *     int[] slotArray = slots.getSlots();
 *     // Place in the middle row
 *     int middleStart = slotArray.length / 2 - 4;
 *     for (int i = 0; i < 9 && middleStart + i < slotArray.length; i++) {
 *         if (i == 4) { // Center of row
 *             setSlot.accept(slotArray[middleStart + i], state.toItemStack());
 *         }
 *     }
 * };
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EmptyState
 * @see PaginatedGUI
 * @see ContentSlots
 */
@FunctionalInterface
public interface EmptyStateRenderer {

    /**
     * Renders the empty state into the GUI.
     *
     * @param emptyState   the empty state configuration
     * @param contentSlots the content slots configuration
     * @param setSlot      consumer to set items in slots (slot index, item)
     */
    void render(
            @NotNull EmptyState emptyState,
            @NotNull ContentSlots contentSlots,
            @NotNull BiConsumer<Integer, UnifiedItemStack> setSlot
    );

    /**
     * Creates a renderer that centers the empty state icon.
     *
     * @return a centered renderer
     */
    @NotNull
    static EmptyStateRenderer centered() {
        return (state, slots, setSlot) -> {
            int[] slotArray = slots.getSlots();
            if (slotArray.length > 0) {
                int centerSlot = slotArray[slotArray.length / 2];
                setSlot.accept(centerSlot, state.toItemStack());
            }
        };
    }

    /**
     * Creates a renderer that fills all content slots with the empty state icon.
     *
     * @return a fill renderer
     */
    @NotNull
    static EmptyStateRenderer fill() {
        return (state, slots, setSlot) -> {
            UnifiedItemStack item = state.toItemStack();
            for (int slot : slots.getSlots()) {
                setSlot.accept(slot, item);
            }
        };
    }

    /**
     * Creates a renderer that places the icon at a specific slot offset.
     *
     * @param slotOffset the offset within the content slots
     * @return a slot-specific renderer
     */
    @NotNull
    static EmptyStateRenderer atSlot(int slotOffset) {
        return (state, slots, setSlot) -> {
            int[] slotArray = slots.getSlots();
            if (slotOffset >= 0 && slotOffset < slotArray.length) {
                setSlot.accept(slotArray[slotOffset], state.toItemStack());
            }
        };
    }
}
