/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.title;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents the title of a scoreboard.
 *
 * <p>Scoreboard titles can be static, dynamic (per-player), or animated.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface ScoreboardTitle {

    /**
     * Creates a static title with the given content.
     *
     * @param content the title content
     * @return a new static title
     * @since 1.0.0
     */
    @NotNull
    static ScoreboardTitle of(@NotNull Component content) {
        return new StaticTitle(content);
    }

    /**
     * Creates a static title from a MiniMessage string.
     *
     * @param content the MiniMessage content
     * @return a new static title
     * @since 1.0.0
     */
    @NotNull
    static ScoreboardTitle of(@NotNull String content) {
        return new StaticTitle(MiniMessage.miniMessage().deserialize(content));
    }

    /**
     * Creates a dynamic title with the given provider.
     *
     * @param provider the function that provides the title per-player
     * @return a new dynamic title
     * @since 1.0.0
     */
    @NotNull
    static ScoreboardTitle dynamic(@NotNull Function<UnifiedPlayer, Component> provider) {
        return new DynamicTitle(provider);
    }

    /**
     * Renders this title for a specific player.
     *
     * @param player the player to render for
     * @return the rendered title component
     * @since 1.0.0
     */
    @NotNull
    Component render(@NotNull UnifiedPlayer player);

    /**
     * Returns whether this title is dynamic.
     *
     * @return true if the title is dynamic or animated
     * @since 1.0.0
     */
    boolean isDynamic();

    /**
     * Returns whether this title is animated.
     *
     * @return true if the title is animated
     * @since 1.0.0
     */
    default boolean isAnimated() {
        return false;
    }
}

/**
 * A static scoreboard title.
 */
class StaticTitle implements ScoreboardTitle {

    private final Component content;

    StaticTitle(@NotNull Component content) {
        this.content = content;
    }

    @Override
    public @NotNull Component render(@NotNull UnifiedPlayer player) {
        return content;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public String toString() {
        return "StaticTitle{content=" + content + '}';
    }
}

/**
 * A dynamic scoreboard title.
 */
class DynamicTitle implements ScoreboardTitle {

    private final Function<UnifiedPlayer, Component> provider;

    DynamicTitle(@NotNull Function<UnifiedPlayer, Component> provider) {
        this.provider = provider;
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
        return "DynamicTitle{}";
    }
}
