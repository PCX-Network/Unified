/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import java.lang.annotation.*;

/**
 * Marks a test method as requiring a specific world configuration.
 *
 * <p>When applied to a test method, a MockWorld with the specified
 * properties will be created and can be injected as a parameter.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @UnifiedTest
 * class WorldOperationsTest {
 *
 *     @Test
 *     @WithWorld(name = "arena", type = WorldType.FLAT)
 *     void testArenaWorld(MockWorld arena) {
 *         assertThat(arena.getName()).isEqualTo("arena");
 *
 *         MockPlayer player = server.addPlayer("Steve");
 *         player.teleport(arena.getSpawnLocation());
 *
 *         assertThat(player.getWorld()).isEqualTo(arena);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WithWorld {

    /**
     * The world name.
     *
     * @return the name
     */
    String name();

    /**
     * The world type/environment.
     *
     * @return the world type
     */
    WorldType type() default WorldType.NORMAL;

    /**
     * The world seed.
     *
     * @return the seed (0 for random)
     */
    long seed() default 0;

    /**
     * The spawn location X coordinate.
     *
     * @return the X coordinate
     */
    int spawnX() default 0;

    /**
     * The spawn location Y coordinate.
     *
     * @return the Y coordinate
     */
    int spawnY() default 64;

    /**
     * The spawn location Z coordinate.
     *
     * @return the Z coordinate
     */
    int spawnZ() default 0;

    /**
     * World type enumeration for annotation use.
     */
    enum WorldType {
        /** Normal overworld. */
        NORMAL,
        /** Nether dimension. */
        NETHER,
        /** End dimension. */
        THE_END,
        /** Flat/superflat world. */
        FLAT,
        /** Void world. */
        VOID
    }
}
