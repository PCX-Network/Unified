/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */

/**
 * Guice-based dependency injection system for the UnifiedPlugin API.
 *
 * <p>This package provides a comprehensive dependency injection framework built on
 * Google Guice 7.0.0, extended with Minecraft-specific features like player scopes,
 * world scopes, and plugin lifecycle management.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>Annotations</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.inject.Service} - Marks a class as an injectable service</li>
 *   <li>{@link sh.pcx.unified.inject.PlayerScoped} - One instance per player session</li>
 *   <li>{@link sh.pcx.unified.inject.WorldScoped} - One instance per world</li>
 *   <li>{@link sh.pcx.unified.inject.PluginScoped} - One instance per plugin</li>
 *   <li>{@link sh.pcx.unified.inject.Platform} - Qualifier for platform-specific implementations</li>
 *   <li>{@link sh.pcx.unified.inject.Config} - Inject configuration values</li>
 * </ul>
 *
 * <h3>Lifecycle Annotations</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.inject.PostConstruct} - Called after injection completes</li>
 *   <li>{@link sh.pcx.unified.inject.PreDestroy} - Called before object destruction</li>
 *   <li>{@link sh.pcx.unified.inject.OnReload} - Called on configuration reload</li>
 * </ul>
 *
 * <h3>Modules</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.inject.UnifiedModule} - Base module with core infrastructure</li>
 *   <li>{@link sh.pcx.unified.inject.FeatureModule} - Base for optional features</li>
 *   <li>{@link sh.pcx.unified.inject.PlatformModule} - Base for platform-specific bindings</li>
 * </ul>
 *
 * <h3>Injector Management</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.inject.InjectorFactory} - Create configured injectors</li>
 *   <li>{@link sh.pcx.unified.inject.InjectorHolder} - Global injector registry</li>
 *   <li>{@link sh.pcx.unified.inject.ServiceLocator} - Static service location fallback</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>1. Create a Module</h3>
 * <pre>{@code
 * public class MyPluginModule extends UnifiedModule {
 *     private final MyPlugin plugin;
 *
 *     public MyPluginModule(MyPlugin plugin) {
 *         this.plugin = plugin;
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         super.configure();  // Important: call super!
 *
 *         // Bind your plugin
 *         bind(MyPlugin.class).toInstance(plugin);
 *
 *         // Bind services
 *         bind(PlayerManager.class).in(Singleton.class);
 *         bind(PlayerSession.class).in(PlayerScoped.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>2. Create the Injector</h3>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *     private Injector injector;
 *
 *     @Override
 *     public void onEnable() {
 *         injector = InjectorFactory.builder()
 *             .module(new MyPluginModule(this))
 *             .forPlugin(getName())
 *             .build();
 *
 *         // Get your main service
 *         MyService service = injector.getInstance(MyService.class);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         InjectorFactory.shutdown(injector);
 *         InjectorHolder.unregister(getName());
 *     }
 * }
 * }</pre>
 *
 * <h3>3. Define Services</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class PlayerManager {
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Inject
 *     @Config("settings.cache-ttl")
 *     private int cacheTtl;
 *
 *     @PostConstruct
 *     public void initialize() {
 *         // Set up after injection
 *     }
 *
 *     @PreDestroy
 *     public void cleanup() {
 *         // Clean up before shutdown
 *     }
 *
 *     @OnReload
 *     public void onReload() {
 *         // Handle config reload
 *     }
 * }
 * }</pre>
 *
 * <h2>Injection Types</h2>
 *
 * <h3>Constructor Injection (Preferred)</h3>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final DatabaseService database;
 *     private final ConfigService config;
 *
 *     @Inject
 *     public MyService(DatabaseService database, ConfigService config) {
 *         this.database = database;
 *         this.config = config;
 *     }
 * }
 * }</pre>
 *
 * <h3>Field Injection</h3>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     @Inject
 *     private DatabaseService database;
 *
 *     @Inject
 *     @Config("debug.enabled")
 *     private boolean debugEnabled;
 * }
 * }</pre>
 *
 * <h3>Method Injection</h3>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private DatabaseService database;
 *
 *     @Inject
 *     public void setDatabase(DatabaseService database) {
 *         this.database = database;
 *     }
 * }
 * }</pre>
 *
 * <h2>Scopes</h2>
 *
 * <h3>Player Scope</h3>
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerSession {
 *     // One instance per player
 * }
 *
 * // Using scoped services
 * try (var ctx = scopeManager.enterPlayer(player.getUniqueId())) {
 *     PlayerSession session = injector.getInstance(PlayerSession.class);
 * }
 * }</pre>
 *
 * <h3>World Scope</h3>
 * <pre>{@code
 * @Service
 * @WorldScoped
 * public class WorldConfig {
 *     // One instance per world
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.inject.scope
 * @see sh.pcx.unified.inject.lifecycle
 */
package sh.pcx.unified.inject;
