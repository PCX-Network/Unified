/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A conflict resolution strategy that always accepts the most recent write.
 *
 * <p>LastWriteWins (LWW) is a simple and widely-used conflict resolution
 * strategy in distributed systems. It resolves conflicts by comparing
 * timestamps and choosing the value with the more recent timestamp.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a LWW resolver using system time
 * LastWriteWins<PlayerData> resolver = LastWriteWins.systemTime(
 *     data -> data.getLastModified()
 * );
 *
 * // Create a LWW resolver using a timestamp extractor
 * LastWriteWins<PlayerData> resolver = LastWriteWins.of(
 *     data -> data.getTimestamp()
 * );
 *
 * // Use with cache
 * Cache<UUID, PlayerData> cache = caches.<UUID, PlayerData>builder()
 *     .conflictResolver(resolver)
 *     .build();
 * }</pre>
 *
 * <h2>Considerations</h2>
 * <ul>
 *   <li>Clock synchronization is important in distributed environments</li>
 *   <li>Does not preserve concurrent updates - one will be lost</li>
 *   <li>Simple and predictable behavior</li>
 *   <li>Works well when updates are infrequent or sequential</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.
 *
 * @param <V> the value type
 * @since 1.0.0
 * @author Supatuck
 * @see ConflictResolver
 */
public final class LastWriteWins<V> implements ConflictResolver<V> {

    private final TimestampExtractor<V> timestampExtractor;

    /**
     * Creates a new LastWriteWins resolver with the given timestamp extractor.
     *
     * @param timestampExtractor function to extract timestamp from values
     */
    private LastWriteWins(@NotNull TimestampExtractor<V> timestampExtractor) {
        this.timestampExtractor = timestampExtractor;
    }

    /**
     * Creates a LastWriteWins resolver using the provided timestamp extractor.
     *
     * @param <V>       the value type
     * @param extractor function to extract timestamp from values
     * @return a new LastWriteWins resolver
     * @since 1.0.0
     */
    @NotNull
    public static <V> LastWriteWins<V> of(@NotNull TimestampExtractor<V> extractor) {
        return new LastWriteWins<>(extractor);
    }

    /**
     * Creates a LastWriteWins resolver that uses the current system time
     * when values don't have embedded timestamps.
     *
     * <p>This should only be used when you can guarantee that the incoming
     * value is always newer than the existing value, or when timestamp
     * accuracy is not critical.
     *
     * @param <V> the value type
     * @return a resolver that assumes incoming is always newer
     * @since 1.0.0
     */
    @NotNull
    public static <V> LastWriteWins<V> assumeNewest() {
        return new LastWriteWins<>(value -> System.currentTimeMillis());
    }

    /**
     * Creates a LastWriteWins resolver for values implementing Timestamped.
     *
     * @param <V> the value type
     * @return a resolver using the Timestamped interface
     * @since 1.0.0
     */
    @NotNull
    public static <V extends ConflictResolver.Timestamped> LastWriteWins<V> forTimestamped() {
        return new LastWriteWins<>(ConflictResolver.Timestamped::getTimestamp);
    }

    /**
     * Creates a LastWriteWins resolver using a system time extractor.
     *
     * <p>The extractor is called for each value to get its timestamp.
     * The timestamps should be in milliseconds since epoch.
     *
     * @param <V>       the value type
     * @param extractor function to extract timestamp in millis
     * @return a resolver using the provided extractor
     * @since 1.0.0
     */
    @NotNull
    public static <V> LastWriteWins<V> systemTime(@NotNull java.util.function.ToLongFunction<V> extractor) {
        return new LastWriteWins<>(extractor::applyAsLong);
    }

    @Override
    @Nullable
    public V resolve(@Nullable V existing, @NotNull V incoming) {
        if (existing == null) {
            return incoming;
        }

        long existingTimestamp = timestampExtractor.extractTimestamp(existing);
        long incomingTimestamp = timestampExtractor.extractTimestamp(incoming);

        // Prefer incoming on equal timestamps (true last-write-wins)
        return incomingTimestamp >= existingTimestamp ? incoming : existing;
    }

    /**
     * Returns a resolver that prefers existing values on equal timestamps.
     *
     * <p>This is useful when you want to avoid overwrites when timestamps
     * are equal (first-in-wins for ties).
     *
     * @return a modified resolver preferring existing on ties
     * @since 1.0.0
     */
    @NotNull
    public ConflictResolver<V> preferExistingOnTie() {
        return (existing, incoming) -> {
            if (existing == null) {
                return incoming;
            }

            long existingTimestamp = timestampExtractor.extractTimestamp(existing);
            long incomingTimestamp = timestampExtractor.extractTimestamp(incoming);

            // Prefer existing on equal timestamps
            return incomingTimestamp > existingTimestamp ? incoming : existing;
        };
    }

    /**
     * Returns a resolver that uses a tolerance window for comparing timestamps.
     *
     * <p>If the timestamps are within the tolerance, they are considered
     * equal and the incoming value wins.
     *
     * @param toleranceMillis the tolerance window in milliseconds
     * @return a resolver with tolerance
     * @since 1.0.0
     */
    @NotNull
    public ConflictResolver<V> withTolerance(long toleranceMillis) {
        return (existing, incoming) -> {
            if (existing == null) {
                return incoming;
            }

            long existingTimestamp = timestampExtractor.extractTimestamp(existing);
            long incomingTimestamp = timestampExtractor.extractTimestamp(incoming);

            long diff = incomingTimestamp - existingTimestamp;
            if (Math.abs(diff) <= toleranceMillis) {
                return incoming;  // Within tolerance, prefer incoming
            }
            return diff > 0 ? incoming : existing;
        };
    }

    /**
     * Functional interface for extracting timestamps from values.
     *
     * @param <V> the value type
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface TimestampExtractor<V> {

        /**
         * Extracts the timestamp from a value.
         *
         * @param value the value
         * @return the timestamp in milliseconds since epoch
         */
        long extractTimestamp(@NotNull V value);
    }

    @Override
    public String toString() {
        return "LastWriteWins{}";
    }
}
