/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.generation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Processes blocks during structure placement.
 *
 * <p>Structure processors can modify, replace, or remove blocks as a structure
 * is being placed. This allows for effects like weathering, randomization,
 * and terrain adaptation.
 *
 * <h2>Built-in Processors:</h2>
 * <ul>
 *   <li>{@link #blockAge(float)} - Adds moss and weathering</li>
 *   <li>{@link #blockReplace(BlockType, BlockType, float)} - Replaces blocks</li>
 *   <li>{@link #gravity()} - Makes floating blocks fall</li>
 *   <li>{@link #airProtection()} - Prevents air from overwriting</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a custom processor
 * public class MossyStoneProcessor extends StructureProcessor {
 *     private final float chance;
 *
 *     public MossyStoneProcessor(float chance) {
 *         this.chance = chance;
 *     }
 *
 *     @Override
 *     public BlockInfo process(BlockInfo input, BlockInfo existing, Context context) {
 *         if (input.blockType().equals(BlockType.STONE_BRICKS)) {
 *             if (context.random().nextFloat() < chance) {
 *                 return input.withBlockType(BlockType.MOSSY_STONE_BRICKS);
 *             }
 *         }
 *         return input;
 *     }
 * }
 *
 * // Use in structure registration
 * structures.register("myplugin:ruins")
 *     .schematic(ruinsSchematic)
 *     .processors(processors -> {
 *         processors.add(new MossyStoneProcessor(0.4f));
 *     })
 *     .register();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see Structure
 * @see StructureService
 */
public abstract class StructureProcessor {

    /**
     * Default constructor for structure processors.
     *
     * @since 1.0.0
     */
    protected StructureProcessor() {
    }

    /**
     * Processes a block during structure placement.
     *
     * @param input    the block being placed from the structure
     * @param existing the existing block at the target location
     * @param context  the processing context
     * @return the processed block info, or null to skip placement
     * @since 1.0.0
     */
    @Nullable
    public abstract BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                      @NotNull Context context);

    // ==================== Built-in Processors ====================

    /**
     * Creates a processor that adds weathering/aging to blocks.
     *
     * @param mossiness the chance for blocks to become mossy (0.0-1.0)
     * @return the aging processor
     * @since 1.0.0
     */
    @NotNull
    public static StructureProcessor blockAge(float mossiness) {
        return new BlockAgeProcessor(mossiness);
    }

    /**
     * Creates a processor that replaces blocks.
     *
     * @param from   the block to replace
     * @param to     the replacement block
     * @param chance the replacement chance (0.0-1.0)
     * @return the replacement processor
     * @since 1.0.0
     */
    @NotNull
    public static StructureProcessor blockReplace(@NotNull BlockType from, @NotNull BlockType to, float chance) {
        return new BlockReplaceProcessor(from, to, chance);
    }

    /**
     * Creates a processor that makes floating blocks fall.
     *
     * @return the gravity processor
     * @since 1.0.0
     */
    @NotNull
    public static StructureProcessor gravity() {
        return GravityProcessor.INSTANCE;
    }

    /**
     * Creates a processor that prevents air blocks from overwriting.
     *
     * @return the air protection processor
     * @since 1.0.0
     */
    @NotNull
    public static StructureProcessor airProtection() {
        return AirProtectionProcessor.INSTANCE;
    }

    /**
     * Creates a processor that only places blocks on solid ground.
     *
     * @return the ground placement processor
     * @since 1.0.0
     */
    @NotNull
    public static StructureProcessor groundOnly() {
        return GroundOnlyProcessor.INSTANCE;
    }

    // ==================== Nested Types ====================

    /**
     * Information about a block during processing.
     *
     * @param blockType the block type
     * @param blockData the block data (may be null)
     * @param x         the world X coordinate
     * @param y         the world Y coordinate
     * @param z         the world Z coordinate
     * @since 1.0.0
     */
    public record BlockInfo(
            @NotNull BlockType blockType,
            @Nullable BlockData blockData,
            int x, int y, int z
    ) {
        /**
         * Creates a new BlockInfo with a different block type.
         *
         * @param newType the new block type
         * @return a new BlockInfo with the changed type
         * @since 1.0.0
         */
        @NotNull
        public BlockInfo withBlockType(@NotNull BlockType newType) {
            return new BlockInfo(newType, null, x, y, z);
        }

        /**
         * Creates a new BlockInfo with different block data.
         *
         * @param newData the new block data
         * @return a new BlockInfo with the changed data
         * @since 1.0.0
         */
        @NotNull
        public BlockInfo withBlockData(@Nullable BlockData newData) {
            return new BlockInfo(blockType, newData, x, y, z);
        }
    }

    /**
     * Context provided during block processing.
     *
     * @since 1.0.0
     */
    public interface Context {

        /**
         * Gets the random number generator.
         *
         * @return the random
         * @since 1.0.0
         */
        @NotNull
        Random random();

        /**
         * Gets the world info.
         *
         * @return the world info
         * @since 1.0.0
         */
        @NotNull
        WorldInfo worldInfo();

        /**
         * Gets the structure rotation.
         *
         * @return the rotation
         * @since 1.0.0
         */
        @NotNull
        StructureService.Rotation rotation();

        /**
         * Gets a block at the specified world coordinates.
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         * @param z the Z coordinate
         * @return the block type
         * @since 1.0.0
         */
        @NotNull
        BlockType getBlock(int x, int y, int z);
    }

    // ==================== Built-in Processor Implementations ====================

    private static final class BlockAgeProcessor extends StructureProcessor {
        private final float mossiness;

        BlockAgeProcessor(float mossiness) {
            this.mossiness = mossiness;
        }

        @Override
        @Nullable
        public BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                 @NotNull Context context) {
            if (context.random().nextFloat() >= mossiness) {
                return input;
            }

            BlockType aged = switch (input.blockType().key()) {
                case "minecraft:stone_bricks" -> BlockType.of("minecraft:mossy_stone_bricks");
                case "minecraft:cobblestone" -> BlockType.of("minecraft:mossy_cobblestone");
                case "minecraft:stone_brick_slab" -> BlockType.of("minecraft:mossy_stone_brick_slab");
                case "minecraft:stone_brick_stairs" -> BlockType.of("minecraft:mossy_stone_brick_stairs");
                case "minecraft:stone_brick_wall" -> BlockType.of("minecraft:mossy_stone_brick_wall");
                case "minecraft:cobblestone_slab" -> BlockType.of("minecraft:mossy_cobblestone_slab");
                case "minecraft:cobblestone_stairs" -> BlockType.of("minecraft:mossy_cobblestone_stairs");
                case "minecraft:cobblestone_wall" -> BlockType.of("minecraft:mossy_cobblestone_wall");
                default -> null;
            };

            return aged != null ? input.withBlockType(aged) : input;
        }
    }

    private static final class BlockReplaceProcessor extends StructureProcessor {
        private final BlockType from;
        private final BlockType to;
        private final float chance;

        BlockReplaceProcessor(BlockType from, BlockType to, float chance) {
            this.from = from;
            this.to = to;
            this.chance = chance;
        }

        @Override
        @Nullable
        public BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                 @NotNull Context context) {
            if (input.blockType().equals(from) && context.random().nextFloat() < chance) {
                return input.withBlockType(to);
            }
            return input;
        }
    }

    private static final class GravityProcessor extends StructureProcessor {
        static final GravityProcessor INSTANCE = new GravityProcessor();

        @Override
        @Nullable
        public BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                 @NotNull Context context) {
            // Check if block below is air
            BlockType below = context.getBlock(input.x(), input.y() - 1, input.z());
            if (below.isAir()) {
                // Find ground level
                int groundY = input.y() - 1;
                while (groundY > context.worldInfo().getMinHeight() &&
                       context.getBlock(input.x(), groundY - 1, input.z()).isAir()) {
                    groundY--;
                }
                return new BlockInfo(input.blockType(), input.blockData(), input.x(), groundY, input.z());
            }
            return input;
        }
    }

    private static final class AirProtectionProcessor extends StructureProcessor {
        static final AirProtectionProcessor INSTANCE = new AirProtectionProcessor();

        @Override
        @Nullable
        public BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                 @NotNull Context context) {
            // Don't place air blocks
            if (input.blockType().isAir()) {
                return null;
            }
            return input;
        }
    }

    private static final class GroundOnlyProcessor extends StructureProcessor {
        static final GroundOnlyProcessor INSTANCE = new GroundOnlyProcessor();

        @Override
        @Nullable
        public BlockInfo process(@NotNull BlockInfo input, @NotNull BlockInfo existing,
                                 @NotNull Context context) {
            // Only place if on solid ground
            BlockType below = context.getBlock(input.x(), input.y() - 1, input.z());
            if (below.isAir() || below.isLiquid()) {
                return null;
            }
            return input;
        }
    }
}
