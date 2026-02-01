/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Defines which inventory slots are used for displaying paginated content
 * versus navigation and decoration.
 *
 * <p>ContentSlots maps content indices (0, 1, 2, ...) to actual inventory
 * slot numbers, allowing flexible GUI layouts with reserved areas for
 * navigation buttons, borders, and other UI elements.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Use all slots in a 54-slot inventory
 * ContentSlots full = ContentSlots.full(54);
 *
 * // Use only rows 0-3 (36 slots), leaving row 4 for navigation
 * ContentSlots rows = ContentSlots.rows(0, 4, 9); // rows 0-3, 9 columns
 *
 * // Use specific slots
 * ContentSlots custom = ContentSlots.of(10, 11, 12, 13, 14, 15, 16,
 *                                       19, 20, 21, 22, 23, 24, 25);
 *
 * // Create a bordered area (exclude first and last row/column)
 * ContentSlots bordered = ContentSlots.bordered(54);
 *
 * // Get slots for rendering
 * int[] slots = contentSlots.getSlots();
 * for (int i = 0; i < items.size() && i < slots.length; i++) {
 *     inventory.setItem(slots[i], items.get(i));
 * }
 *
 * // Check if a slot is for content
 * if (contentSlots.isContentSlot(slot)) {
 *     int index = contentSlots.getContentIndex(slot);
 *     // Handle item click...
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PaginatedGUI
 * @see PaginationConfig
 */
public final class ContentSlots {

    /**
     * Number of columns in an inventory row.
     */
    public static final int COLUMNS = 9;

    private final int[] slots;
    private final BitSet slotSet;
    private final int[] slotToIndex;
    private final int inventorySize;

    /**
     * Creates a ContentSlots from an array of slot numbers.
     *
     * @param slots         the slot numbers to use for content
     * @param inventorySize the total inventory size
     * @throws IllegalArgumentException if any slot is out of bounds
     */
    private ContentSlots(@NotNull int[] slots, int inventorySize) {
        this.inventorySize = inventorySize;
        this.slots = slots.clone();
        this.slotSet = new BitSet(inventorySize);
        this.slotToIndex = new int[inventorySize];
        Arrays.fill(slotToIndex, -1);

        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (slot < 0 || slot >= inventorySize) {
                throw new IllegalArgumentException(
                        "Slot " + slot + " is out of bounds for inventory size " + inventorySize);
            }
            slotSet.set(slot);
            slotToIndex[slot] = i;
        }
    }

    /**
     * Creates a ContentSlots using all slots in the inventory.
     *
     * @param inventorySize the total inventory size
     * @return ContentSlots using all slots
     * @throws IllegalArgumentException if inventorySize is invalid
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots full(int inventorySize) {
        validateInventorySize(inventorySize);
        int[] slots = IntStream.range(0, inventorySize).toArray();
        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a ContentSlots from specific slot numbers.
     *
     * @param inventorySize the total inventory size
     * @param slots         the slot numbers to use for content
     * @return ContentSlots for the specified slots
     * @throws IllegalArgumentException if any slot is invalid
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots of(int inventorySize, int... slots) {
        validateInventorySize(inventorySize);
        Objects.requireNonNull(slots, "slots cannot be null");
        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a ContentSlots for specific rows.
     *
     * @param startRow the first row (0-indexed)
     * @param endRow   the row after the last row (exclusive)
     * @param columns  the number of columns per row
     * @return ContentSlots for the specified rows
     * @throws IllegalArgumentException if row indices are invalid
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots rows(int startRow, int endRow, int columns) {
        if (startRow < 0 || endRow < startRow || columns < 1 || columns > COLUMNS) {
            throw new IllegalArgumentException(
                    "Invalid row parameters: startRow=" + startRow +
                    ", endRow=" + endRow + ", columns=" + columns);
        }

        int rowCount = endRow - startRow;
        int totalRows = endRow; // Assume inventory has at least endRow rows
        int inventorySize = totalRows * COLUMNS;

        int[] slots = new int[rowCount * columns];
        int index = 0;
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < columns; col++) {
                slots[index++] = row * COLUMNS + col;
            }
        }

        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a ContentSlots for specific rows, using full width.
     *
     * @param startRow      the first row (0-indexed)
     * @param endRow        the row after the last row (exclusive)
     * @param inventorySize the total inventory size
     * @return ContentSlots for the specified rows
     * @throws IllegalArgumentException if row indices are invalid
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots rows(int startRow, int endRow, int inventorySize, boolean fullWidth) {
        validateInventorySize(inventorySize);
        int maxRow = inventorySize / COLUMNS;
        if (startRow < 0 || endRow > maxRow || startRow >= endRow) {
            throw new IllegalArgumentException(
                    "Invalid row range: startRow=" + startRow + ", endRow=" + endRow +
                    ", maxRow=" + maxRow);
        }

        int[] slots = new int[(endRow - startRow) * COLUMNS];
        int index = 0;
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                slots[index++] = row * COLUMNS + col;
            }
        }

        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a ContentSlots with a border excluded (outer ring of slots).
     *
     * <p>This excludes the first row, last row, first column, and last column,
     * leaving a centered content area.
     *
     * @param inventorySize the total inventory size
     * @return ContentSlots for the inner area
     * @throws IllegalArgumentException if inventorySize is too small
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots bordered(int inventorySize) {
        validateInventorySize(inventorySize);
        int rows = inventorySize / COLUMNS;
        if (rows < 3) {
            throw new IllegalArgumentException(
                    "Bordered layout requires at least 3 rows, got " + rows);
        }

        int innerRows = rows - 2;
        int innerCols = COLUMNS - 2;
        int[] slots = new int[innerRows * innerCols];

        int index = 0;
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < COLUMNS - 1; col++) {
                slots[index++] = row * COLUMNS + col;
            }
        }

        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a ContentSlots excluding specific slots.
     *
     * @param inventorySize  the total inventory size
     * @param excludedSlots the slots to exclude
     * @return ContentSlots excluding the specified slots
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots excluding(int inventorySize, int... excludedSlots) {
        validateInventorySize(inventorySize);
        BitSet excluded = new BitSet(inventorySize);
        for (int slot : excludedSlots) {
            if (slot >= 0 && slot < inventorySize) {
                excluded.set(slot);
            }
        }

        int[] slots = IntStream.range(0, inventorySize)
                .filter(s -> !excluded.get(s))
                .toArray();

        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Creates a rectangular content area.
     *
     * @param startSlot     the top-left corner slot
     * @param width         the width in columns
     * @param height        the height in rows
     * @param inventorySize the total inventory size
     * @return ContentSlots for the rectangle
     * @throws IllegalArgumentException if the rectangle is out of bounds
     * @since 1.0.0
     */
    @NotNull
    public static ContentSlots rectangle(int startSlot, int width, int height, int inventorySize) {
        validateInventorySize(inventorySize);
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "Width and height must be at least 1");
        }

        int startRow = startSlot / COLUMNS;
        int startCol = startSlot % COLUMNS;

        if (startCol + width > COLUMNS) {
            throw new IllegalArgumentException(
                    "Rectangle extends past right edge of inventory");
        }

        int maxRow = inventorySize / COLUMNS;
        if (startRow + height > maxRow) {
            throw new IllegalArgumentException(
                    "Rectangle extends past bottom of inventory");
        }

        int[] slots = new int[width * height];
        int index = 0;
        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                slots[index++] = row * COLUMNS + col;
            }
        }

        return new ContentSlots(slots, inventorySize);
    }

    /**
     * Validates that the inventory size is valid.
     */
    private static void validateInventorySize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(
                    "Inventory size must be a multiple of 9 between 9 and 54, was: " + size);
        }
    }

    // ==================== Instance Methods ====================

    /**
     * Returns the array of content slot numbers.
     *
     * @return a copy of the slot numbers array
     * @since 1.0.0
     */
    @NotNull
    public int[] getSlots() {
        return slots.clone();
    }

    /**
     * Returns the number of content slots.
     *
     * @return the number of content slots
     * @since 1.0.0
     */
    public int getSlotCount() {
        return slots.length;
    }

    /**
     * Returns the total inventory size.
     *
     * @return the inventory size
     * @since 1.0.0
     */
    public int getInventorySize() {
        return inventorySize;
    }

    /**
     * Checks if a slot is a content slot.
     *
     * @param slot the slot to check
     * @return true if the slot is used for content
     * @since 1.0.0
     */
    public boolean isContentSlot(int slot) {
        return slot >= 0 && slot < inventorySize && slotSet.get(slot);
    }

    /**
     * Gets the content index for a slot.
     *
     * <p>The content index is the position in the paginated item list
     * that corresponds to this slot.
     *
     * @param slot the slot number
     * @return the content index, or -1 if not a content slot
     * @since 1.0.0
     */
    public int getContentIndex(int slot) {
        if (slot < 0 || slot >= inventorySize) {
            return -1;
        }
        return slotToIndex[slot];
    }

    /**
     * Gets the slot number for a content index.
     *
     * @param index the content index
     * @return the slot number, or -1 if index is out of bounds
     * @since 1.0.0
     */
    public int getSlotForIndex(int index) {
        if (index < 0 || index >= slots.length) {
            return -1;
        }
        return slots[index];
    }

    /**
     * Returns a stream of content slot numbers.
     *
     * @return an IntStream of slot numbers
     * @since 1.0.0
     */
    @NotNull
    public IntStream stream() {
        return Arrays.stream(slots);
    }

    /**
     * Combines this ContentSlots with another, including all slots from both.
     *
     * @param other the other ContentSlots to combine with
     * @return a new ContentSlots containing slots from both
     * @since 1.0.0
     */
    @NotNull
    public ContentSlots union(@NotNull ContentSlots other) {
        Objects.requireNonNull(other, "other cannot be null");
        int maxSize = Math.max(this.inventorySize, other.inventorySize);

        BitSet combined = (BitSet) this.slotSet.clone();
        combined.or(other.slotSet);

        int[] newSlots = combined.stream().toArray();
        return new ContentSlots(newSlots, maxSize);
    }

    /**
     * Creates a ContentSlots with slots from this that are not in the other.
     *
     * @param other the ContentSlots to subtract
     * @return a new ContentSlots with the difference
     * @since 1.0.0
     */
    @NotNull
    public ContentSlots minus(@NotNull ContentSlots other) {
        Objects.requireNonNull(other, "other cannot be null");

        BitSet difference = (BitSet) this.slotSet.clone();
        difference.andNot(other.slotSet);

        int[] newSlots = difference.stream().toArray();
        return new ContentSlots(newSlots, this.inventorySize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentSlots that = (ContentSlots) o;
        return inventorySize == that.inventorySize && Arrays.equals(slots, that.slots);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(inventorySize);
        result = 31 * result + Arrays.hashCode(slots);
        return result;
    }

    @Override
    public String toString() {
        return String.format("ContentSlots{slots=%s, count=%d, inventorySize=%d}",
                Arrays.toString(slots), slots.length, inventorySize);
    }
}
