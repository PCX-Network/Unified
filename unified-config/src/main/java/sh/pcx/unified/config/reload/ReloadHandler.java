/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.reload;

import org.jetbrains.annotations.NotNull;

/**
 * Handler for configuration reload events.
 *
 * <p>ReloadHandler is invoked when a watched configuration file is modified
 * and successfully reloaded. It receives a {@link ReloadResult} containing
 * either the new configuration or error information.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * configService.watch(PluginConfig.class, configPath, result -> {
 *     if (result.isSuccess()) {
 *         this.config = result.get();
 *         logger.info("Configuration reloaded successfully!");
 *     } else {
 *         logger.warning("Failed to reload config: " + result.getErrorMessage());
 *     }
 * });
 * }</pre>
 *
 * <h2>With Validation</h2>
 * <pre>{@code
 * configService.watch(PluginConfig.class, configPath, result -> {
 *     if (result.isSuccess()) {
 *         PluginConfig newConfig = result.get();
 *
 *         // Validate before applying
 *         ValidationResult validation = configService.validate(newConfig);
 *         if (validation.isValid()) {
 *             this.config = newConfig;
 *             applyConfiguration();
 *         } else {
 *             logger.warning("Reloaded config is invalid:");
 *             validation.getErrors().forEach(e ->
 *                 logger.warning("  - " + e.getFullMessage())
 *             );
 *         }
 *     }
 * });
 * }</pre>
 *
 * <h2>Using Method Reference</h2>
 * <pre>{@code
 * public class MyPlugin {
 *     private PluginConfig config;
 *
 *     public void enable() {
 *         configService.watch(PluginConfig.class, configPath, this::onConfigReload);
 *     }
 *
 *     private void onConfigReload(ReloadResult<PluginConfig> result) {
 *         result.ifSuccess(newConfig -> {
 *             this.config = newConfig;
 *             // Notify other systems
 *             eventBus.post(new ConfigReloadedEvent(newConfig));
 *         }).ifFailure(error ->
 *             logger.severe("Config reload failed: " + error.getMessage())
 *         );
 *     }
 * }
 * }</pre>
 *
 * @param <T> the configuration type
 * @author Supatuck
 * @since 1.0.0
 * @see ReloadResult
 * @see ConfigWatcher
 */
@FunctionalInterface
public interface ReloadHandler<T> {

    /**
     * Called when a configuration reload occurs.
     *
     * <p>This method is called on a background thread. If you need to
     * update Bukkit state, ensure you schedule it on the main thread.</p>
     *
     * @param result the reload result
     * @since 1.0.0
     */
    void onReload(@NotNull ReloadResult<T> result);

    /**
     * Creates a handler that only processes successful reloads.
     *
     * @param consumer the success consumer
     * @param <T> the configuration type
     * @return the reload handler
     * @since 1.0.0
     */
    @NotNull
    static <T> ReloadHandler<T> onSuccess(@NotNull java.util.function.Consumer<T> consumer) {
        return result -> {
            if (result.isSuccess()) {
                consumer.accept(result.get());
            }
        };
    }

    /**
     * Creates a handler with separate success and failure callbacks.
     *
     * @param successConsumer called on successful reload
     * @param failureConsumer called on failed reload
     * @param <T> the configuration type
     * @return the reload handler
     * @since 1.0.0
     */
    @NotNull
    static <T> ReloadHandler<T> of(
            @NotNull java.util.function.Consumer<T> successConsumer,
            @NotNull java.util.function.Consumer<Throwable> failureConsumer
    ) {
        return result -> {
            if (result.isSuccess()) {
                successConsumer.accept(result.get());
            } else {
                failureConsumer.accept(result.getError());
            }
        };
    }

    /**
     * Creates a composed handler that calls multiple handlers.
     *
     * @param other the other handler
     * @return a composed handler
     * @since 1.0.0
     */
    @NotNull
    default ReloadHandler<T> andThen(@NotNull ReloadHandler<T> other) {
        return result -> {
            this.onReload(result);
            other.onReload(result);
        };
    }
}
