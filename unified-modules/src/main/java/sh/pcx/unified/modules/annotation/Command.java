/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a module class as a command executor that should be automatically registered.
 *
 * <p>When a class annotated with {@code @Module} also has {@code @Command}, the module
 * system will automatically register it as a command executor when the module is enabled.
 * The command is automatically unregistered when the module is disabled.
 *
 * <p>Classes with this annotation should implement the platform's command interface
 * (e.g., {@code org.bukkit.command.TabExecutor} for Paper/Spigot).
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Command</h3>
 * <pre>{@code
 * @Module(name = "Economy")
 * @Command
 * public class EconomyModule implements TabExecutor {
 *
 *     @Override
 *     public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
 *         // Handle /economy command
 *         return true;
 *     }
 *
 *     @Override
 *     public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
 *         return List.of("balance", "pay", "top");
 *     }
 * }
 * }</pre>
 *
 * <h3>Command with Configuration</h3>
 * <pre>{@code
 * @Module(name = "Warps")
 * @Command(
 *     name = "warp",
 *     aliases = {"w", "warps"},
 *     description = "Teleport to warp locations",
 *     permission = "myplugin.warp"
 * )
 * public class WarpModule implements TabExecutor {
 *     // Command implementation
 * }
 * }</pre>
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>Command is registered when module state changes to ENABLED</li>
 *   <li>Command is unregistered when module state changes to DISABLED or UNLOADED</li>
 *   <li>Commands are automatically re-registered on module reload</li>
 *   <li>The command name defaults to the module name in lowercase</li>
 * </ul>
 *
 * <h2>Dynamic Registration</h2>
 * <p>Unlike traditional plugin.yml commands, @Command allows runtime registration
 * and unregistration, enabling true hot-reload functionality.
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Module
 * @see Listen
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    /**
     * The primary name of the command.
     *
     * <p>This is the main command that players type. For example, {@code "warp"}
     * would create the command {@code /warp}.
     *
     * <p>If not specified, defaults to the module name in lowercase.
     *
     * @return the command name
     */
    String name() default "";

    /**
     * Alternative names for the command.
     *
     * <p>Aliases provide shorter or alternative ways to invoke the same command.
     * For example, {@code /w} and {@code /warps} as aliases for {@code /warp}.
     *
     * @return an array of command aliases
     */
    String[] aliases() default {};

    /**
     * A brief description of what the command does.
     *
     * <p>This description is shown in help commands and command listings.
     *
     * @return the command description
     */
    String description() default "";

    /**
     * The usage string for the command.
     *
     * <p>This is displayed when the command is used incorrectly.
     * Use standard command syntax: {@code <required>} and {@code [optional]}.
     *
     * <h3>Example</h3>
     * <pre>
     * usage = "/warp <name> [player]"
     * </pre>
     *
     * @return the command usage string
     */
    String usage() default "";

    /**
     * The permission required to use this command.
     *
     * <p>Players without this permission will receive a "no permission" message.
     * If empty, no permission check is performed (command is available to all).
     *
     * @return the permission node, or empty for no permission requirement
     */
    String permission() default "";

    /**
     * The message shown when a player lacks permission.
     *
     * <p>If empty, uses the server's default "no permission" message.
     *
     * @return the permission denied message
     */
    String permissionMessage() default "";

    /**
     * Whether this command can only be run by players.
     *
     * <p>When {@code true}, the console and command blocks cannot execute
     * this command. They will receive an appropriate error message.
     *
     * @return {@code true} if the command is player-only
     */
    boolean playerOnly() default false;
}
