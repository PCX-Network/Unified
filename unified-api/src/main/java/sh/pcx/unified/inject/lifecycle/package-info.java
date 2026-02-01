/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */

/**
 * Lifecycle management for dependency-injected objects.
 *
 * <p>This package provides infrastructure for processing lifecycle annotations
 * and managing the initialization and cleanup of injected objects.</p>
 *
 * <h2>Components</h2>
 *
 * <h3>LifecycleProcessor</h3>
 * <p>Central processor that discovers and invokes lifecycle methods. It caches
 * reflection data for performance and handles error cases gracefully.</p>
 *
 * <pre>{@code
 * @Inject
 * private LifecycleProcessor lifecycle;
 *
 * // Manual invocation
 * lifecycle.invokePostConstruct(myObject);
 * lifecycle.invokePreDestroy(myObject);
 *
 * // Trigger reload on all registered objects
 * lifecycle.triggerReload();
 * }</pre>
 *
 * <h3>LifecycleListener</h3>
 * <p>Guice InjectionListener that automatically processes {@code @PostConstruct}
 * methods after injection. Registered automatically by UnifiedModule.</p>
 *
 * <h2>Supported Annotations</h2>
 *
 * <h3>@PostConstruct</h3>
 * <p>Called immediately after all dependencies are injected:</p>
 * <pre>{@code
 * @PostConstruct
 * public void initialize() {
 *     // Dependencies are available here
 *     database.connect();
 * }
 * }</pre>
 *
 * <h3>@PreDestroy</h3>
 * <p>Called before object destruction or scope exit:</p>
 * <pre>{@code
 * @PreDestroy
 * public void cleanup() {
 *     // Clean up resources
 *     database.disconnect();
 * }
 * }</pre>
 *
 * <h3>@OnReload</h3>
 * <p>Called when plugin configuration is reloaded:</p>
 * <pre>{@code
 * @OnReload
 * public void reloadConfig() {
 *     // Re-read configuration values
 *     this.maxPlayers = config.getInt("max-players");
 * }
 * }</pre>
 *
 * <h2>Execution Order</h2>
 * <ul>
 *   <li><b>PostConstruct:</b> Superclass first, then subclass (by priority)</li>
 *   <li><b>PreDestroy:</b> Subclass first, then superclass (by priority)</li>
 *   <li><b>OnReload:</b> By priority (lower values first)</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.PostConstruct
 * @see sh.pcx.unified.inject.PreDestroy
 * @see sh.pcx.unified.inject.OnReload
 */
package sh.pcx.unified.inject.lifecycle;
