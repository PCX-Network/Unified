/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.gui.pagination;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A text-based search filter for paginated GUIs.
 *
 * <p>SearchFilter provides flexible text search capabilities including
 * case-insensitive matching, multi-field search, fuzzy matching, and
 * pattern-based searches. It is designed for use with search boxes
 * in inventory GUIs.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple single-field search
 * SearchFilter<Player> nameSearch = SearchFilter.of(Player::getName);
 * nameSearch.setQuery("Notch");
 * boolean matches = nameSearch.test(player);
 *
 * // Multi-field search
 * SearchFilter<ShopItem> itemSearch = SearchFilter.<ShopItem>builder()
 *     .searchField(ShopItem::getName)
 *     .searchField(ShopItem::getDescription)
 *     .searchField(item -> item.getCategory().toString())
 *     .caseSensitive(false)
 *     .build();
 * itemSearch.setQuery("diamond sword");
 *
 * // Fuzzy matching
 * SearchFilter<Quest> questSearch = SearchFilter.<Quest>builder()
 *     .searchField(Quest::getName)
 *     .fuzzyMatch(true)
 *     .minSimilarity(0.6)
 *     .build();
 *
 * // With custom word matching
 * SearchFilter<Item> tagSearch = SearchFilter.<Item>builder()
 *     .searchField(Item::getName)
 *     .matchAllWords(true) // All words must match
 *     .build();
 * tagSearch.setQuery("rare epic legendary");
 *
 * // Prefix matching
 * SearchFilter<String> prefixSearch = SearchFilter.prefix(Function.identity());
 * prefixSearch.setQuery("dia"); // Matches "diamond", "diagonal", etc.
 *
 * // Using with paginated GUI
 * paginatedGui.setFilter(itemSearch);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>SearchFilter is thread-safe for the {@link #test} method once configured.
 * The {@link #setQuery} method should not be called concurrently.
 *
 * @param <T> the type of items to search
 * @since 1.0.0
 * @author Supatuck
 * @see Filter
 * @see FilterBuilder
 * @see PaginatedGUI
 */
public final class SearchFilter<T> implements Filter<T> {

    private final List<Function<T, String>> searchFields;
    private final boolean caseSensitive;
    private final boolean matchAllWords;
    private final boolean fuzzyMatch;
    private final double minSimilarity;
    private final SearchMode searchMode;
    private final String name;

    private volatile String query;
    private volatile String[] queryWords;
    private volatile Pattern queryPattern;

    /**
     * Private constructor used by Builder.
     */
    private SearchFilter(@NotNull Builder<T> builder) {
        this.searchFields = new ArrayList<>(builder.searchFields);
        this.caseSensitive = builder.caseSensitive;
        this.matchAllWords = builder.matchAllWords;
        this.fuzzyMatch = builder.fuzzyMatch;
        this.minSimilarity = builder.minSimilarity;
        this.searchMode = builder.searchMode;
        this.name = builder.name;
        this.query = "";
        this.queryWords = new String[0];
    }

    /**
     * Creates a simple search filter for a single field.
     *
     * @param <T>         the item type
     * @param searchField the field to search
     * @return a SearchFilter for the field
     * @since 1.0.0
     */
    @NotNull
    public static <T> SearchFilter<T> of(@NotNull Function<T, String> searchField) {
        return SearchFilter.<T>builder()
                .searchField(searchField)
                .build();
    }

    /**
     * Creates a search filter for multiple fields.
     *
     * @param <T>          the item type
     * @param searchFields the fields to search
     * @return a SearchFilter for the fields
     * @since 1.0.0
     */
    @SafeVarargs
    @NotNull
    public static <T> SearchFilter<T> of(@NotNull Function<T, String>... searchFields) {
        Builder<T> builder = builder();
        for (Function<T, String> field : searchFields) {
            builder.searchField(field);
        }
        return builder.build();
    }

    /**
     * Creates a prefix-matching search filter.
     *
     * @param <T>         the item type
     * @param searchField the field to search
     * @return a prefix-matching SearchFilter
     * @since 1.0.0
     */
    @NotNull
    public static <T> SearchFilter<T> prefix(@NotNull Function<T, String> searchField) {
        return SearchFilter.<T>builder()
                .searchField(searchField)
                .searchMode(SearchMode.PREFIX)
                .build();
    }

    /**
     * Creates a new SearchFilter builder.
     *
     * @param <T> the item type
     * @return a new Builder instance
     * @since 1.0.0
     */
    @NotNull
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    // ==================== Query Management ====================

    /**
     * Sets the search query.
     *
     * @param query the search query, or null/empty to match all items
     * @since 1.0.0
     */
    public void setQuery(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            this.query = "";
            this.queryWords = new String[0];
            this.queryPattern = null;
            return;
        }

        String trimmed = query.trim();
        this.query = caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
        this.queryWords = this.query.split("\\s+");

        if (searchMode == SearchMode.REGEX) {
            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                this.queryPattern = Pattern.compile(trimmed, flags);
            } catch (Exception e) {
                // Invalid regex, fall back to literal matching
                this.queryPattern = null;
            }
        }
    }

    /**
     * Returns the current search query.
     *
     * @return the current query
     * @since 1.0.0
     */
    @NotNull
    public String getQuery() {
        return query;
    }

    /**
     * Checks if a query is currently set.
     *
     * @return true if a non-empty query is set
     * @since 1.0.0
     */
    public boolean hasQuery() {
        return !query.isEmpty();
    }

    /**
     * Clears the current search query.
     *
     * @since 1.0.0
     */
    public void clearQuery() {
        setQuery(null);
    }

    // ==================== Filter Implementation ====================

    @Override
    public boolean test(@NotNull T item) {
        if (!hasQuery()) {
            return true; // No query matches everything
        }

        for (Function<T, String> field : searchFields) {
            String value = field.apply(item);
            if (value != null && matches(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests if a value matches the current query.
     */
    private boolean matches(@NotNull String value) {
        String searchValue = caseSensitive ? value : value.toLowerCase(Locale.ROOT);

        switch (searchMode) {
            case CONTAINS:
                return matchContains(searchValue);
            case PREFIX:
                return matchPrefix(searchValue);
            case EXACT:
                return matchExact(searchValue);
            case REGEX:
                return matchRegex(value);
            case WORD:
                return matchWords(searchValue);
            default:
                return matchContains(searchValue);
        }
    }

    private boolean matchContains(@NotNull String value) {
        if (matchAllWords) {
            for (String word : queryWords) {
                if (!value.contains(word)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String word : queryWords) {
                if (value.contains(word)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean matchPrefix(@NotNull String value) {
        if (matchAllWords) {
            String[] valueWords = value.split("\\s+");
            for (String queryWord : queryWords) {
                boolean found = false;
                for (String valueWord : valueWords) {
                    if (valueWord.startsWith(queryWord)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        } else {
            return value.startsWith(query);
        }
    }

    private boolean matchExact(@NotNull String value) {
        return value.equals(query);
    }

    private boolean matchRegex(@NotNull String value) {
        if (queryPattern == null) {
            return false;
        }
        return queryPattern.matcher(value).find();
    }

    private boolean matchWords(@NotNull String value) {
        String[] valueWords = value.split("\\s+");

        if (matchAllWords) {
            for (String queryWord : queryWords) {
                boolean found = false;
                for (String valueWord : valueWords) {
                    if (fuzzyMatch) {
                        if (calculateSimilarity(queryWord, valueWord) >= minSimilarity) {
                            found = true;
                            break;
                        }
                    } else if (valueWord.equals(queryWord)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        } else {
            for (String queryWord : queryWords) {
                for (String valueWord : valueWords) {
                    if (fuzzyMatch) {
                        if (calculateSimilarity(queryWord, valueWord) >= minSimilarity) {
                            return true;
                        }
                    } else if (valueWord.equals(queryWord)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Calculates similarity between two strings using Levenshtein distance.
     */
    private double calculateSimilarity(@NotNull String s1, @NotNull String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    private int levenshteinDistance(@NotNull String s1, @NotNull String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];

        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[len2];
    }

    @Override
    @Nullable
    public String getDisplayName() {
        return name;
    }

    @Override
    @Nullable
    public String getDescription() {
        if (hasQuery()) {
            return "Searching for: " + query;
        }
        return "Enter text to search";
    }

    // ==================== Search Mode ====================

    /**
     * Enum defining different search matching modes.
     *
     * @since 1.0.0
     */
    public enum SearchMode {
        /**
         * Match if the query is contained anywhere in the value.
         */
        CONTAINS,

        /**
         * Match if the value starts with the query.
         */
        PREFIX,

        /**
         * Match only if the value exactly equals the query.
         */
        EXACT,

        /**
         * Match using the query as a regular expression.
         */
        REGEX,

        /**
         * Match on word boundaries.
         */
        WORD
    }

    // ==================== Builder ====================

    /**
     * Builder for creating {@link SearchFilter} instances.
     *
     * @param <T> the item type
     * @since 1.0.0
     */
    public static final class Builder<T> {

        private final List<Function<T, String>> searchFields;
        private boolean caseSensitive;
        private boolean matchAllWords;
        private boolean fuzzyMatch;
        private double minSimilarity;
        private SearchMode searchMode;
        private String name;

        /**
         * Creates a new Builder with default values.
         */
        private Builder() {
            this.searchFields = new ArrayList<>();
            this.caseSensitive = false;
            this.matchAllWords = false;
            this.fuzzyMatch = false;
            this.minSimilarity = 0.7;
            this.searchMode = SearchMode.CONTAINS;
            this.name = "Search";
        }

        /**
         * Adds a field to search.
         *
         * @param field the field getter function
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> searchField(@NotNull Function<T, String> field) {
            Objects.requireNonNull(field, "field cannot be null");
            searchFields.add(field);
            return this;
        }

        /**
         * Adds multiple fields to search.
         *
         * @param fields the field getter functions
         * @return this builder
         * @since 1.0.0
         */
        @SafeVarargs
        @NotNull
        public final Builder<T> searchFields(@NotNull Function<T, String>... fields) {
            for (Function<T, String> field : fields) {
                searchField(field);
            }
            return this;
        }

        /**
         * Sets whether search is case-sensitive.
         *
         * @param caseSensitive true for case-sensitive search
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        /**
         * Sets whether all query words must match.
         *
         * <p>When true, "diamond sword" requires both "diamond" AND "sword"
         * to be present. When false, either word matching is sufficient.
         *
         * @param matchAll true to require all words
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> matchAllWords(boolean matchAll) {
            this.matchAllWords = matchAll;
            return this;
        }

        /**
         * Enables fuzzy matching for typo tolerance.
         *
         * @param fuzzy true to enable fuzzy matching
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> fuzzyMatch(boolean fuzzy) {
            this.fuzzyMatch = fuzzy;
            if (fuzzy && searchMode == SearchMode.CONTAINS) {
                this.searchMode = SearchMode.WORD;
            }
            return this;
        }

        /**
         * Sets the minimum similarity for fuzzy matching.
         *
         * @param similarity the minimum similarity (0.0 to 1.0)
         * @return this builder
         * @throws IllegalArgumentException if similarity is not in range
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> minSimilarity(double similarity) {
            if (similarity < 0.0 || similarity > 1.0) {
                throw new IllegalArgumentException(
                        "Similarity must be between 0.0 and 1.0, was: " + similarity);
            }
            this.minSimilarity = similarity;
            return this;
        }

        /**
         * Sets the search matching mode.
         *
         * @param mode the search mode
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> searchMode(@NotNull SearchMode mode) {
            this.searchMode = Objects.requireNonNull(mode);
            return this;
        }

        /**
         * Sets the display name for this filter.
         *
         * @param name the display name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder<T> name(@NotNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Builds the SearchFilter.
         *
         * @return the constructed SearchFilter
         * @throws IllegalStateException if no search fields are configured
         * @since 1.0.0
         */
        @NotNull
        public SearchFilter<T> build() {
            if (searchFields.isEmpty()) {
                throw new IllegalStateException("At least one search field is required");
            }
            return new SearchFilter<>(this);
        }
    }
}
