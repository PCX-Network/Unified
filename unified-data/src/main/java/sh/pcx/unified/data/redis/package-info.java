/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Redis integration for the UnifiedPlugin API.
 *
 * <p>This package provides a unified Redis API that supports both Jedis and
 * Lettuce client implementations. It offers synchronous, asynchronous, and
 * reactive operations with built-in connection pooling, pub/sub messaging,
 * and Lua script execution.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.redis.RedisService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.data.redis.RedisConfig} - Configuration record</li>
 *   <li>{@link sh.pcx.unified.data.redis.RedisConnection} - Connection wrapper</li>
 *   <li>{@link sh.pcx.unified.data.redis.RedisClient} - Client type enum</li>
 *   <li>{@link sh.pcx.unified.data.redis.KeyNamespace} - Key prefixing utility</li>
 *   <li>{@link sh.pcx.unified.data.redis.RedisPubSub} - Pub/sub messaging</li>
 *   <li>{@link sh.pcx.unified.data.redis.RedisLuaScript} - Lua script execution</li>
 * </ul>
 *
 * <h2>Getting Started</h2>
 * <pre>{@code
 * // Configure Redis
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .password("secret")
 *     .client(RedisClient.LETTUCE)  // or JEDIS
 *     .build();
 *
 * // Create the service (via ServiceRegistry or directly)
 * RedisService redis = new LettuceRedisService(config);
 *
 * // Basic operations
 * redis.set("key", "value");
 * Optional<String> value = redis.get("key");
 *
 * // Async operations
 * redis.setAsync("key", "value")
 *     .thenRun(() -> System.out.println("Done!"));
 *
 * // Key namespacing
 * KeyNamespace ns = redis.namespace("myplugin");
 * ns.set("player:uuid", "data");  // Stores "myplugin:player:uuid"
 *
 * // Pub/Sub
 * redis.subscribe("channel", msg -> System.out.println(msg));
 * redis.publish("channel", "Hello!");
 *
 * // Shutdown
 * redis.shutdown();
 * }</pre>
 *
 * <h2>Client Implementations</h2>
 * <p>Two client implementations are supported:
 *
 * <h3>Jedis ({@link sh.pcx.unified.data.redis.jedis})</h3>
 * <ul>
 *   <li>Simple, synchronous API</li>
 *   <li>Connection pooling via JedisPool</li>
 *   <li>Widely used and well-documented</li>
 * </ul>
 *
 * <h3>Lettuce ({@link sh.pcx.unified.data.redis.lettuce})</h3>
 * <ul>
 *   <li>Native async/reactive support</li>
 *   <li>Thread-safe shared connections</li>
 *   <li>Automatic reconnection</li>
 * </ul>
 *
 * <h2>Serialization</h2>
 * <p>The package includes serializers for storing complex objects:
 * <ul>
 *   <li>{@link sh.pcx.unified.data.redis.RedisSerializer} - Serializer interface</li>
 *   <li>{@link sh.pcx.unified.data.redis.JsonRedisSerializer} - JSON via Gson</li>
 *   <li>{@link sh.pcx.unified.data.redis.BinaryRedisSerializer} - Java serialization</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All service implementations are thread-safe. Jedis uses connection pooling
 * while Lettuce uses multiplexed connections.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.data.redis.RedisService
 */
package sh.pcx.unified.data.redis;
