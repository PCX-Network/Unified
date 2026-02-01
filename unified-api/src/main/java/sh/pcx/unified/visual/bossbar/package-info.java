/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Boss bar API with multi-bar support and progress animations.
 *
 * <p>The boss bar system provides:
 * <ul>
 *   <li>Multiple boss bars per player</li>
 *   <li>Progress animations</li>
 *   <li>Color and overlay customization</li>
 *   <li>Per-player targeting</li>
 *   <li>Countdown timers</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * BossBarDisplay bar = bossBarService.create()
 *     .title(Component.text("World Boss: Dragon"))
 *     .color(BossBarColor.RED)
 *     .overlay(BossBarOverlay.PROGRESS)
 *     .progress(1.0f)
 *     .addPlayer(player)
 *     .build();
 *
 * // Update progress
 * bar.setProgress(boss.getHealth() / boss.getMaxHealth());
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.bossbar;

import org.jspecify.annotations.NullMarked;
