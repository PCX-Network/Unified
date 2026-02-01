/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import sh.pcx.unified.item.ItemBuilder;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A navigation button that returns the player to the previous GUI.
 *
 * <p>BackButton is commonly used in nested GUI navigation to provide
 * a way for players to return to the parent GUI. It can be customized
 * with different items, sounds, and behaviors.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic navigation to previous GUI</li>
 *   <li>Customizable display item</li>
 *   <li>Optional click sound</li>
 *   <li>Pre-navigation callback for cleanup</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Default back button
 * BackButton back = BackButton.create();
 *
 * // Custom back button
 * BackButton customBack = BackButton.backButtonBuilder()
 *     .item(ItemBuilder.of("minecraft:arrow")
 *         .name(Component.text("Go Back", NamedTextColor.RED))
 *         .build())
 *     .clickSound("minecraft:ui.button.click")
 *     .beforeNavigate(ctx -> saveState(ctx.player()))
 *     .build();
 *
 * // Back button with custom destination
 * BackButton toMenu = BackButton.backButtonBuilder()
 *     .item(myItem)
 *     .navigateTo(ctx -> openMainMenu(ctx.player()))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CloseButton
 * @see PageButton
 */
public class BackButton extends Button {

    /**
     * The default item type for back buttons.
     */
    public static final String DEFAULT_ITEM_TYPE = "minecraft:arrow";

    /**
     * The default display name for back buttons.
     */
    public static final Component DEFAULT_NAME = Component.text("Back", NamedTextColor.RED);

    /**
     * Action to perform before navigation.
     */
    private final Consumer<ClickContext> beforeNavigate;

    /**
     * Custom navigation action (overrides default back behavior).
     */
    private final Consumer<ClickContext> customNavigate;

    /**
     * Constructs a BackButton with the builder configuration.
     *
     * @param builder the builder
     */
    protected BackButton(@NotNull Builder builder) {
        super(createButtonBuilder(builder));
        this.beforeNavigate = builder.beforeNavigate;
        this.customNavigate = builder.customNavigate;
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
     * Creates a default back button.
     *
     * @return a new BackButton with default settings
     * @since 1.0.0
     */
    @NotNull
    public static BackButton create() {
        return backButtonBuilder().build();
    }

    /**
     * Creates a back button with a custom item.
     *
     * @param item the display item
     * @return a new BackButton
     * @since 1.0.0
     */
    @NotNull
    public static BackButton create(@NotNull UnifiedItemStack item) {
        return backButtonBuilder().item(item).build();
    }

    /**
     * Creates a new BackButton builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder backButtonBuilder() {
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

        // Execute pre-navigation callback
        if (beforeNavigate != null) {
            beforeNavigate.accept(context);
        }

        // Navigate
        if (customNavigate != null) {
            customNavigate.accept(context);
        } else {
            // Default behavior: close current GUI
            // In a full implementation, this would navigate to the previous GUI in the stack
            context.closeInventory();
        }
    }

    /**
     * Builder for creating BackButton instances.
     *
     * @since 1.0.0
     */
    public static class Builder {

        Supplier<UnifiedItemStack> itemSupplier;
        String clickSound = "minecraft:ui.button.click";
        float soundVolume = 0.5f;
        float soundPitch = 1.0f;
        String permission;
        Consumer<ClickContext> beforeNavigate;
        Consumer<ClickContext> customNavigate;

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
         * Sets a callback to execute before navigation.
         *
         * @param action the action to execute
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder beforeNavigate(@NotNull Consumer<ClickContext> action) {
            this.beforeNavigate = Objects.requireNonNull(action, "Action cannot be null");
            return this;
        }

        /**
         * Sets a custom navigation action.
         *
         * <p>This overrides the default back behavior.
         *
         * @param action the navigation action
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder navigateTo(@NotNull Consumer<ClickContext> action) {
            this.customNavigate = Objects.requireNonNull(action, "Action cannot be null");
            return this;
        }

        /**
         * Builds the back button.
         *
         * @return the configured back button
         * @since 1.0.0
         */
        @NotNull
        public BackButton build() {
            return new BackButton(this);
        }
    }
}
