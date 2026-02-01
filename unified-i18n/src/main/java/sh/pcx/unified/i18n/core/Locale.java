/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents a locale with language code and optional country code.
 *
 * <p>Locales follow the standard language tag format (e.g., "en_US", "de_DE", "ja_JP").
 * The language code is a two-letter ISO 639-1 code, and the country code is an
 * optional two-letter ISO 3166-1 alpha-2 code.
 *
 * <h2>Common Locales</h2>
 * <p>Pre-defined constants are provided for commonly used locales:
 * <pre>{@code
 * Locale english = Locale.US_ENGLISH;
 * Locale german = Locale.GERMAN;
 * Locale japanese = Locale.JAPANESE;
 * }</pre>
 *
 * <h2>Creating Locales</h2>
 * <pre>{@code
 * // From language and country codes
 * Locale locale = Locale.of("en", "US");
 *
 * // From a locale string
 * Locale locale = Locale.parse("en_US");
 *
 * // From a Java Locale
 * Locale locale = Locale.from(java.util.Locale.GERMANY);
 * }</pre>
 *
 * <h2>Minecraft Client Locales</h2>
 * <p>Minecraft clients send locale settings in a slightly different format
 * (e.g., "en_us" lowercase). Use {@link #fromMinecraft(String)} for parsing:
 * <pre>{@code
 * Locale clientLocale = Locale.fromMinecraft("en_us");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class Locale implements Comparable<Locale> {

    /**
     * Pattern for validating locale strings.
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}(_[A-Z]{2})?$", Pattern.CASE_INSENSITIVE);

    // ===== Common Locales =====

    /**
     * US English locale (en_US).
     */
    public static final Locale US_ENGLISH = new Locale("en", "US");

    /**
     * UK English locale (en_GB).
     */
    public static final Locale UK_ENGLISH = new Locale("en", "GB");

    /**
     * German locale (de_DE).
     */
    public static final Locale GERMAN = new Locale("de", "DE");

    /**
     * French locale (fr_FR).
     */
    public static final Locale FRENCH = new Locale("fr", "FR");

    /**
     * Spanish locale (es_ES).
     */
    public static final Locale SPANISH = new Locale("es", "ES");

    /**
     * Italian locale (it_IT).
     */
    public static final Locale ITALIAN = new Locale("it", "IT");

    /**
     * Portuguese (Brazil) locale (pt_BR).
     */
    public static final Locale PORTUGUESE_BR = new Locale("pt", "BR");

    /**
     * Russian locale (ru_RU).
     */
    public static final Locale RUSSIAN = new Locale("ru", "RU");

    /**
     * Japanese locale (ja_JP).
     */
    public static final Locale JAPANESE = new Locale("ja", "JP");

    /**
     * Korean locale (ko_KR).
     */
    public static final Locale KOREAN = new Locale("ko", "KR");

    /**
     * Simplified Chinese locale (zh_CN).
     */
    public static final Locale CHINESE_SIMPLIFIED = new Locale("zh", "CN");

    /**
     * Traditional Chinese locale (zh_TW).
     */
    public static final Locale CHINESE_TRADITIONAL = new Locale("zh", "TW");

    /**
     * Polish locale (pl_PL).
     */
    public static final Locale POLISH = new Locale("pl", "PL");

    /**
     * Dutch locale (nl_NL).
     */
    public static final Locale DUTCH = new Locale("nl", "NL");

    /**
     * Swedish locale (sv_SE).
     */
    public static final Locale SWEDISH = new Locale("sv", "SE");

    private final String language;
    private final String country;
    private final String code;
    private final int hashCode;

    /**
     * Creates a new Locale with the specified language and country.
     *
     * @param language the two-letter language code (ISO 639-1)
     * @param country  the two-letter country code (ISO 3166-1 alpha-2), or null
     */
    private Locale(@NotNull String language, @Nullable String country) {
        this.language = language.toLowerCase();
        this.country = country != null ? country.toUpperCase() : null;
        this.code = this.country != null ? this.language + "_" + this.country : this.language;
        this.hashCode = this.code.hashCode();
    }

    /**
     * Creates a new Locale with the specified language and country codes.
     *
     * @param language the two-letter language code (ISO 639-1)
     * @param country  the two-letter country code (ISO 3166-1 alpha-2)
     * @return the locale instance
     * @throws IllegalArgumentException if the codes are invalid
     * @since 1.0.0
     */
    @NotNull
    public static Locale of(@NotNull String language, @NotNull String country) {
        Objects.requireNonNull(language, "language cannot be null");
        Objects.requireNonNull(country, "country cannot be null");
        validateLanguage(language);
        validateCountry(country);
        return new Locale(language, country);
    }

    /**
     * Creates a new Locale with only a language code.
     *
     * @param language the two-letter language code (ISO 639-1)
     * @return the locale instance
     * @throws IllegalArgumentException if the code is invalid
     * @since 1.0.0
     */
    @NotNull
    public static Locale ofLanguage(@NotNull String language) {
        Objects.requireNonNull(language, "language cannot be null");
        validateLanguage(language);
        return new Locale(language, null);
    }

    /**
     * Parses a locale string in the format "xx" or "xx_YY".
     *
     * @param localeString the locale string to parse
     * @return the parsed locale
     * @throws IllegalArgumentException if the format is invalid
     * @since 1.0.0
     */
    @NotNull
    public static Locale parse(@NotNull String localeString) {
        Objects.requireNonNull(localeString, "localeString cannot be null");

        String normalized = localeString.replace("-", "_");

        if (!LOCALE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid locale format: " + localeString);
        }

        String[] parts = normalized.split("_");
        if (parts.length == 1) {
            return new Locale(parts[0], null);
        } else {
            return new Locale(parts[0], parts[1]);
        }
    }

    /**
     * Attempts to parse a locale string, returning empty if invalid.
     *
     * @param localeString the locale string to parse
     * @return the parsed locale, or empty if invalid
     * @since 1.0.0
     */
    @NotNull
    public static Optional<Locale> tryParse(@Nullable String localeString) {
        if (localeString == null || localeString.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(localeString));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a Locale from a Minecraft client locale string.
     *
     * <p>Minecraft sends locales in lowercase format (e.g., "en_us"),
     * which this method normalizes.
     *
     * @param minecraftLocale the Minecraft locale string
     * @return the parsed locale
     * @throws IllegalArgumentException if the format is invalid
     * @since 1.0.0
     */
    @NotNull
    public static Locale fromMinecraft(@NotNull String minecraftLocale) {
        Objects.requireNonNull(minecraftLocale, "minecraftLocale cannot be null");
        return parse(minecraftLocale);
    }

    /**
     * Attempts to create a Locale from a Minecraft client locale string.
     *
     * @param minecraftLocale the Minecraft locale string
     * @return the parsed locale, or empty if invalid
     * @since 1.0.0
     */
    @NotNull
    public static Optional<Locale> tryFromMinecraft(@Nullable String minecraftLocale) {
        return tryParse(minecraftLocale);
    }

    /**
     * Creates a Locale from a Java Locale.
     *
     * @param javaLocale the Java Locale
     * @return the corresponding Locale
     * @since 1.0.0
     */
    @NotNull
    public static Locale from(@NotNull java.util.Locale javaLocale) {
        Objects.requireNonNull(javaLocale, "javaLocale cannot be null");
        String language = javaLocale.getLanguage();
        String country = javaLocale.getCountry();
        if (country == null || country.isEmpty()) {
            return new Locale(language, null);
        }
        return new Locale(language, country);
    }

    /**
     * Returns the language code.
     *
     * @return the two-letter language code (lowercase)
     * @since 1.0.0
     */
    @NotNull
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the country code if present.
     *
     * @return the two-letter country code (uppercase), or empty if not specified
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getCountry() {
        return Optional.ofNullable(country);
    }

    /**
     * Returns the full locale code.
     *
     * <p>Returns "xx" for language-only locales, or "xx_YY" for
     * language-country locales.
     *
     * @return the locale code
     * @since 1.0.0
     */
    @NotNull
    public String getCode() {
        return code;
    }

    /**
     * Returns whether this locale has a country code.
     *
     * @return true if a country code is specified
     * @since 1.0.0
     */
    public boolean hasCountry() {
        return country != null;
    }

    /**
     * Returns a language-only version of this locale.
     *
     * <p>If this locale is already language-only, returns this.
     *
     * @return a locale with only the language component
     * @since 1.0.0
     */
    @NotNull
    public Locale withoutCountry() {
        if (!hasCountry()) {
            return this;
        }
        return new Locale(language, null);
    }

    /**
     * Returns this locale with a different country.
     *
     * @param country the new country code
     * @return a new locale with the specified country
     * @since 1.0.0
     */
    @NotNull
    public Locale withCountry(@NotNull String country) {
        validateCountry(country);
        return new Locale(language, country);
    }

    /**
     * Converts this locale to a Java Locale.
     *
     * @return the corresponding Java Locale
     * @since 1.0.0
     */
    @NotNull
    public java.util.Locale toJavaLocale() {
        if (country != null) {
            return new java.util.Locale(language, country);
        }
        return new java.util.Locale(language);
    }

    /**
     * Returns the locale code in Minecraft format (lowercase).
     *
     * @return the lowercase locale code (e.g., "en_us")
     * @since 1.0.0
     */
    @NotNull
    public String toMinecraftFormat() {
        return code.toLowerCase();
    }

    /**
     * Checks if this locale matches or is a parent of another.
     *
     * <p>For example, "en" matches "en_US" and "en_GB".
     *
     * @param other the locale to check against
     * @return true if this locale matches or is a parent
     * @since 1.0.0
     */
    public boolean matches(@NotNull Locale other) {
        if (this.equals(other)) {
            return true;
        }
        // Language-only locale matches any same-language locale
        return !this.hasCountry() && this.language.equals(other.language);
    }

    /**
     * Checks if this locale is related to another (same language family).
     *
     * @param other the locale to compare
     * @return true if both locales share the same language
     * @since 1.0.0
     */
    public boolean isRelatedTo(@NotNull Locale other) {
        return this.language.equals(other.language);
    }

    @Override
    public int compareTo(@NotNull Locale other) {
        return this.code.compareTo(other.code);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Locale other)) {
            return false;
        }
        return this.code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return code;
    }

    private static void validateLanguage(String language) {
        if (language.length() != 2 || !language.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("Invalid language code: " + language);
        }
    }

    private static void validateCountry(String country) {
        if (country.length() != 2 || !country.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("Invalid country code: " + country);
        }
    }
}
