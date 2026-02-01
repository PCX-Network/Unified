/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of standard inventory layouts with their sizes and dimensions.
 *
 * <p>This enum provides predefined layouts for common Minecraft inventory types,
 * including chest inventories of various sizes, hoppers, and dispensers.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Use a standard chest layout
 * Layout layout = Layout.CHEST_27;
 * int size = layout.getSize(); // 27
 * int rows = layout.getRows(); // 3
 * int columns = layout.getColumns(); // 9
 *
 * // Check if a slot is in the bottom row
 * boolean isBottomRow = slot >= layout.getSize() - layout.getColumns();
 *
 * // Create a GUI with this layout
 * public class MyGUI extends AbstractGUI {
 *     public MyGUI(GUIContext context) {
 *         super(context, Layout.CHEST_54);
 *     }
 * }
 * }</pre>
 *
 * <h2>Available Layouts</h2>
 * <table>
 *   <tr><th>Layout</th><th>Size</th><th>Rows</th><th>Columns</th><th>Description</th></tr>
 *   <tr><td>CHEST_9</td><td>9</td><td>1</td><td>9</td><td>Single row chest</td></tr>
 *   <tr><td>CHEST_18</td><td>18</td><td>2</td><td>9</td><td>Two row chest</td></tr>
 *   <tr><td>CHEST_27</td><td>27</td><td>3</td><td>9</td><td>Standard single chest</td></tr>
 *   <tr><td>CHEST_36</td><td>36</td><td>4</td><td>9</td><td>Four row chest</td></tr>
 *   <tr><td>CHEST_45</td><td>45</td><td>5</td><td>9</td><td>Five row chest</td></tr>
 *   <tr><td>CHEST_54</td><td>54</td><td>6</td><td>9</td><td>Double chest</td></tr>
 *   <tr><td>HOPPER</td><td>5</td><td>1</td><td>5</td><td>Hopper inventory</td></tr>
 *   <tr><td>DISPENSER</td><td>9</td><td>3</td><td>3</td><td>Dispenser/dropper inventory</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AbstractGUI
 */
public enum Layout {

    /**
     * Single row chest (9 slots).
     */
    CHEST_9(9, 1, 9, InventoryType.CHEST),

    /**
     * Two row chest (18 slots).
     */
    CHEST_18(18, 2, 9, InventoryType.CHEST),

    /**
     * Standard single chest (27 slots, 3 rows).
     */
    CHEST_27(27, 3, 9, InventoryType.CHEST),

    /**
     * Four row chest (36 slots).
     */
    CHEST_36(36, 4, 9, InventoryType.CHEST),

    /**
     * Five row chest (45 slots).
     */
    CHEST_45(45, 5, 9, InventoryType.CHEST),

    /**
     * Double chest (54 slots, 6 rows).
     */
    CHEST_54(54, 6, 9, InventoryType.CHEST),

    /**
     * Hopper inventory (5 slots in a single row).
     */
    HOPPER(5, 1, 5, InventoryType.HOPPER),

    /**
     * Dispenser/dropper inventory (9 slots in a 3x3 grid).
     */
    DISPENSER(9, 3, 3, InventoryType.DISPENSER);

    private final int size;
    private final int rows;
    private final int columns;
    private final InventoryType inventoryType;

    /**
     * Creates a new layout with the specified dimensions.
     *
     * @param size          the total number of slots
     * @param rows          the number of rows
     * @param columns       the number of columns
     * @param inventoryType the underlying inventory type
     */
    Layout(int size, int rows, int columns, @NotNull InventoryType inventoryType) {
        this.size = size;
        this.rows = rows;
        this.columns = columns;
        this.inventoryType = inventoryType;
    }

    /**
     * Returns the total number of slots in this layout.
     *
     * @return the number of slots
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the number of rows in this layout.
     *
     * @return the number of rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of columns in this layout.
     *
     * @return the number of columns
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the underlying inventory type for this layout.
     *
     * @return the inventory type
     */
    @NotNull
    public InventoryType getInventoryType() {
        return inventoryType;
    }

    /**
     * Converts a row and column position to a slot index.
     *
     * <p>Both row and column are zero-indexed.
     *
     * @param row    the row (0-indexed)
     * @param column the column (0-indexed)
     * @return the slot index
     * @throws IllegalArgumentException if the position is out of bounds
     */
    public int toSlot(int row, int column) {
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException("Row out of bounds: " + row + " (max: " + (rows - 1) + ")");
        }
        if (column < 0 || column >= columns) {
            throw new IllegalArgumentException("Column out of bounds: " + column + " (max: " + (columns - 1) + ")");
        }
        return row * columns + column;
    }

    /**
     * Returns the row for a given slot index.
     *
     * @param slot the slot index
     * @return the row (0-indexed)
     * @throws IllegalArgumentException if the slot is out of bounds
     */
    public int getRow(int slot) {
        validateSlot(slot);
        return slot / columns;
    }

    /**
     * Returns the column for a given slot index.
     *
     * @param slot the slot index
     * @return the column (0-indexed)
     * @throws IllegalArgumentException if the slot is out of bounds
     */
    public int getColumn(int slot) {
        validateSlot(slot);
        return slot % columns;
    }

    /**
     * Checks if the given slot is on the border of the layout.
     *
     * @param slot the slot index
     * @return true if the slot is on the border
     * @throws IllegalArgumentException if the slot is out of bounds
     */
    public boolean isBorder(int slot) {
        validateSlot(slot);
        int row = getRow(slot);
        int col = getColumn(slot);
        return row == 0 || row == rows - 1 || col == 0 || col == columns - 1;
    }

    /**
     * Checks if the given slot is in the center area (not on border).
     *
     * @param slot the slot index
     * @return true if the slot is in the center
     * @throws IllegalArgumentException if the slot is out of bounds
     */
    public boolean isCenter(int slot) {
        return !isBorder(slot);
    }

    /**
     * Validates that a slot index is within bounds.
     *
     * @param slot the slot index to validate
     * @throws IllegalArgumentException if the slot is out of bounds
     */
    public void validateSlot(int slot) {
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Slot out of bounds: " + slot + " (max: " + (size - 1) + ")");
        }
    }

    /**
     * Returns a layout for the specified number of rows.
     *
     * @param rows the number of rows (1-6)
     * @return the corresponding layout
     * @throws IllegalArgumentException if the row count is invalid
     */
    @NotNull
    public static Layout forRows(int rows) {
        return switch (rows) {
            case 1 -> CHEST_9;
            case 2 -> CHEST_18;
            case 3 -> CHEST_27;
            case 4 -> CHEST_36;
            case 5 -> CHEST_45;
            case 6 -> CHEST_54;
            default -> throw new IllegalArgumentException("Invalid row count: " + rows + " (must be 1-6)");
        };
    }

    /**
     * Returns a layout for the specified size.
     *
     * @param size the number of slots
     * @return the corresponding layout
     * @throws IllegalArgumentException if no layout matches the size
     */
    @NotNull
    public static Layout forSize(int size) {
        for (Layout layout : values()) {
            if (layout.size == size) {
                return layout;
            }
        }
        throw new IllegalArgumentException("No layout matches size: " + size);
    }

    /**
     * Enumeration of underlying Minecraft inventory types.
     */
    public enum InventoryType {
        /**
         * Standard chest inventory.
         */
        CHEST,

        /**
         * Hopper inventory.
         */
        HOPPER,

        /**
         * Dispenser or dropper inventory.
         */
        DISPENSER
    }
}
