/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Marks a test class or method as requiring a mock server.
 *
 * <p>This is a lighter alternative to {@link UnifiedTest} that only
 * provides the server without plugin loading.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @ServerTest
 * class ServerOperationsTest {
 *
 *     @Test
 *     void testServerProperties(MockServer server) {
 *         assertThat(server.getName()).isEqualTo("MockServer");
 *         assertThat(server.getOnlinePlayers()).isEmpty();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(UnifiedTestExtension.class)
public @interface ServerTest {

    /**
     * The Minecraft version to simulate.
     *
     * @return the version string (e.g., "1.21.4")
     */
    String minecraftVersion() default "1.21.4";

    /**
     * The maximum number of players.
     *
     * @return the max players
     */
    int maxPlayers() default 20;

    /**
     * Whether online mode is enabled.
     *
     * @return the online mode setting
     */
    boolean onlineMode() default true;
}
