/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.line;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A dynamic scoreboard line that can display different content per-player.
 *
 * <p>Dynamic lines use a provider function to generate content based on
 * the player viewing the scoreboard.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class DynamicLine implements ScoreboardLine {

    private final Function<UnifiedPlayer, Component> provider;

    private DynamicLine(@NotNull Function<UnifiedPlayer, Component> provider) {
        this.provider = provider;
    }

    /**
     * Creates a dynamic line with the given provider.
     *
     * @param provider the function that provides content per-player
     * @return a new dynamic line
     * @since 1.0.0
     */
    @NotNull
    public static DynamicLine of(@NotNull Function<UnifiedPlayer, Component> provider) {
        return new DynamicLine(provider);
    }

    @Override
    public @NotNull Component render(@NotNull UnifiedPlayer player) {
        return provider.apply(player);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public String toString() {
        return "DynamicLine{}";
    }
}
