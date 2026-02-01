/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Lettuce-based Redis implementation.
 *
 * <p>This package contains the Lettuce client implementation of the Redis API.
 * Lettuce is an asynchronous, thread-safe Redis client with native support
 * for CompletableFuture and reactive programming.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.redis.lettuce.LettuceRedisService} - Service implementation</li>
 *   <li>{@link sh.pcx.unified.data.redis.lettuce.LettuceProvider} - Client provider</li>
 *   <li>{@link sh.pcx.unified.data.redis.lettuce.LettuceConnectionManager} - Connection manager</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Configure with Lettuce
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .client(RedisClient.LETTUCE)
 *     .build();
 *
 * // Create the service
 * LettuceRedisService redis = new LettuceRedisService(config);
 *
 * // Sync operations
 * redis.set("key", "value");
 *
 * // Native async operations
 * redis.setAsync("key", "value")
 *     .thenCompose(v -> redis.getAsync("key"))
 *     .thenAccept(value -> System.out.println(value));
 * }</pre>
 *
 * <h2>Connection Model</h2>
 * <p>Unlike Jedis, Lettuce connections are:
 * <ul>
 *   <li>Thread-safe and can be shared across threads</li>
 *   <li>Multiplexed - one connection handles many concurrent operations</li>
 *   <li>Auto-reconnecting on connection loss</li>
 * </ul>
 *
 * <h2>Async Advantages</h2>
 * <p>Lettuce's async operations don't block threads, making it ideal for:
 * <ul>
 *   <li>High-throughput applications</li>
 *   <li>Non-blocking programming</li>
 *   <li>Integration with reactive frameworks</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.data.redis.lettuce.LettuceRedisService
 */
package sh.pcx.unified.data.redis.lettuce;
