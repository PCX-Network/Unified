/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import sh.pcx.unified.modules.core.ModuleContext;

/**
 * Interface for modules that require cleanup when disabled.
 *
 * <p>The {@link #onDisable(ModuleContext)} method is called when a module
 * transitions from {@link ModuleState#ENABLED} to {@link ModuleState#DISABLED}
 * or when the plugin is shutting down. This is the opportunity for modules
 * to save data, release resources, and perform cleanup.
 *
 * <h2>When onDisable is Called</h2>
 * <ul>
 *   <li>Admin command: {@code /modules disable ModuleName}</li>
 *   <li>API call: {@code moduleManager.disable("ModuleName")}</li>
 *   <li>Plugin shutdown (server stop or plugin reload)</li>
 *   <li>Module unload request</li>
 * </ul>
 *
 * <h2>Disable Sequence</h2>
 * <ol>
 *   <li>{@link #onDisable(ModuleContext)} is called</li>
 *   <li>Scheduled tasks are cancelled</li>
 *   <li>Event listeners are unregistered</li>
 *   <li>Commands are unregistered</li>
 *   <li>Module state changes to DISABLED</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * public class BattlePassModule implements Initializable, Disableable {
 *
 *     private PassProgressManager progressManager;
 *     private DatabaseConnection database;
 *     private Cache<UUID, PlayerProgress> progressCache;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         this.database = new DatabaseConnection(context.getConfig());
 *         this.progressCache = Caffeine.newBuilder().build();
 *         this.progressManager = new PassProgressManager(database, progressCache);
 *     }
 *
 *     @Override
 *     public void onDisable(ModuleContext context) {
 *         context.getLogger().info("Saving all player progress...");
 *
 *         // Save all cached data
 *         progressCache.asMap().forEach((uuid, progress) -> {
 *             progressManager.saveProgress(uuid, progress);
 *         });
 *
 *         // Flush any pending database operations
 *         database.flush();
 *
 *         // Close database connection
 *         database.close();
 *
 *         // Clear caches
 *         progressCache.invalidateAll();
 *
 *         context.getLogger().info("BattlePass module disabled successfully");
 *     }
 * }
 * }</pre>
 *
 * <h2>Important Considerations</h2>
 * <ul>
 *   <li>This method should complete quickly (avoid blocking operations)</li>
 *   <li>Save any unsaved data before releasing resources</li>
 *   <li>Close all external connections (database, network)</li>
 *   <li>Cancel any pending async operations</li>
 *   <li>Don't assume other modules are still available</li>
 * </ul>
 *
 * <h2>Async Cleanup</h2>
 * <p>For modules with async operations, ensure cleanup completes:
 *
 * <pre>{@code
 * @Override
 * public void onDisable(ModuleContext context) {
 *     // Signal async tasks to stop
 *     running.set(false);
 *
 *     // Wait for pending operations (with timeout)
 *     try {
 *         if (!pendingOperations.await(5, TimeUnit.SECONDS)) {
 *             context.getLogger().warn("Some operations did not complete");
 *         }
 *     } catch (InterruptedException e) {
 *         Thread.currentThread().interrupt();
 *     }
 *
 *     // Now safe to close resources
 *     database.close();
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Initializable
 * @see ModuleContext
 */
@FunctionalInterface
public interface Disableable {

    /**
     * Called when the module is being disabled or the plugin is shutting down.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Save all unsaved data to disk or database</li>
     *   <li>Close database connections</li>
     *   <li>Release external resources</li>
     *   <li>Cancel pending async operations</li>
     *   <li>Clear caches and temporary data</li>
     *   <li>Notify external services of shutdown</li>
     * </ul>
     *
     * <p>This method should complete quickly. For lengthy operations,
     * consider using async saving during normal operation and only
     * doing final flush here.
     *
     * @param context the module context providing access to plugin services
     */
    void onDisable(ModuleContext context);
}
