/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import sh.pcx.unified.player.UnifiedPlayer;

import java.lang.annotation.*;

/**
 * Automatically creates a mock player for the test method.
 *
 * <p>When applied to a test method, a MockPlayer with the specified
 * properties will be created and can be injected as a parameter.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @UnifiedTest
 * class PlayerTest {
 *
 *     @Test
 *     @WithPlayer("Steve")
 *     void testPlayer(MockPlayer steve) {
 *         assertThat(steve.getName()).isEqualTo("Steve");
 *     }
 *
 *     @Test
 *     @WithPlayer(name = "Admin", op = true)
 *     @WithPermission("myplugin.admin")
 *     void testAdminPlayer(MockPlayer admin) {
 *         assertThat(admin.isOp()).isTrue();
 *         assertThat(admin.hasPermission("myplugin.admin")).isTrue();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(WithPlayers.class)
public @interface WithPlayer {

    /**
     * The player name.
     *
     * @return the name
     */
    String value() default "";

    /**
     * The player name (alternative to value).
     *
     * @return the name
     */
    String name() default "";

    /**
     * Whether the player is an operator.
     *
     * @return true if op
     */
    boolean op() default false;

    /**
     * The game mode.
     *
     * @return the game mode
     */
    UnifiedPlayer.GameMode gameMode() default UnifiedPlayer.GameMode.SURVIVAL;

    /**
     * The initial health.
     *
     * @return the health
     */
    double health() default 20.0;

    /**
     * The initial food level.
     *
     * @return the food level
     */
    int foodLevel() default 20;
}
