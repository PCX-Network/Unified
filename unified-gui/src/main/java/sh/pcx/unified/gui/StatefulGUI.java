/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Interface for GUIs that maintain state across lifecycle events.
 *
 * <p>StatefulGUI provides a contract for GUIs that need to store and
 * manage state data. This is useful for GUIs with dynamic content,
 * pagination, search filters, or other interactive features.
 *
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class PlayerListGUI extends AbstractGUI implements StatefulGUI {
 *
 *     private static final StateKey<Integer> PAGE = StateKey.ofInt("page");
 *     private static final StateKey<String> SEARCH = StateKey.ofString("search");
 *     private static final StateKey<Boolean> SHOW_OFFLINE = StateKey.ofBoolean("showOffline");
 *
 *     public PlayerListGUI(GUIContext context) {
 *         super(context, Layout.CHEST_54);
 *         setTitle(Component.text("Player List"));
 *
 *         // Initialize default state
 *         setState(PAGE, 1);
 *         setState(SEARCH, "");
 *         setState(SHOW_OFFLINE, false);
 *     }
 *
 *     @Override
 *     protected void setup() {
 *         int page = getState(PAGE, 1);
 *         String search = getState(SEARCH, "");
 *         boolean showOffline = getState(SHOW_OFFLINE, false);
 *
 *         // Build GUI based on state
 *         List<PlayerData> players = filterPlayers(search, showOffline);
 *         List<PlayerData> pageContent = paginate(players, page, 45);
 *
 *         for (int i = 0; i < pageContent.size(); i++) {
 *             setSlot(i, createPlayerButton(pageContent.get(i)));
 *         }
 *
 *         // Pagination controls
 *         setSlot(45, previousPageButton(page > 1));
 *         setSlot(53, nextPageButton(hasNextPage(players.size(), page)));
 *     }
 *
 *     private Slot previousPageButton(boolean enabled) {
 *         return Slot.of(previousArrow())
 *             .visible(enabled)
 *             .onClick(click -> {
 *                 setState(PAGE, getState(PAGE, 1) - 1);
 *                 return ClickResult.REFRESH;
 *             });
 *     }
 * }
 * }</pre>
 *
 * <h2>State Persistence</h2>
 * <p>State is maintained for the lifetime of the GUI instance. When the
 * GUI is closed and reopened, a new instance is created with fresh state
 * unless the GUIManager is configured to preserve state.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see GUIState
 * @see StateKey
 * @see AbstractGUI
 */
public interface StatefulGUI {

    /**
     * Returns the state container for this GUI.
     *
     * @return the GUI state
     */
    @NotNull
    GUIState getState();

    /**
     * Sets a value in the GUI state.
     *
     * @param <T>   the value type
     * @param key   the state key
     * @param value the value to store
     */
    default <T> void setState(@NotNull StateKey<T> key, @NotNull T value) {
        getState().set(key, value);
    }

    /**
     * Gets a value from the GUI state.
     *
     * @param <T> the value type
     * @param key the state key
     * @return an Optional containing the value if present
     */
    @NotNull
    default <T> Optional<T> getState(@NotNull StateKey<T> key) {
        return getState().get(key);
    }

    /**
     * Gets a value from the GUI state, or returns a default.
     *
     * @param <T>          the value type
     * @param key          the state key
     * @param defaultValue the default value if not present
     * @return the stored value or the default
     */
    @NotNull
    default <T> T getState(@NotNull StateKey<T> key, @NotNull T defaultValue) {
        return getState().getOrDefault(key, defaultValue);
    }

    /**
     * Checks if a value exists in the GUI state.
     *
     * @param key the state key
     * @return true if a value is stored for the key
     */
    default boolean hasState(@NotNull StateKey<?> key) {
        return getState().has(key);
    }

    /**
     * Removes a value from the GUI state.
     *
     * @param <T> the value type
     * @param key the state key
     * @return an Optional containing the removed value if it existed
     */
    @NotNull
    default <T> Optional<T> removeState(@NotNull StateKey<T> key) {
        return getState().remove(key);
    }

    /**
     * Clears all values from the GUI state.
     */
    default void clearState() {
        getState().clear();
    }

    /**
     * Increments an integer state value and returns the new value.
     *
     * @param key    the state key for an integer value
     * @param amount the amount to increment by
     * @return the new value after incrementing
     */
    default int incrementState(@NotNull StateKey<Integer> key, int amount) {
        return getState().increment(key, amount);
    }

    /**
     * Toggles a boolean state value and returns the new value.
     *
     * @param key the state key for a boolean value
     * @return the new value after toggling
     */
    default boolean toggleState(@NotNull StateKey<Boolean> key) {
        return getState().toggle(key);
    }
}
