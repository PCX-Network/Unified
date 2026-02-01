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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Base button component for GUI interactions.
 *
 * <p>Button represents a clickable element in a GUI that responds to player
 * interactions. It supports multiple states, different click types, cooldowns,
 * permissions, and conditional visibility.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Multiple click type handlers (left, right, shift, etc.)</li>
 *   <li>State-based item display</li>
 *   <li>Click cooldowns to prevent spam</li>
 *   <li>Permission-based visibility and interaction</li>
 *   <li>Sound feedback on click</li>
 *   <li>Conditional visibility</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple button with single click handler
 * Button simple = Button.builder()
 *     .item(ItemBuilder.of("minecraft:diamond")
 *         .name(Component.text("Click Me!"))
 *         .build())
 *     .onClick(ctx -> ctx.player().sendMessage("Clicked!"))
 *     .build();
 *
 * // Button with different handlers for left/right click
 * Button multiClick = Button.builder()
 *     .item(myItem)
 *     .onClick(ClickType.LEFT, ctx -> doLeftAction())
 *     .onClick(ClickType.RIGHT, ctx -> doRightAction())
 *     .onClick(ClickType.SHIFT_LEFT, ctx -> doShiftLeftAction())
 *     .build();
 *
 * // Button with cooldown and permission
 * Button restricted = Button.builder()
 *     .item(myItem)
 *     .permission("myplugin.admin")
 *     .cooldown(1000) // 1 second cooldown
 *     .onClick(ctx -> doAdminAction())
 *     .build();
 *
 * // Conditional button
 * Button conditional = Button.builder()
 *     .item(myItem)
 *     .visibleWhen(player -> player.hasPermission("show.button"))
 *     .onClick(ctx -> doAction())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ButtonBuilder
 * @see ToggleButton
 * @see CycleButton
 */
public class Button implements GUIComponent {

    /**
     * The item supplier for this button.
     */
    private final Supplier<UnifiedItemStack> itemSupplier;

    /**
     * Click handlers mapped by click type.
     */
    private final Map<ClickType, Consumer<ClickContext>> clickHandlers;

    /**
     * The default click handler (used when no specific handler is registered).
     */
    private final Consumer<ClickContext> defaultHandler;

    /**
     * Permission required to see/use this button.
     */
    private final String permission;

    /**
     * Cooldown in milliseconds between clicks.
     */
    private final long cooldownMs;

    /**
     * Condition for button visibility.
     */
    private final Predicate<UnifiedPlayer> visibilityCondition;

    /**
     * Sound to play on click (null for no sound).
     */
    private final String clickSound;

    /**
     * Volume of the click sound.
     */
    private final float soundVolume;

    /**
     * Pitch of the click sound.
     */
    private final float soundPitch;

    /**
     * Whether this button is currently enabled.
     */
    private volatile boolean enabled;

    /**
     * The current state of this button.
     */
    private volatile ButtonState state;

    /**
     * State-specific item overrides.
     */
    private final Map<ButtonState, Supplier<UnifiedItemStack>> stateItems;

    /**
     * Last click timestamps per player for cooldown tracking.
     */
    private final Map<java.util.UUID, Long> cooldownTracker;

    /**
     * Constructs a new Button with the specified configuration.
     *
     * @param builder the builder containing configuration
     */
    protected Button(@NotNull ButtonBuilder builder) {
        this.itemSupplier = Objects.requireNonNull(builder.itemSupplier, "Item supplier cannot be null");
        this.clickHandlers = new EnumMap<>(builder.clickHandlers);
        this.defaultHandler = builder.defaultHandler;
        this.permission = builder.permission;
        this.cooldownMs = builder.cooldownMs;
        this.visibilityCondition = builder.visibilityCondition;
        this.clickSound = builder.clickSound;
        this.soundVolume = builder.soundVolume;
        this.soundPitch = builder.soundPitch;
        this.enabled = builder.enabled;
        this.state = builder.initialState;
        this.stateItems = new EnumMap<>(builder.stateItems);
        this.cooldownTracker = new java.util.concurrent.ConcurrentHashMap<>();
    }

    /**
     * Creates a new ButtonBuilder.
     *
     * @return a new ButtonBuilder instance
     * @since 1.0.0
     */
    @NotNull
    public static ButtonBuilder builder() {
        return new ButtonBuilder();
    }

    /**
     * Creates a simple button with an item and click handler.
     *
     * @param item    the button item
     * @param onClick the click handler
     * @return the created button
     * @since 1.0.0
     */
    @NotNull
    public static Button of(@NotNull UnifiedItemStack item, @NotNull Consumer<ClickContext> onClick) {
        return builder()
            .item(item)
            .onClick(onClick)
            .build();
    }

    /**
     * Creates a simple button with an item type and click handler.
     *
     * @param itemType the item type (e.g., "minecraft:diamond")
     * @param name     the display name
     * @param onClick  the click handler
     * @return the created button
     * @since 1.0.0
     */
    @NotNull
    public static Button of(@NotNull String itemType, @NotNull Component name,
                            @NotNull Consumer<ClickContext> onClick) {
        return builder()
            .item(ItemBuilder.of(itemType).name(name).build())
            .onClick(onClick)
            .build();
    }

    /**
     * Returns the item to display for this button.
     *
     * <p>The returned item depends on the current state and any state-specific
     * item overrides configured for this button.
     *
     * @return the current display item
     * @since 1.0.0
     */
    @Override
    @NotNull
    public UnifiedItemStack getItem() {
        // Check for state-specific item
        Supplier<UnifiedItemStack> stateSupplier = stateItems.get(state);
        if (stateSupplier != null) {
            return stateSupplier.get();
        }
        return itemSupplier.get();
    }

    /**
     * Returns the item to display for a specific player.
     *
     * <p>This method considers player-specific visibility and permission checks.
     *
     * @param player the player viewing the GUI
     * @return the item to display, or null if the button should be hidden
     * @since 1.0.0
     */
    @Nullable
    public UnifiedItemStack getItemFor(@NotNull UnifiedPlayer player) {
        if (!isVisibleTo(player)) {
            return null;
        }
        return getItem();
    }

    /**
     * Handles a click on this button.
     *
     * @param context the click context
     * @since 1.0.0
     */
    public void handleClick(@NotNull ClickContext context) {
        UnifiedPlayer player = context.player();

        // Check if enabled
        if (!enabled) {
            return;
        }

        // Check permission
        if (permission != null && !player.hasPermission(permission)) {
            return;
        }

        // Check visibility
        if (!isVisibleTo(player)) {
            return;
        }

        // Check cooldown
        if (!checkCooldown(player)) {
            return;
        }

        // Play click sound
        if (clickSound != null) {
            player.playSound(clickSound, soundVolume, soundPitch);
        }

        // Find appropriate handler
        Consumer<ClickContext> handler = clickHandlers.get(context.clickType());
        if (handler == null) {
            handler = defaultHandler;
        }

        // Execute handler
        if (handler != null) {
            handler.accept(context);
        }
    }

    /**
     * Checks and updates the cooldown for a player.
     *
     * @param player the player
     * @return true if the click should proceed, false if on cooldown
     */
    private boolean checkCooldown(@NotNull UnifiedPlayer player) {
        if (cooldownMs <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        Long lastClick = cooldownTracker.get(player.getUniqueId());

        if (lastClick != null && now - lastClick < cooldownMs) {
            return false;
        }

        cooldownTracker.put(player.getUniqueId(), now);
        return true;
    }

    /**
     * Checks if this button is visible to a player.
     *
     * @param player the player
     * @return true if the button should be visible
     * @since 1.0.0
     */
    public boolean isVisibleTo(@NotNull UnifiedPlayer player) {
        // Check permission
        if (permission != null && !player.hasPermission(permission)) {
            return false;
        }

        // Check visibility condition
        if (visibilityCondition != null && !visibilityCondition.test(player)) {
            return false;
        }

        return true;
    }

    /**
     * Returns whether this button is currently enabled.
     *
     * @return true if enabled
     * @since 1.0.0
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this button is enabled.
     *
     * @param enabled true to enable, false to disable
     * @since 1.0.0
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the current state of this button.
     *
     * @return the current state
     * @since 1.0.0
     */
    @NotNull
    public ButtonState getState() {
        return state;
    }

    /**
     * Sets the state of this button.
     *
     * @param state the new state
     * @since 1.0.0
     */
    public void setState(@NotNull ButtonState state) {
        this.state = Objects.requireNonNull(state);
    }

    /**
     * Returns the permission required for this button.
     *
     * @return the permission node, or null if none required
     * @since 1.0.0
     */
    @Nullable
    public String getPermission() {
        return permission;
    }

    /**
     * Returns the cooldown in milliseconds.
     *
     * @return the cooldown in milliseconds
     * @since 1.0.0
     */
    public long getCooldownMs() {
        return cooldownMs;
    }

    /**
     * Clears the cooldown for a specific player.
     *
     * @param player the player
     * @since 1.0.0
     */
    public void clearCooldown(@NotNull UnifiedPlayer player) {
        cooldownTracker.remove(player.getUniqueId());
    }

    /**
     * Clears all cooldown tracking.
     *
     * @since 1.0.0
     */
    public void clearAllCooldowns() {
        cooldownTracker.clear();
    }

    /**
     * Types of clicks that can be performed on a button.
     *
     * @since 1.0.0
     */
    public enum ClickType {
        /**
         * Left mouse click.
         */
        LEFT,

        /**
         * Right mouse click.
         */
        RIGHT,

        /**
         * Shift + left click.
         */
        SHIFT_LEFT,

        /**
         * Shift + right click.
         */
        SHIFT_RIGHT,

        /**
         * Middle mouse click (scroll wheel).
         */
        MIDDLE,

        /**
         * Double left click.
         */
        DOUBLE_CLICK,

        /**
         * Drop key pressed (Q by default).
         */
        DROP,

        /**
         * Ctrl + drop key pressed.
         */
        CONTROL_DROP,

        /**
         * Number key pressed (1-9).
         */
        NUMBER_KEY,

        /**
         * Any click type (used for catch-all handlers).
         */
        ANY
    }

    /**
     * States a button can be in.
     *
     * @since 1.0.0
     */
    public enum ButtonState {
        /**
         * Normal/default state.
         */
        NORMAL,

        /**
         * Hovered state (for preview purposes).
         */
        HOVERED,

        /**
         * Pressed/active state.
         */
        PRESSED,

        /**
         * Disabled state.
         */
        DISABLED,

        /**
         * Success state (e.g., after successful action).
         */
        SUCCESS,

        /**
         * Error state (e.g., after failed action).
         */
        ERROR,

        /**
         * Loading state (e.g., during async operation).
         */
        LOADING
    }

    /**
     * Context provided to click handlers.
     *
     * <p>Contains information about the click event including the player,
     * click type, slot position, and the button itself.
     *
     * @param player    the player who clicked
     * @param clickType the type of click performed
     * @param slot      the slot that was clicked
     * @param button    the button that was clicked
     * @param hotbarKey the hotbar key pressed (0-8), or -1 if not applicable
     * @since 1.0.0
     */
    public record ClickContext(
        @NotNull UnifiedPlayer player,
        @NotNull ClickType clickType,
        int slot,
        @NotNull Button button,
        int hotbarKey
    ) {
        /**
         * Constructs a ClickContext without hotbar key.
         *
         * @param player    the player who clicked
         * @param clickType the type of click performed
         * @param slot      the slot that was clicked
         * @param button    the button that was clicked
         */
        public ClickContext(@NotNull UnifiedPlayer player, @NotNull ClickType clickType,
                           int slot, @NotNull Button button) {
            this(player, clickType, slot, button, -1);
        }

        /**
         * Checks if this was a left click (including shift-left).
         *
         * @return true if left click
         */
        public boolean isLeftClick() {
            return clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT;
        }

        /**
         * Checks if this was a right click (including shift-right).
         *
         * @return true if right click
         */
        public boolean isRightClick() {
            return clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT;
        }

        /**
         * Checks if shift was held during the click.
         *
         * @return true if shift was held
         */
        public boolean isShiftClick() {
            return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT;
        }

        /**
         * Sends a message to the player.
         *
         * @param message the message to send
         */
        public void sendMessage(@NotNull Component message) {
            player.sendMessage(message);
        }

        /**
         * Sends a message to the player.
         *
         * @param message the message to send
         */
        public void sendMessage(@NotNull String message) {
            player.sendMessage(Component.text(message));
        }

        /**
         * Closes the player's inventory.
         */
        public void closeInventory() {
            player.closeInventory();
        }

        /**
         * Plays a sound to the player.
         *
         * @param sound  the sound name
         * @param volume the volume
         * @param pitch  the pitch
         */
        public void playSound(@NotNull String sound, float volume, float pitch) {
            player.playSound(sound, volume, pitch);
        }
    }
}
