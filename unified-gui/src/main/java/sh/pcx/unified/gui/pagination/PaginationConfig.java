/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Configuration for pagination GUI elements including navigation buttons and page indicators.
 *
 * <p>This class defines the positions and appearances of pagination controls
 * like previous/next buttons and page information displays. It uses the builder
 * pattern for flexible configuration.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create configuration with builder
 * PaginationConfig config = PaginationConfig.builder()
 *     .previousButtonSlot(45)
 *     .nextButtonSlot(53)
 *     .pageInfoSlot(49)
 *     .previousButton(ItemBuilder.of("minecraft:arrow")
 *         .name(Component.text("Previous Page"))
 *         .build())
 *     .nextButton(ItemBuilder.of("minecraft:arrow")
 *         .name(Component.text("Next Page"))
 *         .build())
 *     .pageInfoBuilder(ctx -> ItemBuilder.of("minecraft:paper")
 *         .name(Component.text("Page " + ctx.getDisplayPage() + "/" + ctx.getTotalPages()))
 *         .build())
 *     .build();
 *
 * // Apply to GUI
 * paginatedGui.setPaginationConfig(config);
 *
 * // Use defaults
 * PaginationConfig defaultConfig = PaginationConfig.defaults();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe after construction.
 * The Builder is NOT thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PaginatedGUI
 */
public final class PaginationConfig {

    /**
     * Slot value indicating no slot is assigned.
     */
    public static final int NO_SLOT = -1;

    private final int previousButtonSlot;
    private final int nextButtonSlot;
    private final int pageInfoSlot;
    private final UnifiedItemStack previousButton;
    private final UnifiedItemStack nextButton;
    private final UnifiedItemStack disabledPreviousButton;
    private final UnifiedItemStack disabledNextButton;
    private final PageInfoBuilder pageInfoBuilder;
    private final boolean hideDisabledButtons;
    private final boolean playClickSound;
    private final String clickSound;

    /**
     * Private constructor used by Builder.
     */
    private PaginationConfig(@NotNull Builder builder) {
        this.previousButtonSlot = builder.previousButtonSlot;
        this.nextButtonSlot = builder.nextButtonSlot;
        this.pageInfoSlot = builder.pageInfoSlot;
        this.previousButton = builder.previousButton;
        this.nextButton = builder.nextButton;
        this.disabledPreviousButton = builder.disabledPreviousButton;
        this.disabledNextButton = builder.disabledNextButton;
        this.pageInfoBuilder = builder.pageInfoBuilder;
        this.hideDisabledButtons = builder.hideDisabledButtons;
        this.playClickSound = builder.playClickSound;
        this.clickSound = builder.clickSound;
    }

    /**
     * Creates a new configuration builder.
     *
     * @return a new Builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default configuration with standard navigation buttons.
     *
     * <p>The default configuration places:
     * <ul>
     *   <li>Previous button at slot 45 (bottom-left)</li>
     *   <li>Next button at slot 53 (bottom-right)</li>
     *   <li>Page info at slot 49 (bottom-center)</li>
     * </ul>
     *
     * @return a default PaginationConfig
     * @since 1.0.0
     */
    @NotNull
    public static PaginationConfig defaults() {
        return builder()
                .previousButtonSlot(45)
                .nextButtonSlot(53)
                .pageInfoSlot(49)
                .build();
    }

    /**
     * Creates a configuration for a 6-row inventory (54 slots).
     *
     * @return a PaginationConfig for 6-row inventories
     * @since 1.0.0
     */
    @NotNull
    public static PaginationConfig forSixRows() {
        return builder()
                .previousButtonSlot(45)
                .nextButtonSlot(53)
                .pageInfoSlot(49)
                .build();
    }

    /**
     * Creates a configuration for a 3-row inventory (27 slots).
     *
     * @return a PaginationConfig for 3-row inventories
     * @since 1.0.0
     */
    @NotNull
    public static PaginationConfig forThreeRows() {
        return builder()
                .previousButtonSlot(18)
                .nextButtonSlot(26)
                .pageInfoSlot(22)
                .build();
    }

    // ==================== Getters ====================

    /**
     * Returns the slot for the previous page button.
     *
     * @return the previous button slot, or {@link #NO_SLOT} if not set
     * @since 1.0.0
     */
    public int getPreviousButtonSlot() {
        return previousButtonSlot;
    }

    /**
     * Returns the slot for the next page button.
     *
     * @return the next button slot, or {@link #NO_SLOT} if not set
     * @since 1.0.0
     */
    public int getNextButtonSlot() {
        return nextButtonSlot;
    }

    /**
     * Returns the slot for the page information display.
     *
     * @return the page info slot, or {@link #NO_SLOT} if not set
     * @since 1.0.0
     */
    public int getPageInfoSlot() {
        return pageInfoSlot;
    }

    /**
     * Returns the item stack for the previous page button.
     *
     * @return the previous button item, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getPreviousButton() {
        return previousButton;
    }

    /**
     * Returns the item stack for the next page button.
     *
     * @return the next button item, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getNextButton() {
        return nextButton;
    }

    /**
     * Returns the item stack for the disabled previous page button.
     *
     * <p>This is shown when there is no previous page available.
     *
     * @return the disabled previous button item, or null to hide
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getDisabledPreviousButton() {
        if (hideDisabledButtons) {
            return null;
        }
        return disabledPreviousButton;
    }

    /**
     * Returns the item stack for the disabled next page button.
     *
     * <p>This is shown when there is no next page available.
     *
     * @return the disabled next button item, or null to hide
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getDisabledNextButton() {
        if (hideDisabledButtons) {
            return null;
        }
        return disabledNextButton;
    }

    /**
     * Returns the page info builder function.
     *
     * @return the page info builder, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public PageInfoBuilder getPageInfoBuilder() {
        return pageInfoBuilder;
    }

    /**
     * Returns whether disabled navigation buttons should be hidden.
     *
     * @return true if disabled buttons are hidden
     * @since 1.0.0
     */
    public boolean isHideDisabledButtons() {
        return hideDisabledButtons;
    }

    /**
     * Returns whether to play a click sound on navigation.
     *
     * @return true if click sound is enabled
     * @since 1.0.0
     */
    public boolean isPlayClickSound() {
        return playClickSound;
    }

    /**
     * Returns the sound to play on navigation clicks.
     *
     * @return the click sound key
     * @since 1.0.0
     */
    @NotNull
    public String getClickSound() {
        return clickSound;
    }

    /**
     * Creates a modified copy of this configuration.
     *
     * @return a new Builder initialized with this config's values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .previousButtonSlot(previousButtonSlot)
                .nextButtonSlot(nextButtonSlot)
                .pageInfoSlot(pageInfoSlot)
                .previousButton(previousButton)
                .nextButton(nextButton)
                .disabledPreviousButton(disabledPreviousButton)
                .disabledNextButton(disabledNextButton)
                .pageInfoBuilder(pageInfoBuilder)
                .hideDisabledButtons(hideDisabledButtons)
                .playClickSound(playClickSound)
                .clickSound(clickSound);
    }

    // ==================== Builder ====================

    /**
     * Builder for creating {@link PaginationConfig} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private int previousButtonSlot = NO_SLOT;
        private int nextButtonSlot = NO_SLOT;
        private int pageInfoSlot = NO_SLOT;
        private UnifiedItemStack previousButton;
        private UnifiedItemStack nextButton;
        private UnifiedItemStack disabledPreviousButton;
        private UnifiedItemStack disabledNextButton;
        private PageInfoBuilder pageInfoBuilder;
        private boolean hideDisabledButtons = false;
        private boolean playClickSound = true;
        private String clickSound = "minecraft:ui.button.click";

        /**
         * Creates a new Builder with default values.
         */
        private Builder() {
            // Default button items will be created if not set
        }

        /**
         * Sets the slot for the previous page button.
         *
         * @param slot the slot number, or {@link #NO_SLOT} to disable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder previousButtonSlot(int slot) {
            this.previousButtonSlot = slot;
            return this;
        }

        /**
         * Sets the slot for the next page button.
         *
         * @param slot the slot number, or {@link #NO_SLOT} to disable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder nextButtonSlot(int slot) {
            this.nextButtonSlot = slot;
            return this;
        }

        /**
         * Sets the slot for the page information display.
         *
         * @param slot the slot number, or {@link #NO_SLOT} to disable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder pageInfoSlot(int slot) {
            this.pageInfoSlot = slot;
            return this;
        }

        /**
         * Sets the item for the previous page button.
         *
         * @param item the item to use
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder previousButton(@Nullable UnifiedItemStack item) {
            this.previousButton = item;
            return this;
        }

        /**
         * Sets the item for the next page button.
         *
         * @param item the item to use
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder nextButton(@Nullable UnifiedItemStack item) {
            this.nextButton = item;
            return this;
        }

        /**
         * Sets the item for the disabled previous page button.
         *
         * @param item the item to use when disabled
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disabledPreviousButton(@Nullable UnifiedItemStack item) {
            this.disabledPreviousButton = item;
            return this;
        }

        /**
         * Sets the item for the disabled next page button.
         *
         * @param item the item to use when disabled
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disabledNextButton(@Nullable UnifiedItemStack item) {
            this.disabledNextButton = item;
            return this;
        }

        /**
         * Sets the page info builder function.
         *
         * @param builder the function to build page info items
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder pageInfoBuilder(@Nullable PageInfoBuilder builder) {
            this.pageInfoBuilder = builder;
            return this;
        }

        /**
         * Sets whether to hide disabled navigation buttons.
         *
         * @param hide true to hide disabled buttons
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder hideDisabledButtons(boolean hide) {
            this.hideDisabledButtons = hide;
            return this;
        }

        /**
         * Sets whether to play a click sound on navigation.
         *
         * @param play true to play sounds
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder playClickSound(boolean play) {
            this.playClickSound = play;
            return this;
        }

        /**
         * Sets the click sound to play.
         *
         * @param sound the sound key
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder clickSound(@NotNull String sound) {
            this.clickSound = Objects.requireNonNull(sound);
            return this;
        }

        /**
         * Builds the PaginationConfig.
         *
         * <p>If button items are not explicitly set, default items will be created.
         *
         * @return the built PaginationConfig
         * @since 1.0.0
         */
        @NotNull
        public PaginationConfig build() {
            // Create default items if not set
            if (previousButton == null && previousButtonSlot != NO_SLOT) {
                previousButton = createDefaultPreviousButton();
            }
            if (nextButton == null && nextButtonSlot != NO_SLOT) {
                nextButton = createDefaultNextButton();
            }
            if (disabledPreviousButton == null && previousButtonSlot != NO_SLOT) {
                disabledPreviousButton = createDefaultDisabledButton("No Previous Page");
            }
            if (disabledNextButton == null && nextButtonSlot != NO_SLOT) {
                disabledNextButton = createDefaultDisabledButton("No Next Page");
            }
            if (pageInfoBuilder == null && pageInfoSlot != NO_SLOT) {
                pageInfoBuilder = createDefaultPageInfoBuilder();
            }

            return new PaginationConfig(this);
        }

        private UnifiedItemStack createDefaultPreviousButton() {
            return ItemBuilder.of("minecraft:arrow")
                    .name(Component.text("Previous Page"))
                    .addLore(Component.text("Click to go back"))
                    .build();
        }

        private UnifiedItemStack createDefaultNextButton() {
            return ItemBuilder.of("minecraft:arrow")
                    .name(Component.text("Next Page"))
                    .addLore(Component.text("Click to continue"))
                    .build();
        }

        private UnifiedItemStack createDefaultDisabledButton(String name) {
            return ItemBuilder.of("minecraft:gray_stained_glass_pane")
                    .name(Component.text(name))
                    .build();
        }

        private PageInfoBuilder createDefaultPageInfoBuilder() {
            return context -> ItemBuilder.of("minecraft:paper")
                    .name(Component.text(context.formatPageIndicator()))
                    .addLore(Component.text(context.formatItemCount(context.getItemCount())))
                    .build();
        }
    }

    /**
     * Functional interface for building page information items.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface PageInfoBuilder {

        /**
         * Builds an item stack to display page information.
         *
         * @param context the current page context
         * @return the page info item stack
         * @since 1.0.0
         */
        @Nullable
        UnifiedItemStack buildPageInfo(@NotNull PageContext<?> context);
    }
}
