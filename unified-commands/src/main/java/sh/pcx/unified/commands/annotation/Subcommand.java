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
 * Marks a method as a subcommand handler within a {@link Command} class.
 *
 * <p>Subcommands provide a way to organize related command functionality
 * under a single parent command. The subcommand name becomes part of the
 * command path (e.g., {@code /parent subcommand args}).</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Subcommand</h3>
 * <pre>{@code
 * @Command(name = "arena")
 * public class ArenaCommand {
 *
 *     @Subcommand("create")
 *     public void create(@Sender Player player, @Arg("name") String name) {
 *         // /arena create <name>
 *     }
 *
 *     @Subcommand("delete")
 *     public void delete(@Sender Player player, @Arg("name") String name) {
 *         // /arena delete <name>
 *     }
 * }
 * }</pre>
 *
 * <h3>Nested Subcommands</h3>
 * <pre>{@code
 * @Command(name = "team")
 * public class TeamCommand {
 *
 *     @Subcommand("member add")
 *     public void addMember(
 *         @Sender Player player,
 *         @Arg("player") Player target
 *     ) {
 *         // /team member add <player>
 *     }
 *
 *     @Subcommand("member remove")
 *     public void removeMember(
 *         @Sender Player player,
 *         @Arg("player") Player target
 *     ) {
 *         // /team member remove <player>
 *     }
 *
 *     @Subcommand("settings color")
 *     public void setColor(
 *         @Sender Player player,
 *         @Arg("color") ChatColor color
 *     ) {
 *         // /team settings color <color>
 *     }
 * }
 * }</pre>
 *
 * <h3>Subcommand with Aliases</h3>
 * <pre>{@code
 * @Command(name = "economy")
 * public class EconomyCommand {
 *
 *     @Subcommand(value = "balance", aliases = {"bal", "money"})
 *     public void balance(@Sender Player player) {
 *         // /economy balance, /economy bal, /economy money
 *     }
 *
 *     @Subcommand(value = "pay", aliases = {"send", "transfer"})
 *     public void pay(
 *         @Sender Player player,
 *         @Arg("target") Player target,
 *         @Arg("amount") double amount
 *     ) {
 *         // /economy pay <player> <amount>
 *     }
 * }
 * }</pre>
 *
 * <h3>Overloaded Subcommands</h3>
 * <pre>{@code
 * @Command(name = "time")
 * public class TimeCommand {
 *
 *     @Subcommand("set")
 *     public void setTime(
 *         @Sender Player player,
 *         @Arg("ticks") long ticks
 *     ) {
 *         // /time set 6000
 *     }
 *
 *     @Subcommand("set")
 *     public void setTimeNamed(
 *         @Sender Player player,
 *         @Arg("preset") TimePreset preset
 *     ) {
 *         // /time set day, /time set night
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Command
 * @see Default
 * @see Arg
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {

    /**
     * The subcommand path.
     *
     * <p>This can be a single word for direct subcommands or a space-separated
     * path for nested subcommands. The path is case-insensitive.</p>
     *
     * <pre>{@code
     * @Subcommand("create")           // /parent create
     * @Subcommand("team add")         // /parent team add
     * @Subcommand("settings game mode") // /parent settings game mode
     * }</pre>
     *
     * @return the subcommand path
     */
    String value();

    /**
     * Alternative names for this subcommand.
     *
     * <p>Aliases apply to the last segment of the subcommand path.
     * For example, if the path is "team add" and aliases is {"invite"},
     * both "/parent team add" and "/parent team invite" will work.</p>
     *
     * @return array of subcommand aliases
     */
    String[] aliases() default {};

    /**
     * A brief description of what this subcommand does.
     *
     * <p>Used in help generation and command documentation.</p>
     *
     * @return the subcommand description
     */
    String description() default "";

    /**
     * The usage string for this subcommand.
     *
     * <p>If not specified, usage is auto-generated from method parameters.</p>
     *
     * @return the usage string, empty for auto-generation
     */
    String usage() default "";
}
