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

/**
 * Marks a method parameter to receive the command sender.
 *
 * <p>The command framework injects the appropriate sender object based on
 * the parameter type. If the actual sender doesn't match the expected type
 * (e.g., console trying to run a player-only command), an appropriate error
 * message is sent.</p>
 *
 * <h2>Supported Sender Types</h2>
 * <ul>
 *   <li>{@code CommandSender} - Any sender (player, console, command block)</li>
 *   <li>{@code Player} - Only players can execute</li>
 *   <li>{@code ConsoleCommandSender} - Only console can execute</li>
 *   <li>{@code UnifiedPlayer} - Platform-agnostic player wrapper</li>
 *   <li>{@code Entity} - Any entity sender</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Player-Only Command</h3>
 * <pre>{@code
 * @Command(name = "fly")
 * public class FlyCommand {
 *
 *     @Default
 *     public void toggle(@Sender Player player) {
 *         // Only players can execute this
 *         player.setAllowFlight(!player.getAllowFlight());
 *     }
 * }
 * }</pre>
 *
 * <h3>Any Sender Command</h3>
 * <pre>{@code
 * @Command(name = "broadcast")
 * public class BroadcastCommand {
 *
 *     @Default
 *     public void broadcast(
 *         @Sender CommandSender sender,
 *         @Arg(value = "message", greedy = true) String message
 *     ) {
 *         // Can be run from console or by players
 *         Bukkit.broadcastMessage(message);
 *     }
 * }
 * }</pre>
 *
 * <h3>Console-Only Command</h3>
 * <pre>{@code
 * @Command(name = "maintenance")
 * public class MaintenanceCommand {
 *
 *     @Default
 *     public void toggle(@Sender ConsoleCommandSender console) {
 *         // Only console can execute for safety
 *         MaintenanceMode.toggle();
 *     }
 * }
 * }</pre>
 *
 * <h3>UnifiedPlayer for Cross-Platform</h3>
 * <pre>{@code
 * @Command(name = "stats")
 * public class StatsCommand {
 *
 *     @Default
 *     public void showStats(@Sender UnifiedPlayer player) {
 *         // Works across Paper, Folia, and Sponge
 *         player.sendMessage(Component.text("Your stats..."));
 *     }
 * }
 * }</pre>
 *
 * <h3>Optional Sender with Target Override</h3>
 * <pre>{@code
 * @Command(name = "heal")
 * public class HealCommand {
 *
 *     @Default
 *     public void heal(
 *         @Sender CommandSender sender,
 *         @Arg("target") @Default("@self") Player target
 *     ) {
 *         // /heal - heals yourself (if player)
 *         // /heal Steve - heals Steve
 *         target.setHealth(target.getMaxHealth());
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Command
 * @see Subcommand
 * @see Arg
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Sender {

    /**
     * Custom error message when sender type doesn't match.
     *
     * <p>If empty, a default message is used based on the expected type.
     * Supports color codes and MiniMessage formatting.</p>
     *
     * <pre>{@code
     * @Default
     * public void fly(
     *     @Sender(message = "<red>Only players can toggle flight!")
     *     Player player
     * ) {
     *     player.setAllowFlight(!player.getAllowFlight());
     * }
     * }</pre>
     *
     * @return custom error message, empty for default
     */
    String message() default "";
}
