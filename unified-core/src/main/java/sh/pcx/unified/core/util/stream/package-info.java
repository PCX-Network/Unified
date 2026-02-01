/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Stream operation helpers and utilities.
 *
 * <p>This package provides additional stream operations that extend Java's
 * Stream API with commonly needed functionality for game development.
 *
 * <h2>Filtering</h2>
 * <pre>{@code
 * // Filter by type
 * List<Player> players = StreamUtils.filterByType(entities, Player.class);
 *
 * // Distinct by property
 * List<Player> unique = players.stream()
 *     .filter(StreamUtils.distinctByKey(Player::getName))
 *     .toList();
 * }</pre>
 *
 * <h2>Batching</h2>
 * <pre>{@code
 * // Process items in batches
 * StreamUtils.batch(items.stream(), 10).forEach(batch -> {
 *     processBatch(batch);
 * });
 * }</pre>
 *
 * <h2>Zipping</h2>
 * <pre>{@code
 * // Combine two streams
 * Stream<Pair<A, B>> zipped = StreamUtils.zip(streamA, streamB);
 *
 * // Add indices
 * StreamUtils.zipWithIndex(stream).forEach(indexed -> {
 *     System.out.println(indexed.index() + ": " + indexed.value());
 * });
 * }</pre>
 *
 * <h2>Async Operations</h2>
 * <pre>{@code
 * CompletableFuture<List<Data>> results = StreamUtils.mapAsync(
 *     players.stream(),
 *     player -> loadPlayerDataAsync(player)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.util.stream;
