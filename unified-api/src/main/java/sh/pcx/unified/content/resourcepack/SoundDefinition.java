/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sound definition in a resource pack.
 *
 * <p>SoundDefinition specifies how a sound should be played, including
 * the sound files to use, volume, pitch, and other properties.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * SoundDefinition bossRoar = SoundDefinition.builder()
 *     .sound("myplugin:sounds/boss_roar")
 *     .volume(1.0f)
 *     .pitch(0.8f)
 *     .stream(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record SoundDefinition(
        List<SoundEntry> sounds,
        String subtitle,
        boolean replace
) {

    /**
     * Creates a new sound definition builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple sound definition with one sound.
     *
     * @param soundPath the sound file path
     * @return a SoundDefinition
     * @since 1.0.0
     */
    @NotNull
    public static SoundDefinition of(@NotNull String soundPath) {
        return builder().sound(soundPath).build();
    }

    /**
     * Builder for SoundDefinition.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final List<SoundEntry> sounds = new ArrayList<>();
        private String subtitle;
        private boolean replace = false;

        /**
         * Adds a sound file path.
         *
         * @param soundPath the sound file path (without extension)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder sound(@NotNull String soundPath) {
            sounds.add(new SoundEntry(soundPath, 1.0f, 1.0f, 1, false, false));
            return this;
        }

        /**
         * Adds a sound entry with custom properties.
         *
         * @param entry the sound entry
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder sound(@NotNull SoundEntry entry) {
            sounds.add(entry);
            return this;
        }

        /**
         * Adds a sound with volume and pitch.
         *
         * @param soundPath the sound file path
         * @param volume    the volume (0.0-1.0)
         * @param pitch     the pitch (0.5-2.0)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder sound(@NotNull String soundPath, float volume, float pitch) {
            sounds.add(new SoundEntry(soundPath, volume, pitch, 1, false, false));
            return this;
        }

        /**
         * Sets the subtitle translation key.
         *
         * @param subtitle the subtitle key
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder subtitle(@NotNull String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        /**
         * Sets whether to replace vanilla sounds.
         *
         * @param replace true to replace existing sounds
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder replace(boolean replace) {
            this.replace = replace;
            return this;
        }

        /**
         * Builds the sound definition.
         *
         * @return the constructed SoundDefinition
         * @since 1.0.0
         */
        @NotNull
        public SoundDefinition build() {
            return new SoundDefinition(List.copyOf(sounds), subtitle, replace);
        }
    }
}

/**
 * Represents a single sound file entry.
 *
 * @since 1.0.0
 */
record SoundEntry(
        String name,
        float volume,
        float pitch,
        int weight,
        boolean stream,
        boolean preload
) {

    /**
     * Creates a new sound entry builder.
     *
     * @param name the sound file path
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Builder for SoundEntry.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final String name;
        private float volume = 1.0f;
        private float pitch = 1.0f;
        private int weight = 1;
        private boolean stream = false;
        private boolean preload = false;

        Builder(String name) {
            this.name = name;
        }

        /**
         * Sets the volume.
         *
         * @param volume the volume (0.0-1.0)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Sets the pitch.
         *
         * @param pitch the pitch (0.5-2.0)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        /**
         * Sets the selection weight.
         *
         * @param weight the weight (for random selection)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Sets whether to stream the sound (for long sounds).
         *
         * @param stream true to stream
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        /**
         * Sets whether to preload the sound.
         *
         * @param preload true to preload
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder preload(boolean preload) {
            this.preload = preload;
            return this;
        }

        /**
         * Builds the sound entry.
         *
         * @return the constructed SoundEntry
         * @since 1.0.0
         */
        @NotNull
        public SoundEntry build() {
            return new SoundEntry(name, volume, pitch, weight, stream, preload);
        }
    }
}
