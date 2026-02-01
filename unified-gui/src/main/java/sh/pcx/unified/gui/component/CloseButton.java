/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A button that closes the current GUI when clicked.
 *
 * <p>CloseButton provides a standard way to close GUIs with optional
 * pre-close callbacks for cleanup or confirmation.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Closes the player's inventory</li>
 *   <li>Customizable display item</li>
 *   <li>Optional click sound</li>
 *   <li>Pre-close callback for cleanup</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Default close button
 * CloseButton close = CloseButton.create();
 *
 * // Custom close button
 * CloseButton customClose = CloseButton.closeButtonBuilder()
 *     .item(ItemBuilder.of("minecraft:barrier")
 *         .name(Component.text("Close Menu", NamedTextColor.RED))
 *         .build())
 *     .clickSound("minecraft:block.wooden_door.close")
 *     .beforeClose(ctx -> savePlayerData(ctx.player()))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BackButton
 */
public class CloseButton extends Button {

    /**
     * The default item type for close buttons.
     */
    public static final String DEFAULT_ITEM_TYPE = "minecraft:barrier";

    /**
     * The default display name for close buttons.
     */
    public static final Component DEFAULT_NAME = Component.text("Close", NamedTextColor.RED);

    /**
     * Action to perform before closing.
     */
    private final Consumer<ClickContext> beforeClose;

    /**
     * Constructs a CloseButton with the builder configuration.
     *
     * @param builder the builder
     */
    protected CloseButton(@NotNull Builder builder) {
        super(createButtonBuilder(builder));
        this.beforeClose = builder.beforeClose;
    }

    /**
     * Creates the underlying button builder.
     */
    private static ButtonBuilder createButtonBuilder(@NotNull Builder builder) {
        ButtonBuilder buttonBuilder = new ButtonBuilder();
        buttonBuilder.itemSupplier = builder.itemSupplier != null
            ? builder.itemSupplier
            : () -> ItemBuilder.of(DEFAULT_ITEM_TYPE)
                .name(DEFAULT_NAME)
                .build();
        buttonBuilder.clickSound = builder.clickSound;
        buttonBuilder.soundVolume = builder.soundVolume;
        buttonBuilder.soundPitch = builder.soundPitch;
        buttonBuilder.permission = builder.permission;
        buttonBuilder.enabled = true;
        return buttonBuilder;
    }

    /**
     * Creates a default close button.
     *
     * @return a new CloseButton with default settings
     * @since 1.0.0
     */
    @NotNull
    public static CloseButton create() {
        return closeButtonBuilder().build();
    }

    /**
     * Creates a close button with a custom item.
     *
     * @param item the display item
     * @return a new CloseButton
     * @since 1.0.0
     */
    @NotNull
    public static CloseButton create(@NotNull UnifiedItemStack item) {
        return closeButtonBuilder().item(item).build();
    }

    /**
     * Creates a new CloseButton builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder closeButtonBuilder() {
        return new Builder();
    }

    @Override
    public void handleClick(@NotNull ClickContext context) {
        if (!isEnabled()) {
            return;
        }

        if (!isVisibleTo(context.player())) {
            return;
        }

        // Play click sound (handled by parent if configured)
        super.handleClick(context);

        // Execute pre-close callback
        if (beforeClose != null) {
            beforeClose.accept(context);
        }

        // Close the inventory
        context.closeInventory();
    }

    /**
     * Builder for creating CloseButton instances.
     *
     * @since 1.0.0
     */
    public static class Builder {

        Supplier<UnifiedItemStack> itemSupplier;
        String clickSound = "minecraft:ui.button.click";
        float soundVolume = 0.5f;
        float soundPitch = 1.0f;
        String permission;
        Consumer<ClickContext> beforeClose;

        /**
         * Creates a new Builder.
         */
        Builder() {
            // Package-private constructor
        }

        /**
         * Sets the display item.
         *
         * @param item the item
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder item(@NotNull UnifiedItemStack item) {
            Objects.requireNonNull(item, "Item cannot be null");
            this.itemSupplier = () -> item;
            return this;
        }

        /**
         * Sets a dynamic item supplier.
         *
         * @param supplier the item supplier
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder item(@NotNull Supplier<UnifiedItemStack> supplier) {
            this.itemSupplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
            return this;
        }

        /**
         * Creates an item from type and name.
         *
         * @param type the item type
         * @param name the display name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder item(@NotNull String type, @NotNull Component name) {
            return item(ItemBuilder.of(type).name(name).build());
        }

        /**
         * Sets the click sound.
         *
         * @param sound the sound name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder clickSound(@Nullable String sound) {
            this.clickSound = sound;
            return this;
        }

        /**
         * Sets the click sound with volume and pitch.
         *
         * @param sound  the sound name
         * @param volume the volume
         * @param pitch  the pitch
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder clickSound(@NotNull String sound, float volume, float pitch) {
            this.clickSound = Objects.requireNonNull(sound, "Sound cannot be null");
            this.soundVolume = volume;
            this.soundPitch = pitch;
            return this;
        }

        /**
         * Disables the click sound.
         *
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder noSound() {
            this.clickSound = null;
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
         * Sets a callback to execute before closing.
         *
         * @param action the action to execute
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder beforeClose(@NotNull Consumer<ClickContext> action) {
            this.beforeClose = Objects.requireNonNull(action, "Action cannot be null");
            return this;
        }

        /**
         * Builds the close button.
         *
         * @return the configured close button
         * @since 1.0.0
         */
        @NotNull
        public CloseButton build() {
            return new CloseButton(this);
        }
    }
}
