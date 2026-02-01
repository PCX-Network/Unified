/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Particle effects API with shapes and animations.
 *
 * <p>The particle system provides:
 * <ul>
 *   <li>All Minecraft particle types</li>
 *   <li>Particle shapes (line, circle, sphere, helix)</li>
 *   <li>Animated particle effects</li>
 *   <li>Per-player particles</li>
 *   <li>Colored dust particles</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple particle
 * particleService.spawn(location, ParticleType.FLAME, 50);
 *
 * // Detailed particle
 * particleService.spawn(location)
 *     .particle(ParticleType.DUST)
 *     .color(Color.RED)
 *     .size(1.5f)
 *     .count(100)
 *     .spawn();
 *
 * // Shapes
 * particleService.circle(center, radius, ParticleType.END_ROD, 50);
 * particleService.line(start, end, ParticleType.FLAME, 20);
 * }</pre>
 *
 * @since 1.0.0
 */
@NullMarked
package sh.pcx.unified.visual.particle;

import org.jspecify.annotations.NullMarked;
