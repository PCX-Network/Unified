/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;

/**
 * Result of a click action in a GUI, determining subsequent behavior.
 *
 * <p>Click handlers return a ClickResult to indicate what should happen
 * after processing the click. This provides fine-grained control over
 * GUI behavior.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Deny the click (prevent item pickup - most common)
 * slot.onClick(click -> ClickResult.DENY);
 *
 * // Allow the click (permit item manipulation)
 * slot.onClick(click -> ClickResult.ALLOW);
 *
 * // Close the GUI after click
 * slot.onClick(click -> {
 *     executeAction();
 *     return ClickResult.CLOSE;
 * });
 *
 * // Refresh the GUI to show updated content
 * slot.onClick(click -> {
 *     toggleSetting();
 *     return ClickResult.REFRESH;
 * });
 *
 * // Refresh a specific slot only
 * slot.onClick(click -> {
 *     incrementCounter();
 *     return ClickResult.refreshSlot(click.getSlot());
 * });
 *
 * // Navigate to another GUI
 * slot.onClick(click -> {
 *     click.navigateTo(new SettingsGUI(click.context()));
 *     return ClickResult.DENY;
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickHandler
 * @see ClickContext
 */
public sealed interface ClickResult {

    /**
     * Allow the click action (permits item pickup/placement).
     *
     * <p>Use this when you want the player to be able to manipulate
     * items in the slot, such as in an item editor GUI.
     */
    ClickResult ALLOW = new Allow();

    /**
     * Deny the click action (prevents item pickup/placement).
     *
     * <p>This is the most common result for GUI buttons and
     * should be the default for most interactive elements.
     */
    ClickResult DENY = new Deny();

    /**
     * Close the GUI after processing the click.
     *
     * <p>Use this for actions that should dismiss the GUI,
     * such as confirmation buttons or close buttons.
     */
    ClickResult CLOSE = new Close();

    /**
     * Refresh the entire GUI after processing the click.
     *
     * <p>Use this when the click changes state that affects
     * multiple slots or the overall GUI appearance.
     */
    ClickResult REFRESH = new Refresh();

    /**
     * Checks if this result allows the inventory action.
     *
     * @return true if the action should be allowed
     */
    boolean isAllowed();

    /**
     * Checks if this result should close the GUI.
     *
     * @return true if the GUI should be closed
     */
    boolean shouldClose();

    /**
     * Checks if this result should refresh the GUI.
     *
     * @return true if the GUI should be refreshed
     */
    boolean shouldRefresh();

    /**
     * Returns the specific slot to refresh, or -1 for all slots.
     *
     * @return the slot to refresh, or -1 for full refresh
     */
    int getRefreshSlot();

    /**
     * Creates a result that refreshes a specific slot only.
     *
     * <p>This is more efficient than a full refresh when only
     * one slot needs to be updated.
     *
     * @param slot the slot to refresh
     * @return a ClickResult that refreshes the specified slot
     */
    @NotNull
    static ClickResult refreshSlot(int slot) {
        return new RefreshSlot(slot);
    }

    /**
     * Allow result - permits the inventory action.
     */
    record Allow() implements ClickResult {
        @Override
        public boolean isAllowed() {
            return true;
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public boolean shouldRefresh() {
            return false;
        }

        @Override
        public int getRefreshSlot() {
            return -1;
        }
    }

    /**
     * Deny result - cancels the inventory action.
     */
    record Deny() implements ClickResult {
        @Override
        public boolean isAllowed() {
            return false;
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public boolean shouldRefresh() {
            return false;
        }

        @Override
        public int getRefreshSlot() {
            return -1;
        }
    }

    /**
     * Close result - closes the GUI.
     */
    record Close() implements ClickResult {
        @Override
        public boolean isAllowed() {
            return false;
        }

        @Override
        public boolean shouldClose() {
            return true;
        }

        @Override
        public boolean shouldRefresh() {
            return false;
        }

        @Override
        public int getRefreshSlot() {
            return -1;
        }
    }

    /**
     * Refresh result - refreshes all slots.
     */
    record Refresh() implements ClickResult {
        @Override
        public boolean isAllowed() {
            return false;
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public boolean shouldRefresh() {
            return true;
        }

        @Override
        public int getRefreshSlot() {
            return -1;
        }
    }

    /**
     * Refresh slot result - refreshes a specific slot.
     */
    record RefreshSlot(int slot) implements ClickResult {
        @Override
        public boolean isAllowed() {
            return false;
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public boolean shouldRefresh() {
            return true;
        }

        @Override
        public int getRefreshSlot() {
            return slot;
        }
    }
}
