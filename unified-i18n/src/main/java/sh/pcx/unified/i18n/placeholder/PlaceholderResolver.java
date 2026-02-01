/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Resolves placeholders in text by replacing them with their computed values.
 *
 * <p>The placeholder resolver uses registered handlers to compute placeholder values
 * and replaces them in the input text. It supports caching, async resolution, and
 * relational placeholders.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * PlaceholderResolver resolver = PlaceholderResolver.builder()
 *     .registry(placeholderRegistry)
 *     .cache(placeholderCache)
 *     .parser(PlaceholderParser.standard())
 *     .build();
 *
 * // Simple resolution
 * String result = resolver.resolve("Hello, %player_name%!", context);
 *
 * // With fallback
 * String safe = resolver.resolve("Balance: %vault_balance%", context, "0.00");
 *
 * // Async resolution for slow placeholders
 * CompletableFuture<String> async = resolver.resolveAsync(
 *     "Stats: %db_player_stats%",
 *     context
 * );
 *
 * // Resolve specific placeholder
 * PlaceholderResult result = resolver.resolvePlaceholder("player_name", context);
 * }</pre>
 *
 * <h2>Resolution Order</h2>
 * <ol>
 *   <li>Check cache for cached value</li>
 *   <li>Look up handler in registry</li>
 *   <li>Execute handler with context</li>
 *   <li>Cache result if cacheable</li>
 *   <li>Return value or fallback</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderRegistry
 * @see PlaceholderParser
 * @see PlaceholderCache
 */
public final class PlaceholderResolver {

    private final PlaceholderRegistry registry;
    private final PlaceholderCache cache;
    private final PlaceholderParser parser;
    private final String defaultFallback;
    private final boolean preserveUnknown;
    private final Map<String, BiFunction<PlaceholderContext, String, PlaceholderResult>> customResolvers;

    private PlaceholderResolver(Builder builder) {
        this.registry = builder.registry;
        this.cache = builder.cache;
        this.parser = builder.parser;
        this.defaultFallback = builder.defaultFallback;
        this.preserveUnknown = builder.preserveUnknown;
        this.customResolvers = new ConcurrentHashMap<>();
    }

    /**
     * Creates a resolver with the given registry.
     *
     * @param registry the placeholder registry
     * @return a new resolver
     */
    @NotNull
    public static PlaceholderResolver create(@NotNull PlaceholderRegistry registry) {
        return builder().registry(registry).build();
    }

    /**
     * Creates a new builder for configuring the resolver.
     *
     * @return a new builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves all placeholders in the given text.
     *
     * @param text    the text containing placeholders
     * @param context the resolution context
     * @return the text with placeholders replaced
     */
    @NotNull
    public String resolve(@NotNull String text, @NotNull PlaceholderContext context) {
        return resolve(text, context, defaultFallback);
    }

    /**
     * Resolves all placeholders in the given text with a custom fallback.
     *
     * @param text     the text containing placeholders
     * @param context  the resolution context
     * @param fallback the fallback value for unresolved placeholders
     * @return the text with placeholders replaced
     */
    @NotNull
    public String resolve(@NotNull String text, @NotNull PlaceholderContext context, @Nullable String fallback) {
        Objects.requireNonNull(text, "text cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        if (!parser.containsPlaceholders(text)) {
            return text;
        }

        List<PlaceholderParser.ParsedPlaceholder> placeholders = parser.findAll(text);
        if (placeholders.isEmpty()) {
            return text;
        }

        // Sort by start index descending to replace from end to beginning
        // This preserves indices during replacement
        placeholders.sort((a, b) -> Integer.compare(b.getStartIndex(), a.getStartIndex()));

        StringBuilder result = new StringBuilder(text);
        for (PlaceholderParser.ParsedPlaceholder parsed : placeholders) {
            PlaceholderResult resolution = resolveSingle(parsed, context);

            String replacement;
            if (resolution.isPresent()) {
                replacement = resolution.get();
            } else if (preserveUnknown) {
                continue; // Keep original placeholder
            } else {
                replacement = fallback != null ? fallback : "";
            }

            result.replace(parsed.getStartIndex(), parsed.getEndIndex(), replacement);
        }

        return result.toString();
    }

    /**
     * Resolves placeholders asynchronously.
     *
     * <p>This method is useful when placeholders may require slow operations.
     * All placeholder resolutions are performed in parallel.
     *
     * @param text    the text containing placeholders
     * @param context the resolution context
     * @return a future that completes with the resolved text
     */
    @NotNull
    public CompletableFuture<String> resolveAsync(@NotNull String text, @NotNull PlaceholderContext context) {
        Objects.requireNonNull(text, "text cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        if (!parser.containsPlaceholders(text)) {
            return CompletableFuture.completedFuture(text);
        }

        List<PlaceholderParser.ParsedPlaceholder> placeholders = parser.findAll(text);
        if (placeholders.isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }

        // Resolve all placeholders in parallel
        List<CompletableFuture<ResolvedPair>> futures = new ArrayList<>();
        for (PlaceholderParser.ParsedPlaceholder parsed : placeholders) {
            CompletableFuture<ResolvedPair> future = CompletableFuture.supplyAsync(() -> {
                PlaceholderResult result = resolveSingle(parsed, context);
                return new ResolvedPair(parsed, result);
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                // Sort by start index descending
                List<ResolvedPair> resolved = new ArrayList<>();
                for (CompletableFuture<ResolvedPair> f : futures) {
                    resolved.add(f.join());
                }
                resolved.sort((a, b) -> Integer.compare(
                    b.parsed.getStartIndex(),
                    a.parsed.getStartIndex()
                ));

                StringBuilder result = new StringBuilder(text);
                for (ResolvedPair pair : resolved) {
                    String replacement;
                    if (pair.result.isPresent()) {
                        replacement = pair.result.get();
                    } else if (preserveUnknown) {
                        continue;
                    } else {
                        replacement = defaultFallback != null ? defaultFallback : "";
                    }
                    result.replace(
                        pair.parsed.getStartIndex(),
                        pair.parsed.getEndIndex(),
                        replacement
                    );
                }
                return result.toString();
            });
    }

    /**
     * Resolves a single placeholder.
     *
     * @param placeholder the placeholder string (e.g., "player_name" or "%player_name%")
     * @param context     the resolution context
     * @return the resolution result
     */
    @NotNull
    public PlaceholderResult resolvePlaceholder(@NotNull String placeholder, @NotNull PlaceholderContext context) {
        Objects.requireNonNull(placeholder, "placeholder cannot be null");
        Objects.requireNonNull(context, "context cannot be null");

        return parser.tryParse(placeholder)
            .map(parsed -> resolveSingle(parsed, context))
            .orElse(PlaceholderResult.empty());
    }

    /**
     * Resolves a parsed placeholder.
     */
    private PlaceholderResult resolveSingle(PlaceholderParser.ParsedPlaceholder parsed, PlaceholderContext context) {
        String expansion = parsed.getExpansion();
        String identifier = parsed.getIdentifier();
        String fullKey = parsed.getFullKey();

        // Check custom resolvers first
        BiFunction<PlaceholderContext, String, PlaceholderResult> customResolver = customResolvers.get(expansion);
        if (customResolver != null) {
            PlaceholderResult result = customResolver.apply(context, identifier);
            if (result.isPresent()) {
                return result;
            }
        }

        // Check cache
        if (cache != null) {
            java.util.Optional<String> cached;
            if (parsed.isRelational() && context.isRelational()) {
                cached = cache.get(
                    context.getPlayerUUID().orElse(null),
                    context.getRelationalPlayer().map(p -> p.getUniqueId()).orElse(null),
                    fullKey
                );
            } else if (context.hasPlayer()) {
                cached = cache.get(context.getPlayerUUID().orElse(null), fullKey);
            } else {
                cached = cache.get(fullKey);
            }

            if (cached.isPresent()) {
                return PlaceholderResult.cached(cached.get());
            }
        }

        // Resolve from registry
        if (registry != null) {
            PlaceholderResult result = registry.resolve(expansion, identifier, context);
            if (result.isPresent() && cache != null) {
                // Cache the result
                cacheResult(parsed, context, result.get());
            }
            return result;
        }

        return PlaceholderResult.empty();
    }

    /**
     * Caches a resolved result.
     */
    private void cacheResult(PlaceholderParser.ParsedPlaceholder parsed, PlaceholderContext context, String value) {
        if (cache == null) return;

        String fullKey = parsed.getFullKey();
        CacheTTL ttl = registry != null ? registry.getCacheTTL(parsed.getExpansion(), parsed.getIdentifier()) : CacheTTL.DEFAULT;

        if (parsed.isRelational() && context.isRelational()) {
            cache.put(
                context.getPlayerUUID().orElse(null),
                context.getRelationalPlayer().map(p -> p.getUniqueId()).orElse(null),
                fullKey,
                value,
                ttl
            );
        } else if (context.hasPlayer()) {
            cache.put(context.getPlayerUUID().orElse(null), fullKey, value, ttl);
        } else {
            cache.put(fullKey, value, ttl);
        }
    }

    /**
     * Registers a custom resolver for an expansion.
     *
     * @param expansion the expansion identifier
     * @param resolver  the resolver function
     */
    public void registerCustomResolver(@NotNull String expansion,
                                        @NotNull BiFunction<PlaceholderContext, String, PlaceholderResult> resolver) {
        customResolvers.put(expansion.toLowerCase(), resolver);
    }

    /**
     * Removes a custom resolver.
     *
     * @param expansion the expansion identifier
     */
    public void unregisterCustomResolver(@NotNull String expansion) {
        customResolvers.remove(expansion.toLowerCase());
    }

    /**
     * Returns the placeholder registry.
     *
     * @return the registry
     */
    @Nullable
    public PlaceholderRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns the placeholder cache.
     *
     * @return the cache
     */
    @Nullable
    public PlaceholderCache getCache() {
        return cache;
    }

    /**
     * Returns the parser.
     *
     * @return the parser
     */
    @NotNull
    public PlaceholderParser getParser() {
        return parser;
    }

    /**
     * Internal pair class for async resolution.
     */
    private static final class ResolvedPair {
        final PlaceholderParser.ParsedPlaceholder parsed;
        final PlaceholderResult result;

        ResolvedPair(PlaceholderParser.ParsedPlaceholder parsed, PlaceholderResult result) {
            this.parsed = parsed;
            this.result = result;
        }
    }

    /**
     * Builder for creating {@link PlaceholderResolver} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private PlaceholderRegistry registry;
        private PlaceholderCache cache;
        private PlaceholderParser parser = PlaceholderParser.standard();
        private String defaultFallback = "";
        private boolean preserveUnknown = false;

        private Builder() {}

        /**
         * Sets the placeholder registry.
         *
         * @param registry the registry
         * @return this builder
         */
        @NotNull
        public Builder registry(@Nullable PlaceholderRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Sets the placeholder cache.
         *
         * @param cache the cache
         * @return this builder
         */
        @NotNull
        public Builder cache(@Nullable PlaceholderCache cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets the placeholder parser.
         *
         * @param parser the parser
         * @return this builder
         */
        @NotNull
        public Builder parser(@NotNull PlaceholderParser parser) {
            this.parser = Objects.requireNonNull(parser, "parser cannot be null");
            return this;
        }

        /**
         * Sets the default fallback value for unresolved placeholders.
         *
         * @param fallback the fallback value
         * @return this builder
         */
        @NotNull
        public Builder defaultFallback(@Nullable String fallback) {
            this.defaultFallback = fallback;
            return this;
        }

        /**
         * Sets whether to preserve unknown placeholders in the text.
         *
         * @param preserve {@code true} to keep unknown placeholders
         * @return this builder
         */
        @NotNull
        public Builder preserveUnknown(boolean preserve) {
            this.preserveUnknown = preserve;
            return this;
        }

        /**
         * Builds the resolver.
         *
         * @return a new PlaceholderResolver
         */
        @NotNull
        public PlaceholderResolver build() {
            return new PlaceholderResolver(this);
        }
    }
}
