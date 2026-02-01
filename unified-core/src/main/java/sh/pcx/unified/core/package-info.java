/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core runtime implementation for the UnifiedPlugin API.
 *
 * <p>The unified-core module provides the main implementation classes and
 * utilities that power the UnifiedPlugin API. This module is designed to be
 * bundled into a single deployable JAR that includes all necessary dependencies.
 *
 * <h2>Module Structure</h2>
 * <ul>
 *   <li>{@code util} - Core utilities (TimeUtils, MathUtils, CooldownManager)</li>
 *   <li>{@code util.collection} - Specialized collections (WeightedCollection, CircularBuffer, ExpiringMap)</li>
 *   <li>{@code util.pagination} - Pagination support (Page, Paginator)</li>
 *   <li>{@code util.stream} - Stream helpers (StreamUtils)</li>
 *   <li>{@code util.concurrent} - Observable collections (ObservableValue, ObservableList, ObservableMap)</li>
 *   <li>{@code item} - Item building implementation (CoreItemBuilder)</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Java 21 features (records, sealed classes, pattern matching)</li>
 *   <li>Thread-safe implementations</li>
 *   <li>Comprehensive Javadoc documentation</li>
 *   <li>Minimal external dependencies</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Most classes in this module are accessed through static factory methods
 * or constructors:
 * <pre>{@code
 * // Time utilities
 * Duration duration = TimeUtils.parse("1h30m");
 *
 * // Math utilities
 * int random = MathUtils.randomInt(1, 100);
 *
 * // Cooldown management
 * CooldownManager cooldowns = new CooldownManager();
 *
 * // Weighted random selection
 * WeightedCollection<String> loot = new WeightedCollection<>();
 *
 * // Expiring cache
 * ExpiringMap<UUID, Data> cache = new ExpiringMap<>(5, TimeUnit.MINUTES);
 *
 * // Observable values
 * ObservableValue<Integer> health = new ObservableValue<>(100);
 *
 * // Pagination
 * Page<Player> page = Paginator.paginate(players, 1, 10);
 *
 * // Item building
 * UnifiedItemStack item = CoreItemBuilder.create("minecraft:diamond_sword")
 *     .name(Component.text("Excalibur"))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core;
