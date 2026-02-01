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
 * Annotation for defining default values and behaviors in commands.
 *
 * <p>This annotation serves two purposes depending on where it's applied:</p>
 * <ul>
 *   <li><b>On methods:</b> Marks the method as the default handler when no subcommand matches</li>
 *   <li><b>On parameters:</b> Specifies a default value when the argument is not provided</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Default Command Handler</h3>
 * <pre>{@code
 * @Command(name = "spawn")
 * public class SpawnCommand {
 *
 *     @Default
 *     public void teleportToSpawn(@Sender Player player) {
 *         // Executed when user types just "/spawn"
 *         player.teleport(player.getWorld().getSpawnLocation());
 *     }
 *
 *     @Subcommand("set")
 *     public void setSpawn(@Sender Player player) {
 *         // Executed when user types "/spawn set"
 *         player.getWorld().setSpawnLocation(player.getLocation());
 *     }
 * }
 * }</pre>
 *
 * <h3>Default Parameter Value</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void giveItem(
 *     @Sender Player sender,
 *     @Arg("player") Player target,
 *     @Arg("material") Material material,
 *     @Arg("amount") @Default("1") int amount
 * ) {
 *     // /give Steve diamond -> amount = 1
 *     // /give Steve diamond 64 -> amount = 64
 *     target.getInventory().addItem(new ItemStack(material, amount));
 * }
 * }</pre>
 *
 * <h3>Special Default Values</h3>
 * <pre>{@code
 * @Subcommand("teleport")
 * public void teleport(
 *     @Sender Player sender,
 *     @Arg("target") @Default("@self") Player target
 * ) {
 *     // "@self" resolves to the command sender
 * }
 *
 * @Subcommand("time")
 * public void setTime(
 *     @Sender Player sender,
 *     @Arg("world") @Default("@world") World world,
 *     @Arg("time") long time
 * ) {
 *     // "@world" resolves to sender's current world
 * }
 * }</pre>
 *
 * <h3>Multiple Optional Parameters</h3>
 * <pre>{@code
 * @Subcommand("msg")
 * public void sendMessage(
 *     @Sender Player sender,
 *     @Arg("target") @Default("@lastrecipient") Player target,
 *     @Arg(value = "message", greedy = true) String message
 * ) {
 *     // Defaults to last message recipient
 * }
 * }</pre>
 *
 * <h3>Default with Validation</h3>
 * <pre>{@code
 * @Subcommand("setlevel")
 * public void setLevel(
 *     @Sender Player sender,
 *     @Arg(value = "level", min = 0, max = 100) @Default("50") int level
 * ) {
 *     // Default value is 50, but user input is validated
 * }
 * }</pre>
 *
 * <h2>Special Default Values</h2>
 * <table border="1">
 *   <tr><th>Value</th><th>Description</th><th>Applicable Types</th></tr>
 *   <tr><td>{@code @self}</td><td>The command sender</td><td>Player, Entity</td></tr>
 *   <tr><td>{@code @world}</td><td>Sender's current world</td><td>World</td></tr>
 *   <tr><td>{@code @location}</td><td>Sender's current location</td><td>Location</td></tr>
 *   <tr><td>{@code @null}</td><td>Null value</td><td>Any nullable type</td></tr>
 *   <tr><td>{@code @empty}</td><td>Empty collection/string</td><td>String, List, Set</td></tr>
 * </table>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Command
 * @see Subcommand
 * @see Arg
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Default {

    /**
     * The default value for this parameter.
     *
     * <p>When applied to a parameter, this value is used when the argument
     * is not provided by the user. The value is parsed using the same parser
     * as the parameter type.</p>
     *
     * <p>When applied to a method, this value is ignored and the method
     * becomes the default handler.</p>
     *
     * <pre>{@code
     * @Default("64")        // Numeric default
     * @Default("true")      // Boolean default
     * @Default("DIAMOND")   // Enum default
     * @Default("@self")     // Special placeholder
     * }</pre>
     *
     * @return the default value as a string
     */
    String value() default "";
}
