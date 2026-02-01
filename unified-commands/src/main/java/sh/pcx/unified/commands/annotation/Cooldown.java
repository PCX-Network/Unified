/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Applies a cooldown to a command or subcommand.
 *
 * <p>Cooldowns prevent command spam by enforcing a minimum time between
 * executions. The cooldown is tracked per-player by default, but can be
 * configured for global, per-world, or custom scope tracking.</p>
 *
 * <h2>Cooldown Scopes</h2>
 * <ul>
 *   <li><b>PLAYER</b> - Per-player cooldown (default)</li>
 *   <li><b>GLOBAL</b> - Shared cooldown across all players</li>
 *   <li><b>WORLD</b> - Per-world cooldown</li>
 *   <li><b>CUSTOM</b> - Custom scope using {@link #scopeKey()}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Cooldown</h3>
 * <pre>{@code
 * @Command(name = "spawn")
 * public class SpawnCommand {
 *
 *     @Default
 *     @Cooldown(value = 30, unit = TimeUnit.SECONDS)
 *     public void teleportToSpawn(@Sender Player player) {
 *         // 30 second cooldown per player
 *         player.teleport(player.getWorld().getSpawnLocation());
 *     }
 * }
 * }</pre>
 *
 * <h3>Global Cooldown</h3>
 * <pre>{@code
 * @Subcommand("broadcast")
 * @Cooldown(value = 5, unit = TimeUnit.MINUTES, scope = Cooldown.Scope.GLOBAL)
 * public void broadcast(
 *     @Sender CommandSender sender,
 *     @Arg(value = "message", greedy = true) String message
 * ) {
 *     // 5 minute cooldown shared by all players
 *     Bukkit.broadcastMessage(message);
 * }
 * }</pre>
 *
 * <h3>Bypass Permission</h3>
 * <pre>{@code
 * @Subcommand("home")
 * @Cooldown(
 *     value = 60,
 *     unit = TimeUnit.SECONDS,
 *     bypass = "homes.cooldown.bypass"
 * )
 * public void teleportHome(@Sender Player player) {
 *     // Players with permission bypass the cooldown
 * }
 * }</pre>
 *
 * <h3>Custom Cooldown Message</h3>
 * <pre>{@code
 * @Subcommand("kit")
 * @Cooldown(
 *     value = 24,
 *     unit = TimeUnit.HOURS,
 *     message = "<red>You can claim this kit again in <yellow>{remaining}</yellow>!"
 * )
 * public void claimKit(
 *     @Sender Player player,
 *     @Arg("name") String kitName
 * ) {
 *     // Custom message with remaining time placeholder
 * }
 * }</pre>
 *
 * <h3>Per-Argument Cooldown</h3>
 * <pre>{@code
 * @Subcommand("warp")
 * @Cooldown(
 *     value = 10,
 *     unit = TimeUnit.SECONDS,
 *     scope = Cooldown.Scope.CUSTOM,
 *     scopeKey = "{player}:{arg0}"  // player + warp name
 * )
 * public void warp(
 *     @Sender Player player,
 *     @Arg("name") String warpName
 * ) {
 *     // Different cooldowns per warp destination
 * }
 * }</pre>
 *
 * <h3>Class-Level Cooldown</h3>
 * <pre>{@code
 * @Command(name = "admin")
 * @Cooldown(value = 5, unit = TimeUnit.SECONDS)
 * public class AdminCommand {
 *
 *     @Subcommand("reload")
 *     public void reload(@Sender CommandSender sender) {
 *         // Inherits 5 second cooldown
 *     }
 *
 *     @Subcommand("backup")
 *     @Cooldown(value = 1, unit = TimeUnit.HOURS)
 *     public void backup(@Sender CommandSender sender) {
 *         // Overrides with 1 hour cooldown
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.commands.execution.CooldownManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Cooldown {

    /**
     * The cooldown duration.
     *
     * <p>Combined with {@link #unit()} to determine the total cooldown time.</p>
     *
     * @return the cooldown duration value
     */
    long value();

    /**
     * The time unit for the cooldown duration.
     *
     * <p>Common values are {@code SECONDS}, {@code MINUTES}, and {@code HOURS}.</p>
     *
     * @return the time unit
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * The scope of the cooldown tracking.
     *
     * @return the cooldown scope
     */
    Scope scope() default Scope.PLAYER;

    /**
     * Permission node to bypass this cooldown.
     *
     * <p>Players with this permission are not subject to the cooldown.</p>
     *
     * @return bypass permission, empty for no bypass
     */
    String bypass() default "";

    /**
     * Custom message shown when cooldown is active.
     *
     * <p>Supports placeholders:</p>
     * <ul>
     *   <li>{@code {remaining}} - Human-readable remaining time</li>
     *   <li>{@code {seconds}} - Remaining seconds</li>
     *   <li>{@code {command}} - The command name</li>
     * </ul>
     *
     * <p>Supports color codes and MiniMessage formatting.</p>
     *
     * @return custom message, empty for default
     */
    String message() default "";

    /**
     * Custom scope key for {@link Scope#CUSTOM} scope.
     *
     * <p>Supports placeholders:</p>
     * <ul>
     *   <li>{@code {player}} - Player UUID</li>
     *   <li>{@code {world}} - World name</li>
     *   <li>{@code {arg0}}, {@code {arg1}}, etc. - Command arguments</li>
     * </ul>
     *
     * @return custom scope key template
     */
    String scopeKey() default "";

    /**
     * Whether the cooldown persists across server restarts.
     *
     * <p>When {@code true}, cooldowns are saved and restored on restart.
     * Useful for long cooldowns like daily rewards.</p>
     *
     * @return {@code true} for persistent cooldowns
     */
    boolean persistent() default false;

    /**
     * Cooldown scope types.
     */
    enum Scope {
        /**
         * Per-player cooldown (each player has their own cooldown).
         */
        PLAYER,

        /**
         * Global cooldown (shared across all players).
         */
        GLOBAL,

        /**
         * Per-world cooldown (separate cooldown per world).
         */
        WORLD,

        /**
         * Custom scope using the scopeKey template.
         */
        CUSTOM
    }
}
