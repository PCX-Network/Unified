/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Sound playback API with 3D audio and custom sounds.
 *
 * <p>The sound system provides:
 * <ul>
 *   <li>All Minecraft sounds</li>
 *   <li>3D positional audio</li>
 *   <li>Sound categories for volume settings</li>
 *   <li>Custom resource pack sounds</li>
 *   <li>Per-player sound playback</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple sound
 * soundService.play(location, SoundType.ENTITY_ENDER_DRAGON_GROWL);
 *
 * // Detailed sound
 * soundService.play(location)
 *     .sound(SoundType.ENTITY_PLAYER_LEVELUP)
 *     .category(SoundCategory.PLAYERS)
 *     .volume(1.0f)
 *     .pitch(1.2f)
 *     .play();
 *
 * // Custom sound
 * soundService.play(location)
 *     .custom("myplugin:custom_sound")
 *     .play();
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.sound;

import org.jspecify.annotations.NullMarked;
