/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.messages;

import sh.pcx.unified.i18n.core.Locale;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * Source for loading translation messages.
 *
 * <p>A MessageSource represents a location or resource from which translation
 * messages can be loaded. Implementations include file-based sources (YAML, JSON),
 * resource sources (from JAR files), and database sources.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Load from a YAML file
 * MessageSource fileSource = FileMessageSource.yaml(Path.of("lang/en_US.yml"));
 *
 * // Load from plugin resources
 * MessageSource resourceSource = ResourceMessageSource.yaml(plugin, "lang/en_US.yml");
 *
 * // Register with I18nService
 * i18n.registerSource(Locale.US_ENGLISH, fileSource);
 * }</pre>
 *
 * <h2>Message Format</h2>
 * <p>Messages are loaded as a flat map of key-value pairs:
 * <pre>
 * messages.welcome -> "Welcome to the server!"
 * messages.goodbye -> "Goodbye, {player}!"
 * items.one -> "You have {count} item"
 * items.other -> "You have {count} items"
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MessageBundle
 * @see FileMessageSource
 */
public interface MessageSource {

    /**
     * Loads all messages from this source.
     *
     * <p>Returns a flat map where keys are dot-separated paths
     * and values are the raw message strings.
     *
     * @return an unmodifiable map of message keys to values
     * @throws IOException if loading fails
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> load() throws IOException;

    /**
     * Reloads messages from this source.
     *
     * <p>This is equivalent to {@link #load()} but may perform
     * additional cleanup or caching operations.
     *
     * @return the reloaded messages
     * @throws IOException if reloading fails
     * @since 1.0.0
     */
    @NotNull
    default Map<String, String> reload() throws IOException {
        return load();
    }

    /**
     * Returns a description of this source for logging/debugging.
     *
     * @return a descriptive string (e.g., file path, resource name)
     * @since 1.0.0
     */
    @NotNull
    String getDescription();

    /**
     * Checks if this source supports hot reloading.
     *
     * @return true if the source can detect and reload changes
     * @since 1.0.0
     */
    default boolean supportsHotReload() {
        return false;
    }

    /**
     * Returns the locale this source provides messages for.
     *
     * @return the locale, or null if not bound to a specific locale
     * @since 1.0.0
     */
    @NotNull
    Locale getLocale();

    /**
     * Returns the priority of this source for merging.
     *
     * <p>Higher priority sources override lower priority sources
     * when messages have the same key.
     *
     * @return the priority (default is 0)
     * @since 1.0.0
     */
    default int getPriority() {
        return 0;
    }
}
