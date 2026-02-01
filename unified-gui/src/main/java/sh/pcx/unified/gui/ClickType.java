/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

/**
 * Enumeration of all possible click types in a GUI inventory.
 *
 * <p>This enum provides a unified representation of click actions that can
 * occur in inventory GUIs, abstracting platform-specific implementations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Check click type in handler
 * slot.onClick(click -> {
 *     switch (click.getClickType()) {
 *         case LEFT -> handleLeftClick();
 *         case RIGHT -> handleRightClick();
 *         case SHIFT_LEFT -> handleBulkAction();
 *         case MIDDLE -> handleCreativeAction();
 *         case DROP -> handleDrop();
 *         default -> { }
 *     }
 *     return ClickResult.DENY;
 * });
 *
 * // Check for shift click
 * if (click.getClickType().isShiftClick()) {
 *     // Handle bulk operation
 * }
 *
 * // Check for any left click variant
 * if (click.getClickType().isLeftClick()) {
 *     // Handle selection
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickContext
 * @see ClickHandler
 */
public enum ClickType {

    /**
     * Standard left mouse click.
     */
    LEFT(true, false, false, false),

    /**
     * Standard right mouse click.
     */
    RIGHT(false, true, false, false),

    /**
     * Shift + left mouse click.
     */
    SHIFT_LEFT(true, false, true, false),

    /**
     * Shift + right mouse click.
     */
    SHIFT_RIGHT(false, true, true, false),

    /**
     * Middle mouse click (mouse wheel).
     *
     * <p>In creative mode, this typically clones the clicked item.
     */
    MIDDLE(false, false, false, false),

    /**
     * Drop key (Q) pressed while hovering over a slot.
     */
    DROP(false, false, false, true),

    /**
     * Control + drop key (Ctrl+Q) pressed while hovering over a slot.
     *
     * <p>This typically drops the entire stack.
     */
    CONTROL_DROP(false, false, false, true),

    /**
     * Double left click.
     *
     * <p>This typically collects all matching items to the cursor.
     */
    DOUBLE_CLICK(true, false, false, false),

    /**
     * Number key pressed (1-9) to swap with hotbar slot.
     */
    NUMBER_KEY(false, false, false, false),

    /**
     * Offhand swap key (F) pressed.
     */
    OFFHAND_SWAP(false, false, false, false),

    /**
     * Creative middle click to clone item.
     */
    CREATIVE_CLONE(false, false, false, false),

    /**
     * Left click while dragging items.
     */
    DRAG_LEFT(true, false, false, false),

    /**
     * Right click while dragging items.
     */
    DRAG_RIGHT(false, true, false, false),

    /**
     * Middle click while dragging items.
     */
    DRAG_MIDDLE(false, false, false, false),

    /**
     * Unknown or unsupported click type.
     */
    UNKNOWN(false, false, false, false);

    private final boolean leftClick;
    private final boolean rightClick;
    private final boolean shiftClick;
    private final boolean dropClick;

    /**
     * Creates a new click type with the specified properties.
     *
     * @param leftClick  whether this is a left click variant
     * @param rightClick whether this is a right click variant
     * @param shiftClick whether this is a shift click variant
     * @param dropClick  whether this is a drop click variant
     */
    ClickType(boolean leftClick, boolean rightClick, boolean shiftClick, boolean dropClick) {
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.shiftClick = shiftClick;
        this.dropClick = dropClick;
    }

    /**
     * Checks if this is any variant of a left click.
     *
     * @return true if this is a left click (including shift+left, double, drag left)
     */
    public boolean isLeftClick() {
        return leftClick;
    }

    /**
     * Checks if this is any variant of a right click.
     *
     * @return true if this is a right click (including shift+right, drag right)
     */
    public boolean isRightClick() {
        return rightClick;
    }

    /**
     * Checks if this is any variant of a shift click.
     *
     * @return true if this is a shift click (shift+left or shift+right)
     */
    public boolean isShiftClick() {
        return shiftClick;
    }

    /**
     * Checks if this is any variant of a drop action.
     *
     * @return true if this is a drop action (Q or Ctrl+Q)
     */
    public boolean isDropClick() {
        return dropClick;
    }

    /**
     * Checks if this is a keyboard-based action rather than mouse click.
     *
     * @return true if triggered by keyboard (number keys, drop, offhand swap)
     */
    public boolean isKeyboardAction() {
        return this == NUMBER_KEY || this == OFFHAND_SWAP || dropClick;
    }

    /**
     * Checks if this is a drag operation.
     *
     * @return true if this is a drag action
     */
    public boolean isDragAction() {
        return this == DRAG_LEFT || this == DRAG_RIGHT || this == DRAG_MIDDLE;
    }

    /**
     * Checks if this is a creative mode specific action.
     *
     * @return true if this requires creative mode
     */
    public boolean isCreativeAction() {
        return this == CREATIVE_CLONE;
    }
}
