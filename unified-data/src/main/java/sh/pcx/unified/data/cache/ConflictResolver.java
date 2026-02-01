/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for resolving conflicts when concurrent modifications occur.
 *
 * <p>ConflictResolver is used when multiple updates to the same cache key
 * occur concurrently, or in distributed caching scenarios where updates
 * from multiple servers may conflict.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Custom conflict resolver
 * ConflictResolver<PlayerData> resolver = (existing, incoming) -> {
 *     if (incoming.getLastModified().isAfter(existing.getLastModified())) {
 *         return incoming;  // Last-write-wins
 *     }
 *     return existing;
 * };
 *
 * // Use with cache builder
 * Cache<UUID, PlayerData> cache = caches.<UUID, PlayerData>builder()
 *     .conflictResolver(resolver)
 *     .build();
 *
 * // Use built-in resolvers
 * Cache<UUID, PlayerData> cache = caches.<UUID, PlayerData>builder()
 *     .conflictResolver(ConflictResolver.lastWriteWins())
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe as conflict resolution may occur
 * from multiple threads concurrently.
 *
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see LastWriteWins
 * @see MergeStrategy
 */
@FunctionalInterface
public interface ConflictResolver<V> {

    /**
     * Resolves a conflict between an existing value and an incoming value.
     *
     * @param existing the current cached value (may be null if not present)
     * @param incoming the new value being written
     * @return the value to store (may be existing, incoming, or a merged value)
     * @since 1.0.0
     */
    @Nullable
    V resolve(@Nullable V existing, @NotNull V incoming);

    /**
     * Returns a resolver that always uses the incoming (new) value.
     *
     * <p>This is equivalent to standard cache behavior where puts overwrite
     * existing values unconditionally.
     *
     * @param <V> the value type
     * @return a resolver that always returns the incoming value
     * @since 1.0.0
     */
    @NotNull
    static <V> ConflictResolver<V> lastWriteWins() {
        return (existing, incoming) -> incoming;
    }

    /**
     * Returns a resolver that keeps the existing value if present.
     *
     * <p>Useful for "put if absent" semantics where the first write wins.
     *
     * @param <V> the value type
     * @return a resolver that prefers existing values
     * @since 1.0.0
     */
    @NotNull
    static <V> ConflictResolver<V> firstWriteWins() {
        return (existing, incoming) -> existing != null ? existing : incoming;
    }

    /**
     * Returns a resolver that throws an exception on conflict.
     *
     * <p>Useful when conflicts should never occur and represent an error.
     *
     * @param <V> the value type
     * @return a resolver that throws on conflict
     * @since 1.0.0
     */
    @NotNull
    static <V> ConflictResolver<V> throwOnConflict() {
        return (existing, incoming) -> {
            if (existing != null) {
                throw new CacheConflictException("Conflict detected: existing value present");
            }
            return incoming;
        };
    }

    /**
     * Returns a resolver based on timestamps in the values.
     *
     * <p>Values must implement {@link Timestamped} interface to provide
     * timestamp information for comparison.
     *
     * @param <V> the value type (must be Timestamped)
     * @return a timestamp-based resolver
     * @since 1.0.0
     */
    @NotNull
    static <V extends Timestamped> ConflictResolver<V> byTimestamp() {
        return (existing, incoming) -> {
            if (existing == null) {
                return incoming;
            }
            return incoming.getTimestamp() >= existing.getTimestamp() ? incoming : existing;
        };
    }

    /**
     * Returns a resolver based on version numbers in the values.
     *
     * <p>Values must implement {@link Versioned} interface to provide
     * version information for comparison.
     *
     * @param <V> the value type (must be Versioned)
     * @return a version-based resolver
     * @since 1.0.0
     */
    @NotNull
    static <V extends Versioned> ConflictResolver<V> byVersion() {
        return (existing, incoming) -> {
            if (existing == null) {
                return incoming;
            }
            return incoming.getVersion() >= existing.getVersion() ? incoming : existing;
        };
    }

    /**
     * Creates a resolver that uses a merge strategy.
     *
     * @param <V>      the value type
     * @param strategy the merge strategy to use
     * @return a resolver using the merge strategy
     * @since 1.0.0
     */
    @NotNull
    static <V> ConflictResolver<V> withMerge(@NotNull MergeStrategy<V> strategy) {
        return (existing, incoming) -> {
            if (existing == null) {
                return incoming;
            }
            return strategy.merge(existing, incoming);
        };
    }

    /**
     * Interface for values that have a timestamp.
     *
     * @since 1.0.0
     */
    interface Timestamped {
        /**
         * Returns the timestamp of this value in milliseconds since epoch.
         *
         * @return the timestamp
         */
        long getTimestamp();
    }

    /**
     * Interface for values that have a version number.
     *
     * @since 1.0.0
     */
    interface Versioned {
        /**
         * Returns the version number of this value.
         *
         * @return the version
         */
        long getVersion();
    }

    /**
     * Exception thrown when a conflict cannot be resolved.
     *
     * @since 1.0.0
     */
    class CacheConflictException extends RuntimeException {

        /**
         * Creates a new cache conflict exception.
         *
         * @param message the error message
         */
        public CacheConflictException(String message) {
            super(message);
        }

        /**
         * Creates a new cache conflict exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public CacheConflictException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
