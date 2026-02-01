/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a font definition in a resource pack.
 *
 * <p>FontDefinition specifies custom fonts and glyphs that can be used
 * in text components, including bitmap fonts, space providers, and
 * TTF fonts.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Custom emoji font
 * FontDefinition emojis = FontDefinition.builder()
 *     .provider(BitmapFontProvider.builder()
 *         .file("myplugin:font/emojis.png")
 *         .height(8)
 *         .ascent(7)
 *         .chars(List.of("\uE000\uE001\uE002\uE003"))
 *         .build())
 *     .build();
 *
 * // Negative space font for GUI alignment
 * FontDefinition space = FontDefinition.builder()
 *     .provider(SpaceFontProvider.builder()
 *         .advance('\uF801', -1)
 *         .advance('\uF808', -8)
 *         .advance('\uF832', -32)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record FontDefinition(List<FontProvider> providers) {

    /**
     * Creates a new font definition builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for FontDefinition.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final List<FontProvider> providers = new ArrayList<>();

        /**
         * Adds a font provider.
         *
         * @param provider the font provider
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder provider(@NotNull FontProvider provider) {
            providers.add(provider);
            return this;
        }

        /**
         * Builds the font definition.
         *
         * @return the constructed FontDefinition
         * @since 1.0.0
         */
        @NotNull
        public FontDefinition build() {
            return new FontDefinition(List.copyOf(providers));
        }
    }
}

/**
 * Base interface for font providers.
 *
 * @since 1.0.0
 */
sealed interface FontProvider permits BitmapFontProvider, SpaceFontProvider, TtfFontProvider {

    /**
     * Returns the provider type.
     *
     * @return the type identifier
     * @since 1.0.0
     */
    @NotNull
    String getType();
}

/**
 * Bitmap font provider for image-based glyphs.
 *
 * @since 1.0.0
 */
record BitmapFontProvider(
        String file,
        int height,
        int ascent,
        List<String> chars
) implements FontProvider {

    @Override
    @NotNull
    public String getType() {
        return "bitmap";
    }

    /**
     * Creates a new bitmap font provider builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for BitmapFontProvider.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String file;
        private int height = 8;
        private int ascent = 7;
        private final List<String> chars = new ArrayList<>();

        /**
         * Sets the texture file path.
         *
         * @param file the file path
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder file(@NotNull String file) {
            this.file = file;
            return this;
        }

        /**
         * Sets the glyph height.
         *
         * @param height the height in pixels
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder height(int height) {
            this.height = height;
            return this;
        }

        /**
         * Sets the ascent (vertical offset).
         *
         * @param ascent the ascent in pixels
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder ascent(int ascent) {
            this.ascent = ascent;
            return this;
        }

        /**
         * Sets the character rows.
         *
         * @param chars the character strings (one per row in texture)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder chars(@NotNull List<String> chars) {
            this.chars.addAll(chars);
            return this;
        }

        /**
         * Adds a character row.
         *
         * @param charRow the character string for one row
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder chars(@NotNull String charRow) {
            this.chars.add(charRow);
            return this;
        }

        /**
         * Builds the bitmap font provider.
         *
         * @return the constructed BitmapFontProvider
         * @since 1.0.0
         */
        @NotNull
        public BitmapFontProvider build() {
            return new BitmapFontProvider(file, height, ascent, List.copyOf(chars));
        }
    }
}

/**
 * Space font provider for negative/positive spacing characters.
 *
 * @since 1.0.0
 */
record SpaceFontProvider(Map<Character, Integer> advances) implements FontProvider {

    @Override
    @NotNull
    public String getType() {
        return "space";
    }

    /**
     * Creates a new space font provider builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SpaceFontProvider.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private final Map<Character, Integer> advances = new HashMap<>();

        /**
         * Sets the advances map.
         *
         * @param advances the character to pixel advance map
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder advances(@NotNull Map<Character, Integer> advances) {
            this.advances.putAll(advances);
            return this;
        }

        /**
         * Adds a single advance mapping.
         *
         * @param character the character
         * @param pixels    the advance in pixels (negative for left shift)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder advance(char character, int pixels) {
            this.advances.put(character, pixels);
            return this;
        }

        /**
         * Builds the space font provider.
         *
         * @return the constructed SpaceFontProvider
         * @since 1.0.0
         */
        @NotNull
        public SpaceFontProvider build() {
            return new SpaceFontProvider(Map.copyOf(advances));
        }
    }
}

/**
 * TTF font provider for TrueType fonts.
 *
 * @since 1.0.0
 */
record TtfFontProvider(
        String file,
        float size,
        float oversample,
        String skip,
        float shift
) implements FontProvider {

    @Override
    @NotNull
    public String getType() {
        return "ttf";
    }

    /**
     * Creates a new TTF font provider builder.
     *
     * @return a new Builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TtfFontProvider.
     *
     * @since 1.0.0
     */
    public static final class Builder {
        private String file;
        private float size = 11.0f;
        private float oversample = 2.0f;
        private String skip = "";
        private float shift = 0.0f;

        /**
         * Sets the TTF file path.
         *
         * @param file the file path
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder file(@NotNull String file) {
            this.file = file;
            return this;
        }

        /**
         * Sets the font size.
         *
         * @param size the size
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder size(float size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the oversample factor.
         *
         * @param oversample the oversample factor
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder oversample(float oversample) {
            this.oversample = oversample;
            return this;
        }

        /**
         * Sets characters to skip.
         *
         * @param skip the characters to skip
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder skip(@NotNull String skip) {
            this.skip = skip;
            return this;
        }

        /**
         * Sets the vertical shift.
         *
         * @param shift the shift
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder shift(float shift) {
            this.shift = shift;
            return this;
        }

        /**
         * Builds the TTF font provider.
         *
         * @return the constructed TtfFontProvider
         * @since 1.0.0
         */
        @NotNull
        public TtfFontProvider build() {
            return new TtfFontProvider(file, size, oversample, skip, shift);
        }
    }
}
