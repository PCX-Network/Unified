/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual;

import sh.pcx.unified.visual.bossbar.BossBarService;
import sh.pcx.unified.visual.hologram.HologramService;
import sh.pcx.unified.visual.particle.ParticleService;
import sh.pcx.unified.visual.scoreboard.ScoreboardService;
import sh.pcx.unified.visual.sound.SoundService;
import sh.pcx.unified.visual.title.TitleService;
import org.jetbrains.annotations.NotNull;

/**
 * Facade interface providing access to all visual and audio services.
 *
 * <p>This interface serves as a single entry point to obtain any visual
 * service, making it convenient to inject all visual capabilities at once.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private VisualServices visual;
 *
 * public void showWelcome(UnifiedPlayer player) {
 *     // Use title service
 *     visual.titles().send(player)
 *         .title(Component.text("Welcome!"))
 *         .send();
 *
 *     // Use particle service
 *     visual.particles().spawn(player.getLocation(), ParticleType.HEART, 10);
 *
 *     // Use sound service
 *     visual.sounds().playTo(player, SoundType.ENTITY_PLAYER_LEVELUP);
 *
 *     // Create scoreboard
 *     visual.scoreboards().create(player)
 *         .title(Component.text("My Server"))
 *         .line(15, Component.text("Welcome, " + player.getName()))
 *         .build();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface VisualServices {

    /**
     * Returns the hologram service.
     *
     * @return the hologram service
     * @since 1.0.0
     */
    @NotNull
    HologramService holograms();

    /**
     * Returns the scoreboard service.
     *
     * @return the scoreboard service
     * @since 1.0.0
     */
    @NotNull
    ScoreboardService scoreboards();

    /**
     * Returns the boss bar service.
     *
     * @return the boss bar service
     * @since 1.0.0
     */
    @NotNull
    BossBarService bossBars();

    /**
     * Returns the title service.
     *
     * @return the title service
     * @since 1.0.0
     */
    @NotNull
    TitleService titles();

    /**
     * Returns the particle service.
     *
     * @return the particle service
     * @since 1.0.0
     */
    @NotNull
    ParticleService particles();

    /**
     * Returns the sound service.
     *
     * @return the sound service
     * @since 1.0.0
     */
    @NotNull
    SoundService sounds();
}
