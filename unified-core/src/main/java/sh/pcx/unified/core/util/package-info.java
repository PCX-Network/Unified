/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Core utility classes for the UnifiedPlugin API.
 *
 * <p>This package provides general-purpose utility classes that are commonly
 * needed in Minecraft plugin development.
 *
 * <h2>Time Utilities</h2>
 * <p>The {@link sh.pcx.unified.core.util.TimeUtils} class provides
 * duration parsing and formatting:
 * <pre>{@code
 * Duration duration = TimeUtils.parse("1h30m");
 * String formatted = TimeUtils.format(duration);
 * String relative = TimeUtils.relative(instant);
 * }</pre>
 *
 * <h2>Math Utilities</h2>
 * <p>The {@link sh.pcx.unified.core.util.MathUtils} class provides
 * common math operations for game development:
 * <pre>{@code
 * int random = MathUtils.randomInt(1, 100);
 * double clamped = MathUtils.clamp(value, 0.0, 1.0);
 * double lerped = MathUtils.lerp(start, end, 0.5);
 * }</pre>
 *
 * <h2>Cooldown Management</h2>
 * <p>The {@link sh.pcx.unified.core.util.CooldownManager} class provides
 * thread-safe cooldown tracking:
 * <pre>{@code
 * CooldownManager cooldowns = new CooldownManager();
 * cooldowns.set(player, "ability", Duration.ofSeconds(30));
 * if (cooldowns.isOnCooldown(player, "ability")) {
 *     Duration remaining = cooldowns.getRemaining(player, "ability");
 * }
 * }</pre>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code collection} - Specialized collections (WeightedCollection, CircularBuffer, ExpiringMap)</li>
 *   <li>{@code pagination} - Pagination utilities</li>
 *   <li>{@code stream} - Stream operation helpers</li>
 *   <li>{@code concurrent} - Observable collections and concurrent utilities</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.core.util;
