/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.formatting;

import sh.pcx.unified.i18n.core.Locale;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongPredicate;

/**
 * Handles pluralization rules for different languages.
 *
 * <p>Different languages have different rules for plural forms. This class
 * provides language-specific rules for selecting the correct plural form
 * based on a count value.
 *
 * <h2>Supported Categories</h2>
 * <p>Following the CLDR (Common Locale Data Repository) standard:
 * <ul>
 *   <li><b>zero</b> - Zero quantity (used in some languages)</li>
 *   <li><b>one</b> - Singular form</li>
 *   <li><b>two</b> - Dual form (used in some languages)</li>
 *   <li><b>few</b> - Paucal form (small plural, e.g., 2-4 in Slavic)</li>
 *   <li><b>many</b> - Large plural form</li>
 *   <li><b>other</b> - General plural (fallback)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PluralRules rules = PluralRules.forLocale(Locale.RUSSIAN);
 * PluralCategory category = rules.select(5);
 * // Returns MANY for Russian (5 = "other" in English but "many" in Russian)
 *
 * // In message files:
 * // items:
 * //   one: "You have {count} item"
 * //   other: "You have {count} items"
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PluralCategory
 */
public final class PluralRules {

    private static final Map<String, PluralRules> RULES_BY_LANGUAGE = new ConcurrentHashMap<>();

    // Common rule implementations
    private static final PluralRules ENGLISH_RULES;
    private static final PluralRules SLAVIC_RULES;
    private static final PluralRules EAST_SLAVIC_RULES;
    private static final PluralRules NO_PLURAL_RULES;
    private static final PluralRules FRENCH_RULES;
    private static final PluralRules ARABIC_RULES;

    static {
        // English-like: one, other
        ENGLISH_RULES = new PluralRules(
                n -> n == 1,   // one
                n -> false,    // two
                n -> false,    // few
                n -> false     // many
        );

        // French-like: 0 and 1 are singular
        FRENCH_RULES = new PluralRules(
                n -> n == 0 || n == 1,
                n -> false,
                n -> false,
                n -> false
        );

        // Slavic languages (Polish, Czech, Slovak)
        SLAVIC_RULES = new PluralRules(
                n -> n == 1,
                n -> false,
                n -> n >= 2 && n <= 4,
                n -> false
        );

        // East Slavic (Russian, Ukrainian, Belarusian)
        EAST_SLAVIC_RULES = new PluralRules(
                n -> n % 10 == 1 && n % 100 != 11,
                n -> false,
                n -> n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20),
                n -> true  // many is the fallback for other cases
        );

        // No plural distinctions (Chinese, Japanese, Korean, Vietnamese)
        NO_PLURAL_RULES = new PluralRules(
                n -> false,
                n -> false,
                n -> false,
                n -> false
        );

        // Arabic (complex rules)
        ARABIC_RULES = new PluralRules(
                n -> n == 1,
                n -> n == 2,
                n -> n % 100 >= 3 && n % 100 <= 10,
                n -> n % 100 >= 11 && n % 100 <= 99
        );

        // Register language mappings
        // English and Germanic
        RULES_BY_LANGUAGE.put("en", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("de", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("nl", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("sv", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("da", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("no", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("it", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("es", ENGLISH_RULES);
        RULES_BY_LANGUAGE.put("pt", ENGLISH_RULES);

        // French-like
        RULES_BY_LANGUAGE.put("fr", FRENCH_RULES);

        // Slavic
        RULES_BY_LANGUAGE.put("pl", SLAVIC_RULES);
        RULES_BY_LANGUAGE.put("cs", SLAVIC_RULES);
        RULES_BY_LANGUAGE.put("sk", SLAVIC_RULES);

        // East Slavic
        RULES_BY_LANGUAGE.put("ru", EAST_SLAVIC_RULES);
        RULES_BY_LANGUAGE.put("uk", EAST_SLAVIC_RULES);
        RULES_BY_LANGUAGE.put("be", EAST_SLAVIC_RULES);

        // No plurals
        RULES_BY_LANGUAGE.put("zh", NO_PLURAL_RULES);
        RULES_BY_LANGUAGE.put("ja", NO_PLURAL_RULES);
        RULES_BY_LANGUAGE.put("ko", NO_PLURAL_RULES);
        RULES_BY_LANGUAGE.put("vi", NO_PLURAL_RULES);
        RULES_BY_LANGUAGE.put("th", NO_PLURAL_RULES);

        // Arabic
        RULES_BY_LANGUAGE.put("ar", ARABIC_RULES);
    }

    private final LongPredicate isOne;
    private final LongPredicate isTwo;
    private final LongPredicate isFew;
    private final LongPredicate isMany;

    /**
     * Creates plural rules with custom predicates.
     *
     * @param isOne  predicate for "one" category
     * @param isTwo  predicate for "two" category
     * @param isFew  predicate for "few" category
     * @param isMany predicate for "many" category
     */
    private PluralRules(LongPredicate isOne, LongPredicate isTwo,
                        LongPredicate isFew, LongPredicate isMany) {
        this.isOne = isOne;
        this.isTwo = isTwo;
        this.isFew = isFew;
        this.isMany = isMany;
    }

    /**
     * Returns the plural rules for the given locale.
     *
     * @param locale the locale
     * @return the plural rules for the locale's language
     * @since 1.0.0
     */
    @NotNull
    public static PluralRules forLocale(@NotNull Locale locale) {
        Objects.requireNonNull(locale, "locale cannot be null");
        return forLanguage(locale.getLanguage());
    }

    /**
     * Returns the plural rules for the given language code.
     *
     * @param language the two-letter language code
     * @return the plural rules for the language
     * @since 1.0.0
     */
    @NotNull
    public static PluralRules forLanguage(@NotNull String language) {
        Objects.requireNonNull(language, "language cannot be null");
        return RULES_BY_LANGUAGE.getOrDefault(language.toLowerCase(), ENGLISH_RULES);
    }

    /**
     * Registers custom plural rules for a language.
     *
     * @param language the language code
     * @param rules    the plural rules
     * @since 1.0.0
     */
    public static void register(@NotNull String language, @NotNull PluralRules rules) {
        Objects.requireNonNull(language, "language cannot be null");
        Objects.requireNonNull(rules, "rules cannot be null");
        RULES_BY_LANGUAGE.put(language.toLowerCase(), rules);
    }

    /**
     * Creates a builder for custom plural rules.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Selects the plural category for the given count.
     *
     * @param count the quantity
     * @return the appropriate plural category
     * @since 1.0.0
     */
    @NotNull
    public PluralCategory select(long count) {
        long n = Math.abs(count);

        if (n == 0) {
            return PluralCategory.ZERO;
        }
        if (isOne.test(n)) {
            return PluralCategory.ONE;
        }
        if (isTwo.test(n)) {
            return PluralCategory.TWO;
        }
        if (isFew.test(n)) {
            return PluralCategory.FEW;
        }
        if (isMany.test(n)) {
            return PluralCategory.MANY;
        }
        return PluralCategory.OTHER;
    }

    /**
     * Returns the message key suffix for the given count.
     *
     * <p>For example, for count=1 in English, returns "one".
     * This suffix is appended to the base message key.
     *
     * @param count the quantity
     * @return the key suffix (e.g., "one", "other")
     * @since 1.0.0
     */
    @NotNull
    public String getKeySuffix(long count) {
        return select(count).getKey();
    }

    /**
     * Checks if the given category is applicable for this language.
     *
     * @param category the category to check
     * @return true if the category is used in this language
     * @since 1.0.0
     */
    public boolean hasCategory(@NotNull PluralCategory category) {
        return switch (category) {
            case ZERO -> true; // Always check zero
            case ONE -> true;  // Most languages have singular
            case TWO -> RULES_BY_LANGUAGE.values().stream().anyMatch(r -> r == ARABIC_RULES);
            case FEW -> this == SLAVIC_RULES || this == EAST_SLAVIC_RULES || this == ARABIC_RULES;
            case MANY -> this == EAST_SLAVIC_RULES || this == ARABIC_RULES;
            case OTHER -> true; // Always have other as fallback
        };
    }

    /**
     * Builder for creating custom plural rules.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private LongPredicate isOne = n -> n == 1;
        private LongPredicate isTwo = n -> false;
        private LongPredicate isFew = n -> false;
        private LongPredicate isMany = n -> false;

        private Builder() {}

        /**
         * Sets the predicate for the "one" category.
         *
         * @param predicate the predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder one(@NotNull LongPredicate predicate) {
            this.isOne = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Sets the predicate for the "two" category.
         *
         * @param predicate the predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder two(@NotNull LongPredicate predicate) {
            this.isTwo = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Sets the predicate for the "few" category.
         *
         * @param predicate the predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder few(@NotNull LongPredicate predicate) {
            this.isFew = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Sets the predicate for the "many" category.
         *
         * @param predicate the predicate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder many(@NotNull LongPredicate predicate) {
            this.isMany = Objects.requireNonNull(predicate);
            return this;
        }

        /**
         * Builds the plural rules.
         *
         * @return the configured PluralRules
         * @since 1.0.0
         */
        @NotNull
        public PluralRules build() {
            return new PluralRules(isOne, isTwo, isFew, isMany);
        }
    }
}
