/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration record for cache instances.
 *
 * <p>CacheConfig encapsulates all configuration options for creating a cache,
 * including size limits, expiration policies, and feature flags.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a basic configuration
 * CacheConfig config = CacheConfig.builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterWrite(Duration.ofHours(1))
 *     .build();
 *
 * // Create a configuration with all options
 * CacheConfig config = CacheConfig.builder()
 *     .name("player-data")
 *     .maximumSize(1000)
 *     .expireAfterAccess(Duration.ofMinutes(30))
 *     .expireAfterWrite(Duration.ofHours(1))
 *     .refreshAfterWrite(Duration.ofMinutes(5))
 *     .weakKeys(false)
 *     .weakValues(false)
 *     .softValues(true)
 *     .recordStats(true)
 *     .build();
 * }</pre>
 *
 * @param name              the cache name for identification
 * @param maximumSize       the maximum number of entries (0 = unlimited)
 * @param maximumWeight     the maximum total weight of entries (0 = use size)
 * @param expireAfterAccess time after last access before expiration (null = no expiration)
 * @param expireAfterWrite  time after write before expiration (null = no expiration)
 * @param refreshAfterWrite time after write before async refresh (null = no refresh)
 * @param weakKeys          whether to use weak references for keys
 * @param weakValues        whether to use weak references for values
 * @param softValues        whether to use soft references for values
 * @param recordStats       whether to record cache statistics
 * @since 1.0.0
 * @author Supatuck
 * @see CacheService
 */
public record CacheConfig(
        @NotNull String name,
        long maximumSize,
        long maximumWeight,
        @Nullable Duration expireAfterAccess,
        @Nullable Duration expireAfterWrite,
        @Nullable Duration refreshAfterWrite,
        boolean weakKeys,
        boolean weakValues,
        boolean softValues,
        boolean recordStats
) {

    /**
     * Default maximum cache size if not specified.
     */
    public static final long DEFAULT_MAXIMUM_SIZE = 10_000L;

    /**
     * Default TTL for cached entries.
     */
    public static final Duration DEFAULT_EXPIRE_AFTER_WRITE = Duration.ofHours(1);

    /**
     * Creates a new CacheConfig with validation.
     *
     * @param name              the cache name
     * @param maximumSize       the maximum size
     * @param maximumWeight     the maximum weight
     * @param expireAfterAccess expire after access duration
     * @param expireAfterWrite  expire after write duration
     * @param refreshAfterWrite refresh after write duration
     * @param weakKeys          use weak keys
     * @param weakValues        use weak values
     * @param softValues        use soft values
     * @param recordStats       record statistics
     */
    public CacheConfig {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (maximumSize < 0) {
            throw new IllegalArgumentException("maximumSize must be non-negative");
        }
        if (maximumWeight < 0) {
            throw new IllegalArgumentException("maximumWeight must be non-negative");
        }
        if (weakValues && softValues) {
            throw new IllegalArgumentException("Cannot use both weak and soft values");
        }
        if (expireAfterAccess != null && expireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("expireAfterAccess must be non-negative");
        }
        if (expireAfterWrite != null && expireAfterWrite.isNegative()) {
            throw new IllegalArgumentException("expireAfterWrite must be non-negative");
        }
        if (refreshAfterWrite != null && refreshAfterWrite.isNegative()) {
            throw new IllegalArgumentException("refreshAfterWrite must be non-negative");
        }
    }

    /**
     * Creates a new builder for CacheConfig.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with this config's values.
     *
     * @return a new builder with this config's values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .maximumSize(maximumSize)
                .maximumWeight(maximumWeight)
                .expireAfterAccess(expireAfterAccess)
                .expireAfterWrite(expireAfterWrite)
                .refreshAfterWrite(refreshAfterWrite)
                .weakKeys(weakKeys)
                .weakValues(weakValues)
                .softValues(softValues)
                .recordStats(recordStats);
    }

    /**
     * Creates a simple configuration with just a name and default settings.
     *
     * @param name the cache name
     * @return a new CacheConfig with defaults
     * @since 1.0.0
     */
    @NotNull
    public static CacheConfig withDefaults(@NotNull String name) {
        return builder()
                .name(name)
                .maximumSize(DEFAULT_MAXIMUM_SIZE)
                .expireAfterWrite(DEFAULT_EXPIRE_AFTER_WRITE)
                .recordStats(true)
                .build();
    }

    /**
     * Checks if this cache has a size limit.
     *
     * @return true if maximumSize is greater than 0
     * @since 1.0.0
     */
    public boolean hasSizeLimit() {
        return maximumSize > 0;
    }

    /**
     * Checks if this cache has a weight limit.
     *
     * @return true if maximumWeight is greater than 0
     * @since 1.0.0
     */
    public boolean hasWeightLimit() {
        return maximumWeight > 0;
    }

    /**
     * Checks if this cache has any expiration configured.
     *
     * @return true if any expiration is configured
     * @since 1.0.0
     */
    public boolean hasExpiration() {
        return expireAfterAccess != null || expireAfterWrite != null;
    }

    /**
     * Checks if this cache has refresh configured.
     *
     * @return true if refresh is configured
     * @since 1.0.0
     */
    public boolean hasRefresh() {
        return refreshAfterWrite != null;
    }

    /**
     * Builder for {@link CacheConfig}.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String name;
        private long maximumSize = 0;
        private long maximumWeight = 0;
        private Duration expireAfterAccess;
        private Duration expireAfterWrite;
        private Duration refreshAfterWrite;
        private boolean weakKeys = false;
        private boolean weakValues = false;
        private boolean softValues = false;
        private boolean recordStats = false;

        private Builder() {}

        /**
         * Sets the cache name.
         *
         * @param name the cache name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the maximum number of entries in the cache.
         *
         * @param maximumSize the maximum size (0 = unlimited)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        /**
         * Sets the maximum total weight of entries in the cache.
         *
         * @param maximumWeight the maximum weight (0 = use size)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maximumWeight(long maximumWeight) {
            this.maximumWeight = maximumWeight;
            return this;
        }

        /**
         * Sets the duration after last access before an entry expires.
         *
         * @param duration the expiration duration (null = no expiration)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expireAfterAccess(@Nullable Duration duration) {
            this.expireAfterAccess = duration;
            return this;
        }

        /**
         * Sets the duration after write before an entry expires.
         *
         * @param duration the expiration duration (null = no expiration)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder expireAfterWrite(@Nullable Duration duration) {
            this.expireAfterWrite = duration;
            return this;
        }

        /**
         * Sets the duration after write before an entry is refreshed.
         *
         * <p>Refresh happens asynchronously in the background while the
         * old value is still returned to callers.
         *
         * @param duration the refresh duration (null = no refresh)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder refreshAfterWrite(@Nullable Duration duration) {
            this.refreshAfterWrite = duration;
            return this;
        }

        /**
         * Enables weak references for cache keys.
         *
         * <p>When enabled, keys are held with weak references, allowing
         * them to be garbage collected when no longer strongly referenced.
         *
         * @param weakKeys true to use weak keys
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder weakKeys(boolean weakKeys) {
            this.weakKeys = weakKeys;
            return this;
        }

        /**
         * Enables weak references for cache values.
         *
         * <p>When enabled, values are held with weak references, allowing
         * them to be garbage collected when no longer strongly referenced.
         * Cannot be used with softValues.
         *
         * @param weakValues true to use weak values
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder weakValues(boolean weakValues) {
            this.weakValues = weakValues;
            return this;
        }

        /**
         * Enables soft references for cache values.
         *
         * <p>When enabled, values are held with soft references, allowing
         * them to be garbage collected under memory pressure. Cannot be
         * used with weakValues.
         *
         * @param softValues true to use soft values
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder softValues(boolean softValues) {
            this.softValues = softValues;
            return this;
        }

        /**
         * Enables recording of cache statistics.
         *
         * <p>When enabled, the cache tracks hit/miss counts, load times,
         * eviction counts, and other metrics accessible via {@link CacheStats}.
         *
         * @param recordStats true to record statistics
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        /**
         * Builds the CacheConfig instance.
         *
         * @return a new CacheConfig
         * @throws IllegalStateException if name is not set
         * @since 1.0.0
         */
        @NotNull
        public CacheConfig build() {
            if (name == null) {
                throw new IllegalStateException("name is required");
            }
            return new CacheConfig(
                    name,
                    maximumSize,
                    maximumWeight,
                    expireAfterAccess,
                    expireAfterWrite,
                    refreshAfterWrite,
                    weakKeys,
                    weakValues,
                    softValues,
                    recordStats
            );
        }
    }
}
