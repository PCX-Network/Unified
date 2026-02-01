/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Resolves conflicts when concurrent modifications occur on different servers.
 *
 * <p>When player data is modified on multiple servers before synchronization
 * completes, a conflict can occur. The SyncConflictResolver determines which
 * value should be kept.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Custom resolver that merges numeric values
 * SyncConflictResolver mergeResolver = (playerId, key, local, remote) -> {
 *     // For numeric types, add the differences
 *     if (local.getValue() instanceof Number && remote.getValue() instanceof Number) {
 *         double localVal = ((Number) local.getValue()).doubleValue();
 *         double remoteVal = ((Number) remote.getValue()).doubleValue();
 *
 *         // If both increased, sum the increases
 *         if (local.getPreviousValue() instanceof Number) {
 *             double prevVal = ((Number) local.getPreviousValue()).doubleValue();
 *             double localDelta = localVal - prevVal;
 *             double remoteDelta = remoteVal - prevVal;
 *             return Resolution.useValue(prevVal + localDelta + remoteDelta);
 *         }
 *     }
 *
 *     // Default to last-write-wins
 *     return local.getTimestamp().isAfter(remote.getTimestamp())
 *         ? Resolution.useLocal()
 *         : Resolution.useRemote();
 * };
 *
 * crossServerSync.setConflictResolver(mergeResolver);
 * }</pre>
 *
 * <h2>Built-in Resolvers</h2>
 * <ul>
 *   <li>{@link #lastWriteWins()} - Uses the most recent value by timestamp</li>
 *   <li>{@link #localWins()} - Always keeps the local value</li>
 *   <li>{@link #remoteWins()} - Always uses the remote value</li>
 *   <li>{@link #mergeNumeric()} - Sums numeric deltas for counters</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CrossServerSync
 */
@FunctionalInterface
public interface SyncConflictResolver {

    /**
     * Resolves a conflict between local and remote values.
     *
     * @param playerId the player's UUID
     * @param key      the conflicting data key
     * @param local    the local change
     * @param remote   the remote change
     * @return the resolution indicating which value to use
     * @since 1.0.0
     */
    @NotNull
    Resolution resolve(@NotNull UUID playerId, @NotNull DataKey<?> key,
                       @NotNull ConflictValue local, @NotNull ConflictValue remote);

    // ==================== Built-in Resolvers ====================

    /**
     * Returns a resolver that uses the most recent value based on timestamp.
     *
     * <p>This is the default resolver. If timestamps are equal, the remote
     * value is preferred to ensure consistency across the network.
     *
     * @return the last-write-wins resolver
     * @since 1.0.0
     */
    @NotNull
    static SyncConflictResolver lastWriteWins() {
        return (playerId, key, local, remote) ->
                local.getTimestamp().isAfter(remote.getTimestamp())
                        ? Resolution.useLocal()
                        : Resolution.useRemote();
    }

    /**
     * Returns a resolver that always keeps the local value.
     *
     * <p>Use this when local changes should never be overwritten by remote
     * changes. Be aware this can cause data inconsistency across servers.
     *
     * @return the local-wins resolver
     * @since 1.0.0
     */
    @NotNull
    static SyncConflictResolver localWins() {
        return (playerId, key, local, remote) -> Resolution.useLocal();
    }

    /**
     * Returns a resolver that always uses the remote value.
     *
     * <p>Use this when remote changes should always be accepted. This ensures
     * consistency but may lose local changes.
     *
     * @return the remote-wins resolver
     * @since 1.0.0
     */
    @NotNull
    static SyncConflictResolver remoteWins() {
        return (playerId, key, local, remote) -> Resolution.useRemote();
    }

    /**
     * Returns a resolver that merges numeric values by summing deltas.
     *
     * <p>For numeric values, this calculates the change (delta) made by each
     * side and applies both deltas. This is useful for counters like kills,
     * deaths, or economy balances where both changes should be preserved.
     *
     * <p>For non-numeric values, falls back to last-write-wins.
     *
     * @return the merge-numeric resolver
     * @since 1.0.0
     */
    @NotNull
    static SyncConflictResolver mergeNumeric() {
        return (playerId, key, local, remote) -> {
            Object localVal = local.getValue();
            Object remoteVal = remote.getValue();
            Object localPrev = local.getPreviousValue();

            // Only merge if all values are numeric and we know the previous value
            if (localVal instanceof Number && remoteVal instanceof Number && localPrev instanceof Number) {
                double prevValue = ((Number) localPrev).doubleValue();
                double localDelta = ((Number) localVal).doubleValue() - prevValue;
                double remoteDelta = ((Number) remoteVal).doubleValue() - prevValue;
                double merged = prevValue + localDelta + remoteDelta;

                // Preserve the original type
                if (localVal instanceof Integer) {
                    return Resolution.useValue((int) merged);
                } else if (localVal instanceof Long) {
                    return Resolution.useValue((long) merged);
                } else if (localVal instanceof Float) {
                    return Resolution.useValue((float) merged);
                } else {
                    return Resolution.useValue(merged);
                }
            }

            // Fall back to last-write-wins
            return local.getTimestamp().isAfter(remote.getTimestamp())
                    ? Resolution.useLocal()
                    : Resolution.useRemote();
        };
    }

    /**
     * Returns a resolver that uses a custom merge function for specific keys.
     *
     * @param keyResolver resolver for specific keys
     * @param fallback    resolver to use for other keys
     * @return a combined resolver
     * @since 1.0.0
     */
    @NotNull
    static SyncConflictResolver forKey(@NotNull DataKey<?> targetKey,
                                        @NotNull SyncConflictResolver keyResolver,
                                        @NotNull SyncConflictResolver fallback) {
        return (playerId, key, local, remote) -> {
            if (key.equals(targetKey)) {
                return keyResolver.resolve(playerId, key, local, remote);
            }
            return fallback.resolve(playerId, key, local, remote);
        };
    }

    // ==================== Nested Types ====================

    /**
     * Represents a value in a conflict with its metadata.
     *
     * @since 1.0.0
     */
    interface ConflictValue {

        /**
         * Returns the current value.
         *
         * @return the value, may be null
         */
        @Nullable
        Object getValue();

        /**
         * Returns the previous value before this change.
         *
         * @return the previous value, may be null if unknown
         */
        @Nullable
        Object getPreviousValue();

        /**
         * Returns when this change was made.
         *
         * @return the change timestamp
         */
        @NotNull
        Instant getTimestamp();

        /**
         * Returns the server that made this change.
         *
         * @return the server name
         */
        @NotNull
        String getServerName();

        /**
         * Returns the data version at time of change.
         *
         * @return the data version
         */
        long getDataVersion();
    }

    /**
     * The result of conflict resolution.
     *
     * @since 1.0.0
     */
    final class Resolution {
        private final ResolutionType type;
        private final Object customValue;

        private Resolution(ResolutionType type, Object customValue) {
            this.type = type;
            this.customValue = customValue;
        }

        /**
         * Returns a resolution to use the local value.
         *
         * @return resolution for local value
         */
        @NotNull
        public static Resolution useLocal() {
            return new Resolution(ResolutionType.USE_LOCAL, null);
        }

        /**
         * Returns a resolution to use the remote value.
         *
         * @return resolution for remote value
         */
        @NotNull
        public static Resolution useRemote() {
            return new Resolution(ResolutionType.USE_REMOTE, null);
        }

        /**
         * Returns a resolution to use a custom merged value.
         *
         * @param value the merged value
         * @return resolution for custom value
         */
        @NotNull
        public static Resolution useValue(@Nullable Object value) {
            return new Resolution(ResolutionType.USE_CUSTOM, value);
        }

        /**
         * Returns a resolution to skip/ignore this change.
         *
         * @return resolution to skip
         */
        @NotNull
        public static Resolution skip() {
            return new Resolution(ResolutionType.SKIP, null);
        }

        /**
         * Returns the resolution type.
         *
         * @return the type
         */
        @NotNull
        public ResolutionType getType() {
            return type;
        }

        /**
         * Returns the custom value if type is USE_CUSTOM.
         *
         * @return the custom value
         */
        @Nullable
        public Object getCustomValue() {
            return customValue;
        }

        /**
         * Checks if this resolution uses the local value.
         *
         * @return true if using local
         */
        public boolean isLocal() {
            return type == ResolutionType.USE_LOCAL;
        }

        /**
         * Checks if this resolution uses the remote value.
         *
         * @return true if using remote
         */
        public boolean isRemote() {
            return type == ResolutionType.USE_REMOTE;
        }

        /**
         * Checks if this resolution uses a custom value.
         *
         * @return true if using custom
         */
        public boolean isCustom() {
            return type == ResolutionType.USE_CUSTOM;
        }

        /**
         * Checks if this resolution skips the change.
         *
         * @return true if skipping
         */
        public boolean isSkip() {
            return type == ResolutionType.SKIP;
        }
    }

    /**
     * Types of conflict resolution.
     *
     * @since 1.0.0
     */
    enum ResolutionType {
        /**
         * Use the local value.
         */
        USE_LOCAL,

        /**
         * Use the remote value.
         */
        USE_REMOTE,

        /**
         * Use a custom merged value.
         */
        USE_CUSTOM,

        /**
         * Skip this change entirely.
         */
        SKIP
    }
}
