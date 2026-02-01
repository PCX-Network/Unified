/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Marks a test class as a UnifiedPlugin test.
 *
 * <p>This annotation automatically sets up and tears down the MockServer
 * for the test class. It also enables injection of MockServer, plugins,
 * and other test resources.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @UnifiedTest
 * class MyPluginTest {
 *
 *     @Inject
 *     MockServer server;
 *
 *     @Inject
 *     MyPlugin plugin;
 *
 *     @Test
 *     void testSomething() {
 *         MockPlayer player = server.addPlayer("Steve");
 *         assertThat(player).isNotNull();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see ServerTest
 * @see WorldTest
 * @see WithPlayer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(UnifiedTestExtension.class)
public @interface UnifiedTest {

    /**
     * Plugin classes to automatically load.
     *
     * @return the plugin classes to load
     */
    Class<?>[] plugins() default {};

    /**
     * Whether to reset the server between tests.
     *
     * @return true to reset between tests (default: true)
     */
    boolean resetBetweenTests() default true;

    /**
     * The default world name to create.
     *
     * @return the default world name
     */
    String defaultWorld() default "world";
}
