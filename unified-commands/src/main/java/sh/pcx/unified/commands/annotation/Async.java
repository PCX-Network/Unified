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
 * Marks a command or subcommand for asynchronous execution.
 *
 * <p>Commands annotated with {@code @Async} are executed off the main server thread,
 * preventing blocking operations from causing server lag. This is essential for
 * commands that perform I/O operations like database queries, file access, or
 * HTTP requests.</p>
 *
 * <h2>Important Considerations</h2>
 * <ul>
 *   <li>Async commands cannot directly modify game state (blocks, entities, etc.)</li>
 *   <li>Use the scheduler to sync back to the main thread when needed</li>
 *   <li>Player references may become invalid if the player disconnects</li>
 *   <li>Folia-compatible: Uses region scheduler for proper async handling</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Async Command</h3>
 * <pre>{@code
 * @Command(name = "stats")
 * public class StatsCommand {
 *
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Default
 *     @Async
 *     public void showStats(@Sender Player player) {
 *         // Runs async - safe to query database
 *         PlayerStats stats = database.loadStats(player.getUniqueId());
 *         player.sendMessage("Kills: " + stats.getKills());
 *     }
 * }
 * }</pre>
 *
 * <h3>Async with Sync Callback</h3>
 * <pre>{@code
 * @Command(name = "lookup")
 * public class LookupCommand {
 *
 *     @Inject
 *     private Scheduler scheduler;
 *
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Subcommand("player")
 *     @Async
 *     public void lookupPlayer(
 *         @Sender Player sender,
 *         @Arg("name") String targetName
 *     ) {
 *         // Async database query
 *         PlayerData data = database.findPlayer(targetName);
 *
 *         // Sync back to main thread for world interaction
 *         scheduler.runSync(() -> {
 *             if (data != null && data.getLastLocation() != null) {
 *                 sender.teleport(data.getLastLocation());
 *             }
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h3>Async File Operations</h3>
 * <pre>{@code
 * @Subcommand("export")
 * @Async
 * @Permission("admin.export")
 * public void exportData(@Sender CommandSender sender) {
 *     try {
 *         // File I/O in async context
 *         Path exportPath = dataFolder.resolve("export.json");
 *         Files.writeString(exportPath, dataManager.toJson());
 *         sender.sendMessage("Export complete!");
 *     } catch (IOException e) {
 *         sender.sendMessage("Export failed: " + e.getMessage());
 *     }
 * }
 * }</pre>
 *
 * <h3>Async HTTP Request</h3>
 * <pre>{@code
 * @Subcommand("mojang")
 * @Async
 * public void lookupMojang(
 *     @Sender Player sender,
 *     @Arg("username") String username
 * ) {
 *     // HTTP request won't block the main thread
 *     MojangProfile profile = mojangAPI.getProfile(username);
 *     if (profile != null) {
 *         sender.sendMessage("UUID: " + profile.getUuid());
 *     } else {
 *         sender.sendMessage("Player not found!");
 *     }
 * }
 * }</pre>
 *
 * <h3>Class-Level Async</h3>
 * <pre>{@code
 * @Command(name = "database")
 * @Async  // All subcommands run async
 * public class DatabaseCommand {
 *
 *     @Subcommand("query")
 *     public void query(@Sender CommandSender sender, @Arg("sql") String sql) {
 *         // Runs async
 *     }
 *
 *     @Subcommand("status")
 *     public void status(@Sender CommandSender sender) {
 *         // Also runs async
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Command
 * @see Subcommand
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Async {

    /**
     * The executor pool to use for this async command.
     *
     * <p>By default, commands use the common async pool. Specify a custom
     * executor name for commands that need dedicated thread pools (e.g.,
     * for rate limiting or prioritization).</p>
     *
     * <pre>{@code
     * @Async(executor = "database")
     * public void query(...) {
     *     // Uses the "database" executor pool
     * }
     * }</pre>
     *
     * @return the executor pool name, empty for default pool
     */
    String executor() default "";

    /**
     * Timeout in milliseconds for async execution.
     *
     * <p>If the command takes longer than this timeout, it may be cancelled
     * and an error message sent to the sender. Set to 0 for no timeout.</p>
     *
     * @return timeout in milliseconds, 0 for no timeout
     */
    long timeout() default 0;

    /**
     * Whether to catch and handle exceptions gracefully.
     *
     * <p>When {@code true} (default), exceptions in async commands are caught
     * and an error message is sent to the sender. When {@code false}, exceptions
     * propagate normally.</p>
     *
     * @return {@code true} to handle exceptions gracefully
     */
    boolean handleExceptions() default true;
}
