/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses placeholder syntax from text.
 *
 * <p>The placeholder parser identifies placeholders in text using the standard
 * {@code %placeholder%} syntax and extracts the identifier, expansion, and any
 * arguments.
 *
 * <h2>Supported Syntax</h2>
 * <ul>
 *   <li>{@code %expansion_placeholder%} - Standard placeholder</li>
 *   <li>{@code %expansion_placeholder_arg%} - Placeholder with argument</li>
 *   <li>{@code %rel_expansion_placeholder%} - Relational placeholder</li>
 *   <li>{@code {expansion_placeholder}} - Bracket syntax (alternative)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlaceholderParser parser = PlaceholderParser.standard();
 *
 * // Parse a single placeholder
 * ParsedPlaceholder parsed = parser.parse("%player_name%");
 * // expansion = "player", identifier = "name", args = []
 *
 * // Parse with arguments
 * ParsedPlaceholder withArgs = parser.parse("%player_stat_kills%");
 * // expansion = "player", identifier = "stat_", args = ["kills"]
 *
 * // Find all placeholders in text
 * List<ParsedPlaceholder> all = parser.findAll(
 *     "Hello %player_name%, your balance is %vault_balance%!"
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderResolver
 * @see ParsedPlaceholder
 */
public final class PlaceholderParser {

    /**
     * Standard placeholder pattern: %expansion_identifier%
     */
    private static final Pattern STANDARD_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");

    /**
     * Bracket placeholder pattern: {expansion_identifier}
     */
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    /**
     * Relational placeholder prefix.
     */
    private static final String RELATIONAL_PREFIX = "rel_";

    private final Pattern pattern;
    private final boolean supportsBrackets;
    private final boolean supportsRelational;

    private PlaceholderParser(Builder builder) {
        this.pattern = builder.pattern;
        this.supportsBrackets = builder.supportsBrackets;
        this.supportsRelational = builder.supportsRelational;
    }

    /**
     * Creates a parser with standard placeholder syntax.
     *
     * @return a standard parser
     */
    @NotNull
    public static PlaceholderParser standard() {
        return builder().build();
    }

    /**
     * Creates a parser that also supports bracket syntax.
     *
     * @return a parser with bracket support
     */
    @NotNull
    public static PlaceholderParser withBrackets() {
        return builder().supportsBrackets(true).build();
    }

    /**
     * Creates a new builder for configuring the parser.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses a single placeholder string.
     *
     * @param placeholder the placeholder (with or without delimiters)
     * @return the parsed placeholder
     * @throws IllegalArgumentException if the placeholder is invalid
     */
    @NotNull
    public ParsedPlaceholder parse(@NotNull String placeholder) {
        Objects.requireNonNull(placeholder, "placeholder cannot be null");

        String inner = extractInner(placeholder);
        if (inner == null) {
            throw new IllegalArgumentException("Invalid placeholder syntax: " + placeholder);
        }

        return parseInner(inner, placeholder);
    }

    /**
     * Attempts to parse a placeholder, returning null if invalid.
     *
     * @param placeholder the placeholder string
     * @return the parsed placeholder, or null if invalid
     */
    @NotNull
    public java.util.Optional<ParsedPlaceholder> tryParse(@NotNull String placeholder) {
        try {
            return java.util.Optional.of(parse(placeholder));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Finds all placeholders in the given text.
     *
     * @param text the text to search
     * @return a list of parsed placeholders
     */
    @NotNull
    public List<ParsedPlaceholder> findAll(@NotNull String text) {
        Objects.requireNonNull(text, "text cannot be null");

        List<ParsedPlaceholder> results = new ArrayList<>();

        // Find standard %...% placeholders
        Matcher standardMatcher = STANDARD_PATTERN.matcher(text);
        while (standardMatcher.find()) {
            String full = standardMatcher.group(0);
            String inner = standardMatcher.group(1);
            try {
                ParsedPlaceholder parsed = parseInner(inner, full);
                parsed.setStartIndex(standardMatcher.start());
                parsed.setEndIndex(standardMatcher.end());
                results.add(parsed);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid placeholders
            }
        }

        // Find bracket {...} placeholders if supported
        if (supportsBrackets) {
            Matcher bracketMatcher = BRACKET_PATTERN.matcher(text);
            while (bracketMatcher.find()) {
                String full = bracketMatcher.group(0);
                String inner = bracketMatcher.group(1);
                try {
                    ParsedPlaceholder parsed = parseInner(inner, full);
                    parsed.setStartIndex(bracketMatcher.start());
                    parsed.setEndIndex(bracketMatcher.end());
                    parsed.setBracketSyntax(true);
                    results.add(parsed);
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid placeholders
                }
            }
        }

        return results;
    }

    /**
     * Checks if the text contains any placeholders.
     *
     * @param text the text to check
     * @return {@code true} if placeholders are found
     */
    public boolean containsPlaceholders(@NotNull String text) {
        Objects.requireNonNull(text, "text cannot be null");
        if (STANDARD_PATTERN.matcher(text).find()) {
            return true;
        }
        return supportsBrackets && BRACKET_PATTERN.matcher(text).find();
    }

    /**
     * Counts the number of placeholders in the text.
     *
     * @param text the text to count in
     * @return the number of placeholders
     */
    public int countPlaceholders(@NotNull String text) {
        return findAll(text).size();
    }

    /**
     * Extracts the inner content from a placeholder.
     */
    private String extractInner(String placeholder) {
        // Handle %...%
        if (placeholder.startsWith("%") && placeholder.endsWith("%") && placeholder.length() > 2) {
            return placeholder.substring(1, placeholder.length() - 1);
        }
        // Handle {...}
        if (supportsBrackets && placeholder.startsWith("{") && placeholder.endsWith("}") && placeholder.length() > 2) {
            return placeholder.substring(1, placeholder.length() - 1);
        }
        // Handle bare identifier
        if (placeholder.matches("[a-zA-Z0-9_]+")) {
            return placeholder;
        }
        return null;
    }

    /**
     * Parses the inner content of a placeholder.
     */
    private ParsedPlaceholder parseInner(String inner, String original) {
        boolean relational = false;
        String working = inner;

        // Check for relational prefix
        if (supportsRelational && working.toLowerCase().startsWith(RELATIONAL_PREFIX)) {
            relational = true;
            working = working.substring(RELATIONAL_PREFIX.length());
        }

        // Find the expansion (first part before underscore)
        int underscoreIndex = working.indexOf('_');
        if (underscoreIndex == -1) {
            // No underscore - entire thing is the expansion with no identifier
            return new ParsedPlaceholder(original, working, "", Collections.emptyList(), relational);
        }

        String expansion = working.substring(0, underscoreIndex);
        String remainder = working.substring(underscoreIndex + 1);

        // Parse the remainder as identifier and potential arguments
        // The identifier might be a prefix ending with _ that captures the rest as an argument
        return new ParsedPlaceholder(original, expansion, remainder, Collections.emptyList(), relational);
    }

    /**
     * A parsed placeholder with its components.
     *
     * @since 1.0.0
     */
    public static final class ParsedPlaceholder {

        private final String original;
        private final String expansion;
        private final String identifier;
        private final List<String> arguments;
        private final boolean relational;
        private int startIndex = -1;
        private int endIndex = -1;
        private boolean bracketSyntax = false;

        /**
         * Creates a new parsed placeholder.
         *
         * @param original   the original placeholder string
         * @param expansion  the expansion identifier
         * @param identifier the placeholder identifier
         * @param arguments  any arguments
         * @param relational whether this is relational
         */
        public ParsedPlaceholder(String original, String expansion, String identifier,
                                  List<String> arguments, boolean relational) {
            this.original = original;
            this.expansion = expansion.toLowerCase();
            this.identifier = identifier;
            this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
            this.relational = relational;
        }

        /**
         * Returns the original placeholder string.
         *
         * @return the original string
         */
        @NotNull
        public String getOriginal() {
            return original;
        }

        /**
         * Returns the expansion identifier.
         *
         * @return the expansion
         */
        @NotNull
        public String getExpansion() {
            return expansion;
        }

        /**
         * Returns the placeholder identifier (part after expansion_).
         *
         * @return the identifier
         */
        @NotNull
        public String getIdentifier() {
            return identifier;
        }

        /**
         * Returns the full placeholder key (expansion_identifier).
         *
         * @return the full key
         */
        @NotNull
        public String getFullKey() {
            if (identifier.isEmpty()) {
                return expansion;
            }
            return expansion + "_" + identifier;
        }

        /**
         * Returns any arguments extracted from the placeholder.
         *
         * @return the arguments
         */
        @NotNull
        public List<String> getArguments() {
            return arguments;
        }

        /**
         * Checks if this is a relational placeholder.
         *
         * @return {@code true} if relational
         */
        public boolean isRelational() {
            return relational;
        }

        /**
         * Returns the start index in the original text.
         *
         * @return the start index, or -1 if not from findAll
         */
        public int getStartIndex() {
            return startIndex;
        }

        void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        /**
         * Returns the end index in the original text.
         *
         * @return the end index, or -1 if not from findAll
         */
        public int getEndIndex() {
            return endIndex;
        }

        void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }

        /**
         * Checks if this placeholder used bracket syntax.
         *
         * @return {@code true} if bracket syntax
         */
        public boolean isBracketSyntax() {
            return bracketSyntax;
        }

        void setBracketSyntax(boolean bracketSyntax) {
            this.bracketSyntax = bracketSyntax;
        }

        @Override
        public String toString() {
            return "ParsedPlaceholder{" +
                    "expansion='" + expansion + '\'' +
                    ", identifier='" + identifier + '\'' +
                    (relational ? ", relational=true" : "") +
                    (!arguments.isEmpty() ? ", arguments=" + arguments : "") +
                    '}';
        }
    }

    /**
     * Builder for creating {@link PlaceholderParser} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private Pattern pattern = STANDARD_PATTERN;
        private boolean supportsBrackets = false;
        private boolean supportsRelational = true;

        private Builder() {}

        /**
         * Sets whether bracket syntax is supported.
         *
         * @param supports {@code true} to support brackets
         * @return this builder
         */
        @NotNull
        public Builder supportsBrackets(boolean supports) {
            this.supportsBrackets = supports;
            return this;
        }

        /**
         * Sets whether relational placeholders are supported.
         *
         * @param supports {@code true} to support relational
         * @return this builder
         */
        @NotNull
        public Builder supportsRelational(boolean supports) {
            this.supportsRelational = supports;
            return this;
        }

        /**
         * Sets a custom pattern for placeholder matching.
         *
         * @param pattern the pattern
         * @return this builder
         */
        @NotNull
        public Builder pattern(@NotNull Pattern pattern) {
            this.pattern = Objects.requireNonNull(pattern, "pattern cannot be null");
            return this;
        }

        /**
         * Builds the parser.
         *
         * @return a new PlaceholderParser
         */
        @NotNull
        public PlaceholderParser build() {
            return new PlaceholderParser(this);
        }
    }
}
