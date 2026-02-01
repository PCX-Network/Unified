/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;

import java.util.logging.Logger;

/**
 * Base module for optional plugin features.
 *
 * <p>FeatureModule provides a framework for implementing optional functionality
 * that can be enabled or disabled based on configuration, dependencies, or
 * runtime conditions. Features can gracefully degrade when dependencies are
 * unavailable.</p>
 *
 * <h2>Feature Lifecycle</h2>
 * <ol>
 *   <li>{@link #isEnabled()} is called to check if the feature should be loaded</li>
 *   <li>If enabled, {@link #configure()} is called to set up bindings</li>
 *   <li>If disabled, a stub/no-op implementation may be bound instead</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Conditional Feature</h3>
 * <pre>{@code
 * public class VaultEconomyModule extends FeatureModule {
 *
 *     @Override
 *     public String getName() {
 *         return "Vault Economy Integration";
 *     }
 *
 *     @Override
 *     public boolean isEnabled() {
 *         // Only enable if Vault plugin is present
 *         return Bukkit.getPluginManager().getPlugin("Vault") != null;
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         bind(EconomyProvider.class).to(VaultEconomyProvider.class).in(Singleton.class);
 *     }
 *
 *     @Override
 *     protected void configureFallback() {
 *         // Provide no-op implementation when Vault is not available
 *         bind(EconomyProvider.class).to(NoOpEconomyProvider.class).in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Configuration-Based Feature</h3>
 * <pre>{@code
 * public class MetricsModule extends FeatureModule {
 *     private final boolean metricsEnabled;
 *
 *     public MetricsModule(ConfigService config) {
 *         this.metricsEnabled = config.getBoolean("metrics.enabled", true);
 *     }
 *
 *     @Override
 *     public String getName() {
 *         return "bStats Metrics";
 *     }
 *
 *     @Override
 *     public boolean isEnabled() {
 *         return metricsEnabled;
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         bind(MetricsService.class).to(BStatsMetricsService.class).in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Feature with Dependencies</h3>
 * <pre>{@code
 * public class RedisMessagingModule extends FeatureModule {
 *
 *     @Override
 *     public String getName() {
 *         return "Redis Messaging";
 *     }
 *
 *     @Override
 *     public String[] getDependencies() {
 *         return new String[] { "jedis" };
 *     }
 *
 *     @Override
 *     public boolean isEnabled() {
 *         // Check if Redis library is available
 *         try {
 *             Class.forName("redis.clients.jedis.Jedis");
 *             return true;
 *         } catch (ClassNotFoundException e) {
 *             return false;
 *         }
 *     }
 *
 *     @Override
 *     protected void configure() {
 *         bind(MessagingService.class).to(RedisMessagingService.class).in(Singleton.class);
 *     }
 *
 *     @Override
 *     protected void configureFallback() {
 *         // Use local-only messaging when Redis unavailable
 *         bind(MessagingService.class).to(LocalMessagingService.class).in(Singleton.class);
 *     }
 * }
 * }</pre>
 *
 * <h3>Installing Feature Modules</h3>
 * <pre>{@code
 * public class MyPluginModule extends UnifiedModule {
 *     @Override
 *     protected void configure() {
 *         super.configure();
 *
 *         // Install feature modules - they handle their own enable/disable logic
 *         installFeature(new VaultEconomyModule());
 *         installFeature(new PlaceholderAPIModule());
 *         installFeature(new DiscordIntegrationModule());
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see UnifiedModule
 * @see PlatformModule
 */
public abstract class FeatureModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger(FeatureModule.class.getName());

    private boolean configured = false;

    /**
     * Gets the human-readable name of this feature.
     *
     * <p>Used for logging and debugging purposes.</p>
     *
     * @return the feature name
     */
    public abstract String getName();

    /**
     * Checks if this feature should be enabled.
     *
     * <p>Override this method to implement conditional feature loading based on
     * configuration, available plugins, library presence, or other runtime conditions.</p>
     *
     * @return {@code true} if the feature should be enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Gets the library dependencies required by this feature.
     *
     * <p>Override to specify required libraries. The feature will be disabled
     * if any required library is unavailable.</p>
     *
     * @return array of required library names
     */
    public String[] getDependencies() {
        return new String[0];
    }

    /**
     * Gets the plugin dependencies required by this feature.
     *
     * <p>Override to specify required plugins. The feature will be disabled
     * if any required plugin is not loaded.</p>
     *
     * @return array of required plugin names
     */
    public String[] getPluginDependencies() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method handles the enable/disable logic and delegates to
     * {@link #configureFeature()} for enabled features or
     * {@link #configureFallback()} for disabled features.</p>
     */
    @Override
    protected final void configure() {
        if (configured) {
            return;
        }
        configured = true;

        if (shouldEnable()) {
            LOGGER.info("Enabling feature: " + getName());
            try {
                configureFeature();
            } catch (Exception e) {
                LOGGER.warning("Failed to configure feature '" + getName() + "': " + e.getMessage());
                LOGGER.info("Falling back to disabled configuration for: " + getName());
                configureFallback();
            }
        } else {
            LOGGER.fine("Feature disabled: " + getName());
            configureFallback();
        }
    }

    /**
     * Determines if this feature should be enabled based on all conditions.
     *
     * @return {@code true} if all conditions are met
     */
    private boolean shouldEnable() {
        // Check user override
        if (!isEnabled()) {
            return false;
        }

        // Check library dependencies
        for (String dependency : getDependencies()) {
            if (!isLibraryAvailable(dependency)) {
                LOGGER.fine("Feature '" + getName() + "' disabled: missing library '" + dependency + "'");
                return false;
            }
        }

        // Check plugin dependencies
        for (String plugin : getPluginDependencies()) {
            if (!isPluginAvailable(plugin)) {
                LOGGER.fine("Feature '" + getName() + "' disabled: missing plugin '" + plugin + "'");
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a library is available on the classpath.
     *
     * <p>Override to customize library detection logic.</p>
     *
     * @param libraryName the library name to check
     * @return {@code true} if the library is available
     */
    protected boolean isLibraryAvailable(String libraryName) {
        // Default implementation - subclasses can override with actual checks
        return true;
    }

    /**
     * Checks if a plugin is loaded and available.
     *
     * <p>Override to customize plugin detection logic.</p>
     *
     * @param pluginName the plugin name to check
     * @return {@code true} if the plugin is available
     */
    protected boolean isPluginAvailable(String pluginName) {
        // Default implementation - subclasses can override with actual checks
        return true;
    }

    /**
     * Configures bindings when the feature is enabled.
     *
     * <p>Override this method to set up your feature's Guice bindings.</p>
     */
    protected void configureFeature() {
        // Default: no bindings
    }

    /**
     * Configures fallback bindings when the feature is disabled.
     *
     * <p>Override this method to provide no-op or stub implementations
     * when the feature cannot be enabled. This allows dependent code to
     * function without null checks.</p>
     */
    protected void configureFallback() {
        // Default: no fallback bindings
    }

    /**
     * Gets the binder for subclass use.
     *
     * <p>This is a convenience method that delegates to the protected binder()
     * method from AbstractModule.</p>
     *
     * @return the Guice binder
     */
    protected Binder getBinder() {
        return binder();
    }
}
