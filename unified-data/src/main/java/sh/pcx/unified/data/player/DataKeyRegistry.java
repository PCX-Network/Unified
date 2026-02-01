/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing and tracking all registered {@link DataKey} instances.
 *
 * <p>The DataKeyRegistry provides a central location for registering, looking up,
 * and managing data keys. It ensures key uniqueness and provides query capabilities
 * for finding keys by namespace, type, or persistence settings.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get the registry
 * DataKeyRegistry registry = playerDataService.getKeyRegistry();
 *
 * // Register your plugin's keys
 * registry.register(MyPluginData.KILLS);
 * registry.register(MyPluginData.DEATHS);
 * registry.register(MyPluginData.BALANCE);
 *
 * // Or register all keys at once
 * registry.registerAll(
 *     MyPluginData.KILLS,
 *     MyPluginData.DEATHS,
 *     MyPluginData.BALANCE
 * );
 *
 * // Look up a key by name
 * Optional<DataKey<?>> key = registry.get("myplugin:kills");
 *
 * // Get all keys for a namespace
 * Set<DataKey<?>> myKeys = registry.getByNamespace("myplugin");
 *
 * // Get all persistent keys
 * Set<DataKey<?>> persistentKeys = registry.getPersistentKeys();
 * }</pre>
 *
 * <h2>Registration Benefits</h2>
 * <ul>
 *   <li>Key validation - ensures no duplicate keys</li>
 *   <li>Discovery - find all registered keys</li>
 *   <li>Schema generation - generate database schemas from registered keys</li>
 *   <li>Documentation - auto-generate data dictionary</li>
 *   <li>Admin tools - view and modify player data dynamically</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The registry is fully thread-safe. Keys can be registered and queried
 * from any thread without external synchronization.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataKey
 * @see PlayerDataService
 */
public final class DataKeyRegistry {

    private final Map<String, DataKey<?>> keys = new ConcurrentHashMap<>();
    private final Map<String, Set<DataKey<?>>> namespaceIndex = new ConcurrentHashMap<>();

    /**
     * Creates a new empty DataKeyRegistry.
     *
     * @since 1.0.0
     */
    public DataKeyRegistry() {
        // Default constructor
    }

    /**
     * Registers a data key with the registry.
     *
     * <p>Each key can only be registered once. Attempting to register a key
     * with the same identifier as an existing key will throw an exception.
     *
     * @param key the data key to register
     * @return this registry for method chaining
     * @throws IllegalArgumentException if a key with the same identifier is already registered
     * @throws NullPointerException     if key is null
     * @since 1.0.0
     */
    @NotNull
    public DataKeyRegistry register(@NotNull DataKey<?> key) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }

        DataKey<?> existing = keys.putIfAbsent(key.getKey(), key);
        if (existing != null && existing != key) {
            throw new IllegalArgumentException(
                    "A DataKey with identifier '" + key.getKey() + "' is already registered");
        }

        // Update namespace index
        String namespace = key.getNamespace();
        namespaceIndex.computeIfAbsent(namespace, k -> ConcurrentHashMap.newKeySet()).add(key);

        return this;
    }

    /**
     * Registers multiple data keys at once.
     *
     * @param keys the data keys to register
     * @return this registry for method chaining
     * @throws IllegalArgumentException if any key is already registered
     * @throws NullPointerException     if keys array or any key is null
     * @since 1.0.0
     */
    @NotNull
    public DataKeyRegistry registerAll(@NotNull DataKey<?>... keys) {
        if (keys == null) {
            throw new NullPointerException("keys must not be null");
        }
        for (DataKey<?> key : keys) {
            register(key);
        }
        return this;
    }

    /**
     * Registers multiple data keys from a collection.
     *
     * @param keys the data keys to register
     * @return this registry for method chaining
     * @throws IllegalArgumentException if any key is already registered
     * @throws NullPointerException     if keys collection or any key is null
     * @since 1.0.0
     */
    @NotNull
    public DataKeyRegistry registerAll(@NotNull Collection<DataKey<?>> keys) {
        if (keys == null) {
            throw new NullPointerException("keys must not be null");
        }
        for (DataKey<?> key : keys) {
            register(key);
        }
        return this;
    }

    /**
     * Unregisters a data key from the registry.
     *
     * <p>Note: Unregistering a key does not remove existing data stored with
     * that key from player profiles. It only removes the key from the registry.
     *
     * @param key the data key to unregister
     * @return true if the key was registered and has been removed
     * @since 1.0.0
     */
    public boolean unregister(@NotNull DataKey<?> key) {
        if (key == null) {
            return false;
        }

        boolean removed = keys.remove(key.getKey(), key);
        if (removed) {
            String namespace = key.getNamespace();
            Set<DataKey<?>> namespaceKeys = namespaceIndex.get(namespace);
            if (namespaceKeys != null) {
                namespaceKeys.remove(key);
                if (namespaceKeys.isEmpty()) {
                    namespaceIndex.remove(namespace);
                }
            }
        }
        return removed;
    }

    /**
     * Unregisters a data key by its identifier.
     *
     * @param keyId the key identifier
     * @return the unregistered key, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public DataKey<?> unregister(@NotNull String keyId) {
        DataKey<?> key = keys.remove(keyId);
        if (key != null) {
            String namespace = key.getNamespace();
            Set<DataKey<?>> namespaceKeys = namespaceIndex.get(namespace);
            if (namespaceKeys != null) {
                namespaceKeys.remove(key);
                if (namespaceKeys.isEmpty()) {
                    namespaceIndex.remove(namespace);
                }
            }
        }
        return key;
    }

    /**
     * Retrieves a data key by its identifier.
     *
     * @param keyId the key identifier
     * @return an Optional containing the key if found
     * @since 1.0.0
     */
    @NotNull
    public Optional<DataKey<?>> get(@NotNull String keyId) {
        return Optional.ofNullable(keys.get(keyId));
    }

    /**
     * Retrieves a typed data key by its identifier.
     *
     * <p>This method performs an unchecked cast. Use with caution.
     *
     * @param keyId the key identifier
     * @param type  the expected type class
     * @param <T>   the expected type
     * @return an Optional containing the typed key if found and type matches
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<DataKey<T>> get(@NotNull String keyId, @NotNull Class<T> type) {
        DataKey<?> key = keys.get(keyId);
        if (key != null && key.getType().equals(type)) {
            return Optional.of((DataKey<T>) key);
        }
        return Optional.empty();
    }

    /**
     * Checks if a key is registered.
     *
     * @param keyId the key identifier
     * @return true if the key is registered
     * @since 1.0.0
     */
    public boolean contains(@NotNull String keyId) {
        return keys.containsKey(keyId);
    }

    /**
     * Checks if a specific key instance is registered.
     *
     * @param key the data key
     * @return true if the exact key instance is registered
     * @since 1.0.0
     */
    public boolean contains(@NotNull DataKey<?> key) {
        if (key == null) {
            return false;
        }
        DataKey<?> registered = keys.get(key.getKey());
        return registered == key;
    }

    /**
     * Returns all registered data keys.
     *
     * @return an unmodifiable collection of all registered keys
     * @since 1.0.0
     */
    @NotNull
    public Collection<DataKey<?>> getAll() {
        return Collections.unmodifiableCollection(keys.values());
    }

    /**
     * Returns all registered key identifiers.
     *
     * @return an unmodifiable set of all key identifiers
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getKeyIds() {
        return Collections.unmodifiableSet(keys.keySet());
    }

    /**
     * Returns all data keys in a specific namespace.
     *
     * @param namespace the namespace to filter by
     * @return an unmodifiable set of keys in the namespace
     * @since 1.0.0
     */
    @NotNull
    public Set<DataKey<?>> getByNamespace(@NotNull String namespace) {
        Set<DataKey<?>> result = namespaceIndex.get(namespace);
        return result != null ? Collections.unmodifiableSet(result) : Set.of();
    }

    /**
     * Returns all unique namespaces that have registered keys.
     *
     * @return an unmodifiable set of all namespaces
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getNamespaces() {
        return Collections.unmodifiableSet(namespaceIndex.keySet());
    }

    /**
     * Returns all persistent data keys.
     *
     * <p>Persistent keys are those that are saved to and loaded from the database.
     *
     * @return an unmodifiable set of persistent keys
     * @since 1.0.0
     */
    @NotNull
    public Set<DataKey<?>> getPersistentKeys() {
        return keys.values().stream()
                .filter(DataKey::isPersistent)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns all transient (non-persistent) data keys.
     *
     * <p>Transient keys are stored in memory only and are not persisted.
     *
     * @return an unmodifiable set of transient keys
     * @since 1.0.0
     */
    @NotNull
    public Set<DataKey<?>> getTransientKeys() {
        return keys.values().stream()
                .filter(k -> !k.isPersistent())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns all data keys of a specific value type.
     *
     * @param type the value type to filter by
     * @param <T>  the value type
     * @return an unmodifiable set of keys with the specified type
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Set<DataKey<T>> getByType(@NotNull Class<T> type) {
        return keys.values().stream()
                .filter(k -> k.getType().equals(type))
                .map(k -> (DataKey<T>) k)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the number of registered keys.
     *
     * @return the number of registered keys
     * @since 1.0.0
     */
    public int size() {
        return keys.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no keys are registered
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    /**
     * Clears all registered keys from the registry.
     *
     * <p>Warning: This does not affect existing player data. Use with caution.
     *
     * @since 1.0.0
     */
    public void clear() {
        keys.clear();
        namespaceIndex.clear();
    }

    @Override
    public String toString() {
        return "DataKeyRegistry{" +
                "keyCount=" + keys.size() +
                ", namespaces=" + namespaceIndex.keySet() +
                '}';
    }
}
