/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents a single slot in a GUI with an item and click handler.
 *
 * <p>Slot combines an item display with click handling logic. Slots can be
 * static (fixed item) or dynamic (item from supplier), and can have visibility
 * conditions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Static slot with click handler
 * Slot button = Slot.of(diamondItem)
 *     .onClick(click -> {
 *         giveReward(click.getPlayer());
 *         return ClickResult.CLOSE;
 *     });
 *
 * // Dynamic slot that updates on refresh
 * Slot counter = Slot.dynamic(() -> createCounterItem(getCount()))
 *     .onClick(click -> {
 *         incrementCount();
 *         return ClickResult.refreshSlot(click.getSlot());
 *     });
 *
 * // Conditional visibility
 * Slot adminButton = Slot.of(adminPanelItem)
 *     .visibleIf(click -> click.getPlayer().hasPermission("admin"))
 *     .onClick(click -> navigateToAdminPanel());
 *
 * // Slot with different handlers per click type
 * Slot bidirectional = Slot.of(valueItem)
 *     .onClick(click -> {
 *         if (click.isLeftClick()) {
 *             increment();
 *         } else if (click.isRightClick()) {
 *             decrement();
 *         }
 *         return ClickResult.REFRESH;
 *     });
 * }</pre>
 *
 * <h2>Content Slot</h2>
 * <pre>{@code
 * // Marker for pagination content areas
 * Slot.CONTENT  // Used in patterns to mark content slots
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickHandler
 * @see SlotPattern
 * @see AbstractGUI
 */
public final class Slot {

    /**
     * Marker slot for pagination content areas.
     *
     * <p>Use this in slot patterns to indicate where paginated
     * content should be placed.
     */
    public static final Slot CONTENT = new Slot(null, null, ClickHandler.DENY, ctx -> true);

    /**
     * Empty slot that allows clicks (for item manipulation).
     */
    public static final Slot EMPTY_ALLOW = new Slot(null, null, ClickHandler.ALLOW, ctx -> true);

    /**
     * Empty slot that denies clicks (default GUI behavior).
     */
    public static final Slot EMPTY_DENY = new Slot(null, null, ClickHandler.DENY, ctx -> true);

    private final UnifiedItemStack item;
    private final Supplier<UnifiedItemStack> itemSupplier;
    private final ClickHandler clickHandler;
    private final Predicate<GUIContext> visibilityCondition;

    /**
     * Creates a new slot with the specified properties.
     *
     * @param item                the static item (or null if using supplier)
     * @param itemSupplier        the dynamic item supplier (or null if static)
     * @param clickHandler        the click handler
     * @param visibilityCondition the visibility condition
     */
    private Slot(
            @Nullable UnifiedItemStack item,
            @Nullable Supplier<UnifiedItemStack> itemSupplier,
            @NotNull ClickHandler clickHandler,
            @NotNull Predicate<GUIContext> visibilityCondition
    ) {
        this.item = item;
        this.itemSupplier = itemSupplier;
        this.clickHandler = Objects.requireNonNull(clickHandler, "clickHandler cannot be null");
        this.visibilityCondition = Objects.requireNonNull(visibilityCondition, "visibilityCondition cannot be null");
    }

    /**
     * Creates a static slot with the given item.
     *
     * @param item the item to display
     * @return a new slot with the item
     */
    @NotNull
    public static Slot of(@NotNull UnifiedItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        return new Slot(item, null, ClickHandler.DENY, ctx -> true);
    }

    /**
     * Creates a dynamic slot with an item supplier.
     *
     * <p>The supplier is called each time the GUI is refreshed.
     *
     * @param itemSupplier supplier for the item
     * @return a new dynamic slot
     */
    @NotNull
    public static Slot dynamic(@NotNull Supplier<UnifiedItemStack> itemSupplier) {
        Objects.requireNonNull(itemSupplier, "itemSupplier cannot be null");
        return new Slot(null, itemSupplier, ClickHandler.DENY, ctx -> true);
    }

    /**
     * Creates an empty slot with the default deny behavior.
     *
     * @return an empty slot
     */
    @NotNull
    public static Slot empty() {
        return EMPTY_DENY;
    }

    /**
     * Creates an empty slot that allows item manipulation.
     *
     * @return an empty slot that allows clicks
     */
    @NotNull
    public static Slot emptyAllowClick() {
        return EMPTY_ALLOW;
    }

    /**
     * Returns the item for this slot.
     *
     * <p>For static slots, returns the fixed item. For dynamic slots,
     * calls the supplier to get the current item.
     *
     * @return an Optional containing the item if any
     */
    @NotNull
    public Optional<UnifiedItemStack> getItem() {
        if (itemSupplier != null) {
            return Optional.ofNullable(itemSupplier.get());
        }
        return Optional.ofNullable(item);
    }

    /**
     * Checks if this slot is dynamic (uses a supplier).
     *
     * @return true if this slot has a dynamic item supplier
     */
    public boolean isDynamic() {
        return itemSupplier != null;
    }

    /**
     * Checks if this slot is the content marker.
     *
     * @return true if this is the content marker slot
     */
    public boolean isContentMarker() {
        return this == CONTENT;
    }

    /**
     * Returns the click handler for this slot.
     *
     * @return the click handler
     */
    @NotNull
    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    /**
     * Handles a click on this slot.
     *
     * @param context the click context
     * @return the click result
     */
    @NotNull
    public ClickResult handleClick(@NotNull ClickContext context) {
        return clickHandler.handle(context);
    }

    /**
     * Checks if this slot should be visible for the given context.
     *
     * @param context the GUI context
     * @return true if the slot should be visible
     */
    public boolean isVisible(@NotNull GUIContext context) {
        return visibilityCondition.test(context);
    }

    /**
     * Returns a new slot with the specified click handler.
     *
     * @param handler the click handler
     * @return a new slot with the handler
     */
    @NotNull
    public Slot onClick(@NotNull ClickHandler handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        return new Slot(item, itemSupplier, handler, visibilityCondition);
    }

    /**
     * Returns a new slot that runs an action and denies the click.
     *
     * @param action the action to run
     * @return a new slot with the action handler
     */
    @NotNull
    public Slot onClickDo(@NotNull java.util.function.Consumer<ClickContext> action) {
        return onClick(ClickHandler.action(action));
    }

    /**
     * Returns a new slot that runs an action and refreshes the GUI.
     *
     * @param action the action to run
     * @return a new slot with the action handler
     */
    @NotNull
    public Slot onClickRefresh(@NotNull java.util.function.Consumer<ClickContext> action) {
        return onClick(ClickHandler.actionAndRefresh(action));
    }

    /**
     * Returns a new slot that runs an action and closes the GUI.
     *
     * @param action the action to run
     * @return a new slot with the action handler
     */
    @NotNull
    public Slot onClickClose(@NotNull java.util.function.Consumer<ClickContext> action) {
        return onClick(ClickHandler.actionAndClose(action));
    }

    /**
     * Returns a new slot with the specified visibility condition.
     *
     * @param condition the visibility condition
     * @return a new slot with the condition
     */
    @NotNull
    public Slot visibleIf(@NotNull Predicate<GUIContext> condition) {
        Objects.requireNonNull(condition, "condition cannot be null");
        return new Slot(item, itemSupplier, clickHandler, condition);
    }

    /**
     * Returns a new slot that is always visible.
     *
     * @return a new slot that is always visible
     */
    @NotNull
    public Slot alwaysVisible() {
        return visibleIf(ctx -> true);
    }

    /**
     * Returns a new slot that is visible based on a boolean supplier.
     *
     * @param visible supplier for visibility
     * @return a new slot with the visibility condition
     */
    @NotNull
    public Slot visible(@NotNull Supplier<Boolean> visible) {
        Objects.requireNonNull(visible, "visible cannot be null");
        return visibleIf(ctx -> visible.get());
    }

    /**
     * Returns a new slot visible only if the player has the permission.
     *
     * @param permission the required permission
     * @return a new slot with permission-based visibility
     */
    @NotNull
    public Slot requirePermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission cannot be null");
        return visibleIf(ctx -> ctx.getPlayer().hasPermission(permission));
    }

    /**
     * Returns a new slot with a different item.
     *
     * @param newItem the new item
     * @return a new slot with the item
     */
    @NotNull
    public Slot withItem(@NotNull UnifiedItemStack newItem) {
        Objects.requireNonNull(newItem, "newItem cannot be null");
        return new Slot(newItem, null, clickHandler, visibilityCondition);
    }

    /**
     * Returns a new slot with a dynamic item supplier.
     *
     * @param supplier the item supplier
     * @return a new dynamic slot
     */
    @NotNull
    public Slot withDynamicItem(@NotNull Supplier<UnifiedItemStack> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return new Slot(null, supplier, clickHandler, visibilityCondition);
    }

    @Override
    public String toString() {
        if (this == CONTENT) {
            return "Slot{CONTENT}";
        }
        return "Slot{item=" + (item != null ? item.getType() : "dynamic") +
                ", dynamic=" + isDynamic() + '}';
    }
}
