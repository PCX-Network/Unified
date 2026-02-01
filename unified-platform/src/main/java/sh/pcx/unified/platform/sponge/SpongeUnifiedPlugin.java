/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.platform.sponge;

import com.google.inject.Inject;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

/**
 * Sponge plugin container wrapper for the UnifiedPlugin API.
 *
 * <p>This class serves as the entry point for the UnifiedPlugin API on Sponge servers.
 * It handles the lifecycle of the platform provider and integrates with Sponge's
 * event-driven plugin lifecycle.
 *
 * <h2>Plugin Lifecycle</h2>
 * <p>The plugin follows Sponge's lifecycle events:
 * <ul>
 *   <li>{@link ConstructPluginEvent} - Plugin instance creation</li>
 *   <li>{@link StartingEngineEvent} - Server is starting, register services</li>
 *   <li>{@link StartedEngineEvent} - Server has started, fully operational</li>
 *   <li>{@link StoppingEngineEvent} - Server is stopping, cleanup</li>
 * </ul>
 *
 * <h2>Dependency Injection</h2>
 * <p>This class uses Sponge's built-in Guice integration for dependency injection.
 * The plugin container, logger, and config directory are automatically injected.
 *
 * <h2>Configuration</h2>
 * <p>Configuration files are stored in the config directory provided by Sponge,
 * typically {@code config/unifiedpluginapi/}.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see SpongePlatformProvider
 */
public final class SpongeUnifiedPlugin {

    private final PluginContainer container;
    private final ComponentLogger logger;
    private final Path configDir;
    private final Game game;

    @Nullable
    private SpongePlatformProvider platformProvider;

    /**
     * Creates a new SpongeUnifiedPlugin instance.
     *
     * <p>This constructor is called by Sponge's plugin loading system with
     * injected dependencies.
     *
     * @param container the plugin container
     * @param logger    the component logger for this plugin
     * @param configDir the configuration directory path
     * @param game      the Sponge Game instance
     * @since 1.0.0
     */
    @Inject
    public SpongeUnifiedPlugin(
            @NotNull PluginContainer container,
            @NotNull ComponentLogger logger,
            @NotNull @ConfigDir(sharedRoot = false) Path configDir,
            @NotNull Game game
    ) {
        this.container = container;
        this.logger = logger;
        this.configDir = configDir;
        this.game = game;
    }

    /**
     * Handles plugin construction.
     *
     * <p>This event is fired when the plugin instance is created.
     * Minimal initialization should occur here.
     *
     * @param event the construct plugin event
     * @since 1.0.0
     */
    @Listener
    public void onConstruct(@NotNull ConstructPluginEvent event) {
        logger.info("UnifiedPlugin API initializing on Sponge...");
    }

    /**
     * Handles server starting event.
     *
     * <p>This event is fired when the server is starting. The platform provider
     * is initialized here so that all unified API services are available before
     * other plugins load.
     *
     * @param event the starting engine event
     * @since 1.0.0
     */
    @Listener
    public void onServerStarting(@NotNull StartingEngineEvent<org.spongepowered.api.Server> event) {
        logger.info("Initializing UnifiedPlugin API platform provider...");

        try {
            platformProvider = new SpongePlatformProvider();
            platformProvider.initialize();

            logger.info("UnifiedPlugin API platform provider initialized successfully");
            logger.info("Running on: {} (Minecraft {})",
                    platformProvider.getSpongePlatform().getServerName(),
                    platformProvider.getSpongePlatform().getMinecraftVersion()
            );
        } catch (Exception e) {
            logger.error("Failed to initialize UnifiedPlugin API platform provider", e);
            throw new RuntimeException("Failed to initialize platform provider", e);
        }
    }

    /**
     * Handles server started event.
     *
     * <p>This event is fired when the server has fully started. Any post-startup
     * initialization can be performed here.
     *
     * @param event the started engine event
     * @since 1.0.0
     */
    @Listener
    public void onServerStarted(@NotNull StartedEngineEvent<org.spongepowered.api.Server> event) {
        logger.info("UnifiedPlugin API is now fully operational");

        // Log some server information
        if (platformProvider != null) {
            SpongePlatform platform = platformProvider.getSpongePlatform();
            logger.info("Platform: {} | API Version: {}",
                    platform.getServerName(),
                    platform.getSpongeApiVersion()
            );
        }
    }

    /**
     * Handles server stopping event.
     *
     * <p>This event is fired when the server is stopping. The platform provider
     * is shut down to clean up resources and clear caches.
     *
     * @param event the stopping engine event
     * @since 1.0.0
     */
    @Listener
    public void onServerStopping(@NotNull StoppingEngineEvent<org.spongepowered.api.Server> event) {
        logger.info("Shutting down UnifiedPlugin API...");

        if (platformProvider != null) {
            platformProvider.shutdown();
            platformProvider = null;
        }

        logger.info("UnifiedPlugin API shutdown complete");
    }

    /**
     * Returns the plugin container.
     *
     * @return the Sponge plugin container
     * @since 1.0.0
     */
    @NotNull
    public PluginContainer getContainer() {
        return container;
    }

    /**
     * Returns the component logger.
     *
     * @return the logger for this plugin
     * @since 1.0.0
     */
    @NotNull
    public ComponentLogger getLogger() {
        return logger;
    }

    /**
     * Returns the configuration directory path.
     *
     * @return the path to the config directory
     * @since 1.0.0
     */
    @NotNull
    public Path getConfigDirectory() {
        return configDir;
    }

    /**
     * Returns the Game instance.
     *
     * @return the Sponge Game instance
     * @since 1.0.0
     */
    @NotNull
    public Game getGame() {
        return game;
    }

    /**
     * Returns the platform provider.
     *
     * @return the platform provider, or null if not initialized
     * @since 1.0.0
     */
    @Nullable
    public SpongePlatformProvider getPlatformProvider() {
        return platformProvider;
    }

    /**
     * Checks if the platform provider is initialized.
     *
     * @return true if the platform provider is available
     * @since 1.0.0
     */
    public boolean isInitialized() {
        return platformProvider != null;
    }
}
