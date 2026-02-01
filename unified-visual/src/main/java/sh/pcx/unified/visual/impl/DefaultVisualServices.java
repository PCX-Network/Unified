/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import sh.pcx.unified.visual.VisualServices;
import sh.pcx.unified.visual.bossbar.BossBarService;
import sh.pcx.unified.visual.hologram.HologramService;
import sh.pcx.unified.visual.particle.ParticleService;
import sh.pcx.unified.visual.scoreboard.ScoreboardService;
import sh.pcx.unified.visual.sound.SoundService;
import sh.pcx.unified.visual.title.TitleService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Default implementation of {@link VisualServices}.
 *
 * <p>This class aggregates all visual services and provides a single
 * point of access. It is designed to be instantiated via dependency
 * injection.
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Singleton
public final class DefaultVisualServices implements VisualServices {

    private final HologramService hologramService;
    private final ScoreboardService scoreboardService;
    private final BossBarService bossBarService;
    private final TitleService titleService;
    private final ParticleService particleService;
    private final SoundService soundService;

    /**
     * Constructs a new default visual services instance.
     *
     * @param hologramService   the hologram service
     * @param scoreboardService the scoreboard service
     * @param bossBarService    the boss bar service
     * @param titleService      the title service
     * @param particleService   the particle service
     * @param soundService      the sound service
     */
    @Inject
    public DefaultVisualServices(
            @NotNull HologramService hologramService,
            @NotNull ScoreboardService scoreboardService,
            @NotNull BossBarService bossBarService,
            @NotNull TitleService titleService,
            @NotNull ParticleService particleService,
            @NotNull SoundService soundService
    ) {
        this.hologramService = Objects.requireNonNull(hologramService, "hologramService");
        this.scoreboardService = Objects.requireNonNull(scoreboardService, "scoreboardService");
        this.bossBarService = Objects.requireNonNull(bossBarService, "bossBarService");
        this.titleService = Objects.requireNonNull(titleService, "titleService");
        this.particleService = Objects.requireNonNull(particleService, "particleService");
        this.soundService = Objects.requireNonNull(soundService, "soundService");
    }

    @Override
    public @NotNull HologramService holograms() {
        return hologramService;
    }

    @Override
    public @NotNull ScoreboardService scoreboards() {
        return scoreboardService;
    }

    @Override
    public @NotNull BossBarService bossBars() {
        return bossBarService;
    }

    @Override
    public @NotNull TitleService titles() {
        return titleService;
    }

    @Override
    public @NotNull ParticleService particles() {
        return particleService;
    }

    @Override
    public @NotNull SoundService sounds() {
        return soundService;
    }
}
