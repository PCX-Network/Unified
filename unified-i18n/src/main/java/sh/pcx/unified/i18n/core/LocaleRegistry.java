/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing available locales in the i18n system.
 *
 * <p>The LocaleRegistry maintains a collection of supported locales along with
 * their metadata such as display names and enabled status. It supports automatic
 * locale matching and provides lookup capabilities for finding suitable locales.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * LocaleRegistry registry = LocaleRegistry.create();
 *
 * // Register available locales
 * registry.register(Locale.US_ENGLISH, "English (US)");
 * registry.register(Locale.GERMAN, "Deutsch");
 * registry.register(Locale.JAPANESE, "日本語");
 *
 * // Find best match for a client locale
 * Locale match = registry.findBestMatch(Locale.parse("de_AT"))
 *     .orElse(registry.getDefault());
 *
 * // Get display name for menus
 * String displayName = registry.getDisplayName(Locale.GERMAN);
 * }</pre>
 *
 * <h2>Locale Matching</h2>
 * <p>When looking for a matching locale, the registry uses this priority:
 * <ol>
 *   <li>Exact match (e.g., de_AT matches de_AT)</li>
 *   <li>Same language (e.g., de_AT matches de_DE if de_AT not available)</li>
 *   <li>Default locale if no match found</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Locale
 */
public final class LocaleRegistry {

    private final Map<Locale, LocaleEntry> locales;
    private final Map<String, Set<Locale>> byLanguage;
    private volatile Locale defaultLocale;

    /**
     * Creates a new empty LocaleRegistry with US English as default.
     */
    private LocaleRegistry() {
        this.locales = new ConcurrentHashMap<>();
        this.byLanguage = new ConcurrentHashMap<>();
        this.defaultLocale = Locale.US_ENGLISH;
    }

    /**
     * Creates a new empty LocaleRegistry.
     *
     * @return a new registry instance
     * @since 1.0.0
     */
    @NotNull
    public static LocaleRegistry create() {
        return new LocaleRegistry();
    }

    /**
     * Creates a LocaleRegistry with standard Minecraft locales pre-registered.
     *
     * @return a registry with standard locales
     * @since 1.0.0
     */
    @NotNull
    public static LocaleRegistry withStandardLocales() {
        LocaleRegistry registry = new LocaleRegistry();

        // Register common Minecraft locales
        registry.register(Locale.US_ENGLISH, "English (US)");
        registry.register(Locale.UK_ENGLISH, "English (UK)");
        registry.register(Locale.GERMAN, "Deutsch");
        registry.register(Locale.FRENCH, "Français");
        registry.register(Locale.SPANISH, "Español (España)");
        registry.register(Locale.ITALIAN, "Italiano");
        registry.register(Locale.PORTUGUESE_BR, "Português (Brasil)");
        registry.register(Locale.RUSSIAN, "Русский");
        registry.register(Locale.JAPANESE, "日本語");
        registry.register(Locale.KOREAN, "한국어");
        registry.register(Locale.CHINESE_SIMPLIFIED, "简体中文");
        registry.register(Locale.CHINESE_TRADITIONAL, "繁體中文");
        registry.register(Locale.POLISH, "Polski");
        registry.register(Locale.DUTCH, "Nederlands");
        registry.register(Locale.SWEDISH, "Svenska");

        return registry;
    }

    /**
     * Registers a locale with a display name.
     *
     * @param locale      the locale to register
     * @param displayName the human-readable display name
     * @since 1.0.0
     */
    public void register(@NotNull Locale locale, @NotNull String displayName) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");

        locales.put(locale, new LocaleEntry(locale, displayName, true));
        byLanguage.computeIfAbsent(locale.getLanguage(), k -> ConcurrentHashMap.newKeySet())
                .add(locale);
    }

    /**
     * Registers a locale with a display name and enabled status.
     *
     * @param locale      the locale to register
     * @param displayName the human-readable display name
     * @param enabled     whether the locale is enabled for selection
     * @since 1.0.0
     */
    public void register(@NotNull Locale locale, @NotNull String displayName, boolean enabled) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");

        locales.put(locale, new LocaleEntry(locale, displayName, enabled));
        byLanguage.computeIfAbsent(locale.getLanguage(), k -> ConcurrentHashMap.newKeySet())
                .add(locale);
    }

    /**
     * Unregisters a locale from the registry.
     *
     * @param locale the locale to unregister
     * @return true if the locale was registered and removed
     * @since 1.0.0
     */
    public boolean unregister(@NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");

        LocaleEntry removed = locales.remove(locale);
        if (removed != null) {
            Set<Locale> languageSet = byLanguage.get(locale.getLanguage());
            if (languageSet != null) {
                languageSet.remove(locale);
                if (languageSet.isEmpty()) {
                    byLanguage.remove(locale.getLanguage());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if a locale is registered.
     *
     * @param locale the locale to check
     * @return true if the locale is registered
     * @since 1.0.0
     */
    public boolean isRegistered(@NotNull Locale locale) {
        return locales.containsKey(locale);
    }

    /**
     * Checks if a locale is registered and enabled.
     *
     * @param locale the locale to check
     * @return true if the locale is registered and enabled
     * @since 1.0.0
     */
    public boolean isEnabled(@NotNull Locale locale) {
        LocaleEntry entry = locales.get(locale);
        return entry != null && entry.enabled();
    }

    /**
     * Sets the enabled status of a locale.
     *
     * @param locale  the locale to modify
     * @param enabled the new enabled status
     * @since 1.0.0
     */
    public void setEnabled(@NotNull Locale locale, boolean enabled) {
        Objects.requireNonNull(locale, "locale cannot be null");

        LocaleEntry entry = locales.get(locale);
        if (entry != null) {
            locales.put(locale, new LocaleEntry(locale, entry.displayName(), enabled));
        }
    }

    /**
     * Returns the display name for a locale.
     *
     * @param locale the locale
     * @return the display name, or the locale code if not registered
     * @since 1.0.0
     */
    @NotNull
    public String getDisplayName(@NotNull Locale locale) {
        LocaleEntry entry = locales.get(locale);
        return entry != null ? entry.displayName() : locale.getCode();
    }

    /**
     * Sets the display name for a locale.
     *
     * @param locale      the locale
     * @param displayName the new display name
     * @since 1.0.0
     */
    public void setDisplayName(@NotNull Locale locale, @NotNull String displayName) {
        Objects.requireNonNull(locale, "locale cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");

        LocaleEntry entry = locales.get(locale);
        if (entry != null) {
            locales.put(locale, new LocaleEntry(locale, displayName, entry.enabled()));
        }
    }

    /**
     * Returns the default locale.
     *
     * @return the default locale
     * @since 1.0.0
     */
    @NotNull
    public Locale getDefault() {
        return defaultLocale;
    }

    /**
     * Sets the default locale.
     *
     * @param locale the new default locale
     * @throws IllegalArgumentException if the locale is not registered
     * @since 1.0.0
     */
    public void setDefault(@NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");

        if (!isRegistered(locale)) {
            throw new IllegalArgumentException("Locale not registered: " + locale);
        }
        this.defaultLocale = locale;
    }

    /**
     * Returns all registered locales.
     *
     * @return an unmodifiable set of registered locales
     * @since 1.0.0
     */
    @NotNull
    public Set<Locale> getAll() {
        return Collections.unmodifiableSet(locales.keySet());
    }

    /**
     * Returns all enabled locales.
     *
     * @return an unmodifiable set of enabled locales
     * @since 1.0.0
     */
    @NotNull
    public Set<Locale> getEnabled() {
        return locales.entrySet().stream()
                .filter(e -> e.getValue().enabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns all locales for a specific language.
     *
     * @param language the language code (e.g., "en", "de")
     * @return all locales with that language, or empty set if none
     * @since 1.0.0
     */
    @NotNull
    public Set<Locale> getByLanguage(@NotNull String language) {
        Objects.requireNonNull(language, "language cannot be null");

        Set<Locale> result = byLanguage.get(language.toLowerCase());
        return result != null ? Collections.unmodifiableSet(result) : Collections.emptySet();
    }

    /**
     * Finds the best matching locale for the given locale.
     *
     * <p>Matching priority:
     * <ol>
     *   <li>Exact match</li>
     *   <li>Same language, different country</li>
     *   <li>Empty if no match</li>
     * </ol>
     *
     * @param locale the locale to match
     * @return the best matching registered locale, or empty if no match
     * @since 1.0.0
     */
    @NotNull
    public Optional<Locale> findBestMatch(@NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");

        // Try exact match first
        if (isEnabled(locale)) {
            return Optional.of(locale);
        }

        // Try same language
        Set<Locale> sameLanguage = byLanguage.get(locale.getLanguage());
        if (sameLanguage != null && !sameLanguage.isEmpty()) {
            // Prefer enabled locales
            for (Locale candidate : sameLanguage) {
                if (isEnabled(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds the best matching locale, with fallback to default.
     *
     * @param locale the locale to match
     * @return the best match, or the default locale
     * @since 1.0.0
     */
    @NotNull
    public Locale findBestMatchOrDefault(@NotNull Locale locale) {
        return findBestMatch(locale).orElse(defaultLocale);
    }

    /**
     * Finds a locale by its code string.
     *
     * @param code the locale code (e.g., "en_US")
     * @return the locale if registered, or empty
     * @since 1.0.0
     */
    @NotNull
    public Optional<Locale> findByCode(@NotNull String code) {
        return Locale.tryParse(code)
                .filter(this::isRegistered);
    }

    /**
     * Returns the number of registered locales.
     *
     * @return the locale count
     * @since 1.0.0
     */
    public int size() {
        return locales.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no locales are registered
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return locales.isEmpty();
    }

    /**
     * Clears all registered locales.
     *
     * @since 1.0.0
     */
    public void clear() {
        locales.clear();
        byLanguage.clear();
    }

    /**
     * Returns all locale entries with their metadata.
     *
     * @return an unmodifiable collection of locale entries
     * @since 1.0.0
     */
    @NotNull
    public Collection<LocaleEntry> getEntries() {
        return Collections.unmodifiableCollection(locales.values());
    }

    /**
     * Represents a registered locale with its metadata.
     *
     * @param locale      the locale
     * @param displayName the display name
     * @param enabled     whether the locale is enabled
     * @since 1.0.0
     */
    public record LocaleEntry(
            @NotNull Locale locale,
            @NotNull String displayName,
            boolean enabled
    ) {
        /**
         * Creates a new LocaleEntry.
         *
         * @param locale      the locale
         * @param displayName the display name
         * @param enabled     whether the locale is enabled
         */
        public LocaleEntry {
            Objects.requireNonNull(locale, "locale cannot be null");
            Objects.requireNonNull(displayName, "displayName cannot be null");
        }
    }
}
