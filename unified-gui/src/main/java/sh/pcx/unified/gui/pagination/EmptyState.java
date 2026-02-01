/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for displaying an empty state in paginated GUIs.
 *
 * <p>EmptyState defines what to show when a paginated GUI has no items,
 * providing a better user experience than simply showing an empty grid.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple empty state
 * EmptyState empty = EmptyState.of(
 *     Component.text("No items found"),
 *     "minecraft:barrier"
 * );
 *
 * // Detailed empty state
 * EmptyState detailed = EmptyState.builder()
 *     .title(Component.text("No Players Online", NamedTextColor.RED))
 *     .description(Component.text("Check back later!"))
 *     .icon("minecraft:player_head")
 *     .build();
 *
 * // Empty state with action hint
 * EmptyState withHint = EmptyState.builder()
 *     .title(Component.text("Your inventory is empty"))
 *     .description(Component.text("Click items in the shop to purchase"))
 *     .icon("minecraft:chest")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PaginatedGUI
 * @see EmptyStateRenderer
 */
public final class EmptyState {

    /**
     * Default empty state for when no custom state is configured.
     */
    public static final EmptyState DEFAULT = EmptyState.builder()
            .title(Component.text("No items", NamedTextColor.GRAY))
            .icon("minecraft:barrier")
            .build();

    private final Component title;
    private final List<Component> description;
    private final String iconType;
    private final UnifiedItemStack customIcon;

    /**
     * Creates a new EmptyState.
     */
    private EmptyState(Builder builder) {
        this.title = builder.title;
        this.description = new ArrayList<>(builder.description);
        this.iconType = builder.iconType;
        this.customIcon = builder.customIcon;
    }

    /**
     * Creates a simple empty state with a title and icon.
     *
     * @param title    the title to display
     * @param iconType the item type for the icon
     * @return the new empty state
     */
    @NotNull
    public static EmptyState of(@NotNull Component title, @NotNull String iconType) {
        return builder().title(title).icon(iconType).build();
    }

    /**
     * Creates an empty state with a custom item icon.
     *
     * @param title the title to display
     * @param icon  the custom icon item
     * @return the new empty state
     */
    @NotNull
    public static EmptyState of(@NotNull Component title, @NotNull UnifiedItemStack icon) {
        return builder().title(title).icon(icon).build();
    }

    /**
     * Creates a new builder for EmptyState.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the title component.
     *
     * @return the title
     */
    @NotNull
    public Component getTitle() {
        return title;
    }

    /**
     * Returns the description lines.
     *
     * @return the description lines (may be empty)
     */
    @NotNull
    public List<Component> getDescription() {
        return new ArrayList<>(description);
    }

    /**
     * Returns the icon type.
     *
     * @return the icon type, or null if using custom icon
     */
    @Nullable
    public String getIconType() {
        return iconType;
    }

    /**
     * Returns the custom icon.
     *
     * @return the custom icon, or null if using icon type
     */
    @Nullable
    public UnifiedItemStack getCustomIcon() {
        return customIcon;
    }

    /**
     * Converts this empty state to a displayable item.
     *
     * @return the item stack representation
     */
    @NotNull
    public UnifiedItemStack toItemStack() {
        if (customIcon != null) {
            return customIcon;
        }

        ItemBuilder builder = ItemBuilder.of(iconType != null ? iconType : "minecraft:barrier")
                .name(title);

        if (!description.isEmpty()) {
            builder.lore(description);
        }

        return builder.build();
    }

    /**
     * Builder for creating EmptyState instances.
     */
    public static final class Builder {

        private Component title = Component.text("No items", NamedTextColor.GRAY);
        private final List<Component> description = new ArrayList<>();
        private String iconType = "minecraft:barrier";
        private UnifiedItemStack customIcon;

        private Builder() {
        }

        /**
         * Sets the title.
         *
         * @param title the title component
         * @return this builder
         */
        @NotNull
        public Builder title(@NotNull Component title) {
            this.title = Objects.requireNonNull(title, "title cannot be null");
            return this;
        }

        /**
         * Sets the title from a string.
         *
         * @param title the title string
         * @return this builder
         */
        @NotNull
        public Builder title(@NotNull String title) {
            return title(Component.text(title));
        }

        /**
         * Adds a description line.
         *
         * @param line the description line
         * @return this builder
         */
        @NotNull
        public Builder description(@NotNull Component line) {
            this.description.add(Objects.requireNonNull(line, "line cannot be null"));
            return this;
        }

        /**
         * Adds a description line from a string.
         *
         * @param line the description line
         * @return this builder
         */
        @NotNull
        public Builder description(@NotNull String line) {
            return description(Component.text(line, NamedTextColor.GRAY));
        }

        /**
         * Sets all description lines.
         *
         * @param lines the description lines
         * @return this builder
         */
        @NotNull
        public Builder description(@NotNull List<Component> lines) {
            this.description.clear();
            this.description.addAll(lines);
            return this;
        }

        /**
         * Sets the icon type.
         *
         * @param iconType the item type for the icon
         * @return this builder
         */
        @NotNull
        public Builder icon(@NotNull String iconType) {
            this.iconType = Objects.requireNonNull(iconType, "iconType cannot be null");
            this.customIcon = null;
            return this;
        }

        /**
         * Sets a custom icon item.
         *
         * @param icon the custom icon
         * @return this builder
         */
        @NotNull
        public Builder icon(@NotNull UnifiedItemStack icon) {
            this.customIcon = Objects.requireNonNull(icon, "icon cannot be null");
            this.iconType = null;
            return this;
        }

        /**
         * Builds the EmptyState.
         *
         * @return the configured empty state
         */
        @NotNull
        public EmptyState build() {
            return new EmptyState(this);
        }
    }
}
