/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import sh.pcx.unified.inject.lifecycle.LifecycleListener;
import sh.pcx.unified.inject.lifecycle.LifecycleProcessor;
import sh.pcx.unified.inject.scope.PlayerScope;
import sh.pcx.unified.inject.scope.PluginScope;
import sh.pcx.unified.inject.scope.ScopeManager;
import sh.pcx.unified.inject.scope.WorldScope;

/**
 * Base Guice module for UnifiedPlugin applications.
 *
 * <p>This module provides the core dependency injection infrastructure including
 * custom scopes, lifecycle processing, and common bindings. Plugin developers
 * should extend this module to add their own bindings.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Player, World, and Plugin scope support</li>
 *   <li>Lifecycle annotations ({@code @PostConstruct}, {@code @PreDestroy}, {@code @OnReload})</li>
 *   <li>Automatic service discovery via {@code @Service} annotation</li>
 *   <li>Configuration injection via {@code @Config} annotation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <h3>Basic Plugin Module</h3>
 * <pre>{@code
 * public class MyPluginModule extends UnifiedModule {
 *
 *     private final MyPlugin plugin;
 *
 *     public MyPluginModule(MyPlugin plugin) {
 *         this.plugin = plugin;
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         // Call super to set up core infrastructure
 *         super.configure();
 *
 *         // Bind your plugin instance
 *         bind(MyPlugin.class).toInstance(plugin);
 *
 *         // Bind services
 *         bind(PlayerManager.class).in(Singleton.class);
 *         bind(EconomyService.class).to(VaultEconomyService.class).in(Singleton.class);
 *
 *         // Bind with scopes
 *         bind(PlayerSession.class).in(PlayerScoped.class);
 *         bind(WorldConfig.class).in(WorldScoped.class);
 *     }
 *
 *     @Provides
 *     @Singleton
 *     public DatabaseService provideDatabase() {
 *         return new HikariDatabaseService(plugin.getConfig());
 *     }
 * }
 * }</pre>
 *
 * <h3>Creating the Injector</h3>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *     private Injector injector;
 *
 *     @Override
 *     public void onEnable() {
 *         injector = InjectorFactory.create(this, new MyPluginModule(this));
 *
 *         // Get instances
 *         PlayerManager playerManager = injector.getInstance(PlayerManager.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Installing Additional Modules</h3>
 * <pre>{@code
 * public class MyPluginModule extends UnifiedModule {
 *     @Override
 *     protected void configure() {
 *         super.configure();
 *
 *         // Install feature modules
 *         install(new DatabaseModule());
 *         install(new MessagingModule());
 *
 *         // Install platform-specific module
 *         install(new PaperModule());
 *     }
 * }
 * }</pre>
 *
 * <h2>Scope Configuration</h2>
 * <p>The base module configures three custom scopes:</p>
 * <ul>
 *   <li>{@link PlayerScoped} - One instance per player session</li>
 *   <li>{@link WorldScoped} - One instance per world</li>
 *   <li>{@link PluginScoped} - One instance per plugin</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see FeatureModule
 * @see PlatformModule
 * @see InjectorFactory
 */
public class UnifiedModule extends AbstractModule {

    private final PlayerScope playerScope;
    private final WorldScope worldScope;
    private final PluginScope pluginScope;

    /**
     * Creates a new UnifiedModule with fresh scope instances.
     */
    public UnifiedModule() {
        this.playerScope = new PlayerScope();
        this.worldScope = new WorldScope();
        this.pluginScope = new PluginScope();
    }

    /**
     * Creates a new UnifiedModule with the provided scope instances.
     *
     * <p>Use this constructor when you need to share scopes across multiple modules
     * or when integrating with an existing scope infrastructure.</p>
     *
     * @param playerScope the player scope instance
     * @param worldScope the world scope instance
     * @param pluginScope the plugin scope instance
     */
    public UnifiedModule(PlayerScope playerScope, WorldScope worldScope, PluginScope pluginScope) {
        this.playerScope = playerScope;
        this.worldScope = worldScope;
        this.pluginScope = pluginScope;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Configures core infrastructure including scopes and lifecycle processing.
     * Subclasses should call {@code super.configure()} at the beginning of their
     * configure method.</p>
     */
    @Override
    protected void configure() {
        // Bind scope annotations to scope implementations
        bindScope(PlayerScoped.class, playerScope);
        bindScope(WorldScoped.class, worldScope);
        bindScope(PluginScoped.class, pluginScope);

        // Bind scope instances for direct injection
        bind(PlayerScope.class).toInstance(playerScope);
        bind(WorldScope.class).toInstance(worldScope);
        bind(PluginScope.class).toInstance(pluginScope);

        // Bind lifecycle processor
        bind(LifecycleProcessor.class).in(Singleton.class);

        // Bind scope manager
        bind(ScopeManager.class).in(Singleton.class);

        // Register lifecycle listener for @PostConstruct/@PreDestroy processing
        bindListener(Matchers.any(), new LifecycleTypeListener());
    }

    /**
     * Provides the player scope instance.
     *
     * @return the player scope
     */
    @Provides
    @Singleton
    protected PlayerScope providePlayerScope() {
        return playerScope;
    }

    /**
     * Provides the world scope instance.
     *
     * @return the world scope
     */
    @Provides
    @Singleton
    protected WorldScope provideWorldScope() {
        return worldScope;
    }

    /**
     * Provides the plugin scope instance.
     *
     * @return the plugin scope
     */
    @Provides
    @Singleton
    protected PluginScope providePluginScope() {
        return pluginScope;
    }

    /**
     * Gets the player scope for subclass access.
     *
     * @return the player scope
     */
    protected PlayerScope getPlayerScope() {
        return playerScope;
    }

    /**
     * Gets the world scope for subclass access.
     *
     * @return the world scope
     */
    protected WorldScope getWorldScope() {
        return worldScope;
    }

    /**
     * Gets the plugin scope for subclass access.
     *
     * @return the plugin scope
     */
    protected PluginScope getPluginScope() {
        return pluginScope;
    }

    /**
     * TypeListener that registers the LifecycleListener for all types.
     */
    private static class LifecycleTypeListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            encounter.register(new LifecycleListener<>());
        }
    }
}
