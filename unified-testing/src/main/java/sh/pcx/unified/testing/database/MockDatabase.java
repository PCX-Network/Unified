/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory mock database for testing database operations.
 *
 * <p>MockDatabase provides a simple in-memory storage that mimics database
 * operations without requiring an actual database connection. This allows
 * tests to run quickly and in isolation.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>In-memory storage by entity type</li>
 *   <li>CRUD operations</li>
 *   <li>Query builder for filtering</li>
 *   <li>Automatic ID generation</li>
 *   <li>Transaction support (simulated)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockDatabase db = server.getMockDatabase();
 *
 * // Store data
 * PlayerData data = new PlayerData(player.getUniqueId(), 100);
 * db.save(data);
 *
 * // Query data
 * Optional<PlayerData> found = db.query(PlayerData.class)
 *     .where("uuid", player.getUniqueId())
 *     .findFirst();
 *
 * assertThat(found).isPresent();
 * assertThat(found.get().getKills()).isEqualTo(100);
 *
 * // Update data
 * found.get().setKills(150);
 * db.save(found.get());
 *
 * // Delete data
 * db.delete(PlayerData.class, player.getUniqueId());
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MockDatabase {

    private final Map<Class<?>, Map<Object, Object>> storage = new ConcurrentHashMap<>();
    private final Map<Class<?>, IdExtractor<?>> idExtractors = new ConcurrentHashMap<>();
    private boolean transactionActive = false;
    private final Map<Class<?>, Map<Object, Object>> transactionBackup = new ConcurrentHashMap<>();

    /**
     * Creates a new mock database.
     */
    public MockDatabase() {
    }

    /**
     * Registers an ID extractor for a specific entity type.
     *
     * <p>The ID extractor is used to automatically determine the primary key
     * of entities when saving.
     *
     * @param <T>       the entity type
     * @param type      the entity class
     * @param extractor the ID extractor function
     */
    public <T> void registerIdExtractor(
        @NotNull Class<T> type,
        @NotNull IdExtractor<T> extractor
    ) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(extractor, "extractor cannot be null");
        idExtractors.put(type, extractor);
    }

    /**
     * Saves an entity to the database.
     *
     * @param <T>    the entity type
     * @param entity the entity to save
     * @return the saved entity
     */
    @NotNull
    public <T> T save(@NotNull T entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        Class<?> type = entity.getClass();
        Object id = extractId(entity);

        Map<Object, Object> typeStorage = storage.computeIfAbsent(type, k -> new ConcurrentHashMap<>());
        typeStorage.put(id, entity);

        return entity;
    }

    /**
     * Saves multiple entities to the database.
     *
     * @param <T>      the entity type
     * @param entities the entities to save
     * @return the saved entities
     */
    @NotNull
    @SafeVarargs
    public final <T> List<T> saveAll(@NotNull T... entities) {
        List<T> saved = new ArrayList<>();
        for (T entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    /**
     * Finds an entity by its ID.
     *
     * @param <T>  the entity type
     * @param type the entity class
     * @param id   the entity ID
     * @return an Optional containing the entity if found
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findById(@NotNull Class<T> type, @NotNull Object id) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(id, "id cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        if (typeStorage == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) typeStorage.get(id));
    }

    /**
     * Finds all entities of a type.
     *
     * @param <T>  the entity type
     * @param type the entity class
     * @return list of all entities
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        if (typeStorage == null) {
            return Collections.emptyList();
        }
        return (List<T>) new ArrayList<>(typeStorage.values());
    }

    /**
     * Creates a query builder for a specific entity type.
     *
     * @param <T>  the entity type
     * @param type the entity class
     * @return a new query builder
     */
    @NotNull
    public <T> Query<T> query(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return new Query<>(this, type);
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param <T>  the entity type
     * @param type the entity class
     * @param id   the entity ID
     * @return true if the entity was deleted
     */
    public <T> boolean delete(@NotNull Class<T> type, @NotNull Object id) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(id, "id cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        if (typeStorage == null) {
            return false;
        }
        return typeStorage.remove(id) != null;
    }

    /**
     * Deletes an entity.
     *
     * @param <T>    the entity type
     * @param entity the entity to delete
     * @return true if the entity was deleted
     */
    public <T> boolean delete(@NotNull T entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        Object id = extractId(entity);
        return delete(entity.getClass(), id);
    }

    /**
     * Deletes all entities of a type.
     *
     * @param type the entity class
     * @return the number of deleted entities
     */
    public int deleteAll(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        if (typeStorage == null) {
            return 0;
        }
        int count = typeStorage.size();
        typeStorage.clear();
        return count;
    }

    /**
     * Counts entities of a type.
     *
     * @param type the entity class
     * @return the count
     */
    public long count(@NotNull Class<?> type) {
        Objects.requireNonNull(type, "type cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        return typeStorage != null ? typeStorage.size() : 0;
    }

    /**
     * Checks if an entity exists by its ID.
     *
     * @param type the entity class
     * @param id   the entity ID
     * @return true if the entity exists
     */
    public boolean exists(@NotNull Class<?> type, @NotNull Object id) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(id, "id cannot be null");

        Map<Object, Object> typeStorage = storage.get(type);
        return typeStorage != null && typeStorage.containsKey(id);
    }

    // ==================== Transaction Support ====================

    /**
     * Begins a transaction.
     *
     * <p>In the mock database, transactions create a backup that can be
     * rolled back. This is simulated behavior for testing.
     */
    public void beginTransaction() {
        if (transactionActive) {
            throw new IllegalStateException("Transaction already active");
        }
        transactionActive = true;

        // Backup current state
        transactionBackup.clear();
        storage.forEach((type, map) -> {
            transactionBackup.put(type, new ConcurrentHashMap<>(map));
        });
    }

    /**
     * Commits the current transaction.
     */
    public void commit() {
        if (!transactionActive) {
            throw new IllegalStateException("No active transaction");
        }
        transactionActive = false;
        transactionBackup.clear();
    }

    /**
     * Rolls back the current transaction.
     */
    public void rollback() {
        if (!transactionActive) {
            throw new IllegalStateException("No active transaction");
        }

        // Restore from backup
        storage.clear();
        transactionBackup.forEach((type, map) -> {
            storage.put(type, new ConcurrentHashMap<>(map));
        });

        transactionActive = false;
        transactionBackup.clear();
    }

    /**
     * Checks if a transaction is active.
     *
     * @return true if a transaction is active
     */
    public boolean isTransactionActive() {
        return transactionActive;
    }

    // ==================== Utility Methods ====================

    /**
     * Resets the database, clearing all data.
     */
    public void reset() {
        storage.clear();
        transactionBackup.clear();
        transactionActive = false;
    }

    /**
     * Returns the total number of stored entities across all types.
     *
     * @return the total count
     */
    public long totalCount() {
        return storage.values().stream()
            .mapToLong(Map::size)
            .sum();
    }

    @SuppressWarnings("unchecked")
    private <T> Object extractId(T entity) {
        IdExtractor<T> extractor = (IdExtractor<T>) idExtractors.get(entity.getClass());
        if (extractor != null) {
            return extractor.extractId(entity);
        }

        // Try to find getId() method via reflection
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("getId");
            return method.invoke(entity);
        } catch (Exception e) {
            // Try getUniqueId()
            try {
                java.lang.reflect.Method method = entity.getClass().getMethod("getUniqueId");
                return method.invoke(entity);
            } catch (Exception e2) {
                // Use hashCode as fallback
                return System.identityHashCode(entity);
            }
        }
    }

    /**
     * Functional interface for extracting IDs from entities.
     *
     * @param <T> the entity type
     */
    @FunctionalInterface
    public interface IdExtractor<T> {
        /**
         * Extracts the ID from an entity.
         *
         * @param entity the entity
         * @return the ID
         */
        Object extractId(@NotNull T entity);
    }

    /**
     * Query builder for filtering database results.
     *
     * @param <T> the entity type
     */
    public static final class Query<T> {
        private final MockDatabase database;
        private final Class<T> type;
        private final List<Predicate<T>> filters = new ArrayList<>();

        Query(MockDatabase database, Class<T> type) {
            this.database = database;
            this.type = type;
        }

        /**
         * Adds a filter condition using a field name and expected value.
         *
         * <p>Uses reflection to access the field.
         *
         * @param fieldName the field name
         * @param value     the expected value
         * @return this query
         */
        @NotNull
        public Query<T> where(@NotNull String fieldName, @Nullable Object value) {
            Objects.requireNonNull(fieldName, "fieldName cannot be null");

            filters.add(entity -> {
                try {
                    // Try getter method first
                    String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    java.lang.reflect.Method getter = type.getMethod(getterName);
                    Object fieldValue = getter.invoke(entity);
                    return Objects.equals(fieldValue, value);
                } catch (Exception e) {
                    // Try direct field access
                    try {
                        java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object fieldValue = field.get(entity);
                        return Objects.equals(fieldValue, value);
                    } catch (Exception e2) {
                        return false;
                    }
                }
            });
            return this;
        }

        /**
         * Adds a filter condition using a predicate.
         *
         * @param predicate the filter predicate
         * @return this query
         */
        @NotNull
        public Query<T> filter(@NotNull Predicate<T> predicate) {
            Objects.requireNonNull(predicate, "predicate cannot be null");
            filters.add(predicate);
            return this;
        }

        /**
         * Finds the first matching entity.
         *
         * @return an Optional containing the first match
         */
        @NotNull
        public Optional<T> findFirst() {
            return database.findAll(type).stream()
                .filter(this::matchesAllFilters)
                .findFirst();
        }

        /**
         * Finds all matching entities.
         *
         * @return list of matching entities
         */
        @NotNull
        public List<T> findAll() {
            return database.findAll(type).stream()
                .filter(this::matchesAllFilters)
                .collect(Collectors.toList());
        }

        /**
         * Counts matching entities.
         *
         * @return the count
         */
        public long count() {
            return database.findAll(type).stream()
                .filter(this::matchesAllFilters)
                .count();
        }

        /**
         * Checks if any entity matches.
         *
         * @return true if at least one match exists
         */
        public boolean exists() {
            return database.findAll(type).stream()
                .anyMatch(this::matchesAllFilters);
        }

        /**
         * Deletes all matching entities.
         *
         * @return the number of deleted entities
         */
        public int deleteAll() {
            List<T> toDelete = findAll();
            toDelete.forEach(database::delete);
            return toDelete.size();
        }

        private boolean matchesAllFilters(T entity) {
            for (Predicate<T> filter : filters) {
                if (!filter.test(entity)) {
                    return false;
                }
            }
            return true;
        }
    }
}
