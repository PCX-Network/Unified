/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Jedis-based Redis implementation.
 *
 * <p>This package contains the Jedis client implementation of the Redis API.
 * Jedis is a synchronous Redis client with connection pooling support.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.redis.jedis.JedisRedisService} - Service implementation</li>
 *   <li>{@link sh.pcx.unified.data.redis.jedis.JedisProvider} - Client provider</li>
 *   <li>{@link sh.pcx.unified.data.redis.jedis.JedisPoolManager} - Connection pool manager</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Configure with Jedis
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .client(RedisClient.JEDIS)
 *     .pool(PoolConfig.builder()
 *         .maxTotal(16)
 *         .maxIdle(8)
 *         .build())
 *     .build();
 *
 * // Create the service
 * JedisRedisService redis = new JedisRedisService(config);
 *
 * // Use the service
 * redis.set("key", "value");
 * }</pre>
 *
 * <h2>Connection Pooling</h2>
 * <p>Jedis uses Apache Commons Pool for connection management. Each operation
 * borrows a connection from the pool and returns it after use.
 *
 * <h2>Thread Safety</h2>
 * <p>Individual Jedis connections are NOT thread-safe. The pool provides
 * thread-safety by giving each thread its own connection.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.data.redis.jedis.JedisRedisService
 */
package sh.pcx.unified.data.redis.jedis;
