/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A type-safe wrapper for cache keys providing consistent hashing and equality.
 *
 * <p>CacheKey wraps any key object and provides additional metadata such as
 * the key namespace/region, ensuring proper key isolation between different
 * cache regions or plugins.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple cache key
 * CacheKey<UUID> playerKey = CacheKey.of(playerUuid);
 *
 * // Create a namespaced cache key
 * CacheKey<UUID> playerKey = CacheKey.of("players", playerUuid);
 *
 * // Create a composite key
 * CacheKey<String> compositeKey = CacheKey.composite("player", uuid, "inventory");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>CacheKey is immutable and thread-safe.
 *
 * @param <K> the underlying key type
 * @since 1.0.0
 * @author Supatuck
 * @see CacheService
 */
public final class CacheKey<K> {

    private static final String DEFAULT_NAMESPACE = "default";

    private final String namespace;
    private final K key;
    private final int hashCode;

    /**
     * Creates a new cache key with the specified namespace and key.
     *
     * @param namespace the namespace for key isolation
     * @param key       the underlying key value
     */
    private CacheKey(@NotNull String namespace, @NotNull K key) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.hashCode = Objects.hash(namespace, key);
    }

    /**
     * Creates a cache key with the default namespace.
     *
     * @param <K> the key type
     * @param key the underlying key value
     * @return a new CacheKey instance
     * @throws NullPointerException if key is null
     * @since 1.0.0
     */
    @NotNull
    public static <K> CacheKey<K> of(@NotNull K key) {
        return new CacheKey<>(DEFAULT_NAMESPACE, key);
    }

    /**
     * Creates a cache key with a specific namespace.
     *
     * @param <K>       the key type
     * @param namespace the namespace for key isolation
     * @param key       the underlying key value
     * @return a new CacheKey instance
     * @throws NullPointerException if namespace or key is null
     * @since 1.0.0
     */
    @NotNull
    public static <K> CacheKey<K> of(@NotNull String namespace, @NotNull K key) {
        return new CacheKey<>(namespace, key);
    }

    /**
     * Creates a composite cache key from multiple components.
     *
     * <p>The components are joined with a separator to form a single string key.
     * This is useful for creating hierarchical cache keys.
     *
     * @param components the key components
     * @return a new CacheKey with the composite string as the key
     * @throws IllegalArgumentException if no components are provided
     * @since 1.0.0
     */
    @NotNull
    public static CacheKey<String> composite(@NotNull Object... components) {
        if (components == null || components.length == 0) {
            throw new IllegalArgumentException("At least one component is required");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(components[i]);
        }
        return new CacheKey<>(DEFAULT_NAMESPACE, sb.toString());
    }

    /**
     * Creates a composite cache key with a specific namespace.
     *
     * @param namespace  the namespace for key isolation
     * @param components the key components
     * @return a new CacheKey with the composite string as the key
     * @throws IllegalArgumentException if no components are provided
     * @since 1.0.0
     */
    @NotNull
    public static CacheKey<String> composite(@NotNull String namespace, @NotNull Object... components) {
        if (components == null || components.length == 0) {
            throw new IllegalArgumentException("At least one component is required");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(components[i]);
        }
        return new CacheKey<>(namespace, sb.toString());
    }

    /**
     * Returns the namespace of this cache key.
     *
     * @return the namespace
     * @since 1.0.0
     */
    @NotNull
    public String namespace() {
        return namespace;
    }

    /**
     * Returns the underlying key value.
     *
     * @return the key value
     * @since 1.0.0
     */
    @NotNull
    public K key() {
        return key;
    }

    /**
     * Returns a string representation suitable for use in distributed caches.
     *
     * @return the serialized key string
     * @since 1.0.0
     */
    @NotNull
    public String toSerializedString() {
        return namespace + ":" + key.toString();
    }

    /**
     * Creates a new cache key in a different namespace.
     *
     * @param newNamespace the new namespace
     * @return a new CacheKey with the same key but different namespace
     * @since 1.0.0
     */
    @NotNull
    public CacheKey<K> withNamespace(@NotNull String newNamespace) {
        return new CacheKey<>(newNamespace, key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CacheKey<?> other)) {
            return false;
        }
        return namespace.equals(other.namespace) && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "CacheKey{namespace='" + namespace + "', key=" + key + "}";
    }
}
