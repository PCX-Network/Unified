/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.hologram;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Defines animations that can be applied to holograms.
 *
 * <p>Animations modify hologram properties over time, such as text colors,
 * rotation, or position. Multiple animations can run simultaneously.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Color cycling animation
 * HologramAnimation colorCycle = HologramAnimation.colorCycle()
 *     .colors(NamedTextColor.RED, NamedTextColor.YELLOW, NamedTextColor.GREEN)
 *     .duration(Duration.ofSeconds(3))
 *     .loop(true)
 *     .build();
 *
 * hologram.startAnimation(colorCycle);
 *
 * // Text scrolling animation
 * HologramAnimation scroll = HologramAnimation.textScroll()
 *     .text(Component.text("This is a scrolling message... "))
 *     .width(20)
 *     .speed(2)
 *     .loop(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Hologram#startAnimation(HologramAnimation)
 */
public sealed interface HologramAnimation permits
        HologramAnimation.ColorCycle,
        HologramAnimation.TextScroll,
        HologramAnimation.Typewriter,
        HologramAnimation.Pulse,
        HologramAnimation.Custom {

    /**
     * Returns the type of this animation.
     *
     * @return the animation type
     * @since 1.0.0
     */
    @NotNull
    Type getType();

    /**
     * Returns whether this animation loops.
     *
     * @return true if the animation loops
     * @since 1.0.0
     */
    boolean isLooping();

    /**
     * Returns the duration of one animation cycle.
     *
     * @return the cycle duration
     * @since 1.0.0
     */
    @NotNull
    Duration getDuration();

    /**
     * Creates a color cycling animation builder.
     *
     * @return a new color cycle builder
     * @since 1.0.0
     */
    @NotNull
    static ColorCycleBuilder colorCycle() {
        return new ColorCycleBuilder();
    }

    /**
     * Creates a text scrolling animation builder.
     *
     * @return a new text scroll builder
     * @since 1.0.0
     */
    @NotNull
    static TextScrollBuilder textScroll() {
        return new TextScrollBuilder();
    }

    /**
     * Creates a typewriter animation builder.
     *
     * @return a new typewriter builder
     * @since 1.0.0
     */
    @NotNull
    static TypewriterBuilder typewriter() {
        return new TypewriterBuilder();
    }

    /**
     * Creates a pulse animation builder.
     *
     * @return a new pulse builder
     * @since 1.0.0
     */
    @NotNull
    static PulseBuilder pulse() {
        return new PulseBuilder();
    }

    /**
     * Creates a custom animation builder.
     *
     * @return a new custom animation builder
     * @since 1.0.0
     */
    @NotNull
    static CustomBuilder custom() {
        return new CustomBuilder();
    }

    /**
     * Enumeration of animation types.
     *
     * @since 1.0.0
     */
    enum Type {
        /** Cycles through colors. */
        COLOR_CYCLE,
        /** Scrolls text horizontally. */
        TEXT_SCROLL,
        /** Types text character by character. */
        TYPEWRITER,
        /** Pulses scale or opacity. */
        PULSE,
        /** Custom animation logic. */
        CUSTOM
    }

    /**
     * Color cycling animation.
     *
     * @param colors   the colors to cycle through
     * @param duration the cycle duration
     * @param loop     whether to loop
     * @param lineIndex the line to animate, or -1 for all lines
     */
    record ColorCycle(
            @NotNull List<TextColor> colors,
            @NotNull Duration duration,
            boolean loop,
            int lineIndex
    ) implements HologramAnimation {
        @Override
        public @NotNull Type getType() { return Type.COLOR_CYCLE; }
        @Override
        public boolean isLooping() { return loop; }
        @Override
        public @NotNull Duration getDuration() { return duration; }
    }

    /**
     * Text scrolling animation.
     *
     * @param text      the text to scroll
     * @param width     the visible width in characters
     * @param duration  the cycle duration
     * @param loop      whether to loop
     * @param lineIndex the line to animate
     */
    record TextScroll(
            @NotNull Component text,
            int width,
            @NotNull Duration duration,
            boolean loop,
            int lineIndex
    ) implements HologramAnimation {
        @Override
        public @NotNull Type getType() { return Type.TEXT_SCROLL; }
        @Override
        public boolean isLooping() { return loop; }
        @Override
        public @NotNull Duration getDuration() { return duration; }
    }

    /**
     * Typewriter animation that reveals text character by character.
     *
     * @param text      the full text to reveal
     * @param duration  the total duration
     * @param loop      whether to loop
     * @param cursor    the cursor character
     * @param lineIndex the line to animate
     */
    record Typewriter(
            @NotNull Component text,
            @NotNull Duration duration,
            boolean loop,
            @NotNull String cursor,
            int lineIndex
    ) implements HologramAnimation {
        @Override
        public @NotNull Type getType() { return Type.TYPEWRITER; }
        @Override
        public boolean isLooping() { return loop; }
        @Override
        public @NotNull Duration getDuration() { return duration; }
    }

    /**
     * Pulse animation for scale or opacity.
     *
     * @param minScale  minimum scale factor
     * @param maxScale  maximum scale factor
     * @param duration  the cycle duration
     * @param loop      whether to loop
     */
    record Pulse(
            float minScale,
            float maxScale,
            @NotNull Duration duration,
            boolean loop
    ) implements HologramAnimation {
        @Override
        public @NotNull Type getType() { return Type.PULSE; }
        @Override
        public boolean isLooping() { return loop; }
        @Override
        public @NotNull Duration getDuration() { return duration; }
    }

    /**
     * Custom animation with user-defined logic.
     *
     * @param updater  function that receives progress (0.0-1.0) and returns the new component
     * @param duration the cycle duration
     * @param loop     whether to loop
     * @param lineIndex the line to animate, or -1 for custom handling
     */
    record Custom(
            @NotNull Function<Float, Component> updater,
            @NotNull Duration duration,
            boolean loop,
            int lineIndex
    ) implements HologramAnimation {
        @Override
        public @NotNull Type getType() { return Type.CUSTOM; }
        @Override
        public boolean isLooping() { return loop; }
        @Override
        public @NotNull Duration getDuration() { return duration; }
    }

    /**
     * Builder for color cycle animations.
     */
    final class ColorCycleBuilder {
        private List<TextColor> colors = List.of();
        private Duration duration = Duration.ofSeconds(1);
        private boolean loop = true;
        private int lineIndex = -1;

        private ColorCycleBuilder() {}

        public ColorCycleBuilder colors(@NotNull TextColor... colors) {
            this.colors = List.of(colors);
            return this;
        }

        public ColorCycleBuilder colors(@NotNull List<TextColor> colors) {
            this.colors = List.copyOf(colors);
            return this;
        }

        public ColorCycleBuilder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public ColorCycleBuilder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public ColorCycleBuilder line(int lineIndex) {
            this.lineIndex = lineIndex;
            return this;
        }

        public ColorCycle build() {
            if (colors.isEmpty()) {
                throw new IllegalStateException("At least one color is required");
            }
            return new ColorCycle(colors, duration, loop, lineIndex);
        }
    }

    /**
     * Builder for text scroll animations.
     */
    final class TextScrollBuilder {
        private Component text = Component.empty();
        private int width = 20;
        private Duration duration = Duration.ofSeconds(5);
        private boolean loop = true;
        private int lineIndex = 0;

        private TextScrollBuilder() {}

        public TextScrollBuilder text(@NotNull Component text) {
            this.text = text;
            return this;
        }

        public TextScrollBuilder width(int width) {
            this.width = width;
            return this;
        }

        public TextScrollBuilder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public TextScrollBuilder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public TextScrollBuilder line(int lineIndex) {
            this.lineIndex = lineIndex;
            return this;
        }

        public TextScroll build() {
            return new TextScroll(text, width, duration, loop, lineIndex);
        }
    }

    /**
     * Builder for typewriter animations.
     */
    final class TypewriterBuilder {
        private Component text = Component.empty();
        private Duration duration = Duration.ofSeconds(3);
        private boolean loop = false;
        private String cursor = "_";
        private int lineIndex = 0;

        private TypewriterBuilder() {}

        public TypewriterBuilder text(@NotNull Component text) {
            this.text = text;
            return this;
        }

        public TypewriterBuilder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public TypewriterBuilder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public TypewriterBuilder cursor(@NotNull String cursor) {
            this.cursor = cursor;
            return this;
        }

        public TypewriterBuilder line(int lineIndex) {
            this.lineIndex = lineIndex;
            return this;
        }

        public Typewriter build() {
            return new Typewriter(text, duration, loop, cursor, lineIndex);
        }
    }

    /**
     * Builder for pulse animations.
     */
    final class PulseBuilder {
        private float minScale = 0.8f;
        private float maxScale = 1.2f;
        private Duration duration = Duration.ofMillis(500);
        private boolean loop = true;

        private PulseBuilder() {}

        public PulseBuilder minScale(float minScale) {
            this.minScale = minScale;
            return this;
        }

        public PulseBuilder maxScale(float maxScale) {
            this.maxScale = maxScale;
            return this;
        }

        public PulseBuilder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public PulseBuilder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public Pulse build() {
            return new Pulse(minScale, maxScale, duration, loop);
        }
    }

    /**
     * Builder for custom animations.
     */
    final class CustomBuilder {
        private Function<Float, Component> updater = _ -> Component.empty();
        private Duration duration = Duration.ofSeconds(1);
        private boolean loop = true;
        private int lineIndex = -1;

        private CustomBuilder() {}

        public CustomBuilder updater(@NotNull Function<Float, Component> updater) {
            this.updater = updater;
            return this;
        }

        public CustomBuilder duration(@NotNull Duration duration) {
            this.duration = duration;
            return this;
        }

        public CustomBuilder loop(boolean loop) {
            this.loop = loop;
            return this;
        }

        public CustomBuilder line(int lineIndex) {
            this.lineIndex = lineIndex;
            return this;
        }

        public Custom build() {
            return new Custom(updater, duration, loop, lineIndex);
        }
    }
}
