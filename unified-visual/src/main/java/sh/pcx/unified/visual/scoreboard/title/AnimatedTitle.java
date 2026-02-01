/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.visual.scoreboard.title;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An animated scoreboard title that cycles through frames.
 *
 * <p>Animated titles display a sequence of frames, transitioning between
 * them based on their configured durations.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class AnimatedTitle implements ScoreboardTitle {

    private final List<TitleFrame> frames;
    private final AtomicInteger currentFrame;

    private AnimatedTitle(@NotNull List<TitleFrame> frames) {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Animated title must have at least one frame");
        }
        this.frames = new ArrayList<>(frames);
        this.currentFrame = new AtomicInteger(0);
    }

    /**
     * Creates an animated title with the given frames and uniform interval.
     *
     * @param frameInterval the interval between frames
     * @param frames        the frame contents
     * @return a new animated title
     * @since 1.0.0
     */
    @NotNull
    public static AnimatedTitle of(@NotNull Duration frameInterval, @NotNull Component... frames) {
        List<TitleFrame> titleFrames = new ArrayList<>();
        for (Component frame : frames) {
            titleFrames.add(TitleFrame.of(frame, frameInterval));
        }
        return new AnimatedTitle(titleFrames);
    }

    /**
     * Creates an animated title with the given frames and uniform interval.
     *
     * @param frameInterval the interval between frames
     * @param frames        the frame contents
     * @return a new animated title
     * @since 1.0.0
     */
    @NotNull
    public static AnimatedTitle of(@NotNull Duration frameInterval, @NotNull List<Component> frames) {
        List<TitleFrame> titleFrames = new ArrayList<>();
        for (Component frame : frames) {
            titleFrames.add(TitleFrame.of(frame, frameInterval));
        }
        return new AnimatedTitle(titleFrames);
    }

    /**
     * Creates an animated title with custom frame definitions.
     *
     * @param frames the title frames
     * @return a new animated title
     * @since 1.0.0
     */
    @NotNull
    public static AnimatedTitle of(@NotNull TitleFrame... frames) {
        return new AnimatedTitle(Arrays.asList(frames));
    }

    /**
     * Creates an animated title with custom frame definitions.
     *
     * @param frames the title frames
     * @return a new animated title
     * @since 1.0.0
     */
    @NotNull
    public static AnimatedTitle of(@NotNull List<TitleFrame> frames) {
        return new AnimatedTitle(frames);
    }

    @Override
    public @NotNull Component render(@NotNull UnifiedPlayer player) {
        int index = currentFrame.get();
        if (index >= 0 && index < frames.size()) {
            return frames.get(index).content();
        }
        return frames.get(0).content();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean isAnimated() {
        return true;
    }

    /**
     * Advances to the next frame.
     *
     * @since 1.0.0
     */
    public void nextFrame() {
        currentFrame.updateAndGet(i -> (i + 1) % frames.size());
    }

    /**
     * Returns the current frame index.
     *
     * @return the current frame index
     * @since 1.0.0
     */
    public int getCurrentFrameIndex() {
        return currentFrame.get();
    }

    /**
     * Returns the current frame.
     *
     * @return the current frame
     * @since 1.0.0
     */
    @NotNull
    public TitleFrame getCurrentFrame() {
        return frames.get(currentFrame.get());
    }

    /**
     * Returns all frames in this animation.
     *
     * @return the list of frames
     * @since 1.0.0
     */
    @NotNull
    public List<TitleFrame> getFrames() {
        return new ArrayList<>(frames);
    }

    /**
     * Returns the number of frames.
     *
     * @return the frame count
     * @since 1.0.0
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Resets the animation to the first frame.
     *
     * @since 1.0.0
     */
    public void reset() {
        currentFrame.set(0);
    }

    @Override
    public String toString() {
        return "AnimatedTitle{frames=" + frames.size() + ", current=" + currentFrame.get() + '}';
    }
}
