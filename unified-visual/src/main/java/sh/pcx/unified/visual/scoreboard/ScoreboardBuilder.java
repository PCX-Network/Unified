/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.visual.scoreboard.line.ConditionalLine;
import sh.pcx.unified.visual.scoreboard.line.DynamicLine;
import sh.pcx.unified.visual.scoreboard.line.ScoreboardLine;
import sh.pcx.unified.visual.scoreboard.line.StaticLine;
import sh.pcx.unified.visual.scoreboard.title.AnimatedTitle;
import sh.pcx.unified.visual.scoreboard.title.ScoreboardTitle;
import sh.pcx.unified.visual.scoreboard.title.TitleFrame;
import sh.pcx.unified.visual.scoreboard.update.UpdateInterval;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent builder for creating {@link Scoreboard} instances.
 *
 * <p>The ScoreboardBuilder provides a convenient way to construct scoreboards
 * with various configurations including titles, lines, and update intervals.
 *
 * <h2>Basic Example</h2>
 * <pre>{@code
 * Scoreboard scoreboard = ScoreboardBuilder.create("my-board")
 *     .title(Component.text("My Server"))
 *     .line(StaticLine.of(Component.text("Welcome!")))
 *     .line(StaticLine.empty())
 *     .line(DynamicLine.of(p -> Component.text("Online: " + getOnlineCount())))
 *     .build();
 * }</pre>
 *
 * <h2>Animated Title Example</h2>
 * <pre>{@code
 * Scoreboard scoreboard = ScoreboardBuilder.create("animated")
 *     .animatedTitle(Duration.ofMillis(200),
 *         Component.text(">> My Server <<", NamedTextColor.GOLD),
 *         Component.text(">> My Server <<", NamedTextColor.YELLOW),
 *         Component.text(">> My Server <<", NamedTextColor.WHITE))
 *     .updateInterval(UpdateInterval.FAST)
 *     .build();
 * }</pre>
 *
 * <h2>Conditional Lines Example</h2>
 * <pre>{@code
 * Scoreboard scoreboard = ScoreboardBuilder.create("game")
 *     .title(Component.text("Game Board"))
 *     .conditionalLine(
 *         player -> player.hasPermission("vip"),
 *         StaticLine.of(Component.text("VIP Status: Active", NamedTextColor.GOLD)))
 *     .line(DynamicLine.of(p -> Component.text("Score: " + getScore(p))))
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Scoreboard
 * @see ScoreboardService
 */
public interface ScoreboardBuilder {

    /**
     * Creates a new scoreboard builder with the given ID.
     *
     * @param id the unique identifier for the scoreboard
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    static ScoreboardBuilder create(@NotNull String id) {
        return new DefaultScoreboardBuilder(id);
    }

    /**
     * Returns the ID of the scoreboard being built.
     *
     * @return the scoreboard ID
     * @since 1.0.0
     */
    @NotNull
    String getId();

    /**
     * Sets the title of the scoreboard.
     *
     * @param title the scoreboard title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder title(@NotNull ScoreboardTitle title);

    /**
     * Sets a static title for the scoreboard.
     *
     * @param title the title text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder title(@NotNull Component title);

    /**
     * Sets a static title using a string.
     *
     * <p>The string will be converted to a Component using MiniMessage format.
     *
     * @param title the title text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder title(@NotNull String title);

    /**
     * Sets a dynamic title that updates per-player.
     *
     * @param titleProvider the function that provides the title for each player
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder dynamicTitle(@NotNull Function<UnifiedPlayer, Component> titleProvider);

    /**
     * Sets an animated title with the given frames and interval.
     *
     * @param frameInterval the time between frame changes
     * @param frames        the title frames
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder animatedTitle(@NotNull Duration frameInterval, @NotNull Component... frames);

    /**
     * Sets an animated title with the given frames and interval.
     *
     * @param frameInterval the time between frame changes
     * @param frames        the title frames
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder animatedTitle(@NotNull Duration frameInterval, @NotNull List<Component> frames);

    /**
     * Sets an animated title with custom frame definitions.
     *
     * @param frames the title frames with custom durations
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder animatedTitle(@NotNull TitleFrame... frames);

    /**
     * Sets an animated title.
     *
     * @param title the animated title
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder animatedTitle(@NotNull AnimatedTitle title);

    /**
     * Adds a line to the scoreboard.
     *
     * @param line the line to add
     * @return this builder
     * @throws IllegalStateException if 15 lines have already been added
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder line(@NotNull ScoreboardLine line);

    /**
     * Adds a static text line.
     *
     * @param text the line text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder line(@NotNull Component text);

    /**
     * Adds a static text line using a string.
     *
     * <p>The string will be converted to a Component using MiniMessage format.
     *
     * @param text the line text
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder line(@NotNull String text);

    /**
     * Adds an empty line (spacer).
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder emptyLine();

    /**
     * Adds a dynamic line that updates per-player.
     *
     * @param lineProvider the function that provides the line content
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder dynamicLine(@NotNull Function<UnifiedPlayer, Component> lineProvider);

    /**
     * Adds a conditional line that is only shown when the condition is met.
     *
     * @param condition the condition to check
     * @param line      the line to show when condition is true
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder conditionalLine(@NotNull Predicate<UnifiedPlayer> condition, @NotNull ScoreboardLine line);

    /**
     * Adds a conditional static line.
     *
     * @param condition the condition to check
     * @param text      the text to show when condition is true
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder conditionalLine(@NotNull Predicate<UnifiedPlayer> condition, @NotNull Component text);

    /**
     * Adds multiple lines at once.
     *
     * @param lines the lines to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder lines(@NotNull ScoreboardLine... lines);

    /**
     * Adds multiple lines at once.
     *
     * @param lines the lines to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder lines(@NotNull Collection<? extends ScoreboardLine> lines);

    /**
     * Sets the update interval for dynamic content.
     *
     * @param interval the update interval
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder updateInterval(@NotNull UpdateInterval interval);

    /**
     * Sets a custom update interval.
     *
     * @param duration the update interval duration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder updateInterval(@NotNull Duration duration);

    /**
     * Enables or disables flicker-free updates.
     *
     * <p>When enabled, lines are updated using a technique that prevents
     * visual flickering during updates. Enabled by default.
     *
     * @param flickerFree true to enable flicker-free updates
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder flickerFree(boolean flickerFree);

    /**
     * Sets whether placeholder resolution is enabled.
     *
     * <p>When enabled, placeholders in the format {@code %placeholder%} are
     * resolved using registered placeholder providers.
     *
     * @param enabled true to enable placeholder resolution
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder placeholders(boolean enabled);

    /**
     * Sets user-defined metadata on the scoreboard.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    ScoreboardBuilder metadata(@NotNull String key, @NotNull Object value);

    /**
     * Builds the scoreboard.
     *
     * @return the constructed scoreboard
     * @throws IllegalStateException if required fields are missing
     * @since 1.0.0
     */
    @NotNull
    Scoreboard build();

    /**
     * Builds and registers the scoreboard with the service.
     *
     * @param service the scoreboard service to register with
     * @return the constructed and registered scoreboard
     * @since 1.0.0
     */
    @NotNull
    default Scoreboard buildAndRegister(@NotNull ScoreboardService service) {
        Scoreboard scoreboard = build();
        service.register(scoreboard);
        return scoreboard;
    }
}

/**
 * Default implementation of the ScoreboardBuilder.
 */
class DefaultScoreboardBuilder implements ScoreboardBuilder {

    private final String id;
    private ScoreboardTitle title;
    private final List<ScoreboardLine> lines = new ArrayList<>();
    private UpdateInterval updateInterval = UpdateInterval.NORMAL;
    private boolean flickerFree = true;
    private boolean placeholdersEnabled = true;
    private final java.util.Map<String, Object> metadata = new java.util.HashMap<>();

    DefaultScoreboardBuilder(@NotNull String id) {
        this.id = id;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull ScoreboardBuilder title(@NotNull ScoreboardTitle title) {
        this.title = title;
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder title(@NotNull Component title) {
        this.title = ScoreboardTitle.of(title);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder title(@NotNull String title) {
        this.title = ScoreboardTitle.of(title);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder dynamicTitle(@NotNull Function<UnifiedPlayer, Component> titleProvider) {
        this.title = ScoreboardTitle.dynamic(titleProvider);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder animatedTitle(@NotNull Duration frameInterval, @NotNull Component... frames) {
        this.title = AnimatedTitle.of(frameInterval, frames);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder animatedTitle(@NotNull Duration frameInterval, @NotNull List<Component> frames) {
        this.title = AnimatedTitle.of(frameInterval, frames);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder animatedTitle(@NotNull TitleFrame... frames) {
        this.title = AnimatedTitle.of(frames);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder animatedTitle(@NotNull AnimatedTitle title) {
        this.title = title;
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder line(@NotNull ScoreboardLine line) {
        if (lines.size() >= Scoreboard.MAX_LINES) {
            throw new IllegalStateException("Cannot add more than " + Scoreboard.MAX_LINES + " lines");
        }
        lines.add(line);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder line(@NotNull Component text) {
        return line(StaticLine.of(text));
    }

    @Override
    public @NotNull ScoreboardBuilder line(@NotNull String text) {
        return line(StaticLine.of(text));
    }

    @Override
    public @NotNull ScoreboardBuilder emptyLine() {
        return line(StaticLine.empty());
    }

    @Override
    public @NotNull ScoreboardBuilder dynamicLine(@NotNull Function<UnifiedPlayer, Component> lineProvider) {
        return line(DynamicLine.of(lineProvider));
    }

    @Override
    public @NotNull ScoreboardBuilder conditionalLine(@NotNull Predicate<UnifiedPlayer> condition,
                                                       @NotNull ScoreboardLine line) {
        return line(ConditionalLine.of(condition, line));
    }

    @Override
    public @NotNull ScoreboardBuilder conditionalLine(@NotNull Predicate<UnifiedPlayer> condition,
                                                       @NotNull Component text) {
        return conditionalLine(condition, StaticLine.of(text));
    }

    @Override
    public @NotNull ScoreboardBuilder lines(@NotNull ScoreboardLine... lines) {
        for (ScoreboardLine line : lines) {
            line(line);
        }
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder lines(@NotNull Collection<? extends ScoreboardLine> lines) {
        for (ScoreboardLine line : lines) {
            line(line);
        }
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder updateInterval(@NotNull UpdateInterval interval) {
        this.updateInterval = interval;
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder updateInterval(@NotNull Duration duration) {
        this.updateInterval = UpdateInterval.custom(duration);
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder flickerFree(boolean flickerFree) {
        this.flickerFree = flickerFree;
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder placeholders(boolean enabled) {
        this.placeholdersEnabled = enabled;
        return this;
    }

    @Override
    public @NotNull ScoreboardBuilder metadata(@NotNull String key, @NotNull Object value) {
        this.metadata.put(key, value);
        return this;
    }

    @Override
    public @NotNull Scoreboard build() {
        if (title == null) {
            title = ScoreboardTitle.of(Component.empty());
        }
        return new DefaultScoreboard(id, title, lines, updateInterval, flickerFree, placeholdersEnabled, metadata);
    }
}
