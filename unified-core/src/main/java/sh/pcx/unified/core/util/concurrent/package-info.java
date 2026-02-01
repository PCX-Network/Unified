/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Observable collections and concurrent utilities.
 *
 * <p>This package provides reactive collection implementations that notify
 * listeners when their contents change, following the observer pattern.
 *
 * <h2>ObservableValue</h2>
 * <p>A single observable value with change notifications:
 * <pre>{@code
 * ObservableValue<Integer> health = new ObservableValue<>(100);
 *
 * health.onChange((oldValue, newValue) -> {
 *     updateHealthBar(newValue);
 * });
 *
 * health.set(80); // Triggers listener
 * health.update(h -> h - 10); // Also triggers listener
 * }</pre>
 *
 * <h2>ObservableList</h2>
 * <p>An observable list with add/remove notifications:
 * <pre>{@code
 * ObservableList<Player> players = new ObservableList<>();
 *
 * players.onAdd(player -> announceJoin(player));
 * players.onRemove(player -> announceLeave(player));
 *
 * players.add(player);    // Triggers onAdd
 * players.remove(player); // Triggers onRemove
 * }</pre>
 *
 * <h2>ObservableMap</h2>
 * <p>An observable map with put/remove notifications:
 * <pre>{@code
 * ObservableMap<UUID, PlayerData> data = new ObservableMap<>();
 *
 * data.onPut((uuid, data) -> saveToDatabase(uuid, data));
 * data.onRemove((uuid, data) -> deleteFromDatabase(uuid));
 *
 * data.put(playerId, playerData); // Triggers onPut
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations in this package are thread-safe and can be safely
 * modified from multiple threads concurrently.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.util.concurrent;
