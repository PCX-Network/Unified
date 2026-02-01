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
 * Marks a class as a command handler within the UnifiedPlugin command framework.
 *
 * <p>Classes annotated with {@code @Command} are automatically discovered and registered
 * with the command system during plugin initialization. Each annotated class represents
 * a top-level command that can have multiple subcommands defined via {@link Subcommand}.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Command</h3>
 * <pre>{@code
 * @Command(name = "spawn", description = "Teleport to spawn")
 * public class SpawnCommand {
 *
 *     @Default
 *     public void execute(@Sender Player player) {
 *         player.teleport(player.getWorld().getSpawnLocation());
 *     }
 * }
 * }</pre>
 *
 * <h3>Command with Aliases</h3>
 * <pre>{@code
 * @Command(
 *     name = "teleport",
 *     aliases = {"tp", "tele"},
 *     description = "Teleport commands"
 * )
 * public class TeleportCommand {
 *
 *     @Subcommand("player")
 *     public void teleportToPlayer(
 *         @Sender Player sender,
 *         @Arg("target") Player target
 *     ) {
 *         sender.teleport(target.getLocation());
 *     }
 * }
 * }</pre>
 *
 * <h3>Command with Permission</h3>
 * <pre>{@code
 * @Command(
 *     name = "gamemode",
 *     aliases = {"gm"},
 *     description = "Change gamemode",
 *     permission = "essentials.gamemode"
 * )
 * @Permission("essentials.gamemode")
 * public class GamemodeCommand {
 *
 *     @Subcommand("creative")
 *     @Permission("essentials.gamemode.creative")
 *     public void creative(@Sender Player player) {
 *         player.setGameMode(GameMode.CREATIVE);
 *     }
 *
 *     @Subcommand("survival")
 *     @Permission("essentials.gamemode.survival")
 *     public void survival(@Sender Player player) {
 *         player.setGameMode(GameMode.SURVIVAL);
 *     }
 * }
 * }</pre>
 *
 * <h3>Command with Mixed Sender Types</h3>
 * <pre>{@code
 * @Command(name = "broadcast", description = "Broadcast a message")
 * public class BroadcastCommand {
 *
 *     @Default
 *     public void broadcast(
 *         @Sender CommandSender sender,
 *         @Arg("message") String message
 *     ) {
 *         Bukkit.broadcastMessage(message);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Subcommand
 * @see Permission
 * @see Arg
 * @see Sender
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    /**
     * The primary name of the command.
     *
     * <p>This is the main identifier used to invoke the command. It should be
     * lowercase and contain no spaces. The name is case-insensitive when matching.</p>
     *
     * <pre>{@code
     * @Command(name = "warp")
     * public class WarpCommand { }
     * // Usage: /warp <args>
     * }</pre>
     *
     * @return the command name
     */
    String name();

    /**
     * Alternative names for the command.
     *
     * <p>Aliases allow the command to be invoked using different names.
     * All aliases are case-insensitive and should be lowercase.</p>
     *
     * <pre>{@code
     * @Command(name = "teleport", aliases = {"tp", "tele", "warp"})
     * public class TeleportCommand { }
     * // Can be invoked as: /teleport, /tp, /tele, or /warp
     * }</pre>
     *
     * @return array of command aliases, empty by default
     */
    String[] aliases() default {};

    /**
     * A brief description of what the command does.
     *
     * <p>This description is displayed in help messages and command listings.
     * Keep it concise but informative.</p>
     *
     * <pre>{@code
     * @Command(
     *     name = "home",
     *     description = "Teleport to your home location"
     * )
     * }</pre>
     *
     * @return the command description
     */
    String description() default "";

    /**
     * The permission required to use this command.
     *
     * <p>If specified, players must have this permission to execute any
     * part of the command. Subcommands can specify additional permissions
     * using {@link Permission}.</p>
     *
     * <pre>{@code
     * @Command(
     *     name = "fly",
     *     permission = "essentials.fly"
     * )
     * }</pre>
     *
     * @return the required permission node, empty for no requirement
     */
    String permission() default "";

    /**
     * The usage string shown when the command is used incorrectly.
     *
     * <p>If not specified, usage is auto-generated from method parameters.
     * Use angle brackets for required arguments and square brackets for optional.</p>
     *
     * <pre>{@code
     * @Command(
     *     name = "give",
     *     usage = "/give <player> <item> [amount]"
     * )
     * }</pre>
     *
     * @return the usage string, empty for auto-generation
     */
    String usage() default "";

    /**
     * Whether this command can only be run by players.
     *
     * <p>When {@code true}, console and command blocks cannot execute this command.
     * The command framework will automatically check and send an error message
     * to non-player senders.</p>
     *
     * <pre>{@code
     * @Command(name = "fly", playerOnly = true)
     * public class FlyCommand {
     *     @Default
     *     public void toggle(@Sender Player player) {
     *         player.setAllowFlight(!player.getAllowFlight());
     *     }
     * }
     * }</pre>
     *
     * @return {@code true} if only players can use this command
     */
    boolean playerOnly() default false;

    /**
     * Whether this command is hidden from help listings.
     *
     * <p>Hidden commands do not appear in help menus or tab completion
     * for players without proper permissions. Useful for admin or debug commands.</p>
     *
     * @return {@code true} to hide from help listings
     */
    boolean hidden() default false;
}
