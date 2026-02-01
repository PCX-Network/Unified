/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.component;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A button that cycles through a list of options on each click.
 *
 * <p>CycleButton is ideal for settings that have multiple possible values,
 * such as difficulty levels, game modes, or sorting options. It supports
 * both forward and backward cycling (left/right click).
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Cycles through arbitrary option list</li>
 *   <li>Left click cycles forward, right click cycles backward</li>
 *   <li>Custom item display for each option</li>
 *   <li>Type-safe option handling with generics</li>
 *   <li>Callback on option change</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple enum cycle
 * CycleButton<Difficulty> difficultyBtn = CycleButton.<Difficulty>cycleButtonBuilder()
 *     .options(Difficulty.values())
 *     .itemMapper(difficulty -> ItemBuilder.of("minecraft:iron_sword")
 *         .name(Component.text("Difficulty: " + difficulty.name()))
 *         .build())
 *     .initialOption(Difficulty.NORMAL)
 *     .onChange((ctx, difficulty) -> setDifficulty(difficulty))
 *     .build();
 *
 * // Custom options with display items
 * CycleButton<String> sortBtn = CycleButton.<String>cycleButtonBuilder()
 *     .option("name", ItemBuilder.of("minecraft:name_tag")
 *         .name(Component.text("Sort: By Name")).build())
 *     .option("date", ItemBuilder.of("minecraft:clock")
 *         .name(Component.text("Sort: By Date")).build())
 *     .option("price", ItemBuilder.of("minecraft:gold_ingot")
 *         .name(Component.text("Sort: By Price")).build())
 *     .initialOption("name")
 *     .onChange((ctx, sortType) -> setSortOrder(sortType))
 *     .build();
 *
 * // Integer range cycle
 * CycleButton<Integer> speedBtn = CycleButton.<Integer>cycleButtonBuilder()
 *     .options(List.of(1, 2, 3, 4, 5))
 *     .itemMapper(speed -> ItemBuilder.of("minecraft:feather")
 *         .name(Component.text("Speed: " + speed))
 *         .amount(speed)
 *         .build())
 *     .initialOption(1)
 *     .wrapAround(true)
 *     .onChange((ctx, speed) -> setSpeed(speed))
 *     .build();
 * }</pre>
 *
 * @param <T> the type of options to cycle through
 * @since 1.0.0
 * @author Supatuck
 * @see Button
 * @see ToggleButton
 */
public class CycleButton<T> extends Button {

    /**
     * The list of options to cycle through.
     */
    private final List<T> options;

    /**
     * The items for each option.
     */
    private final List<UnifiedItemStack> items;

    /**
     * Function to map options to display items.
     */
    private final Function<T, UnifiedItemStack> itemMapper;

    /**
     * The current option index.
     */
    private volatile int currentIndex;

    /**
     * Handler called when the option changes.
     */
    private final BiConsumer<ClickContext, T> changeHandler;

    /**
     * Whether cycling wraps around at the ends.
     */
    private final boolean wrapAround;

    /**
     * Click sound.
     */
    private final String cycleSound;

    /**
     * Sound volume.
     */
    private final float soundVolume;

    /**
     * Sound pitch.
     */
    private final float soundPitch;

    /**
     * Constructs a CycleButton with the builder configuration.
     *
     * @param builder the builder
     */
    protected CycleButton(@NotNull Builder<T> builder) {
        super(createButtonBuilder(builder));
        this.options = Collections.unmodifiableList(new ArrayList<>(builder.options));
        this.items = builder.items != null
            ? Collections.unmodifiableList(new ArrayList<>(builder.items))
            : null;
        this.itemMapper = builder.itemMapper;
        this.currentIndex = builder.initialIndex;
        this.changeHandler = builder.changeHandler;
        this.wrapAround = builder.wrapAround;
        this.cycleSound = builder.cycleSound;
        this.soundVolume = builder.soundVolume;
        this.soundPitch = builder.soundPitch;
    }

    /**
     * Creates the underlying button builder.
     */
    private static <T> ButtonBuilder createButtonBuilder(@NotNull Builder<T> builder) {
        ButtonBuilder buttonBuilder = new ButtonBuilder();
        // Will be overridden by getItem()
        buttonBuilder.itemSupplier = () -> UnifiedItemStack.empty();
        buttonBuilder.permission = builder.permission;
        buttonBuilder.cooldownMs = builder.cooldownMs;
        buttonBuilder.visibilityCondition = builder.visibilityCondition;
        buttonBuilder.enabled = builder.enabled;
        return buttonBuilder;
    }

    /**
     * Creates a new CycleButton builder.
     *
     * @param <T> the option type
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> cycleButtonBuilder() {
        return new Builder<>();
    }

    @Override
    @NotNull
    public UnifiedItemStack getItem() {
        if (items != null && currentIndex < items.size()) {
            return items.get(currentIndex);
        }
        if (itemMapper != null && currentIndex < options.size()) {
            return itemMapper.apply(options.get(currentIndex));
        }
        return UnifiedItemStack.empty();
    }

    @Override
    public void handleClick(@NotNull ClickContext context) {
        if (!isEnabled()) {
            return;
        }

        if (!isVisibleTo(context.player())) {
            return;
        }

        int oldIndex = currentIndex;

        // Determine direction based on click type
        if (context.isRightClick()) {
            cyclePrevious();
        } else {
            cycleNext();
        }

        // Only notify if changed
        if (currentIndex != oldIndex) {
            // Play sound
            if (cycleSound != null) {
                context.player().playSound(cycleSound, soundVolume, soundPitch);
            }

            // Notify handler
            if (changeHandler != null) {
                changeHandler.accept(context, getCurrentOption());
            }
        }
    }

    /**
     * Returns the current option.
     *
     * @return the current option value
     * @since 1.0.0
     */
    @NotNull
    public T getCurrentOption() {
        return options.get(currentIndex);
    }

    /**
     * Returns the current option index.
     *
     * @return the current index
     * @since 1.0.0
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Sets the current option by value.
     *
     * @param option the option to select
     * @return true if the option was found and selected
     * @since 1.0.0
     */
    public boolean setCurrentOption(@NotNull T option) {
        int index = options.indexOf(option);
        if (index >= 0) {
            currentIndex = index;
            return true;
        }
        return false;
    }

    /**
     * Sets the current option by index.
     *
     * @param index the index to select
     * @throws IndexOutOfBoundsException if index is out of range
     * @since 1.0.0
     */
    public void setCurrentIndex(int index) {
        if (index < 0 || index >= options.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of range [0, " + options.size() + ")");
        }
        this.currentIndex = index;
    }

    /**
     * Cycles to the next option.
     *
     * @return the new current option
     * @since 1.0.0
     */
    @NotNull
    public T cycleNext() {
        if (wrapAround) {
            currentIndex = (currentIndex + 1) % options.size();
        } else if (currentIndex < options.size() - 1) {
            currentIndex++;
        }
        return getCurrentOption();
    }

    /**
     * Cycles to the previous option.
     *
     * @return the new current option
     * @since 1.0.0
     */
    @NotNull
    public T cyclePrevious() {
        if (wrapAround) {
            currentIndex = (currentIndex - 1 + options.size()) % options.size();
        } else if (currentIndex > 0) {
            currentIndex--;
        }
        return getCurrentOption();
    }

    /**
     * Returns all available options.
     *
     * @return unmodifiable list of options
     * @since 1.0.0
     */
    @NotNull
    public List<T> getOptions() {
        return options;
    }

    /**
     * Returns the number of options.
     *
     * @return the option count
     * @since 1.0.0
     */
    public int getOptionCount() {
        return options.size();
    }

    /**
     * Checks if the current option is the first.
     *
     * @return true if at the first option
     * @since 1.0.0
     */
    public boolean isFirst() {
        return currentIndex == 0;
    }

    /**
     * Checks if the current option is the last.
     *
     * @return true if at the last option
     * @since 1.0.0
     */
    public boolean isLast() {
        return currentIndex == options.size() - 1;
    }

    /**
     * Builder for creating CycleButton instances.
     *
     * @param <T> the option type
     * @since 1.0.0
     */
    public static class Builder<T> {

        final List<T> options = new ArrayList<>();
        List<UnifiedItemStack> items;
        Function<T, UnifiedItemStack> itemMapper;
        int initialIndex = 0;
        BiConsumer<ClickContext, T> changeHandler;
        boolean wrapAround = true;
        String permission;
        long cooldownMs = 0;
        java.util.function.Predicate<UnifiedPlayer> visibilityCondition;
        boolean enabled = true;
        String cycleSound;
        float soundVolume = 1.0f;
        float soundPitch = 1.0f;

        /**
         * Creates a new Builder.
         */
        Builder() {
            // Package-private constructor
        }

        /**
         * Adds an option with a display item.
         *
         * @param option the option value
         * @param item   the display item for this option
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> option(@NotNull T option, @NotNull UnifiedItemStack item) {
            Objects.requireNonNull(option, "Option cannot be null");
            Objects.requireNonNull(item, "Item cannot be null");
            options.add(option);
            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(item);
            return this;
        }

        /**
         * Sets all options from an array.
         *
         * <p>Requires an item mapper to be set.
         *
         * @param options the options array
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        @SafeVarargs
        public final Builder<T> options(@NotNull T... options) {
            Collections.addAll(this.options, options);
            return this;
        }

        /**
         * Sets all options from a collection.
         *
         * <p>Requires an item mapper to be set.
         *
         * @param options the options collection
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> options(@NotNull Iterable<T> options) {
            for (T option : options) {
                this.options.add(option);
            }
            return this;
        }

        /**
         * Sets the function to map options to display items.
         *
         * @param mapper the mapping function
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> itemMapper(@NotNull Function<T, UnifiedItemStack> mapper) {
            this.itemMapper = Objects.requireNonNull(mapper, "Mapper cannot be null");
            return this;
        }

        /**
         * Sets the initial option by value.
         *
         * @param option the initial option
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> initialOption(@NotNull T option) {
            int index = options.indexOf(option);
            if (index >= 0) {
                this.initialIndex = index;
            }
            return this;
        }

        /**
         * Sets the initial option by index.
         *
         * @param index the initial index
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> initialIndex(int index) {
            this.initialIndex = index;
            return this;
        }

        /**
         * Sets the change handler.
         *
         * @param handler the handler called when the option changes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> onChange(@NotNull BiConsumer<ClickContext, T> handler) {
            this.changeHandler = Objects.requireNonNull(handler, "Handler cannot be null");
            return this;
        }

        /**
         * Sets a simple change handler without context.
         *
         * @param handler the handler called when the option changes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> onChange(@NotNull java.util.function.Consumer<T> handler) {
            Objects.requireNonNull(handler, "Handler cannot be null");
            this.changeHandler = (ctx, option) -> handler.accept(option);
            return this;
        }

        /**
         * Sets whether cycling wraps around at the ends.
         *
         * @param wrapAround true to wrap around
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> wrapAround(boolean wrapAround) {
            this.wrapAround = wrapAround;
            return this;
        }

        /**
         * Sets the required permission.
         *
         * @param permission the permission node
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> permission(@NotNull String permission) {
            this.permission = Objects.requireNonNull(permission, "Permission cannot be null");
            return this;
        }

        /**
         * Sets the click cooldown.
         *
         * @param milliseconds the cooldown in milliseconds
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> cooldown(long milliseconds) {
            if (milliseconds < 0) {
                throw new IllegalArgumentException("Cooldown cannot be negative");
            }
            this.cooldownMs = milliseconds;
            return this;
        }

        /**
         * Sets the visibility condition.
         *
         * @param condition the condition
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> visibleWhen(@NotNull java.util.function.Predicate<UnifiedPlayer> condition) {
            this.visibilityCondition = Objects.requireNonNull(condition, "Condition cannot be null");
            return this;
        }

        /**
         * Sets whether the button is initially enabled.
         *
         * @param enabled true to enable
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the cycle sound.
         *
         * @param sound the sound name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> cycleSound(@NotNull String sound) {
            this.cycleSound = Objects.requireNonNull(sound, "Sound cannot be null");
            return this;
        }

        /**
         * Sets the cycle sound with volume and pitch.
         *
         * @param sound  the sound name
         * @param volume the volume
         * @param pitch  the pitch
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> cycleSound(@NotNull String sound, float volume, float pitch) {
            this.cycleSound = Objects.requireNonNull(sound, "Sound cannot be null");
            this.soundVolume = volume;
            this.soundPitch = pitch;
            return this;
        }

        /**
         * Builds the cycle button.
         *
         * @return the configured cycle button
         * @throws IllegalStateException if required fields are not set
         * @since 1.0.0
         */
        @NotNull
        public CycleButton<T> build() {
            if (options.isEmpty()) {
                throw new IllegalStateException("At least one option must be set");
            }
            if (items == null && itemMapper == null) {
                throw new IllegalStateException("Either items or itemMapper must be set");
            }
            if (initialIndex < 0 || initialIndex >= options.size()) {
                initialIndex = 0;
            }
            return new CycleButton<>(this);
        }
    }
}
