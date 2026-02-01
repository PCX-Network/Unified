/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for handling click events in GUI slots.
 *
 * <p>ClickHandler processes click events and returns a {@link ClickResult}
 * that determines the subsequent behavior (allow, deny, close, refresh).
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple deny handler
 * ClickHandler denyAll = click -> ClickResult.DENY;
 *
 * // Toggle handler
 * ClickHandler toggle = click -> {
 *     boolean current = getState("enabled");
 *     setState("enabled", !current);
 *     return ClickResult.REFRESH;
 * };
 *
 * // Navigation handler
 * ClickHandler navigate = click -> {
 *     click.context().gui().navigateTo(
 *         new SettingsGUI(click.context())
 *     );
 *     return ClickResult.DENY;
 * };
 *
 * // Conditional handler based on click type
 * ClickHandler conditional = click -> {
 *     if (click.getClickType().isLeftClick()) {
 *         increment();
 *     } else if (click.getClickType().isRightClick()) {
 *         decrement();
 *     }
 *     return ClickResult.REFRESH;
 * };
 *
 * // Use with Slot
 * Slot button = Slot.of(item).onClick(click -> {
 *     performAction();
 *     return ClickResult.CLOSE;
 * });
 * }</pre>
 *
 * <h2>Chaining Handlers</h2>
 * <pre>{@code
 * // Chain handlers with andThen
 * ClickHandler logging = click -> {
 *     logger.info("Click at slot " + click.getSlot());
 *     return ClickResult.DENY;
 * };
 *
 * ClickHandler action = click -> {
 *     performAction();
 *     return ClickResult.REFRESH;
 * };
 *
 * // Logging runs first, then action determines the result
 * ClickHandler combined = logging.andThen(action);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ClickContext
 * @see ClickResult
 * @see Slot
 */
@FunctionalInterface
public interface ClickHandler {

    /**
     * A handler that always denies the click action.
     *
     * <p>This is the default handler for GUI slots, preventing
     * any item manipulation.
     */
    ClickHandler DENY = click -> ClickResult.DENY;

    /**
     * A handler that always allows the click action.
     *
     * <p>Use this for slots where item manipulation is intended,
     * such as in item editors.
     */
    ClickHandler ALLOW = click -> ClickResult.ALLOW;

    /**
     * A handler that closes the GUI on click.
     */
    ClickHandler CLOSE = click -> ClickResult.CLOSE;

    /**
     * A handler that refreshes the GUI on click.
     */
    ClickHandler REFRESH = click -> ClickResult.REFRESH;

    /**
     * Handles a click event and returns the result.
     *
     * @param context the click context containing event details
     * @return the result determining subsequent behavior
     */
    @NotNull
    ClickResult handle(@NotNull ClickContext context);

    /**
     * Returns a handler that runs this handler first, then the other.
     *
     * <p>The result from the second handler is returned. This is useful
     * for adding logging, validation, or other preprocessing steps.
     *
     * @param after the handler to run after this one
     * @return a combined handler
     */
    @NotNull
    default ClickHandler andThen(@NotNull ClickHandler after) {
        return click -> {
            handle(click);
            return after.handle(click);
        };
    }

    /**
     * Returns a handler that only processes clicks matching the predicate.
     *
     * <p>If the predicate returns false, {@link ClickResult#DENY} is returned
     * without calling this handler.
     *
     * @param predicate the condition to check
     * @return a filtered handler
     */
    @NotNull
    default ClickHandler filter(@NotNull java.util.function.Predicate<ClickContext> predicate) {
        return click -> predicate.test(click) ? handle(click) : ClickResult.DENY;
    }

    /**
     * Returns a handler that only processes left clicks.
     *
     * @return a handler that filters to left clicks only
     */
    @NotNull
    default ClickHandler onLeftClick() {
        return filter(click -> click.getClickType().isLeftClick());
    }

    /**
     * Returns a handler that only processes right clicks.
     *
     * @return a handler that filters to right clicks only
     */
    @NotNull
    default ClickHandler onRightClick() {
        return filter(click -> click.getClickType().isRightClick());
    }

    /**
     * Returns a handler that only processes shift clicks.
     *
     * @return a handler that filters to shift clicks only
     */
    @NotNull
    default ClickHandler onShiftClick() {
        return filter(click -> click.getClickType().isShiftClick());
    }

    /**
     * Creates a handler that performs an action and denies the click.
     *
     * <p>This is a convenience method for the common pattern of
     * performing an action and preventing item manipulation.
     *
     * @param action the action to perform
     * @return a handler that runs the action and returns DENY
     */
    @NotNull
    static ClickHandler action(@NotNull java.util.function.Consumer<ClickContext> action) {
        return click -> {
            action.accept(click);
            return ClickResult.DENY;
        };
    }

    /**
     * Creates a handler that performs an action and refreshes the GUI.
     *
     * @param action the action to perform
     * @return a handler that runs the action and returns REFRESH
     */
    @NotNull
    static ClickHandler actionAndRefresh(@NotNull java.util.function.Consumer<ClickContext> action) {
        return click -> {
            action.accept(click);
            return ClickResult.REFRESH;
        };
    }

    /**
     * Creates a handler that performs an action and closes the GUI.
     *
     * @param action the action to perform
     * @return a handler that runs the action and returns CLOSE
     */
    @NotNull
    static ClickHandler actionAndClose(@NotNull java.util.function.Consumer<ClickContext> action) {
        return click -> {
            action.accept(click);
            return ClickResult.CLOSE;
        };
    }
}
