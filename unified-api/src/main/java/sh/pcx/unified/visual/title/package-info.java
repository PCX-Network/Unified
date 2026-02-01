/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Title, subtitle, and action bar API.
 *
 * <p>The title system provides:
 * <ul>
 *   <li>Title and subtitle display</li>
 *   <li>Fade in/stay/fade out timing</li>
 *   <li>Action bar messages</li>
 *   <li>Persistent action bars</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * titleService.send(player)
 *     .title(Component.text("VICTORY!", NamedTextColor.GOLD))
 *     .subtitle(Component.text("You won the game!"))
 *     .fadeIn(Duration.ofMillis(500))
 *     .stay(Duration.ofSeconds(3))
 *     .fadeOut(Duration.ofMillis(500))
 *     .send();
 *
 * // Action bar
 * titleService.actionBar(player)
 *     .message(Component.text("Combat Mode Active"))
 *     .duration(Duration.ofSeconds(10))
 *     .send();
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.title;

import org.jspecify.annotations.NullMarked;
