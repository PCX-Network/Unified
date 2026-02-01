/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Provider for creating configured MongoDB client instances.
 *
 * <p>This class provides factory methods for creating MongoDB clients with
 * proper configuration, including connection pooling, timeouts, codec registry,
 * and authentication.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple client
 * MongoConfig config = new MongoConfig("mongodb://localhost:27017", "myDatabase");
 * MongoClient client = MongoClientProvider.createClient(config);
 *
 * // Create a client with custom codecs
 * CodecRegistry customRegistry = CodecRegistryBuilder.create()
 *     .addCodec(new UUIDCodec())
 *     .addCodec(new LocationCodec())
 *     .build();
 *
 * MongoClient client = MongoClientProvider.createClient(config, customRegistry);
 *
 * // Create a client with POJO support
 * MongoClient client = MongoClientProvider.createClientWithPojoSupport(config);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All methods are stateless factory methods.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MongoConfig
 * @see MongoClientManager
 * @see CodecRegistryBuilder
 */
public final class MongoClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientProvider.class);

    private MongoClientProvider() {
        // Utility class
    }

    /**
     * Creates a MongoDB client with the given configuration.
     *
     * @param config the MongoDB configuration
     * @return a new MongoDB client
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClient(@NotNull MongoConfig config) {
        return createClient(config, null);
    }

    /**
     * Creates a MongoDB client with custom codec registry.
     *
     * @param config        the MongoDB configuration
     * @param codecRegistry the custom codec registry (null for default)
     * @return a new MongoDB client
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClient(
            @NotNull MongoConfig config,
            @Nullable CodecRegistry codecRegistry
    ) {
        Objects.requireNonNull(config, "Config cannot be null");

        MongoClientSettings.Builder builder = createBaseSettings(config);

        if (codecRegistry != null) {
            builder.codecRegistry(codecRegistry);
        }

        MongoClientSettings settings = builder.build();
        LOGGER.info("Creating MongoDB client for database: {}", config.database());

        return MongoClients.create(settings);
    }

    /**
     * Creates a MongoDB client with POJO codec support.
     *
     * <p>This client can automatically serialize and deserialize POJOs
     * without requiring manual codec implementation for simple types.
     *
     * @param config the MongoDB configuration
     * @return a new MongoDB client with POJO support
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClientWithPojoSupport(@NotNull MongoConfig config) {
        return createClientWithPojoSupport(config, null);
    }

    /**
     * Creates a MongoDB client with POJO codec support and custom codecs.
     *
     * @param config              the MongoDB configuration
     * @param additionalCodecs    additional codec registry to include (null for none)
     * @return a new MongoDB client with POJO support
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClientWithPojoSupport(
            @NotNull MongoConfig config,
            @Nullable CodecRegistry additionalCodecs
    ) {
        Objects.requireNonNull(config, "Config cannot be null");

        // Create POJO codec provider
        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder()
                .automatic(true)
                .build();

        // Build codec registry with POJO support
        CodecRegistry pojoRegistry = CodecRegistries.fromProviders(pojoCodecProvider);
        CodecRegistry defaultRegistry = MongoClientSettings.getDefaultCodecRegistry();

        CodecRegistry finalRegistry;
        if (additionalCodecs != null) {
            finalRegistry = CodecRegistries.fromRegistries(
                    defaultRegistry,
                    additionalCodecs,
                    pojoRegistry
            );
        } else {
            finalRegistry = CodecRegistries.fromRegistries(defaultRegistry, pojoRegistry);
        }

        return createClient(config, finalRegistry);
    }

    /**
     * Creates a MongoDB client with explicit credentials.
     *
     * @param config      the MongoDB configuration
     * @param credentials the MongoDB credentials
     * @return a new MongoDB client with authentication
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClientWithCredentials(
            @NotNull MongoConfig config,
            @NotNull MongoCredential credentials
    ) {
        return createClientWithCredentials(config, credentials, null);
    }

    /**
     * Creates a MongoDB client with explicit credentials and custom codecs.
     *
     * @param config        the MongoDB configuration
     * @param credentials   the MongoDB credentials
     * @param codecRegistry the custom codec registry (null for default)
     * @return a new MongoDB client with authentication
     * @since 1.0.0
     */
    @NotNull
    public static MongoClient createClientWithCredentials(
            @NotNull MongoConfig config,
            @NotNull MongoCredential credentials,
            @Nullable CodecRegistry codecRegistry
    ) {
        Objects.requireNonNull(config, "Config cannot be null");
        Objects.requireNonNull(credentials, "Credentials cannot be null");

        MongoClientSettings.Builder builder = createBaseSettings(config)
                .credential(credentials);

        if (codecRegistry != null) {
            builder.codecRegistry(codecRegistry);
        }

        LOGGER.info("Creating authenticated MongoDB client for database: {}", config.database());
        return MongoClients.create(builder.build());
    }

    /**
     * Creates MongoDB credentials for username/password authentication.
     *
     * @param username   the username
     * @param password   the password
     * @param authSource the authentication database (usually "admin")
     * @return the MongoDB credentials
     * @since 1.0.0
     */
    @NotNull
    public static MongoCredential createCredentials(
            @NotNull String username,
            @NotNull String password,
            @NotNull String authSource
    ) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(authSource, "Auth source cannot be null");

        return MongoCredential.createCredential(
                username,
                authSource,
                password.toCharArray()
        );
    }

    /**
     * Creates MongoDB credentials using SCRAM-SHA-256 authentication.
     *
     * @param username   the username
     * @param password   the password
     * @param authSource the authentication database
     * @return the MongoDB credentials
     * @since 1.0.0
     */
    @NotNull
    public static MongoCredential createScramSha256Credentials(
            @NotNull String username,
            @NotNull String password,
            @NotNull String authSource
    ) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        Objects.requireNonNull(authSource, "Auth source cannot be null");

        return MongoCredential.createScramSha256Credential(
                username,
                authSource,
                password.toCharArray()
        );
    }

    /**
     * Creates the base MongoDB client settings from configuration.
     *
     * @param config the MongoDB configuration
     * @return a settings builder with base configuration applied
     */
    @NotNull
    private static MongoClientSettings.Builder createBaseSettings(@NotNull MongoConfig config) {
        ConnectionString connectionString = new ConnectionString(config.uri());

        MongoClientSettings.Builder builder = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .serverApi(ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build())
                .retryWrites(config.retryWrites())
                .retryReads(config.retryReads());

        // Apply socket settings
        builder.applyToSocketSettings(socketBuilder -> {
            socketBuilder.connectTimeout((int) config.connectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (config.socketTimeoutMillis() > 0) {
                socketBuilder.readTimeout((int) config.socketTimeoutMillis(), TimeUnit.MILLISECONDS);
            }
        });

        // Apply cluster settings
        builder.applyToClusterSettings(clusterBuilder -> {
            clusterBuilder.serverSelectionTimeout(
                    config.serverSelectionTimeoutMillis(),
                    TimeUnit.MILLISECONDS
            );
        });

        // Apply connection pool settings
        builder.applyToConnectionPoolSettings(poolBuilder -> {
            poolBuilder.maxSize(config.maxPoolSize());
            poolBuilder.minSize(config.minPoolSize());
            if (config.maxIdleTimeMillis() > 0) {
                poolBuilder.maxConnectionIdleTime(config.maxIdleTimeMillis(), TimeUnit.MILLISECONDS);
            }
            poolBuilder.maxWaitTime(config.maxWaitTimeMillis(), TimeUnit.MILLISECONDS);
        });

        // Apply application name if set
        config.getApplicationName().ifPresent(builder::applicationName);

        return builder;
    }

    /**
     * Creates a codec registry combining the default registry with custom codecs.
     *
     * @param customCodecs the custom codec registry
     * @return a combined codec registry
     * @since 1.0.0
     */
    @NotNull
    public static CodecRegistry combineWithDefault(@NotNull CodecRegistry customCodecs) {
        Objects.requireNonNull(customCodecs, "Custom codecs cannot be null");
        return CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                customCodecs
        );
    }

    /**
     * Creates a codec registry combining multiple registries.
     *
     * @param registries the codec registries to combine
     * @return a combined codec registry
     * @since 1.0.0
     */
    @NotNull
    public static CodecRegistry combineRegistries(@NotNull CodecRegistry... registries) {
        Objects.requireNonNull(registries, "Registries cannot be null");
        return CodecRegistries.fromRegistries(registries);
    }
}
