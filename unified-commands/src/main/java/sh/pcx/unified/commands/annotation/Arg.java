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
 * Marks a method parameter as a command argument to be parsed from user input.
 *
 * <p>The command framework automatically parses arguments based on their types.
 * Built-in parsers handle common types like {@code Player}, {@code World},
 * {@code Material}, {@code Duration}, and primitives. Custom parsers can be
 * registered for plugin-specific types.</p>
 *
 * <h2>Supported Types</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Example Input</th><th>Notes</th></tr>
 *   <tr><td>{@code String}</td><td>"hello"</td><td>Single word or quoted</td></tr>
 *   <tr><td>{@code int/Integer}</td><td>"42"</td><td>Integer numbers</td></tr>
 *   <tr><td>{@code double/Double}</td><td>"3.14"</td><td>Decimal numbers</td></tr>
 *   <tr><td>{@code boolean/Boolean}</td><td>"true", "yes"</td><td>Various boolean formats</td></tr>
 *   <tr><td>{@code Player}</td><td>"Steve"</td><td>Online players with tab-complete</td></tr>
 *   <tr><td>{@code OfflinePlayer}</td><td>"Steve"</td><td>Any known player</td></tr>
 *   <tr><td>{@code World}</td><td>"world_nether"</td><td>Loaded worlds</td></tr>
 *   <tr><td>{@code Material}</td><td>"diamond_sword"</td><td>Item/block materials</td></tr>
 *   <tr><td>{@code Duration}</td><td>"1h30m"</td><td>Time durations</td></tr>
 *   <tr><td>Any Enum</td><td>"SURVIVAL"</td><td>Enum constants</td></tr>
 * </table>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Arguments</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void giveItem(
 *     @Sender Player sender,
 *     @Arg("player") Player target,
 *     @Arg("material") Material material,
 *     @Arg("amount") int amount
 * ) {
 *     // /give <player> <material> <amount>
 *     target.getInventory().addItem(new ItemStack(material, amount));
 * }
 * }</pre>
 *
 * <h3>Optional Arguments with Default</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void giveItem(
 *     @Sender Player sender,
 *     @Arg("player") Player target,
 *     @Arg("material") Material material,
 *     @Arg("amount") @Default("1") int amount
 * ) {
 *     // /give <player> <material> [amount]
 *     // amount defaults to 1 if not specified
 * }
 * }</pre>
 *
 * <h3>Greedy String Argument</h3>
 * <pre>{@code
 * @Subcommand("broadcast")
 * public void broadcast(
 *     @Sender CommandSender sender,
 *     @Arg(value = "message", greedy = true) String message
 * ) {
 *     // /broadcast <message...>
 *     // message captures all remaining arguments
 *     Bukkit.broadcastMessage(message);
 * }
 * }</pre>
 *
 * <h3>Custom Tab Completion</h3>
 * <pre>{@code
 * @Subcommand("warp")
 * public void warp(
 *     @Sender Player player,
 *     @Arg("name") @Completions("@warps") String warpName
 * ) {
 *     // Tab completion provided by registered "@warps" provider
 * }
 * }</pre>
 *
 * <h3>Validated Arguments</h3>
 * <pre>{@code
 * @Subcommand("setlevel")
 * public void setLevel(
 *     @Sender Player sender,
 *     @Arg(value = "level", min = 0, max = 100) int level
 * ) {
 *     // level is validated to be between 0 and 100
 *     sender.setLevel(level);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Default
 * @see Completions
 * @see Sender
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {

    /**
     * The name of this argument.
     *
     * <p>This name is used in usage strings, help messages, and error messages.
     * It should be descriptive and lowercase.</p>
     *
     * <pre>{@code
     * @Arg("player")   // displays as <player>
     * @Arg("amount")   // displays as <amount>
     * @Arg("message")  // displays as <message>
     * }</pre>
     *
     * @return the argument name
     */
    String value();

    /**
     * The description of this argument for help messages.
     *
     * <p>Provides additional context about what this argument expects.</p>
     *
     * <pre>{@code
     * @Arg(value = "duration", description = "Time until expiry (e.g., 1h30m)")
     * }</pre>
     *
     * @return the argument description
     */
    String description() default "";

    /**
     * Whether this argument consumes all remaining input.
     *
     * <p>When {@code true}, this argument captures everything after its position.
     * Must be the last argument in the method signature. Typically used for
     * messages, reasons, or other multi-word text.</p>
     *
     * <pre>{@code
     * @Subcommand("ban")
     * public void ban(
     *     @Sender CommandSender sender,
     *     @Arg("player") Player target,
     *     @Arg(value = "reason", greedy = true) String reason
     * ) {
     *     // /ban Steve Breaking the rules multiple times
     *     // reason = "Breaking the rules multiple times"
     * }
     * }</pre>
     *
     * @return {@code true} to capture remaining input
     */
    boolean greedy() default false;

    /**
     * Minimum value for numeric arguments.
     *
     * <p>Only applies to numeric types ({@code int}, {@code double}, etc.).
     * Values below this minimum will cause a validation error.</p>
     *
     * @return minimum allowed value, {@link Double#MIN_VALUE} for no minimum
     */
    double min() default Double.MIN_VALUE;

    /**
     * Maximum value for numeric arguments.
     *
     * <p>Only applies to numeric types. Values above this maximum
     * will cause a validation error.</p>
     *
     * @return maximum allowed value, {@link Double#MAX_VALUE} for no maximum
     */
    double max() default Double.MAX_VALUE;

    /**
     * Minimum length for string arguments.
     *
     * <p>Only applies to {@code String} type. Shorter strings
     * will cause a validation error.</p>
     *
     * @return minimum string length, 0 for no minimum
     */
    int minLength() default 0;

    /**
     * Maximum length for string arguments.
     *
     * <p>Only applies to {@code String} type. Longer strings
     * will cause a validation error.</p>
     *
     * @return maximum string length, {@link Integer#MAX_VALUE} for no maximum
     */
    int maxLength() default Integer.MAX_VALUE;

    /**
     * Regex pattern for string validation.
     *
     * <p>Only applies to {@code String} type. The argument must match
     * this pattern or a validation error is thrown.</p>
     *
     * <pre>{@code
     * @Arg(value = "username", pattern = "^[a-zA-Z0-9_]{3,16}$")
     * }</pre>
     *
     * @return regex pattern, empty for no pattern validation
     */
    String pattern() default "";

    /**
     * Suggestions to show during tab completion.
     *
     * <p>Static list of suggestions. For dynamic suggestions,
     * use {@link Completions} annotation instead.</p>
     *
     * <pre>{@code
     * @Arg(value = "size", suggestions = {"small", "medium", "large"})
     * }</pre>
     *
     * @return array of suggestion strings
     */
    String[] suggestions() default {};
}
