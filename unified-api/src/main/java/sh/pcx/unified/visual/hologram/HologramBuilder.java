/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Builder for creating text holograms.
 *
 * <p>Use this builder to configure hologram properties before creation.
 * Obtain a builder from {@link HologramService#create}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Hologram hologram = hologramService.create(location)
 *     .name("welcome-hologram")
 *     .addLine(Component.text("Welcome!", NamedTextColor.GOLD))
 *     .addLine(Component.text("Server Status: Online", NamedTextColor.GREEN))
 *     .addLine(Component.empty())
 *     .addLine(Component.text("Players: {online}/{max}"))
 *     .billboard(Billboard.CENTER)
 *     .persistent(true)
 *     .lineSpacing(0.25f)
 *     .onClick(event -> event.getPlayer().sendMessage("Clicked!"))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see HologramService
 * @see Hologram
 */
public interface HologramBuilder {

    /**
     * Sets the name of the hologram.
     *
     * <p>Names are optional but useful for persistence and lookup.
     *
     * @param name the hologram name
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder name(@NotNull String name);

    /**
     * Adds a line of text to the hologram.
     *
     * @param text the text to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder addLine(@NotNull Component text);

    /**
     * Adds multiple lines of text to the hologram.
     *
     * @param lines the lines to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder addLines(@NotNull Component... lines);

    /**
     * Adds multiple lines of text to the hologram.
     *
     * @param lines the lines to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder addLines(@NotNull Collection<Component> lines);

    /**
     * Sets the billboard mode.
     *
     * @param billboard the billboard mode
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder billboard(@NotNull Billboard billboard);

    /**
     * Sets the spacing between lines.
     *
     * @param spacing the line spacing in blocks (default: 0.25)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder lineSpacing(float spacing);

    /**
     * Sets whether the hologram persists across server restarts.
     *
     * @param persistent true to persist
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder persistent(boolean persistent);

    /**
     * Sets the hologram to only be visible to specific players.
     *
     * <p>If not called, the hologram is visible to all players by default.
     *
     * @param players the players who can see the hologram
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder viewers(@NotNull UnifiedPlayer... players);

    /**
     * Sets the hologram to only be visible to specific players.
     *
     * @param players the players who can see the hologram
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder viewers(@NotNull Collection<? extends UnifiedPlayer> players);

    /**
     * Sets the click handler for the hologram.
     *
     * @param handler the click handler
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder onClick(@NotNull Consumer<HologramClickEvent> handler);

    /**
     * Sets the view range for this hologram.
     *
     * <p>Players beyond this range will not see the hologram.
     *
     * @param range the view range in blocks
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder viewRange(double range);

    /**
     * Sets the shadow radius for text display entities.
     *
     * @param radius the shadow radius (0 to disable)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder shadowRadius(float radius);

    /**
     * Sets the shadow strength for text display entities.
     *
     * @param strength the shadow strength (0.0-1.0)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder shadowStrength(float strength);

    /**
     * Sets the text opacity.
     *
     * @param opacity the opacity (0-255, where 255 is fully opaque)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder textOpacity(int opacity);

    /**
     * Sets the background color with alpha.
     *
     * @param argb the background color in ARGB format
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder backgroundColor(int argb);

    /**
     * Sets whether the hologram should use a see-through background.
     *
     * @param seeThrough true for see-through background
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder seeThrough(boolean seeThrough);

    /**
     * Sets whether the hologram uses the default background.
     *
     * @param defaultBackground true to use default background
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder defaultBackground(boolean defaultBackground);

    /**
     * Adds an animation to run when the hologram is created.
     *
     * @param animation the animation to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    HologramBuilder animation(@NotNull HologramAnimation animation);

    /**
     * Builds and spawns the hologram.
     *
     * @return the created hologram
     * @since 1.0.0
     */
    @NotNull
    Hologram build();

    /**
     * Builds the hologram without spawning it.
     *
     * <p>Call {@link Hologram#spawn()} to spawn it later.
     *
     * @return the created hologram (not spawned)
     * @since 1.0.0
     */
    @NotNull
    Hologram buildWithoutSpawning();
}
