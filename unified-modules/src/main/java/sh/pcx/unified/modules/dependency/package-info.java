/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Dependency resolution system for module loading order.
 *
 * <p>This package provides components for resolving module dependencies
 * and determining the correct load order:
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.modules.dependency.DependencyResolver} -
 *       Resolves module dependencies and computes topological load order</li>
 *   <li>{@link sh.pcx.unified.modules.dependency.DependencyGraph} -
 *       Directed graph representing module dependency relationships</li>
 *   <li>{@link sh.pcx.unified.modules.dependency.CircularDependencyException} -
 *       Exception thrown when a circular dependency is detected</li>
 * </ul>
 *
 * <h2>Resolution Process</h2>
 * <ol>
 *   <li>Build dependency graph from @Module annotations</li>
 *   <li>Check for circular dependencies</li>
 *   <li>Validate all required dependencies exist</li>
 *   <li>Compute topological order</li>
 *   <li>Sort by priority within each dependency level</li>
 * </ol>
 *
 * <h2>Dependency Types</h2>
 * <ul>
 *   <li><b>Hard dependencies</b>: Required modules that must be loaded first.
 *       If missing, the dependent module fails to load.</li>
 *   <li><b>Soft dependencies</b>: Optional modules that enhance functionality.
 *       The module loads even if soft dependencies are missing.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(
 *     name = "Rewards",
 *     dependencies = {"Economy", "PlayerData"},      // Required
 *     softDependencies = {"Discord", "WebPanel"}     // Optional
 * )
 * public class RewardsModule implements Initializable {
 *
 *     @Inject private EconomyService economy;       // Always available
 *     @Inject @Nullable private DiscordService discord; // May be null
 *
 *     @Override
 *     public void init(ModuleContext context) {
 *         if (discord != null) {
 *             discord.registerNotifications(this);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
package sh.pcx.unified.modules.dependency;
