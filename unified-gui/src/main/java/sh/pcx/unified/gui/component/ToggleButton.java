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

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A button that toggles between enabled and disabled states.
 *
 * <p>ToggleButton provides a convenient way to create on/off switches in GUIs.
 * It displays different items based on its current state and notifies a handler
 * when the state changes.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Distinct items for enabled and disabled states</li>
 *   <li>State change callbacks with player context</li>
 *   <li>Optional sounds for enable/disable actions</li>
 *   <li>Support for dynamic item suppliers</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple toggle
 * ToggleButton toggle = ToggleButton.toggleButtonBuilder()
 *     .enabledItem(ItemBuilder.of("minecraft:lime_wool")
 *         .name(Component.text("Feature: ON"))
 *         .build())
 *     .disabledItem(ItemBuilder.of("minecraft:red_wool")
 *         .name(Component.text("Feature: OFF"))
 *         .build())
 *     .initialState(false)
 *     .onToggle((ctx, enabled) -> {
 *         setFeatureEnabled(ctx.player(), enabled);
 *         ctx.sendMessage("Feature is now " + (enabled ? "ON" : "OFF"));
 *     })
 *     .build();
 *
 * // Toggle with sounds
 * ToggleButton soundToggle = ToggleButton.toggleButtonBuilder()
 *     .enabledItem(enabledItem)
 *     .disabledItem(disabledItem)
 *     .enableSound("minecraft:block.note_block.pling", 1.0f, 2.0f)
 *     .disableSound("minecraft:block.note_block.bass", 1.0f, 0.5f)
 *     .onToggle((ctx, enabled) -> handleToggle(enabled))
 *     .build();
 *
 * // Dynamic toggle with player-specific state
 * ToggleButton playerToggle = ToggleButton.toggleButtonBuilder()
 *     .enabledItem(() -> createEnabledItem())
 *     .disabledItem(() -> createDisabledItem())
 *     .initialStateProvider(player -> getPlayerSetting(player))
 *     .onToggle((ctx, enabled) -> setPlayerSetting(ctx.player(), enabled))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Button
 * @see CycleButton
 */
public class ToggleButton extends Button {

    /**
     * The item to display when enabled.
     */
    private final Supplier<UnifiedItemStack> enabledItemSupplier;

    /**
     * The item to display when disabled.
     */
    private final Supplier<UnifiedItemStack> disabledItemSupplier;

    /**
     * The current toggle state.
     */
    private volatile boolean toggled;

    /**
     * Handler called when the state changes.
     */
    private final BiConsumer<ClickContext, Boolean> toggleHandler;

    /**
     * Sound to play when enabled.
     */
    private final String enableSound;

    /**
     * Sound to play when disabled.
     */
    private final String disableSound;

    /**
     * Volume for enable sound.
     */
    private final float enableSoundVolume;

    /**
     * Pitch for enable sound.
     */
    private final float enableSoundPitch;

    /**
     * Volume for disable sound.
     */
    private final float disableSoundVolume;

    /**
     * Pitch for disable sound.
     */
    private final float disableSoundPitch;

    /**
     * Constructs a ToggleButton with the specified builder configuration.
     *
     * @param builder the builder
     */
    protected ToggleButton(@NotNull Builder builder) {
        super(createButtonBuilder(builder));
        this.enabledItemSupplier = builder.enabledItemSupplier;
        this.disabledItemSupplier = builder.disabledItemSupplier;
        this.toggled = builder.initialState;
        this.toggleHandler = builder.toggleHandler;
        this.enableSound = builder.enableSound;
        this.disableSound = builder.disableSound;
        this.enableSoundVolume = builder.enableSoundVolume;
        this.enableSoundPitch = builder.enableSoundPitch;
        this.disableSoundVolume = builder.disableSoundVolume;
        this.disableSoundPitch = builder.disableSoundPitch;
    }

    /**
     * Creates the underlying button builder.
     */
    private static ButtonBuilder createButtonBuilder(@NotNull Builder builder) {
        ButtonBuilder buttonBuilder = new ButtonBuilder();
        buttonBuilder.itemSupplier = () -> builder.initialState
            ? builder.enabledItemSupplier.get()
            : builder.disabledItemSupplier.get();
        buttonBuilder.permission = builder.permission;
        buttonBuilder.cooldownMs = builder.cooldownMs;
        buttonBuilder.visibilityCondition = builder.visibilityCondition;
        buttonBuilder.enabled = builder.enabled;
        return buttonBuilder;
    }

    /**
     * Creates a new ToggleButton builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder toggleButtonBuilder() {
        return new Builder();
    }

    /**
     * Creates a simple toggle button.
     *
     * @param enabledItem  the item when enabled
     * @param disabledItem the item when disabled
     * @param onToggle     the toggle handler
     * @return the toggle button
     * @since 1.0.0
     */
    @NotNull
    public static ToggleButton of(@NotNull UnifiedItemStack enabledItem,
                                   @NotNull UnifiedItemStack disabledItem,
                                   @NotNull BiConsumer<ClickContext, Boolean> onToggle) {
        return toggleButtonBuilder()
            .enabledItem(enabledItem)
            .disabledItem(disabledItem)
            .onToggle(onToggle)
            .build();
    }

    @Override
    @NotNull
    public UnifiedItemStack getItem() {
        return toggled ? enabledItemSupplier.get() : disabledItemSupplier.get();
    }

    @Override
    public void handleClick(@NotNull ClickContext context) {
        if (!isEnabled()) {
            return;
        }

        // Check permission and visibility from parent
        if (!isVisibleTo(context.player())) {
            return;
        }

        // Toggle the state
        toggled = !toggled;

        // Play appropriate sound
        if (toggled && enableSound != null) {
            context.player().playSound(enableSound, enableSoundVolume, enableSoundPitch);
        } else if (!toggled && disableSound != null) {
            context.player().playSound(disableSound, disableSoundVolume, disableSoundPitch);
        }

        // Notify handler
        if (toggleHandler != null) {
            toggleHandler.accept(context, toggled);
        }
    }

    /**
     * Returns the current toggle state.
     *
     * @return true if enabled/on, false if disabled/off
     * @since 1.0.0
     */
    public boolean isToggled() {
        return toggled;
    }

    /**
     * Sets the toggle state without triggering the handler.
     *
     * @param toggled the new state
     * @since 1.0.0
     */
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    /**
     * Toggles the state without triggering the handler.
     *
     * @return the new state after toggling
     * @since 1.0.0
     */
    public boolean toggle() {
        this.toggled = !this.toggled;
        return this.toggled;
    }

    /**
     * Builder for creating ToggleButton instances.
     *
     * @since 1.0.0
     */
    public static class Builder {

        Supplier<UnifiedItemStack> enabledItemSupplier;
        Supplier<UnifiedItemStack> disabledItemSupplier;
        boolean initialState = false;
        BiConsumer<ClickContext, Boolean> toggleHandler;
        String permission;
        long cooldownMs = 0;
        java.util.function.Predicate<UnifiedPlayer> visibilityCondition;
        boolean enabled = true;
        String enableSound;
        String disableSound;
        float enableSoundVolume = 1.0f;
        float enableSoundPitch = 1.0f;
        float disableSoundVolume = 1.0f;
        float disableSoundPitch = 1.0f;

        /**
         * Creates a new Builder.
         */
        Builder() {
            // Package-private constructor
        }

        /**
         * Sets the item to display when enabled.
         *
         * @param item the enabled item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder enabledItem(@NotNull UnifiedItemStack item) {
            Objects.requireNonNull(item, "Item cannot be null");
            this.enabledItemSupplier = () -> item;
            return this;
        }

        /**
         * Sets a dynamic item supplier for the enabled state.
         *
         * @param supplier the item supplier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder enabledItem(@NotNull Supplier<UnifiedItemStack> supplier) {
            this.enabledItemSupplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
            return this;
        }

        /**
         * Sets the item to display when disabled.
         *
         * @param item the disabled item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disabledItem(@NotNull UnifiedItemStack item) {
            Objects.requireNonNull(item, "Item cannot be null");
            this.disabledItemSupplier = () -> item;
            return this;
        }

        /**
         * Sets a dynamic item supplier for the disabled state.
         *
         * @param supplier the item supplier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disabledItem(@NotNull Supplier<UnifiedItemStack> supplier) {
            this.disabledItemSupplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
            return this;
        }

        /**
         * Creates items from type and names.
         *
         * @param enabledType  the enabled item type
         * @param enabledName  the enabled item name
         * @param disabledType the disabled item type
         * @param disabledName the disabled item name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder items(@NotNull String enabledType, @NotNull Component enabledName,
                            @NotNull String disabledType, @NotNull Component disabledName) {
            enabledItem(ItemBuilder.of(enabledType).name(enabledName).build());
            disabledItem(ItemBuilder.of(disabledType).name(disabledName).build());
            return this;
        }

        /**
         * Sets the initial toggle state.
         *
         * @param enabled true for enabled, false for disabled
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder initialState(boolean enabled) {
            this.initialState = enabled;
            return this;
        }

        /**
         * Sets the toggle handler.
         *
         * @param handler the handler called when toggled
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder onToggle(@NotNull BiConsumer<ClickContext, Boolean> handler) {
            this.toggleHandler = Objects.requireNonNull(handler, "Handler cannot be null");
            return this;
        }

        /**
         * Sets a simple toggle handler without context.
         *
         * @param handler the handler called when toggled
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder onToggle(@NotNull java.util.function.Consumer<Boolean> handler) {
            Objects.requireNonNull(handler, "Handler cannot be null");
            this.toggleHandler = (ctx, enabled) -> handler.accept(enabled);
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
        public Builder permission(@NotNull String permission) {
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
        public Builder cooldown(long milliseconds) {
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
        public Builder visibleWhen(@NotNull java.util.function.Predicate<UnifiedPlayer> condition) {
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
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the sound to play when enabled.
         *
         * @param sound the sound name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder enableSound(@NotNull String sound) {
            this.enableSound = Objects.requireNonNull(sound, "Sound cannot be null");
            return this;
        }

        /**
         * Sets the sound to play when enabled with volume and pitch.
         *
         * @param sound  the sound name
         * @param volume the volume
         * @param pitch  the pitch
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder enableSound(@NotNull String sound, float volume, float pitch) {
            this.enableSound = Objects.requireNonNull(sound, "Sound cannot be null");
            this.enableSoundVolume = volume;
            this.enableSoundPitch = pitch;
            return this;
        }

        /**
         * Sets the sound to play when disabled.
         *
         * @param sound the sound name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disableSound(@NotNull String sound) {
            this.disableSound = Objects.requireNonNull(sound, "Sound cannot be null");
            return this;
        }

        /**
         * Sets the sound to play when disabled with volume and pitch.
         *
         * @param sound  the sound name
         * @param volume the volume
         * @param pitch  the pitch
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder disableSound(@NotNull String sound, float volume, float pitch) {
            this.disableSound = Objects.requireNonNull(sound, "Sound cannot be null");
            this.disableSoundVolume = volume;
            this.disableSoundPitch = pitch;
            return this;
        }

        /**
         * Builds the toggle button.
         *
         * @return the configured toggle button
         * @throws IllegalStateException if required fields are not set
         * @since 1.0.0
         */
        @NotNull
        public ToggleButton build() {
            if (enabledItemSupplier == null) {
                throw new IllegalStateException("Enabled item must be set");
            }
            if (disabledItemSupplier == null) {
                throw new IllegalStateException("Disabled item must be set");
            }
            return new ToggleButton(this);
        }
    }
}
