/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Specialized collection implementations for game development.
 *
 * <p>This package provides thread-safe, specialized collection types that are
 * commonly needed in Minecraft plugin development but not available in the
 * standard Java Collections Framework.
 *
 * <h2>WeightedCollection</h2>
 * <p>A collection that supports weighted random selection, perfect for loot
 * tables and spawn chances:
 * <pre>{@code
 * WeightedCollection<String> loot = new WeightedCollection<>();
 * loot.add("Common Sword", 70);
 * loot.add("Rare Sword", 25);
 * loot.add("Legendary Sword", 5);
 * String item = loot.next();
 * }</pre>
 *
 * <h2>CircularBuffer</h2>
 * <p>A fixed-size ring buffer that overwrites oldest entries, perfect for
 * maintaining history:
 * <pre>{@code
 * CircularBuffer<LogEntry> logs = new CircularBuffer<>(100);
 * logs.add(new LogEntry("Server started"));
 * List<LogEntry> recent = logs.getLast(10);
 * }</pre>
 *
 * <h2>ExpiringMap</h2>
 * <p>A map where entries automatically expire after a TTL:
 * <pre>{@code
 * ExpiringMap<UUID, Session> sessions = new ExpiringMap<>(30, TimeUnit.MINUTES);
 * sessions.put(playerId, session);
 * // Entry expires after 30 minutes
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All collection implementations in this package are thread-safe and can
 * be safely accessed from multiple threads concurrently.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.util.collection;
