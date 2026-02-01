/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.tools.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for generating a GUI class.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * GuiConfig config = GuiConfig.builder()
 *     .title("Shop Menu")
 *     .rows(6)
 *     .type(GuiType.CHEST)
 *     .fillBorder(true)
 *     .pagination(true)
 *     .closeButton(true)
 *     .slot(4, "Header", SlotType.STATIC)
 *     .slot(49, "Close", SlotType.CLOSE)
 *     .build();
 *
 * generator.generateGui(projectPath, "ShopGui", config);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ProjectGenerator
 */
public final class GuiConfig {

    private final String title;
    private final int rows;
    private final GuiType type;
    private final boolean fillBorder;
    private final boolean pagination;
    private final boolean closeButton;
    private final boolean backButton;
    private final List<SlotDef> slots;

    private GuiConfig(Builder builder) {
        this.title = builder.title != null ? builder.title : "Menu";
        this.rows = builder.rows;
        this.type = builder.type;
        this.fillBorder = builder.fillBorder;
        this.pagination = builder.pagination;
        this.closeButton = builder.closeButton;
        this.backButton = builder.backButton;
        this.slots = List.copyOf(builder.slots);
    }

    /**
     * Returns the GUI title.
     *
     * @return the title
     * @since 1.0.0
     */
    @NotNull
    public String title() {
        return title;
    }

    /**
     * Returns the number of rows.
     *
     * @return the rows (1-6 for chest)
     * @since 1.0.0
     */
    public int rows() {
        return rows;
    }

    /**
     * Returns the GUI type.
     *
     * @return the type
     * @since 1.0.0
     */
    @NotNull
    public GuiType type() {
        return type;
    }

    /**
     * Returns whether to fill the border.
     *
     * @return true to fill border
     * @since 1.0.0
     */
    public boolean fillBorder() {
        return fillBorder;
    }

    /**
     * Returns whether to include pagination.
     *
     * @return true for pagination
     * @since 1.0.0
     */
    public boolean pagination() {
        return pagination;
    }

    /**
     * Returns whether to include a close button.
     *
     * @return true for close button
     * @since 1.0.0
     */
    public boolean closeButton() {
        return closeButton;
    }

    /**
     * Returns whether to include a back button.
     *
     * @return true for back button
     * @since 1.0.0
     */
    public boolean backButton() {
        return backButton;
    }

    /**
     * Returns the slot definitions.
     *
     * @return the slots
     * @since 1.0.0
     */
    @NotNull
    public List<SlotDef> slots() {
        return slots;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default configuration.
     *
     * @return the default config
     * @since 1.0.0
     */
    @NotNull
    public static GuiConfig defaults() {
        return builder().build();
    }

    /**
     * GUI types.
     *
     * @since 1.0.0
     */
    public enum GuiType {
        /**
         * Standard chest inventory.
         */
        CHEST,

        /**
         * Hopper inventory (5 slots).
         */
        HOPPER,

        /**
         * Dispenser/dropper inventory (9 slots).
         */
        DISPENSER,

        /**
         * Furnace inventory.
         */
        FURNACE,

        /**
         * Crafting table inventory.
         */
        WORKBENCH,

        /**
         * Anvil inventory.
         */
        ANVIL,

        /**
         * Beacon inventory.
         */
        BEACON
    }

    /**
     * Slot types.
     *
     * @since 1.0.0
     */
    public enum SlotType {
        /**
         * Static display slot.
         */
        STATIC,

        /**
         * Clickable button slot.
         */
        BUTTON,

        /**
         * Close GUI button.
         */
        CLOSE,

        /**
         * Back/previous button.
         */
        BACK,

        /**
         * Next page button.
         */
        NEXT_PAGE,

        /**
         * Previous page button.
         */
        PREV_PAGE,

        /**
         * Dynamic content slot.
         */
        DYNAMIC,

        /**
         * Player input slot.
         */
        INPUT
    }

    /**
     * Definition of a GUI slot.
     *
     * @param slot the slot number
     * @param name the slot name/purpose
     * @param type the slot type
     * @since 1.0.0
     */
    public record SlotDef(
            int slot,
            @NotNull String name,
            @NotNull SlotType type
    ) {
        /**
         * Creates a slot definition.
         *
         * @param slot the slot number
         * @param name the name
         * @param type the type
         * @return the definition
         */
        public static SlotDef of(int slot, @NotNull String name, @NotNull SlotType type) {
            return new SlotDef(slot, name, type);
        }
    }

    /**
     * Builder for {@link GuiConfig}.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String title;
        private int rows = 3;
        private GuiType type = GuiType.CHEST;
        private boolean fillBorder = false;
        private boolean pagination = false;
        private boolean closeButton = false;
        private boolean backButton = false;
        private final List<SlotDef> slots = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the GUI title.
         *
         * @param title the title
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder title(@NotNull String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the number of rows.
         *
         * @param rows the rows (1-6)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder rows(int rows) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("Rows must be 1-6");
            }
            this.rows = rows;
            return this;
        }

        /**
         * Sets the GUI type.
         *
         * @param type the type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder type(@NotNull GuiType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets whether to fill the border.
         *
         * @param fill true to fill
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder fillBorder(boolean fill) {
            this.fillBorder = fill;
            return this;
        }

        /**
         * Sets whether to include pagination.
         *
         * @param pagination true for pagination
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder pagination(boolean pagination) {
            this.pagination = pagination;
            return this;
        }

        /**
         * Sets whether to include a close button.
         *
         * @param close true for close button
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder closeButton(boolean close) {
            this.closeButton = close;
            return this;
        }

        /**
         * Sets whether to include a back button.
         *
         * @param back true for back button
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder backButton(boolean back) {
            this.backButton = back;
            return this;
        }

        /**
         * Adds a slot definition.
         *
         * @param slot the slot number
         * @param name the slot name
         * @param type the slot type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder slot(int slot, @NotNull String name, @NotNull SlotType type) {
            this.slots.add(SlotDef.of(slot, name, type));
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the GUI config
         * @since 1.0.0
         */
        @NotNull
        public GuiConfig build() {
            return new GuiConfig(this);
        }
    }
}
