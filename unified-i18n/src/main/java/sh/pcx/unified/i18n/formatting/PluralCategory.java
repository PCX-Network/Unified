/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Plural categories based on the CLDR (Unicode Common Locale Data Repository).
 *
 * <p>Different languages use different subsets of these categories:
 * <ul>
 *   <li>English uses: one, other</li>
 *   <li>French uses: one, other (0 is singular)</li>
 *   <li>Russian uses: one, few, many, other</li>
 *   <li>Arabic uses: zero, one, two, few, many, other</li>
 *   <li>Chinese, Japanese: only other</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PluralRules
 */
public enum PluralCategory {

    /**
     * Used for quantities of zero in some languages.
     */
    ZERO("zero"),

    /**
     * Singular form (typically n=1).
     */
    ONE("one"),

    /**
     * Dual form (n=2), used in Arabic, Slovenian, etc.
     */
    TWO("two"),

    /**
     * Paucal form for small numbers (e.g., 2-4 in Slavic languages).
     */
    FEW("few"),

    /**
     * Used for larger quantities in some languages.
     */
    MANY("many"),

    /**
     * General plural form (fallback).
     */
    OTHER("other");

    private final String key;

    PluralCategory(String key) {
        this.key = key;
    }

    /**
     * Returns the key used in message files.
     *
     * @return the key (e.g., "one", "other")
     * @since 1.0.0
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Parses a category from a key string.
     *
     * @param key the key string
     * @return the category, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public static PluralCategory fromKey(@NotNull String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (PluralCategory category : values()) {
            if (category.key.equals(normalized)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Checks if the given key is a valid plural category key.
     *
     * @param key the key to check
     * @return true if it's a valid plural key
     * @since 1.0.0
     */
    public static boolean isValidKey(@NotNull String key) {
        return fromKey(key) != null;
    }
}
