/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import sh.pcx.unified.inject.lifecycle.LifecycleProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating configured Guice {@link Injector} instances.
 *
 * <p>InjectorFactory provides a fluent API for creating injectors with proper
 * configuration for the UnifiedPlugin framework. It handles module composition,
 * lifecycle processing, and development/production mode configuration.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Simple creation
 * Injector injector = InjectorFactory.create(new MyPluginModule());
 *
 * // With plugin instance
 * Injector injector = InjectorFactory.create(plugin, new MyPluginModule());
 * }</pre>
 *
 * <h2>Builder Pattern</h2>
 * <pre>{@code
 * Injector injector = InjectorFactory.builder()
 *     .module(new UnifiedModule())
 *     .module(new MyPluginModule())
 *     .module(new DatabaseModule())
 *     .developmentMode(true)
 *     .eagerSingletons(true)
 *     .build();
 * }</pre>
 *
 * <h2>With Module Overrides</h2>
 * <pre>{@code
 * // Useful for testing - override production bindings
 * Injector injector = InjectorFactory.builder()
 *     .module(new MyPluginModule())
 *     .overrideWith(new TestOverrideModule())
 *     .build();
 * }</pre>
 *
 * <h2>With Parent Injector</h2>
 * <pre>{@code
 * // Create child injector that inherits from parent
 * Injector parentInjector = ...;
 * Injector childInjector = InjectorFactory.builder()
 *     .parent(parentInjector)
 *     .module(new ChildModule())
 *     .build();
 * }</pre>
 *
 * <h2>Plugin Integration</h2>
 * <pre>{@code
 * public class MyPlugin extends UnifiedPlugin {
 *     private Injector injector;
 *
 *     @Override
 *     public void onEnable() {
 *         injector = InjectorFactory.builder()
 *             .module(new UnifiedModule())
 *             .module(new MyPluginModule(this))
 *             .module(detectPlatformModule())
 *             .developmentMode(isDebugEnabled())
 *             .build();
 *
 *         // Store for cleanup
 *         InjectorHolder.register(this, injector);
 *
 *         // Start services
 *         injector.getInstance(MyService.class).start();
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         InjectorHolder.unregister(this);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see InjectorHolder
 * @see UnifiedModule
 */
public final class InjectorFactory {

    private static final Logger LOGGER = Logger.getLogger(InjectorFactory.class.getName());

    private InjectorFactory() {
        // Utility class
    }

    /**
     * Creates a new injector with the specified modules.
     *
     * <p>This is a convenience method for simple injector creation.
     * For more control, use {@link #builder()}.</p>
     *
     * @param modules the modules to include
     * @return the configured injector
     */
    public static Injector create(Module... modules) {
        return builder()
            .modules(modules)
            .build();
    }

    /**
     * Creates a new injector for a plugin with the specified modules.
     *
     * <p>The plugin name is automatically registered with {@link InjectorHolder}
     * for later retrieval and cleanup.</p>
     *
     * @param pluginName the plugin name for registration
     * @param modules the modules to include
     * @return the configured injector
     */
    public static Injector create(String pluginName, Module... modules) {
        Injector injector = builder()
            .modules(modules)
            .build();
        InjectorHolder.register(pluginName, injector);
        return injector;
    }

    /**
     * Creates a new builder for fluent injector configuration.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring and creating injectors.
     */
    public static class Builder {
        private final List<Module> modules = new ArrayList<>();
        private final List<Module> overrideModules = new ArrayList<>();
        private Injector parentInjector;
        private Stage stage = Stage.PRODUCTION;
        private boolean eagerSingletons = false;
        private String pluginName;

        /**
         * Adds a module to the injector configuration.
         *
         * @param module the module to add
         * @return this builder for chaining
         */
        public Builder module(Module module) {
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Adds multiple modules to the injector configuration.
         *
         * @param modules the modules to add
         * @return this builder for chaining
         */
        public Builder modules(Module... modules) {
            this.modules.addAll(Arrays.asList(modules));
            return this;
        }

        /**
         * Adds multiple modules to the injector configuration.
         *
         * @param modules the modules to add
         * @return this builder for chaining
         */
        public Builder modules(Iterable<? extends Module> modules) {
            for (Module module : modules) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Adds an override module that takes precedence over regular modules.
         *
         * <p>Override modules are useful for testing, allowing production
         * bindings to be replaced with test doubles.</p>
         *
         * @param module the override module
         * @return this builder for chaining
         */
        public Builder overrideWith(Module module) {
            if (module != null) {
                this.overrideModules.add(module);
            }
            return this;
        }

        /**
         * Adds multiple override modules.
         *
         * @param modules the override modules
         * @return this builder for chaining
         */
        public Builder overrideWith(Module... modules) {
            this.overrideModules.addAll(Arrays.asList(modules));
            return this;
        }

        /**
         * Sets a parent injector for creating a child injector.
         *
         * <p>The child injector inherits all bindings from the parent and
         * can add or override bindings.</p>
         *
         * @param parent the parent injector
         * @return this builder for chaining
         */
        public Builder parent(Injector parent) {
            this.parentInjector = parent;
            return this;
        }

        /**
         * Sets development mode which enables additional runtime checks.
         *
         * <p>In development mode ({@link Stage#DEVELOPMENT}), Guice performs
         * additional validation that can catch configuration errors early.</p>
         *
         * @param development {@code true} for development mode
         * @return this builder for chaining
         */
        public Builder developmentMode(boolean development) {
            this.stage = development ? Stage.DEVELOPMENT : Stage.PRODUCTION;
            return this;
        }

        /**
         * Sets the Guice stage directly.
         *
         * @param stage the stage to use
         * @return this builder for chaining
         */
        public Builder stage(Stage stage) {
            this.stage = stage;
            return this;
        }

        /**
         * Enables eager singleton instantiation.
         *
         * <p>When enabled, all singletons are created immediately during
         * injector construction. This can help catch configuration errors
         * early but increases startup time.</p>
         *
         * @param eager {@code true} to enable eager singletons
         * @return this builder for chaining
         */
        public Builder eagerSingletons(boolean eager) {
            this.eagerSingletons = eager;
            return this;
        }

        /**
         * Sets the plugin name for automatic registration.
         *
         * @param name the plugin name
         * @return this builder for chaining
         */
        public Builder forPlugin(String name) {
            this.pluginName = name;
            return this;
        }

        /**
         * Builds and returns the configured injector.
         *
         * @return the configured injector
         * @throws com.google.inject.CreationException if injector creation fails
         */
        public Injector build() {
            LOGGER.fine("Building injector with " + modules.size() + " modules");

            // Combine modules, applying overrides if present
            Module combinedModule;
            if (modules.isEmpty()) {
                combinedModule = new UnifiedModule();
            } else if (modules.size() == 1 && overrideModules.isEmpty()) {
                combinedModule = modules.get(0);
            } else {
                combinedModule = Modules.combine(modules);
            }

            // Apply overrides
            if (!overrideModules.isEmpty()) {
                Module overrides = Modules.combine(overrideModules);
                combinedModule = Modules.override(combinedModule).with(overrides);
            }

            // Create injector
            Injector injector;
            try {
                if (parentInjector != null) {
                    injector = parentInjector.createChildInjector(combinedModule);
                } else {
                    injector = Guice.createInjector(stage, combinedModule);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create injector", e);
                throw e;
            }

            // Eager singleton instantiation if requested
            if (eagerSingletons) {
                LOGGER.fine("Instantiating eager singletons");
                // Force instantiation by getting all explicit bindings
                injector.getAllBindings().forEach((key, binding) -> {
                    try {
                        injector.getInstance(key);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to eagerly instantiate: " + key, e);
                    }
                });
            }

            // Register with holder if plugin name specified
            if (pluginName != null && !pluginName.isEmpty()) {
                InjectorHolder.register(pluginName, injector);
            }

            LOGGER.info("Injector created successfully");
            return injector;
        }
    }

    /**
     * Shuts down an injector, processing all lifecycle callbacks.
     *
     * <p>This method finds all instances managed by the injector and invokes
     * their {@code @PreDestroy} methods.</p>
     *
     * @param injector the injector to shut down
     */
    public static void shutdown(Injector injector) {
        if (injector == null) {
            return;
        }

        LOGGER.info("Shutting down injector");

        try {
            LifecycleProcessor processor = injector.getInstance(LifecycleProcessor.class);

            // Process all bindings for PreDestroy
            injector.getAllBindings().forEach((key, binding) -> {
                try {
                    Object instance = injector.getInstance(key);
                    processor.invokePreDestroy(instance);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error during shutdown for: " + key, e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting LifecycleProcessor during shutdown", e);
        }

        LOGGER.info("Injector shutdown complete");
    }
}
