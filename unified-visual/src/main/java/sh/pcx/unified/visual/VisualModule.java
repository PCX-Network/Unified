/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual;

import com.google.inject.AbstractModule;
import sh.pcx.unified.visual.bossbar.BossBarService;
import sh.pcx.unified.visual.hologram.HologramService;
import sh.pcx.unified.visual.particle.ParticleService;
import sh.pcx.unified.visual.scoreboard.ScoreboardService;
import sh.pcx.unified.visual.sound.SoundService;
import sh.pcx.unified.visual.title.TitleService;

/**
 * Guice module for visual and audio services.
 *
 * <p>This module provides bindings for:
 * <ul>
 *   <li>{@link HologramService} - Hologram creation and management</li>
 *   <li>{@link ScoreboardService} - Per-player scoreboard management</li>
 *   <li>{@link BossBarService} - Boss bar display and animations</li>
 *   <li>{@link TitleService} - Titles, subtitles, and action bars</li>
 *   <li>{@link ParticleService} - Particle effects and shapes</li>
 *   <li>{@link SoundService} - Sound playback and 3D audio</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Install this module in your plugin's Guice injector:
 * <pre>{@code
 * Injector injector = Guice.createInjector(
 *     new VisualModule(platform),
 *     // other modules...
 * );
 * }</pre>
 *
 * <h2>Platform Detection</h2>
 * <p>The module automatically detects the current platform (Paper, Sponge, etc.)
 * and binds the appropriate implementation for each service.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public abstract class VisualModule extends AbstractModule {

    /**
     * Creates a new visual module.
     */
    protected VisualModule() {
    }

    @Override
    protected void configure() {
        // Subclasses bind platform-specific implementations
        bindHologramService();
        bindScoreboardService();
        bindBossBarService();
        bindTitleService();
        bindParticleService();
        bindSoundService();
    }

    /**
     * Binds the hologram service implementation.
     */
    protected abstract void bindHologramService();

    /**
     * Binds the scoreboard service implementation.
     */
    protected abstract void bindScoreboardService();

    /**
     * Binds the boss bar service implementation.
     */
    protected abstract void bindBossBarService();

    /**
     * Binds the title service implementation.
     */
    protected abstract void bindTitleService();

    /**
     * Binds the particle service implementation.
     */
    protected abstract void bindParticleService();

    /**
     * Binds the sound service implementation.
     */
    protected abstract void bindSoundService();
}
