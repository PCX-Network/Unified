/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Utility classes for scheduler operations.
 *
 * <p>This package contains helper classes for common scheduling tasks:
 * <ul>
 *   <li>{@link sh.pcx.unified.scheduler.util.Ticks} -
 *       Conversion between time units and server ticks</li>
 *   <li>{@link sh.pcx.unified.scheduler.util.TimeUnit} -
 *       Extended time units including Minecraft-specific periods</li>
 * </ul>
 *
 * <h2>Tick Conversion</h2>
 * <p>Minecraft servers run at 20 ticks per second (TPS). The Ticks class
 * provides convenient conversions:
 * <pre>{@code
 * long ticks = Ticks.from(5, TimeUnit.SECONDS);  // 100 ticks
 * Duration duration = Ticks.toDuration(200);      // 10 seconds
 * String formatted = Ticks.format(3700);          // "3m 5s"
 * }</pre>
 *
 * <h2>Minecraft Time Units</h2>
 * <p>The TimeUnit enum includes Minecraft-specific periods:
 * <pre>{@code
 * long dayTicks = TimeUnit.MINECRAFT_DAYS.toTicks(1);  // 24000 ticks
 * long hourTicks = TimeUnit.MINECRAFT_HOURS.toTicks(6); // 6000 ticks
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.scheduler.util;
