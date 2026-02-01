/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.line;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * A static scoreboard line that displays the same content to all players.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class StaticLine implements ScoreboardLine {

    private static final StaticLine EMPTY = new StaticLine(Component.empty());

    private final Component content;

    private StaticLine(@NotNull Component content) {
        this.content = content;
    }

    /**
     * Creates a static line with the given content.
     *
     * @param content the line content
     * @return a new static line
     * @since 1.0.0
     */
    @NotNull
    public static StaticLine of(@NotNull Component content) {
        return new StaticLine(content);
    }

    /**
     * Creates a static line from a MiniMessage string.
     *
     * @param content the MiniMessage content
     * @return a new static line
     * @since 1.0.0
     */
    @NotNull
    public static StaticLine of(@NotNull String content) {
        return new StaticLine(MiniMessage.miniMessage().deserialize(content));
    }

    /**
     * Returns an empty static line (spacer).
     *
     * @return an empty line
     * @since 1.0.0
     */
    @NotNull
    public static StaticLine empty() {
        return EMPTY;
    }

    @Override
    public @NotNull Component render(@NotNull UnifiedPlayer player) {
        return content;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    /**
     * Returns the content of this line.
     *
     * @return the line content
     * @since 1.0.0
     */
    @NotNull
    public Component getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "StaticLine{content=" + content + '}';
    }
}
