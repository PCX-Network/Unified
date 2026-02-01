/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * MongoDB integration for the Unified Plugin API.
 *
 * <p>This package provides a comprehensive, async-first MongoDB driver wrapper
 * using MongoDB Driver 5.5.0 with full support for:
 *
 * <ul>
 *   <li><b>Async Operations</b> - All operations return CompletableFuture</li>
 *   <li><b>Custom Codecs</b> - Type-safe serialization for Minecraft types</li>
 *   <li><b>Change Streams</b> - Real-time data synchronization</li>
 *   <li><b>GridFS</b> - Large file storage (skins, schematics, backups)</li>
 *   <li><b>Aggregation</b> - Complex queries and analytics</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Configure and connect
 * MongoConfig config = MongoConfig.builder()
 *     .uri("mongodb://localhost:27017")
 *     .database("myPlugin")
 *     .applicationName("MyMinecraftPlugin")
 *     .build();
 *
 * // Get the service (typically via dependency injection)
 * @Inject
 * private MongoService mongoService;
 *
 * // Insert a document
 * Document player = new Document("uuid", uuid.toString())
 *     .append("name", "Steve")
 *     .append("balance", 1000.0);
 *
 * mongoService.insertOne("players", player)
 *     .thenAccept(result -> logger.info("Inserted: " + result.getInsertedId()));
 *
 * // Query with type-safe wrapper
 * MongoCollectionWrapper<PlayerData> players =
 *     mongoService.getCollection("players", PlayerData.class);
 *
 * players.findOne(Filters.eq("uuid", uuid.toString()))
 *     .thenAccept(opt -> opt.ifPresent(p -> logger.info("Found: " + p)));
 * }</pre>
 *
 * <h2>Package Structure</h2>
 *
 * <h3>Core Classes</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoConnection} - Connection management</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoConfig} - Configuration record</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoCollectionWrapper} - Type-safe collection wrapper</li>
 * </ul>
 *
 * <h3>Client Management</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoClientProvider} - Client factory</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoClientManager} - Client lifecycle</li>
 *   <li>{@link sh.pcx.unified.data.mongo.CodecRegistryBuilder} - Custom codec registration</li>
 * </ul>
 *
 * <h3>Query Builders</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoQuery} - Fluent find query builder</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoUpdate} - Fluent update builder</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoAggregation} - Aggregation pipeline builder</li>
 *   <li>{@link sh.pcx.unified.data.mongo.MongoRepository} - Base repository pattern</li>
 * </ul>
 *
 * <h3>Real-time Features</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.mongo.ChangeStreamListener} - Change event listener</li>
 *   <li>{@link sh.pcx.unified.data.mongo.ChangeStreamManager} - Subscription management</li>
 *   <li>{@link sh.pcx.unified.data.mongo.GridFSService} - Large file storage</li>
 * </ul>
 *
 * <h3>Custom Codecs</h3>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.mongo.UUIDCodec} - UUID serialization</li>
 *   <li>{@link sh.pcx.unified.data.mongo.LocationCodec} - UnifiedLocation serialization</li>
 *   <li>{@link sh.pcx.unified.data.mongo.ItemStackCodec} - ItemStack serialization</li>
 *   <li>{@link sh.pcx.unified.data.mongo.ComponentCodec} - Adventure Component serialization</li>
 * </ul>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>MongoDB 5.0+ (for change streams: replica set or sharded cluster)</li>
 *   <li>MongoDB Java Driver 5.5.0 (Reactive Streams)</li>
 *   <li>Java 21+</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All service classes are thread-safe. Query builders and update builders
 * are NOT thread-safe and should be created fresh for each operation.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.mongo;
