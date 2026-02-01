/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper for MongoDB client connection providing async operations.
 *
 * <p>This class wraps the MongoDB Reactive Streams driver and provides
 * CompletableFuture-based async operations for seamless integration
 * with Java's async patterns.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MongoConfig config = MongoConfig.builder()
 *     .uri("mongodb://localhost:27017")
 *     .database("myDatabase")
 *     .build();
 *
 * try (MongoConnection connection = new MongoConnection(config)) {
 *     connection.connect().join();
 *
 *     // Access the database
 *     MongoDatabase database = connection.getDatabase();
 *
 *     // Get a collection
 *     MongoCollection<Document> collection = connection.getCollection("players");
 *
 *     // Ping the server
 *     connection.ping().join();
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The underlying MongoClient handles
 * connection pooling and thread safety internally.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoConfig
 * @see MongoService
 */
public class MongoConnection implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConnection.class);

    private final MongoConfig config;
    private final CodecRegistry codecRegistry;
    private final AtomicReference<MongoClient> clientRef = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * Creates a new MongoDB connection with the given configuration.
     *
     * @param config the MongoDB configuration
     * @since 1.0.0
     */
    public MongoConnection(@NotNull MongoConfig config) {
        this(config, null);
    }

    /**
     * Creates a new MongoDB connection with custom codec registry.
     *
     * @param config        the MongoDB configuration
     * @param codecRegistry the custom codec registry (null for default)
     * @since 1.0.0
     */
    public MongoConnection(@NotNull MongoConfig config, @Nullable CodecRegistry codecRegistry) {
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.codecRegistry = codecRegistry;
    }

    /**
     * Establishes the MongoDB connection.
     *
     * @return a future completing when the connection is established
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> connect() {
        if (connected.get()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ConnectionString connectionString = new ConnectionString(config.uri());

                MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .serverApi(ServerApi.builder()
                                .version(ServerApiVersion.V1)
                                .build())
                        .applyToSocketSettings(builder -> {
                            builder.connectTimeout((int) config.connectTimeoutMillis(), TimeUnit.MILLISECONDS);
                            if (config.socketTimeoutMillis() > 0) {
                                builder.readTimeout((int) config.socketTimeoutMillis(), TimeUnit.MILLISECONDS);
                            }
                        })
                        .applyToClusterSettings(builder -> {
                            builder.serverSelectionTimeout(
                                    config.serverSelectionTimeoutMillis(),
                                    TimeUnit.MILLISECONDS
                            );
                        })
                        .applyToConnectionPoolSettings(builder -> {
                            builder.maxSize(config.maxPoolSize());
                            builder.minSize(config.minPoolSize());
                            if (config.maxIdleTimeMillis() > 0) {
                                builder.maxConnectionIdleTime(
                                        config.maxIdleTimeMillis(),
                                        TimeUnit.MILLISECONDS
                                );
                            }
                            builder.maxWaitTime(config.maxWaitTimeMillis(), TimeUnit.MILLISECONDS);
                        })
                        .retryWrites(config.retryWrites())
                        .retryReads(config.retryReads());

                // Apply application name if set
                config.getApplicationName().ifPresent(settingsBuilder::applicationName);

                // Apply custom codec registry if provided
                if (codecRegistry != null) {
                    settingsBuilder.codecRegistry(codecRegistry);
                }

                MongoClient client = MongoClients.create(settingsBuilder.build());
                clientRef.set(client);
                connected.set(true);

                LOGGER.info("Connected to MongoDB: {}", config.database());
                return null;
            } catch (Exception e) {
                LOGGER.error("Failed to connect to MongoDB", e);
                throw new RuntimeException("Failed to connect to MongoDB", e);
            }
        });
    }

    /**
     * Pings the MongoDB server to verify connectivity.
     *
     * @return a future completing when the ping succeeds
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> ping() {
        ensureConnected();
        return toCompletableFuture(
                getDatabase().runCommand(new Document("ping", 1))
        ).thenApply(doc -> null);
    }

    /**
     * Checks if the connection is healthy.
     *
     * @return a future completing with true if connected and healthy
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> isHealthy() {
        if (!connected.get()) {
            return CompletableFuture.completedFuture(false);
        }

        return ping()
                .thenApply(v -> true)
                .exceptionally(e -> {
                    LOGGER.warn("MongoDB health check failed", e);
                    return false;
                });
    }

    /**
     * Returns the underlying MongoDB client.
     *
     * @return the MongoDB client
     * @throws IllegalStateException if not connected
     * @since 1.0.0
     */
    @NotNull
    public MongoClient getClient() {
        ensureConnected();
        return clientRef.get();
    }

    /**
     * Returns the MongoDB database.
     *
     * @return the MongoDB database
     * @throws IllegalStateException if not connected
     * @since 1.0.0
     */
    @NotNull
    public MongoDatabase getDatabase() {
        ensureConnected();
        MongoDatabase database = clientRef.get().getDatabase(config.database());
        if (codecRegistry != null) {
            return database.withCodecRegistry(codecRegistry);
        }
        return database;
    }

    /**
     * Returns a MongoDB database by name.
     *
     * @param databaseName the database name
     * @return the MongoDB database
     * @throws IllegalStateException if not connected
     * @since 1.0.0
     */
    @NotNull
    public MongoDatabase getDatabase(@NotNull String databaseName) {
        ensureConnected();
        MongoDatabase database = clientRef.get().getDatabase(databaseName);
        if (codecRegistry != null) {
            return database.withCodecRegistry(codecRegistry);
        }
        return database;
    }

    /**
     * Returns a collection from the default database.
     *
     * @param collectionName the collection name
     * @return the MongoDB collection
     * @throws IllegalStateException if not connected
     * @since 1.0.0
     */
    @NotNull
    public MongoCollection<Document> getCollection(@NotNull String collectionName) {
        return getDatabase().getCollection(collectionName);
    }

    /**
     * Returns a typed collection from the default database.
     *
     * @param collectionName the collection name
     * @param documentClass  the document class
     * @param <T>            the document type
     * @return the typed MongoDB collection
     * @throws IllegalStateException if not connected
     * @since 1.0.0
     */
    @NotNull
    public <T> MongoCollection<T> getCollection(@NotNull String collectionName, @NotNull Class<T> documentClass) {
        return getDatabase().getCollection(collectionName, documentClass);
    }

    /**
     * Lists all database names.
     *
     * @return a future completing with the list of database names
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<String>> listDatabaseNames() {
        ensureConnected();
        return toCompletableFutureList(clientRef.get().listDatabaseNames());
    }

    /**
     * Lists all collection names in the default database.
     *
     * @return a future completing with the list of collection names
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<List<String>> listCollectionNames() {
        ensureConnected();
        return toCompletableFutureList(getDatabase().listCollectionNames());
    }

    /**
     * Checks if connected to MongoDB.
     *
     * @return true if connected
     * @since 1.0.0
     */
    public boolean isConnected() {
        return connected.get() && clientRef.get() != null;
    }

    /**
     * Disconnects from MongoDB.
     *
     * @since 1.0.0
     */
    public void disconnect() {
        MongoClient client = clientRef.getAndSet(null);
        if (client != null) {
            try {
                client.close();
                connected.set(false);
                LOGGER.info("Disconnected from MongoDB");
            } catch (Exception e) {
                LOGGER.error("Error disconnecting from MongoDB", e);
            }
        }
    }

    /**
     * Closes the MongoDB connection.
     *
     * @since 1.0.0
     */
    @Override
    public void close() {
        disconnect();
    }

    /**
     * Returns the current configuration.
     *
     * @return the MongoDB configuration
     * @since 1.0.0
     */
    @NotNull
    public MongoConfig getConfig() {
        return config;
    }

    /**
     * Returns the codec registry.
     *
     * @return the codec registry, or null if using default
     * @since 1.0.0
     */
    @Nullable
    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    private void ensureConnected() {
        if (!connected.get() || clientRef.get() == null) {
            throw new IllegalStateException("Not connected to MongoDB. Call connect() first.");
        }
    }

    /**
     * Converts a Publisher to a CompletableFuture for a single result.
     *
     * @param publisher the publisher
     * @param <T>       the result type
     * @return a future completing with the first result
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompletableFuture<T> toCompletableFuture(@NotNull Publisher<T> publisher) {
        CompletableFuture<T> future = new CompletableFuture<>();

        publisher.subscribe(new Subscriber<T>() {
            private T result;
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(T t) {
                this.result = t;
                subscription.cancel();
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                future.complete(result);
            }
        });

        return future;
    }

    /**
     * Converts a Publisher to a CompletableFuture for a list of results.
     *
     * @param publisher the publisher
     * @param <T>       the result type
     * @return a future completing with all results
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompletableFuture<List<T>> toCompletableFutureList(@NotNull Publisher<T> publisher) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        List<T> results = new ArrayList<>();

        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                synchronized (results) {
                    results.add(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                future.complete(results);
            }
        });

        return future;
    }

    /**
     * Converts a Publisher to a CompletableFuture for void operations.
     *
     * @param publisher the publisher
     * @return a future completing when the operation is done
     * @since 1.0.0
     */
    @NotNull
    public static CompletableFuture<Void> toCompletableFutureVoid(@NotNull Publisher<?> publisher) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        publisher.subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object o) {
                // Ignore results
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                future.complete(null);
            }
        });

        return future;
    }
}
