/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import sh.pcx.unified.modules.core.ModuleContext;

/**
 * Interface for modules that support hot-reloading without server restart.
 *
 * <p>The {@link #reload(ModuleContext)} method is called when an administrator
 * requests a module reload via command or API. This allows modules to refresh
 * their configuration and state without requiring a full server restart.
 *
 * <h2>When Reload is Triggered</h2>
 * <ul>
 *   <li>Admin command: {@code /modules reload ModuleName}</li>
 *   <li>API call: {@code moduleManager.reload("ModuleName")}</li>
 *   <li>Configuration file change (if file watching is enabled)</li>
 *   <li>Global reload: {@code /modules reload} (all reloadable modules)</li>
 * </ul>
 *
 * <h2>Reload Sequence</h2>
 * <ol>
 *   <li>Configuration is reloaded from disk (if @Configurable with autoReload=true)</li>
 *   <li>{@link #reload(ModuleContext)} is called</li>
 *   <li>Listeners and commands remain registered (unless explicitly changed)</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * @Configurable
 * public class BattlePassModule implements Initializable, Reloadable {
 *
 *     private BattlePassConfig config;
 *     private PassProgressManager progressManager;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         this.config = context.loadConfig(BattlePassConfig.class);
 *         this.progressManager = new PassProgressManager(config);
 *     }
 *
 *     @Override
 *     public void reload(ModuleContext context) {
 *         // Reload configuration
 *         BattlePassConfig newConfig = context.loadConfig(BattlePassConfig.class);
 *
 *         // Apply changes to running state
 *         progressManager.updateConfig(newConfig);
 *         this.config = newConfig;
 *
 *         context.getLogger().info("BattlePass configuration reloaded");
 *     }
 * }
 * }</pre>
 *
 * <h2>Partial Reload</h2>
 * <p>Modules can implement selective reloading to minimize disruption:
 *
 * <pre>{@code
 * @Override
 * public void reload(ModuleContext context) {
 *     BattlePassConfig newConfig = context.loadConfig(BattlePassConfig.class);
 *
 *     // Only update what changed
 *     if (newConfig.getXpMultiplier() != config.getXpMultiplier()) {
 *         progressManager.setXpMultiplier(newConfig.getXpMultiplier());
 *     }
 *
 *     if (!newConfig.getRewards().equals(config.getRewards())) {
 *         progressManager.refreshRewardTable(newConfig.getRewards());
 *     }
 *
 *     this.config = newConfig;
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>If {@code reload()} throws an exception:
 * <ul>
 *   <li>The module remains in its previous state</li>
 *   <li>The exception is logged as a warning</li>
 *   <li>The module continues to operate with old configuration</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Preserve ongoing operations (don't interrupt active tasks)</li>
 *   <li>Validate new configuration before applying</li>
 *   <li>Log meaningful status messages</li>
 *   <li>Consider caching old config for rollback on error</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Initializable
 * @see ModuleContext
 */
@FunctionalInterface
public interface Reloadable {

    /**
     * Called when the module should reload its configuration and state.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Reload configuration from disk</li>
     *   <li>Refresh caches and internal state</li>
     *   <li>Update running managers with new settings</li>
     *   <li>Re-fetch external data if needed</li>
     * </ul>
     *
     * <p>The module should continue operating normally during reload.
     * Avoid operations that could disrupt active users.
     *
     * @param context the module context providing access to plugin services
     * @throws Exception if reload fails (module continues with old state)
     */
    void reload(ModuleContext context) throws Exception;
}
