/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.title;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Builder for sending action bar messages to players.
 *
 * <p>Action bars appear above the hotbar and can be used for status messages,
 * warnings, or other short-term information.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple action bar
 * titleService.actionBar(player)
 *     .message(Component.text("Combat Mode Active"))
 *     .send();
 *
 * // Persistent action bar with duration
 * titleService.actionBar(player)
 *     .message(Component.text("Mana: 100/100"))
 *     .duration(Duration.ofSeconds(10))
 *     .send();
 *
 * // Dynamic action bar
 * titleService.actionBar(player)
 *     .messageSupplier(() -> Component.text("Health: " + player.getHealth()))
 *     .duration(Duration.ofSeconds(30))
 *     .updateInterval(Duration.ofMillis(250))
 *     .send();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TitleService
 */
public interface ActionBarBuilder {

    /**
     * Sets the action bar message.
     *
     * @param message the message to display
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder message(@NotNull Component message);

    /**
     * Sets a dynamic message supplier.
     *
     * <p>The supplier is called on each update to generate the message.
     *
     * @param supplier the message supplier
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder messageSupplier(@NotNull Supplier<Component> supplier);

    /**
     * Sets how long the action bar should be displayed.
     *
     * <p>The action bar will be repeatedly sent until the duration expires.
     * Without a duration, the action bar is sent once and fades naturally.
     *
     * @param duration the display duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder duration(@NotNull Duration duration);

    /**
     * Sets the update interval for persistent action bars.
     *
     * <p>Controls how often the action bar is refreshed. Lower values
     * appear smoother but use more resources.
     *
     * @param interval the update interval (default: 2 ticks)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder updateInterval(@NotNull Duration interval);

    /**
     * Sets the priority of this action bar.
     *
     * <p>When multiple action bars are active, only the highest priority
     * one is shown. Higher values = higher priority.
     *
     * @param priority the priority value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ActionBarBuilder priority(int priority);

    /**
     * Sends the action bar to the player(s).
     *
     * @return a unique ID for this action bar (for cancellation)
     * @since 1.0.0
     */
    @NotNull
    UUID send();
}
