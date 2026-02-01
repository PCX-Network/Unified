/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

import sh.pcx.unified.modules.core.ModuleContext;

/**
 * Interface for modules that require initialization during the loading phase.
 *
 * <p>The {@link #init(ModuleContext)} method is called once when the module
 * transitions from {@link ModuleState#LOADING} to {@link ModuleState#ENABLED}.
 * This is the primary setup point for modules to initialize their state,
 * load configurations, and prepare for operation.
 *
 * <h2>Initialization Order</h2>
 * <ol>
 *   <li>Module class is instantiated</li>
 *   <li>Dependency injection is performed</li>
 *   <li>{@link #init(ModuleContext)} is called</li>
 *   <li>Listeners are registered (if {@code @Listen} present)</li>
 *   <li>Commands are registered (if {@code @Command} present)</li>
 *   <li>Module state changes to ENABLED</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(name = "BattlePass")
 * public class BattlePassModule implements Initializable {
 *
 *     @Inject
 *     private PlayerDataService playerData;
 *
 *     private BattlePassConfig config;
 *     private PassProgressManager progressManager;
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         // Load configuration
 *         this.config = context.loadConfig(BattlePassConfig.class);
 *
 *         // Initialize internal managers
 *         this.progressManager = new PassProgressManager(config, playerData);
 *
 *         // Register additional components
 *         context.getScheduler().runTaskTimer(() -> {
 *             progressManager.processPendingRewards();
 *         }, 20L, 100L);
 *
 *         // Log initialization
 *         context.getLogger().info("BattlePass initialized for Season " + config.getSeason());
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>If {@code init()} throws an exception:
 * <ul>
 *   <li>The module state changes to {@link ModuleState#FAILED}</li>
 *   <li>The exception is logged with full stack trace</li>
 *   <li>Dependent modules will also fail to load</li>
 *   <li>The module can be retried via admin command after fixing the issue</li>
 * </ul>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Keep initialization fast - defer heavy operations to background tasks</li>
 *   <li>Validate configuration early and fail fast if invalid</li>
 *   <li>Use the provided logger for status messages</li>
 *   <li>Store references to injected services for later use</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleContext
 * @see Reloadable
 * @see Disableable
 */
@FunctionalInterface
public interface Initializable {

    /**
     * Called once when the module is first loaded and initialized.
     *
     * <p>Use this method to:
     * <ul>
     *   <li>Load and validate configuration</li>
     *   <li>Initialize internal managers and services</li>
     *   <li>Set up database connections or caches</li>
     *   <li>Register scheduled tasks</li>
     *   <li>Perform one-time setup operations</li>
     * </ul>
     *
     * <p>At this point, all dependencies declared in {@code @Module} are
     * guaranteed to be available and injected.
     *
     * @param context the module context providing access to plugin services
     * @throws Exception if initialization fails (module will enter FAILED state)
     */
    void init(ModuleContext context) throws Exception;
}
