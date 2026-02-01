/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.i18n.core.Locale;
import sh.pcx.unified.i18n.core.LocaleRegistry;
import sh.pcx.unified.i18n.core.MessageKey;
import sh.pcx.unified.i18n.formatting.Replacement;
import sh.pcx.unified.i18n.messages.MessageBundle;
import sh.pcx.unified.i18n.messages.MessageSource;
import sh.pcx.unified.i18n.player.PlayerLocale;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Main service interface for internationalization (i18n) and localization.
 *
 * <p>The I18nService provides a complete localization solution for Minecraft
 * plugins, supporting multiple languages, per-player preferences, and
 * seamless integration with Adventure's MiniMessage format.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Multi-language support:</b> Load and manage multiple locales</li>
 *   <li><b>Per-player preferences:</b> Each player can have their own language</li>
 *   <li><b>Fallback chain:</b> Automatic fallback to default locale then English</li>
 *   <li><b>MiniMessage integration:</b> Full support for rich text formatting</li>
 *   <li><b>Placeholder system:</b> Type-safe placeholder replacements</li>
 *   <li><b>Pluralization:</b> Automatic plural form selection based on count</li>
 *   <li><b>Hot reload:</b> Reload translations without server restart</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Get the service
 * I18nService i18n = UnifiedAPI.services().get(I18nService.class);
 *
 * // Translate a message key
 * Component message = i18n.translate(locale, MessageKey.of("welcome.message"),
 *     Replacement.of("player", playerName));
 *
 * // Send localized message to a player (uses player's preferred locale)
 * i18n.sendMessage(player, MessageKey.of("shop.purchase.success"),
 *     Replacement.of("item", itemName),
 *     Replacement.of("price", 100));
 * }</pre>
 *
 * <h2>Loading Translations</h2>
 * <pre>{@code
 * // Register a message source for a locale
 * i18n.registerSource(Locale.US_ENGLISH, FileMessageSource.yaml(langPath.resolve("en_US.yml")));
 * i18n.registerSource(Locale.GERMAN, FileMessageSource.yaml(langPath.resolve("de_DE.yml")));
 *
 * // Load from resources
 * i18n.registerSource(Locale.US_ENGLISH, ResourceMessageSource.yaml(plugin, "lang/en_US.yml"));
 * }</pre>
 *
 * <h2>Fallback Chain</h2>
 * <p>When a translation is not found, the service follows this fallback order:
 * <ol>
 *   <li>Requested locale (e.g., de_DE)</li>
 *   <li>Language fallback (e.g., de if de_DE not found)</li>
 *   <li>Server default locale</li>
 *   <li>English (en_US) as ultimate fallback</li>
 *   <li>Message key itself if nothing found</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Locale
 * @see MessageKey
 * @see MessageBundle
 * @see Replacement
 */
public interface I18nService extends Service {

    /**
     * Translates a message key to a Component for the specified locale.
     *
     * <p>The message is first looked up in the specified locale's bundle,
     * then follows the fallback chain if not found.
     *
     * @param locale       the target locale
     * @param key          the message key to translate
     * @param replacements optional placeholder replacements
     * @return the translated Component, never null
     * @since 1.0.0
     */
    @NotNull
    Component translate(@NotNull Locale locale, @NotNull MessageKey key, @NotNull Replacement... replacements);

    /**
     * Translates a message key to a Component for a player's locale.
     *
     * <p>This method looks up the player's preferred locale and translates
     * the message accordingly.
     *
     * @param playerId     the player's UUID
     * @param key          the message key to translate
     * @param replacements optional placeholder replacements
     * @return the translated Component, never null
     * @since 1.0.0
     */
    @NotNull
    Component translate(@NotNull UUID playerId, @NotNull MessageKey key, @NotNull Replacement... replacements);

    /**
     * Translates a message key to a plain String for the specified locale.
     *
     * <p>This strips all formatting and returns plain text. Useful for
     * console logging or non-player contexts.
     *
     * @param locale       the target locale
     * @param key          the message key to translate
     * @param replacements optional placeholder replacements
     * @return the translated plain text string
     * @since 1.0.0
     */
    @NotNull
    String translatePlain(@NotNull Locale locale, @NotNull MessageKey key, @NotNull Replacement... replacements);

    /**
     * Translates a raw message string with the specified locale's settings.
     *
     * <p>This is useful for translating user-provided strings that contain
     * MiniMessage formatting and placeholders.
     *
     * @param locale       the target locale for formatting rules
     * @param message      the raw message string
     * @param replacements optional placeholder replacements
     * @return the formatted Component
     * @since 1.0.0
     */
    @NotNull
    Component translateRaw(@NotNull Locale locale, @NotNull String message, @NotNull Replacement... replacements);

    /**
     * Gets the raw, unformatted message string for a key.
     *
     * <p>Returns the message exactly as stored, without applying any
     * formatting or placeholder replacement.
     *
     * @param locale the target locale
     * @param key    the message key
     * @return the raw message, or empty if not found
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getRawMessage(@NotNull Locale locale, @NotNull MessageKey key);

    /**
     * Checks if a message key exists in any loaded locale.
     *
     * @param key the message key to check
     * @return true if the key exists in at least one locale
     * @since 1.0.0
     */
    boolean hasKey(@NotNull MessageKey key);

    /**
     * Checks if a message key exists in a specific locale.
     *
     * @param locale the locale to check
     * @param key    the message key to check
     * @return true if the key exists in the specified locale
     * @since 1.0.0
     */
    boolean hasKey(@NotNull Locale locale, @NotNull MessageKey key);

    // ===== Locale Management =====

    /**
     * Returns the locale registry for managing available locales.
     *
     * @return the locale registry
     * @since 1.0.0
     */
    @NotNull
    LocaleRegistry getLocaleRegistry();

    /**
     * Returns the default server locale.
     *
     * @return the default locale
     * @since 1.0.0
     */
    @NotNull
    Locale getDefaultLocale();

    /**
     * Sets the default server locale.
     *
     * @param locale the new default locale
     * @since 1.0.0
     */
    void setDefaultLocale(@NotNull Locale locale);

    /**
     * Returns all loaded locales.
     *
     * @return an unmodifiable set of loaded locales
     * @since 1.0.0
     */
    @NotNull
    Set<Locale> getLoadedLocales();

    /**
     * Checks if a locale is currently loaded.
     *
     * @param locale the locale to check
     * @return true if the locale is loaded
     * @since 1.0.0
     */
    boolean isLoaded(@NotNull Locale locale);

    // ===== Message Sources =====

    /**
     * Registers a message source for a specific locale.
     *
     * <p>If messages for this locale already exist, the new source's
     * messages are merged, with the new source taking precedence.
     *
     * @param locale the locale for the messages
     * @param source the message source
     * @since 1.0.0
     */
    void registerSource(@NotNull Locale locale, @NotNull MessageSource source);

    /**
     * Loads messages from a directory containing locale files.
     *
     * <p>Files are expected to be named with the locale code (e.g., en_US.yml).
     *
     * @param directory the directory containing language files
     * @return the number of locales loaded
     * @since 1.0.0
     */
    int loadFromDirectory(@NotNull Path directory);

    /**
     * Loads messages asynchronously from a directory.
     *
     * @param directory the directory containing language files
     * @return a future completing with the number of locales loaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> loadFromDirectoryAsync(@NotNull Path directory);

    /**
     * Gets the message bundle for a specific locale.
     *
     * @param locale the locale
     * @return the message bundle, or empty if not loaded
     * @since 1.0.0
     */
    @NotNull
    Optional<MessageBundle> getBundle(@NotNull Locale locale);

    /**
     * Creates a new empty message bundle for a locale.
     *
     * @param locale the locale for the bundle
     * @return the created bundle
     * @since 1.0.0
     */
    @NotNull
    MessageBundle createBundle(@NotNull Locale locale);

    // ===== Player Locale Management =====

    /**
     * Gets a player's locale preference.
     *
     * @param playerId the player's UUID
     * @return the player's locale preference, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    Optional<PlayerLocale> getPlayerLocale(@NotNull UUID playerId);

    /**
     * Gets the effective locale for a player.
     *
     * <p>Returns the player's preferred locale if set, otherwise the
     * detected client locale, or the server default if detection fails.
     *
     * @param playerId the player's UUID
     * @return the effective locale for the player
     * @since 1.0.0
     */
    @NotNull
    Locale getEffectiveLocale(@NotNull UUID playerId);

    /**
     * Sets a player's preferred locale.
     *
     * @param playerId the player's UUID
     * @param locale   the preferred locale, or null to clear
     * @since 1.0.0
     */
    void setPlayerLocale(@NotNull UUID playerId, @Nullable Locale locale);

    /**
     * Clears a player's locale preference.
     *
     * <p>After clearing, the player will use their client locale or
     * the server default.
     *
     * @param playerId the player's UUID
     * @since 1.0.0
     */
    void clearPlayerLocale(@NotNull UUID playerId);

    /**
     * Loads player locale preferences from storage.
     *
     * @param playerId the player's UUID
     * @return a future completing when loading is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> loadPlayerLocale(@NotNull UUID playerId);

    /**
     * Saves a player's locale preference to storage.
     *
     * @param playerId the player's UUID
     * @return a future completing when saving is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> savePlayerLocale(@NotNull UUID playerId);

    // ===== Hot Reload =====

    /**
     * Reloads all message sources.
     *
     * <p>This clears all cached translations and reloads from sources.
     *
     * @return the number of messages reloaded
     * @since 1.0.0
     */
    int reload();

    /**
     * Reloads messages for a specific locale.
     *
     * @param locale the locale to reload
     * @return true if the reload succeeded
     * @since 1.0.0
     */
    boolean reload(@NotNull Locale locale);

    /**
     * Reloads messages asynchronously.
     *
     * @return a future completing with the number of messages reloaded
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> reloadAsync();

    /**
     * Enables or disables hot reload watching.
     *
     * <p>When enabled, file changes are automatically detected and
     * translations are reloaded.
     *
     * @param enabled true to enable, false to disable
     * @since 1.0.0
     */
    void setHotReloadEnabled(boolean enabled);

    /**
     * Checks if hot reload is enabled.
     *
     * @return true if hot reload is enabled
     * @since 1.0.0
     */
    boolean isHotReloadEnabled();

    /**
     * Registers a callback for when translations are reloaded.
     *
     * @param callback the callback to invoke on reload
     * @since 1.0.0
     */
    void onReload(@NotNull Consumer<Set<Locale>> callback);

    // ===== Builder =====

    /**
     * Creates a new I18nService builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new I18nServiceBuilder();
    }

    /**
     * Builder for creating I18nService instances.
     *
     * @since 1.0.0
     */
    interface Builder {

        /**
         * Sets the default locale.
         *
         * @param locale the default locale
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder defaultLocale(@NotNull Locale locale);

        /**
         * Sets the fallback locale (usually English).
         *
         * @param locale the fallback locale
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder fallbackLocale(@NotNull Locale locale);

        /**
         * Adds a message source for a locale.
         *
         * @param locale the locale
         * @param source the message source
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder addSource(@NotNull Locale locale, @NotNull MessageSource source);

        /**
         * Sets the directory to load locale files from.
         *
         * @param directory the language files directory
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder directory(@NotNull Path directory);

        /**
         * Enables hot reload watching.
         *
         * @param enabled true to enable hot reload
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder hotReload(boolean enabled);

        /**
         * Enables MiniMessage parsing.
         *
         * @param enabled true to enable MiniMessage
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder miniMessage(boolean enabled);

        /**
         * Enables legacy color code support.
         *
         * @param enabled true to enable legacy codes
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder legacyColors(boolean enabled);

        /**
         * Sets a custom missing key handler.
         *
         * @param handler the missing key handler
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        Builder missingKeyHandler(@NotNull MissingKeyHandler handler);

        /**
         * Builds the I18nService instance.
         *
         * @return the configured I18nService
         * @since 1.0.0
         */
        @NotNull
        I18nService build();
    }

    /**
     * Handler for missing translation keys.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    interface MissingKeyHandler {

        /**
         * Called when a translation key is not found.
         *
         * @param locale the requested locale
         * @param key    the missing key
         * @return the fallback string to use
         * @since 1.0.0
         */
        @NotNull
        String handle(@NotNull Locale locale, @NotNull MessageKey key);
    }
}
