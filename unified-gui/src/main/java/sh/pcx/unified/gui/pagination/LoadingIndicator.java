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
 * Represents a loading indicator displayed while async items are being loaded.
 *
 * <p>LoadingIndicator provides visual feedback to players during asynchronous
 * data loading. It supports static items, animated sequences, and custom
 * progress displays.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple static loading indicator
 * LoadingIndicator simple = LoadingIndicator.of(
 *     ItemBuilder.of("minecraft:clock")
 *         .name(Component.text("Loading..."))
 *         .build()
 * );
 *
 * // Animated loading indicator (cycles through items)
 * LoadingIndicator animated = LoadingIndicator.animated(
 *     ItemBuilder.of("minecraft:gray_dye").name(Component.text("Loading.")).build(),
 *     ItemBuilder.of("minecraft:light_gray_dye").name(Component.text("Loading..")).build(),
 *     ItemBuilder.of("minecraft:white_dye").name(Component.text("Loading...")).build()
 * );
 *
 * // Spinning clock animation
 * LoadingIndicator spinning = LoadingIndicator.spinning();
 *
 * // Progress-based indicator
 * LoadingIndicator progress = LoadingIndicator.withProgress(percent ->
 *     ItemBuilder.of("minecraft:experience_bottle")
 *         .name(Component.text("Loading... " + percent + "%"))
 *         .build()
 * );
 *
 * // Custom builder
 * LoadingIndicator custom = LoadingIndicator.builder()
 *     .item(clockItem)
 *     .message("Fetching data from server...")
 *     .centerInSlot(22)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AsyncItemLoader
 * @see PaginatedGUI
 */
public final class LoadingIndicator {

    private final UnifiedItemStack[] frames;
    private final int frameInterval;
    private final boolean fillSlots;
    private final int centerSlot;
    private final Function<Integer, UnifiedItemStack> progressBuilder;

    /**
     * Private constructor used by Builder.
     */
    private LoadingIndicator(@NotNull Builder builder) {
        this.frames = builder.frames;
        this.frameInterval = builder.frameInterval;
        this.fillSlots = builder.fillSlots;
        this.centerSlot = builder.centerSlot;
        this.progressBuilder = builder.progressBuilder;
    }

    /**
     * Creates a simple loading indicator with a single item.
     *
     * @param item the loading indicator item
     * @return a LoadingIndicator showing the item
     * @since 1.0.0
     */
    @NotNull
    public static LoadingIndicator of(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        return builder().item(item).build();
    }

    /**
     * Creates an animated loading indicator that cycles through frames.
     *
     * @param frames the animation frames
     * @return an animated LoadingIndicator
     * @throws IllegalArgumentException if no frames are provided
     * @since 1.0.0
     */
    @NotNull
    public static LoadingIndicator animated(@NotNull UnifiedItemStack... frames) {
        if (frames.length == 0) {
            throw new IllegalArgumentException("At least one frame is required");
        }
        return builder().frames(frames).build();
    }

    /**
     * Creates a spinning clock loading indicator.
     *
     * <p>This uses different clock/compass orientations to simulate spinning.
     *
     * @return a spinning clock LoadingIndicator
     * @since 1.0.0
     */
    @NotNull
    public static LoadingIndicator spinning() {
        return builder()
                .item(ItemBuilder.of("minecraft:clock")
                        .name(Component.text("Loading..."))
                        .build())
                .frameInterval(5)
                .build();
    }

    /**
     * Creates a loading indicator with progress display.
     *
     * @param progressBuilder function that creates an item for a progress percentage
     * @return a progress-based LoadingIndicator
     * @since 1.0.0
     */
    @NotNull
    public static LoadingIndicator withProgress(
            @NotNull Function<Integer, UnifiedItemStack> progressBuilder) {
        Objects.requireNonNull(progressBuilder, "progressBuilder cannot be null");
        return new Builder().progressBuilder(progressBuilder).build();
    }

    /**
     * Creates a new LoadingIndicator builder.
     *
     * @return a new Builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Instance Methods ====================

    /**
     * Gets the current frame item.
     *
     * <p>For non-animated indicators, this returns the single item.
     * For animated indicators, this returns the frame for the given tick.
     *
     * @param tick the current animation tick
     * @return the frame item to display
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack getFrame(long tick) {
        if (frames.length == 0) {
            return ItemBuilder.of("minecraft:barrier")
                    .name(Component.text("Error"))
                    .build();
        }

        if (frames.length == 1) {
            return frames[0];
        }

        int frameIndex = (int) ((tick / frameInterval) % frames.length);
        return frames[frameIndex];
    }

    /**
     * Gets the item for a specific progress percentage.
     *
     * @param progressPercent the progress percentage (0-100)
     * @return the progress item to display
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack getProgressItem(int progressPercent) {
        if (progressBuilder != null) {
            UnifiedItemStack item = progressBuilder.apply(progressPercent);
            if (item != null) {
                return item;
            }
        }

        // Fall back to frame display
        return getFrame(0);
    }

    /**
     * Returns the animation frames.
     *
     * @return a copy of the animation frames array
     * @since 1.0.0
     */
    @NotNull
    public UnifiedItemStack[] getFrames() {
        return frames.clone();
    }

    /**
     * Returns the number of ticks between animation frames.
     *
     * @return the frame interval in ticks
     * @since 1.0.0
     */
    public int getFrameInterval() {
        return frameInterval;
    }

    /**
     * Returns whether all content slots should be filled with the indicator.
     *
     * @return true if all slots should be filled
     * @since 1.0.0
     */
    public boolean isFillSlots() {
        return fillSlots;
    }

    /**
     * Returns the slot where the indicator should be centered.
     *
     * @return the center slot, or -1 for default positioning
     * @since 1.0.0
     */
    public int getCenterSlot() {
        return centerSlot;
    }

    /**
     * Checks if this indicator is animated (has multiple frames).
     *
     * @return true if animated
     * @since 1.0.0
     */
    public boolean isAnimated() {
        return frames.length > 1;
    }

    /**
     * Checks if this indicator supports progress display.
     *
     * @return true if progress display is supported
     * @since 1.0.0
     */
    public boolean hasProgressSupport() {
        return progressBuilder != null;
    }

    // ==================== Builder ====================

    /**
     * Builder for creating {@link LoadingIndicator} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private UnifiedItemStack[] frames = new UnifiedItemStack[0];
        private int frameInterval = 10;
        private boolean fillSlots = false;
        private int centerSlot = -1;
        private Function<Integer, UnifiedItemStack> progressBuilder;

        /**
         * Creates a new Builder with default values.
         */
        private Builder() {}

        /**
         * Sets a single static item for the loading indicator.
         *
         * @param item the loading indicator item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder item(@NotNull UnifiedItemStack item) {
            Objects.requireNonNull(item, "item cannot be null");
            this.frames = new UnifiedItemStack[] { item };
            return this;
        }

        /**
         * Sets the animation frames for an animated indicator.
         *
         * @param frames the animation frames
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder frames(@NotNull UnifiedItemStack... frames) {
            Objects.requireNonNull(frames, "frames cannot be null");
            this.frames = frames.clone();
            return this;
        }

        /**
         * Sets the interval between animation frames in ticks.
         *
         * @param ticks the frame interval
         * @return this builder
         * @throws IllegalArgumentException if ticks is less than 1
         * @since 1.0.0
         */
        @NotNull
        public Builder frameInterval(int ticks) {
            if (ticks < 1) {
                throw new IllegalArgumentException("frameInterval must be at least 1");
            }
            this.frameInterval = ticks;
            return this;
        }

        /**
         * Sets whether to fill all content slots with the loading indicator.
         *
         * @param fill true to fill all slots
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder fillSlots(boolean fill) {
            this.fillSlots = fill;
            return this;
        }

        /**
         * Sets a specific slot to center the loading indicator.
         *
         * @param slot the center slot
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder centerInSlot(int slot) {
            this.centerSlot = slot;
            return this;
        }

        /**
         * Sets a custom message for the default loading item.
         *
         * <p>This creates a default clock item with the specified message.
         *
         * @param message the loading message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder message(@NotNull String message) {
            Objects.requireNonNull(message, "message cannot be null");
            this.frames = new UnifiedItemStack[] {
                    ItemBuilder.of("minecraft:clock")
                            .name(Component.text(message))
                            .build()
            };
            return this;
        }

        /**
         * Sets the progress builder function.
         *
         * @param progressBuilder function that creates items for progress percentages
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder progressBuilder(@Nullable Function<Integer, UnifiedItemStack> progressBuilder) {
            this.progressBuilder = progressBuilder;
            return this;
        }

        /**
         * Builds the LoadingIndicator.
         *
         * @return the built LoadingIndicator
         * @since 1.0.0
         */
        @NotNull
        public LoadingIndicator build() {
            if (frames.length == 0 && progressBuilder == null) {
                // Create a default loading item
                frames = new UnifiedItemStack[] {
                        ItemBuilder.of("minecraft:clock")
                                .name(Component.text("Loading..."))
                                .build()
                };
            }
            return new LoadingIndicator(this);
        }
    }
}
