/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract base class for all GUI implementations.
 *
 * <p>AbstractGUI provides the foundational functionality for creating
 * inventory-based GUIs, including navigation, state management, and
 * slot handling.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Navigation stack for back/forward navigation</li>
 *   <li>State management with typed keys</li>
 *   <li>Slot-based item and click handling</li>
 *   <li>Lifecycle callbacks for customization</li>
 * </ul>
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class MyGUI extends AbstractGUI {
 *
 *     public MyGUI(UnifiedPlayer player) {
 *         super(player, Component.text("My GUI"), 54);
 *         setupSlots();
 *     }
 *
 *     private void setupSlots() {
 *         setSlot(0, Slot.of(myItem).onClick(this::handleClick));
 *     }
 *
 *     private ClickResult handleClick(ClickContext ctx) {
 *         ctx.getPlayer().sendMessage(Component.text("Clicked!"));
 *         return ClickResult.DENY;
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Slot
 * @see ClickContext
 * @see GUIContext
 */
public abstract class AbstractGUI {

    /**
     * The player viewing this GUI.
     */
    protected final UnifiedPlayer viewer;

    /**
     * The title of the GUI.
     */
    protected final Component title;

    /**
     * The size of the GUI in slots (must be multiple of 9, max 54).
     */
    protected final int size;

    /**
     * The slots in this GUI.
     */
    protected final Map<Integer, Slot> slots;

    /**
     * State storage for this GUI.
     */
    protected final Map<StateKey<?>, Object> state;

    /**
     * Navigation stack for back navigation.
     */
    protected final Deque<AbstractGUI> navigationStack;

    /**
     * The parent GUI (for back navigation).
     */
    protected AbstractGUI parent;

    /**
     * Whether this GUI is currently open.
     */
    protected volatile boolean isOpen;

    /**
     * Creates a new AbstractGUI.
     *
     * @param viewer the player who will view this GUI
     * @param title  the title of the GUI
     * @param size   the size in slots (must be multiple of 9, 9-54)
     * @throws NullPointerException     if viewer or title is null
     * @throws IllegalArgumentException if size is invalid
     */
    protected AbstractGUI(@NotNull UnifiedPlayer viewer, @NotNull Component title, int size) {
        Objects.requireNonNull(viewer, "viewer cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        validateSize(size);

        this.viewer = viewer;
        this.title = title;
        this.size = size;
        this.slots = new HashMap<>();
        this.state = new HashMap<>();
        this.navigationStack = new ArrayDeque<>();
        this.isOpen = false;
    }

    /**
     * Validates that the size is valid for a GUI.
     */
    private void validateSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(
                    "Size must be a multiple of 9 between 9 and 54, was: " + size);
        }
    }

    // ==================== Slot Management ====================

    /**
     * Sets a slot in the GUI.
     *
     * @param index the slot index
     * @param slot  the slot to set
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public void setSlot(int index, @NotNull Slot slot) {
        validateSlotIndex(index);
        Objects.requireNonNull(slot, "slot cannot be null");
        slots.put(index, slot);
    }

    /**
     * Gets the slot at the specified index.
     *
     * @param index the slot index
     * @return an Optional containing the slot, or empty if no slot at that index
     */
    @NotNull
    public Optional<Slot> getSlot(int index) {
        return Optional.ofNullable(slots.get(index));
    }

    /**
     * Clears a slot.
     *
     * @param index the slot index to clear
     */
    public void clearSlot(int index) {
        slots.remove(index);
    }

    /**
     * Clears all slots.
     */
    public void clearAllSlots() {
        slots.clear();
    }

    /**
     * Validates that a slot index is within range.
     */
    protected void validateSlotIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                    "Slot index " + index + " out of range [0, " + size + ")");
        }
    }

    // ==================== State Management ====================

    /**
     * Sets a state value.
     *
     * @param key   the state key
     * @param value the value to set
     * @param <T>   the value type
     */
    public <T> void setState(@NotNull StateKey<T> key, @Nullable T value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
    }

    /**
     * Sets a state value using a string key.
     *
     * @param key   the state key
     * @param value the value to set
     */
    public void setState(@NotNull String key, @Nullable Object value) {
        setState(StateKey.of(key, Object.class), value);
    }

    /**
     * Gets a state value.
     *
     * @param key the state key
     * @param <T> the value type
     * @return an Optional containing the value, or empty if not set
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getState(@NotNull StateKey<T> key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable((T) state.get(key));
    }

    /**
     * Gets a state value with a default.
     *
     * @param key          the state key
     * @param defaultValue the default value if not set
     * @param <T>          the value type
     * @return the value or the default
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getStateOrDefault(@NotNull StateKey<T> key, @NotNull T defaultValue) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");
        T value = (T) state.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if a state key is set.
     *
     * @param key the state key
     * @return true if the key has a value
     */
    public boolean hasState(@NotNull StateKey<?> key) {
        return state.containsKey(key);
    }

    /**
     * Clears all state.
     */
    public void clearState() {
        state.clear();
    }

    // ==================== Navigation ====================

    /**
     * Navigates to another GUI, pushing this GUI onto the back stack.
     *
     * @param gui the GUI to navigate to
     */
    public void navigateTo(@NotNull AbstractGUI gui) {
        Objects.requireNonNull(gui, "gui cannot be null");
        gui.parent = this;
        gui.open();
    }

    /**
     * Navigates back to the previous GUI.
     */
    public void navigateBack() {
        if (parent != null) {
            close();
            parent.open();
        } else {
            close();
        }
    }

    /**
     * Checks if back navigation is available.
     *
     * @return true if there is a parent GUI to navigate back to
     */
    public boolean canNavigateBack() {
        return parent != null;
    }

    /**
     * Gets the parent GUI.
     *
     * @return an Optional containing the parent GUI
     */
    @NotNull
    public Optional<AbstractGUI> getParent() {
        return Optional.ofNullable(parent);
    }

    // ==================== Lifecycle ====================

    /**
     * Opens the GUI for the viewer.
     *
     * <p>Subclasses should implement the actual inventory opening logic
     * and call this method to set the open state.
     */
    public void open() {
        isOpen = true;
        onOpen();
    }

    /**
     * Closes the GUI.
     *
     * <p>Subclasses should implement the actual inventory closing logic
     * and call this method to set the closed state.
     */
    public void close() {
        isOpen = false;
        onClose();
    }

    /**
     * Refreshes the entire GUI.
     */
    public abstract void refresh();

    /**
     * Updates a specific slot.
     *
     * @param slotIndex the slot to update
     */
    public abstract void updateSlot(int slotIndex);

    /**
     * Called when the GUI is opened.
     *
     * <p>Override this method to perform initialization when the GUI opens.
     */
    protected void onOpen() {
        // Default implementation does nothing
    }

    /**
     * Called when the GUI is closed.
     *
     * <p>Override this method to perform cleanup when the GUI closes.
     */
    protected void onClose() {
        // Default implementation does nothing
    }

    // ==================== Getters ====================

    /**
     * Returns the player viewing this GUI.
     *
     * @return the viewer
     */
    @NotNull
    public UnifiedPlayer getViewer() {
        return viewer;
    }

    /**
     * Returns the title of this GUI.
     *
     * @return the title
     */
    @NotNull
    public Component getTitle() {
        return title;
    }

    /**
     * Returns the size of this GUI in slots.
     *
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns whether this GUI is currently open.
     *
     * @return true if open
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Creates a GUIContext for this GUI.
     *
     * @return a new GUIContext
     */
    @NotNull
    public GUIContext createContext() {
        return GUIContext.of(viewer, this);
    }
}
