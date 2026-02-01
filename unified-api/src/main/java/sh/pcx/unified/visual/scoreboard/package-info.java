/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Per-player scoreboard API with animated titles and dynamic lines.
 *
 * <p>The scoreboard system provides:
 * <ul>
 *   <li>Per-player sidebar scoreboards</li>
 *   <li>Animated titles with cycling/scrolling</li>
 *   <li>Dynamic line updates without flickering</li>
 *   <li>Placeholder integration</li>
 *   <li>Conditional line display</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Scoreboard board = scoreboardService.create(player)
 *     .title(Component.text("My Server", NamedTextColor.GOLD))
 *     .line(10, Component.text("Balance: ${balance}"))
 *     .line(9, Component.empty())
 *     .line(8, Component.text("Kills: {kills}"))
 *     .updateInterval(20)
 *     .build();
 *
 * // Update a line
 * board.setLine(10, Component.text("Balance: $1000"));
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.scoreboard;

import org.jspecify.annotations.NullMarked;
