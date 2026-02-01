/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Context object passed to click handlers containing all click event details.
 *
 * <p>ClickContext provides comprehensive information about a click event,
 * including the player, slot, click type, items involved, and access to
 * the GUI and navigation system.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * slot.onClick(click -> {
 *     // Get basic click info
 *     UnifiedPlayer player = click.getPlayer();
 *     int slot = click.getSlot();
 *     ClickType type = click.getClickType();
 *
 *     // Check what items are involved
 *     Optional<UnifiedItemStack> clicked = click.getClickedItem();
 *     Optional<UnifiedItemStack> cursor = click.getCursorItem();
 *
 *     // Log the action
 *     if (clicked.isPresent()) {
 *         logger.info(player.getName() + " clicked on " + clicked.get().getType());
 *     }
 *
 *     // Access the GUI for state/navigation
 *     click.context().gui().setState("lastClick", slot);
 *
 *     // Handle different click types
 *     if (click.isLeftClick()) {
 *         return handleLeftClick();
 *     } else if (click.isRightClick()) {
 *         return handleRightClick();
 *     }
 *
 *     return ClickResult.DENY;
 * });
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <pre>{@code
 * // Navigate to another GUI
 * click.navigateTo(new SettingsGUI(click.context()));
 *
 * // Go back to previous GUI
 * click.navigateBack();
 *
 * // Close the GUI
 * click.close();
 *
 * // Refresh the current GUI
 * click.refresh();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickHandler
 * @see ClickResult
 * @see ClickType
 */
public final class ClickContext {

    private final GUIContext guiContext;
    private final int slot;
    private final ClickType clickType;
    private final UnifiedItemStack clickedItem;
    private final UnifiedItemStack cursorItem;
    private final int hotbarButton;
    private final boolean isTopInventory;

    /**
     * Creates a new click context.
     *
     * @param guiContext     the parent GUI context
     * @param slot           the slot that was clicked
     * @param clickType      the type of click
     * @param clickedItem    the item in the clicked slot (may be null)
     * @param cursorItem     the item on the cursor (may be null)
     * @param hotbarButton   the hotbar button pressed (-1 if none)
     * @param isTopInventory whether the click was in the top inventory
     */
    public ClickContext(
            @NotNull GUIContext guiContext,
            int slot,
            @NotNull ClickType clickType,
            @Nullable UnifiedItemStack clickedItem,
            @Nullable UnifiedItemStack cursorItem,
            int hotbarButton,
            boolean isTopInventory
    ) {
        this.guiContext = Objects.requireNonNull(guiContext, "guiContext cannot be null");
        this.slot = slot;
        this.clickType = Objects.requireNonNull(clickType, "clickType cannot be null");
        this.clickedItem = clickedItem;
        this.cursorItem = cursorItem;
        this.hotbarButton = hotbarButton;
        this.isTopInventory = isTopInventory;
    }

    /**
     * Returns the parent GUI context.
     *
     * @return the GUI context
     */
    @NotNull
    public GUIContext context() {
        return guiContext;
    }

    /**
     * Returns the player who clicked.
     *
     * @return the player
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return guiContext.getPlayer();
    }

    /**
     * Returns the slot index that was clicked.
     *
     * @return the slot index
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the type of click performed.
     *
     * @return the click type
     */
    @NotNull
    public ClickType getClickType() {
        return clickType;
    }

    /**
     * Returns the item that was in the clicked slot.
     *
     * @return an Optional containing the clicked item, or empty if the slot was empty
     */
    @NotNull
    public Optional<UnifiedItemStack> getClickedItem() {
        return Optional.ofNullable(clickedItem);
    }

    /**
     * Returns the item on the player's cursor when they clicked.
     *
     * @return an Optional containing the cursor item, or empty if no item on cursor
     */
    @NotNull
    public Optional<UnifiedItemStack> getCursorItem() {
        return Optional.ofNullable(cursorItem);
    }

    /**
     * Returns the hotbar button pressed, if any.
     *
     * <p>This is relevant for NUMBER_KEY click types where the player
     * pressed 1-9 to swap with a hotbar slot.
     *
     * @return the hotbar button (0-8), or -1 if not applicable
     */
    public int getHotbarButton() {
        return hotbarButton;
    }

    /**
     * Checks if the click was in the top inventory (the GUI).
     *
     * <p>Returns false if the player clicked in their own inventory
     * portion of the GUI screen.
     *
     * @return true if clicked in the GUI portion
     */
    public boolean isTopInventory() {
        return isTopInventory;
    }

    /**
     * Checks if the click was in the player's inventory portion.
     *
     * @return true if clicked in the player inventory
     */
    public boolean isBottomInventory() {
        return !isTopInventory;
    }

    /**
     * Checks if this was a left click (any variant).
     *
     * @return true if left click
     */
    public boolean isLeftClick() {
        return clickType.isLeftClick();
    }

    /**
     * Checks if this was a right click (any variant).
     *
     * @return true if right click
     */
    public boolean isRightClick() {
        return clickType.isRightClick();
    }

    /**
     * Checks if this was a shift click (any variant).
     *
     * @return true if shift click
     */
    public boolean isShiftClick() {
        return clickType.isShiftClick();
    }

    /**
     * Checks if the clicked slot was empty.
     *
     * @return true if the slot was empty
     */
    public boolean isSlotEmpty() {
        return clickedItem == null || clickedItem.isEmpty();
    }

    /**
     * Checks if the cursor had an item when clicking.
     *
     * @return true if the cursor had an item
     */
    public boolean hasCursorItem() {
        return cursorItem != null && !cursorItem.isEmpty();
    }

    /**
     * Returns the current GUI.
     *
     * @return the GUI, or empty if not in a GUI
     */
    @NotNull
    public Optional<AbstractGUI> getGui() {
        return guiContext.getGui();
    }

    /**
     * Navigates to another GUI, pushing the current GUI onto the back stack.
     *
     * @param gui the GUI to navigate to
     */
    public void navigateTo(@NotNull AbstractGUI gui) {
        getGui().ifPresent(current -> current.navigateTo(gui));
    }

    /**
     * Navigates back to the previous GUI in the back stack.
     */
    public void navigateBack() {
        getGui().ifPresent(AbstractGUI::navigateBack);
    }

    /**
     * Closes the current GUI.
     */
    public void close() {
        getGui().ifPresent(AbstractGUI::close);
    }

    /**
     * Refreshes the current GUI.
     */
    public void refresh() {
        getGui().ifPresent(AbstractGUI::refresh);
    }

    /**
     * Refreshes a specific slot in the current GUI.
     *
     * @param slotIndex the slot to refresh
     */
    public void refreshSlot(int slotIndex) {
        getGui().ifPresent(gui -> gui.updateSlot(slotIndex));
    }

    /**
     * Sends a message to the player who clicked.
     *
     * @param message the message to send
     */
    public void sendMessage(@NotNull net.kyori.adventure.text.Component message) {
        getPlayer().sendMessage(message);
    }

    /**
     * Plays a sound to the player who clicked.
     *
     * @param sound  the sound name/key
     * @param volume the volume (0.0 to 1.0)
     * @param pitch  the pitch (0.5 to 2.0)
     */
    public void playSound(@NotNull String sound, float volume, float pitch) {
        getPlayer().playSound(sound, volume, pitch);
    }

    /**
     * Builder for creating ClickContext instances.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ClickContext instances.
     */
    public static final class Builder {

        private GUIContext guiContext;
        private int slot = 0;
        private ClickType clickType = ClickType.UNKNOWN;
        private UnifiedItemStack clickedItem;
        private UnifiedItemStack cursorItem;
        private int hotbarButton = -1;
        private boolean isTopInventory = true;

        private Builder() {
        }

        /**
         * Sets the GUI context.
         *
         * @param guiContext the GUI context
         * @return this builder
         */
        @NotNull
        public Builder guiContext(@NotNull GUIContext guiContext) {
            this.guiContext = guiContext;
            return this;
        }

        /**
         * Sets the clicked slot.
         *
         * @param slot the slot index
         * @return this builder
         */
        @NotNull
        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        /**
         * Sets the click type.
         *
         * @param clickType the click type
         * @return this builder
         */
        @NotNull
        public Builder clickType(@NotNull ClickType clickType) {
            this.clickType = clickType;
            return this;
        }

        /**
         * Sets the item in the clicked slot.
         *
         * @param clickedItem the clicked item
         * @return this builder
         */
        @NotNull
        public Builder clickedItem(@Nullable UnifiedItemStack clickedItem) {
            this.clickedItem = clickedItem;
            return this;
        }

        /**
         * Sets the item on the cursor.
         *
         * @param cursorItem the cursor item
         * @return this builder
         */
        @NotNull
        public Builder cursorItem(@Nullable UnifiedItemStack cursorItem) {
            this.cursorItem = cursorItem;
            return this;
        }

        /**
         * Sets the hotbar button pressed.
         *
         * @param hotbarButton the hotbar button (0-8 or -1)
         * @return this builder
         */
        @NotNull
        public Builder hotbarButton(int hotbarButton) {
            this.hotbarButton = hotbarButton;
            return this;
        }

        /**
         * Sets whether the click was in the top inventory.
         *
         * @param isTopInventory true if in top inventory
         * @return this builder
         */
        @NotNull
        public Builder topInventory(boolean isTopInventory) {
            this.isTopInventory = isTopInventory;
            return this;
        }

        /**
         * Builds the ClickContext.
         *
         * @return the built ClickContext
         * @throws NullPointerException if guiContext is null
         */
        @NotNull
        public ClickContext build() {
            return new ClickContext(
                    guiContext,
                    slot,
                    clickType,
                    clickedItem,
                    cursorItem,
                    hotbarButton,
                    isTopInventory
            );
        }
    }
}
