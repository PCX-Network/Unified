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
 * Model definitions for resource packs.
 *
 * <p>This class contains types for defining item models, block models,
 * display transforms, and armor textures for custom resource packs.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class ResourcePackModels {
    private ResourcePackModels() {}

    /**
     * Represents an item model definition.
     *
     * @since 1.0.0
     */
    public record ItemModel(
            String parent,
            Map<String, String> textures,
            ItemDisplay display
    ) {

        /**
         * Creates a new item model builder.
         *
         * @return a new Builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for ItemModel.
         *
         * @since 1.0.0
         */
        public static final class Builder {
            private String parent = "minecraft:item/generated";
            private final Map<String, String> textures = new HashMap<>();
            private ItemDisplay display;

            /**
             * Sets the parent model.
             *
             * @param parent the parent model key
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder parent(@NotNull String parent) {
                this.parent = parent;
                return this;
            }

            /**
             * Adds a texture layer.
             *
             * @param layer   the layer name (e.g., "layer0")
             * @param texture the texture key
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder texture(@NotNull String layer, @NotNull String texture) {
                textures.put(layer, texture);
                return this;
            }

            /**
             * Sets the display properties.
             *
             * @param display the display configuration
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder display(@NotNull ItemDisplay display) {
                this.display = display;
                return this;
            }

            /**
             * Builds the item model.
             *
             * @return the constructed ItemModel
             * @since 1.0.0
             */
            @NotNull
            public ItemModel build() {
                return new ItemModel(parent, Map.copyOf(textures), display);
            }
        }
    }

    /**
     * Represents a block model definition.
     *
     * @since 1.0.0
     */
    public record BlockModel(
            String parent,
            Map<String, String> textures,
            List<BlockElement> elements
    ) {

        /**
         * Creates a new block model builder.
         *
         * @return a new Builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for BlockModel.
         *
         * @since 1.0.0
         */
        public static final class Builder {
            private String parent = "minecraft:block/cube_all";
            private final Map<String, String> textures = new HashMap<>();
            private final List<BlockElement> elements = new ArrayList<>();

            /**
             * Sets the parent model.
             *
             * @param parent the parent model key
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder parent(@NotNull String parent) {
                this.parent = parent;
                return this;
            }

            /**
             * Adds a texture.
             *
             * @param name    the texture variable name
             * @param texture the texture key
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder texture(@NotNull String name, @NotNull String texture) {
                textures.put(name, texture);
                return this;
            }

            /**
             * Adds a custom element.
             *
             * @param element the block element
             * @return this builder
             * @since 1.0.0
             */
            @NotNull
            public Builder element(@NotNull BlockElement element) {
                elements.add(element);
                return this;
            }

            /**
             * Builds the block model.
             *
             * @return the constructed BlockModel
             * @since 1.0.0
             */
            @NotNull
            public BlockModel build() {
                return new BlockModel(parent, Map.copyOf(textures), List.copyOf(elements));
            }
        }
    }

    /**
     * Display transformations for item models.
     *
     * @since 1.0.0
     */
    public record ItemDisplay(
            DisplayTransform thirdPersonRightHand,
            DisplayTransform thirdPersonLeftHand,
            DisplayTransform firstPersonRightHand,
            DisplayTransform firstPersonLeftHand,
            DisplayTransform gui,
            DisplayTransform head,
            DisplayTransform ground,
            DisplayTransform fixed
    ) {

        /**
         * Creates a new display builder.
         *
         * @return a new Builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private DisplayTransform thirdPersonRightHand;
            private DisplayTransform thirdPersonLeftHand;
            private DisplayTransform firstPersonRightHand;
            private DisplayTransform firstPersonLeftHand;
            private DisplayTransform gui;
            private DisplayTransform head;
            private DisplayTransform ground;
            private DisplayTransform fixed;

            @NotNull
            public Builder thirdPersonRightHand(@NotNull DisplayTransform transform) {
                this.thirdPersonRightHand = transform;
                return this;
            }

            @NotNull
            public Builder thirdPersonLeftHand(@NotNull DisplayTransform transform) {
                this.thirdPersonLeftHand = transform;
                return this;
            }

            @NotNull
            public Builder firstPersonRightHand(@NotNull DisplayTransform transform) {
                this.firstPersonRightHand = transform;
                return this;
            }

            @NotNull
            public Builder firstPersonLeftHand(@NotNull DisplayTransform transform) {
                this.firstPersonLeftHand = transform;
                return this;
            }

            @NotNull
            public Builder gui(@NotNull DisplayTransform transform) {
                this.gui = transform;
                return this;
            }

            @NotNull
            public Builder head(@NotNull DisplayTransform transform) {
                this.head = transform;
                return this;
            }

            @NotNull
            public Builder ground(@NotNull DisplayTransform transform) {
                this.ground = transform;
                return this;
            }

            @NotNull
            public Builder fixed(@NotNull DisplayTransform transform) {
                this.fixed = transform;
                return this;
            }

            @NotNull
            public ItemDisplay build() {
                return new ItemDisplay(thirdPersonRightHand, thirdPersonLeftHand,
                        firstPersonRightHand, firstPersonLeftHand, gui, head, ground, fixed);
            }
        }
    }

    /**
     * Transform values for display positions.
     *
     * @since 1.0.0
     */
    public record DisplayTransform(
            float[] rotation,
            float[] translation,
            float[] scale
    ) {

        /**
         * Creates a transform with rotation.
         *
         * @param x X rotation
         * @param y Y rotation
         * @param z Z rotation
         * @return a DisplayTransform
         * @since 1.0.0
         */
        @NotNull
        public static DisplayTransform rotation(float x, float y, float z) {
            return new DisplayTransform(new float[]{x, y, z}, null, null);
        }

        /**
         * Creates a transform with translation.
         *
         * @param x X translation
         * @param y Y translation
         * @param z Z translation
         * @return a DisplayTransform
         * @since 1.0.0
         */
        @NotNull
        public static DisplayTransform translation(float x, float y, float z) {
            return new DisplayTransform(null, new float[]{x, y, z}, null);
        }

        /**
         * Creates a transform with scale.
         *
         * @param x X scale
         * @param y Y scale
         * @param z Z scale
         * @return a DisplayTransform
         * @since 1.0.0
         */
        @NotNull
        public static DisplayTransform scale(float x, float y, float z) {
            return new DisplayTransform(null, null, new float[]{x, y, z});
        }
    }

    /**
     * Element definition for block models.
     *
     * @since 1.0.0
     */
    public record BlockElement(
            float[] from,
            float[] to,
            Map<String, BlockFace> faces
    ) {

        /**
         * Creates a new element builder.
         *
         * @return a new Builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private float[] from = {0, 0, 0};
            private float[] to = {16, 16, 16};
            private final Map<String, BlockFace> faces = new HashMap<>();

            @NotNull
            public Builder from(float x, float y, float z) {
                this.from = new float[]{x, y, z};
                return this;
            }

            @NotNull
            public Builder to(float x, float y, float z) {
                this.to = new float[]{x, y, z};
                return this;
            }

            @NotNull
            public Builder face(@NotNull String side, @NotNull BlockFace face) {
                faces.put(side, face);
                return this;
            }

            @NotNull
            public BlockElement build() {
                return new BlockElement(from, to, Map.copyOf(faces));
            }
        }
    }

    /**
     * Face definition for block elements.
     *
     * @since 1.0.0
     */
    public record BlockFace(
            float[] uv,
            String texture,
            String cullface,
            int rotation,
            int tintindex
    ) {

        /**
         * Creates a simple face with texture.
         *
         * @param texture the texture reference
         * @return a BlockFace
         * @since 1.0.0
         */
        @NotNull
        public static BlockFace of(@NotNull String texture) {
            return new BlockFace(null, texture, null, 0, -1);
        }

        /**
         * Creates a face with UV mapping.
         *
         * @param texture the texture reference
         * @param u1      U start
         * @param v1      V start
         * @param u2      U end
         * @param v2      V end
         * @return a BlockFace
         * @since 1.0.0
         */
        @NotNull
        public static BlockFace of(@NotNull String texture, float u1, float v1, float u2, float v2) {
            return new BlockFace(new float[]{u1, v1, u2, v2}, texture, null, 0, -1);
        }
    }

    /**
     * Armor texture definition.
     *
     * @since 1.0.0
     */
    public record ArmorTexture(
            String layer1,
            String layer2
    ) {

        /**
         * Creates a new armor texture builder.
         *
         * @return a new Builder
         * @since 1.0.0
         */
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String layer1;
            private String layer2;

            @NotNull
            public Builder layer1(@NotNull String texture) {
                this.layer1 = texture;
                return this;
            }

            @NotNull
            public Builder layer2(@NotNull String texture) {
                this.layer2 = texture;
                return this;
            }

            @NotNull
            public ArmorTexture build() {
                return new ArmorTexture(layer1, layer2);
            }
        }
    }
}
