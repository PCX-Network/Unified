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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fluent builder for creating {@link Button} instances.
 *
 * <p>ButtonBuilder provides a clean, chainable API for configuring buttons
 * with various properties including items, click handlers, permissions,
 * cooldowns, and state-specific displays.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple button
 * Button button = Button.builder()
 *     .item(ItemBuilder.of("minecraft:diamond").name("Click Me").build())
 *     .onClick(ctx -> ctx.player().sendMessage("Clicked!"))
 *     .build();
 *
 * // Button with multiple click handlers
 * Button multiHandler = Button.builder()
 *     .item(myItem)
 *     .onClick(Button.ClickType.LEFT, ctx -> doLeftAction())
 *     .onClick(Button.ClickType.RIGHT, ctx -> doRightAction())
 *     .onClick(Button.ClickType.SHIFT_LEFT, ctx -> doShiftAction())
 *     .build();
 *
 * // Button with all features
 * Button advanced = Button.builder()
 *     .item(myItem)
 *     .permission("myplugin.use")
 *     .cooldown(500) // 500ms cooldown
 *     .clickSound("minecraft:ui.button.click", 1.0f, 1.0f)
 *     .visibleWhen(player -> player.getLevel() >= 10)
 *     .stateItem(Button.ButtonState.DISABLED, disabledItem)
 *     .stateItem(Button.ButtonState.LOADING, loadingItem)
 *     .onClick(ctx -> doAction())
 *     .build();
 *
 * // Dynamic item supplier
 * Button dynamic = Button.builder()
 *     .item(() -> createDynamicItem())
 *     .onClick(ctx -> doAction())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Button
 */
public class ButtonBuilder {

    /**
     * The item supplier for the button.
     */
    Supplier<UnifiedItemStack> itemSupplier;

    /**
     * Click handlers mapped by click type.
     */
    final Map<Button.ClickType, Consumer<Button.ClickContext>> clickHandlers = new EnumMap<>(Button.ClickType.class);

    /**
     * The default click handler.
     */
    Consumer<Button.ClickContext> defaultHandler;

    /**
     * Permission required for the button.
     */
    String permission;

    /**
     * Cooldown in milliseconds.
     */
    long cooldownMs = 0;

    /**
     * Visibility condition.
     */
    Predicate<UnifiedPlayer> visibilityCondition;

    /**
     * Click sound.
     */
    String clickSound;

    /**
     * Sound volume.
     */
    float soundVolume = 1.0f;

    /**
     * Sound pitch.
     */
    float soundPitch = 1.0f;

    /**
     * Whether the button is enabled.
     */
    boolean enabled = true;

    /**
     * Initial button state.
     */
    Button.ButtonState initialState = Button.ButtonState.NORMAL;

    /**
     * State-specific items.
     */
    final Map<Button.ButtonState, Supplier<UnifiedItemStack>> stateItems = new EnumMap<>(Button.ButtonState.class);

    /**
     * Creates a new ButtonBuilder.
     */
    public ButtonBuilder() {
        // Default constructor
    }

    /**
     * Sets the display item for the button.
     *
     * @param item the item to display
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder item(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "Item cannot be null");
        this.itemSupplier = () -> item;
        return this;
    }

    /**
     * Sets a dynamic item supplier for the button.
     *
     * <p>The supplier is called each time the item needs to be displayed,
     * allowing for dynamic updates.
     *
     * @param supplier the item supplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder item(@NotNull Supplier<UnifiedItemStack> supplier) {
        this.itemSupplier = Objects.requireNonNull(supplier, "Item supplier cannot be null");
        return this;
    }

    /**
     * Creates an item with the specified type and name.
     *
     * @param type the item type (e.g., "minecraft:diamond")
     * @param name the display name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder item(@NotNull String type, @NotNull Component name) {
        return item(ItemBuilder.of(type).name(name).build());
    }

    /**
     * Creates an item with the specified type, name, and lore.
     *
     * @param type the item type
     * @param name the display name
     * @param lore the lore lines
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder item(@NotNull String type, @NotNull Component name, @NotNull List<Component> lore) {
        return item(ItemBuilder.of(type).name(name).lore(lore).build());
    }

    /**
     * Sets the default click handler.
     *
     * <p>This handler is used when no specific handler is registered for
     * the click type.
     *
     * @param handler the click handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder onClick(@NotNull Consumer<Button.ClickContext> handler) {
        this.defaultHandler = Objects.requireNonNull(handler, "Handler cannot be null");
        return this;
    }

    /**
     * Sets a click handler for a specific click type.
     *
     * @param clickType the click type
     * @param handler   the click handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder onClick(@NotNull Button.ClickType clickType,
                                 @NotNull Consumer<Button.ClickContext> handler) {
        Objects.requireNonNull(clickType, "Click type cannot be null");
        Objects.requireNonNull(handler, "Handler cannot be null");
        this.clickHandlers.put(clickType, handler);
        return this;
    }

    /**
     * Sets handlers for left and right click.
     *
     * @param leftHandler  the left click handler
     * @param rightHandler the right click handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder onClick(@NotNull Consumer<Button.ClickContext> leftHandler,
                                 @NotNull Consumer<Button.ClickContext> rightHandler) {
        onClick(Button.ClickType.LEFT, leftHandler);
        onClick(Button.ClickType.RIGHT, rightHandler);
        return this;
    }

    /**
     * Sets the required permission.
     *
     * <p>Players without this permission will not see or interact with the button.
     *
     * @param permission the permission node
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder permission(@NotNull String permission) {
        this.permission = Objects.requireNonNull(permission, "Permission cannot be null");
        return this;
    }

    /**
     * Sets the click cooldown.
     *
     * @param milliseconds the cooldown in milliseconds
     * @return this builder
     * @throws IllegalArgumentException if milliseconds is negative
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder cooldown(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("Cooldown cannot be negative");
        }
        this.cooldownMs = milliseconds;
        return this;
    }

    /**
     * Sets the click sound.
     *
     * @param sound the sound name (e.g., "minecraft:ui.button.click")
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder clickSound(@NotNull String sound) {
        this.clickSound = Objects.requireNonNull(sound, "Sound cannot be null");
        return this;
    }

    /**
     * Sets the click sound with volume and pitch.
     *
     * @param sound  the sound name
     * @param volume the volume (0.0 - 1.0)
     * @param pitch  the pitch (0.5 - 2.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder clickSound(@NotNull String sound, float volume, float pitch) {
        this.clickSound = Objects.requireNonNull(sound, "Sound cannot be null");
        this.soundVolume = volume;
        this.soundPitch = pitch;
        return this;
    }

    /**
     * Sets the visibility condition.
     *
     * @param condition the condition predicate
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder visibleWhen(@NotNull Predicate<UnifiedPlayer> condition) {
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
    public ButtonBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Sets the initial button state.
     *
     * @param state the initial state
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder initialState(@NotNull Button.ButtonState state) {
        this.initialState = Objects.requireNonNull(state, "State cannot be null");
        return this;
    }

    /**
     * Sets the item to display for a specific state.
     *
     * @param state the button state
     * @param item  the item to display in that state
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder stateItem(@NotNull Button.ButtonState state, @NotNull UnifiedItemStack item) {
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(item, "Item cannot be null");
        this.stateItems.put(state, () -> item);
        return this;
    }

    /**
     * Sets a dynamic item supplier for a specific state.
     *
     * @param state    the button state
     * @param supplier the item supplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder stateItem(@NotNull Button.ButtonState state,
                                   @NotNull Supplier<UnifiedItemStack> supplier) {
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.stateItems.put(state, supplier);
        return this;
    }

    /**
     * Configures the disabled state item.
     *
     * @param item the item to show when disabled
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder disabledItem(@NotNull UnifiedItemStack item) {
        return stateItem(Button.ButtonState.DISABLED, item);
    }

    /**
     * Configures the loading state item.
     *
     * @param item the item to show when loading
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder loadingItem(@NotNull UnifiedItemStack item) {
        return stateItem(Button.ButtonState.LOADING, item);
    }

    /**
     * Applies a consumer function to this builder.
     *
     * @param consumer the consumer to apply
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder apply(@NotNull Consumer<ButtonBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    /**
     * Conditionally applies a consumer function.
     *
     * @param condition the condition
     * @param consumer  the consumer to apply if condition is true
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public ButtonBuilder applyIf(boolean condition, @NotNull Consumer<ButtonBuilder> consumer) {
        if (condition) {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Builds the button.
     *
     * @return the configured button
     * @throws IllegalStateException if required fields are not set
     * @since 1.0.0
     */
    @NotNull
    public Button build() {
        if (itemSupplier == null) {
            throw new IllegalStateException("Item must be set before building");
        }
        return new Button(this);
    }
}
