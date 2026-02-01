/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

/**
 * Marks a test class as an integration test.
 *
 * <p>Integration tests are typically slower and may require more setup.
 * They are tagged with "integration" to allow separate execution from
 * unit tests.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @IntegrationTest
 * class FullGameFlowTest {
 *
 *     @Test
 *     void testCompleteGameSession() {
 *         MockServer server = MockServer.start();
 *         MyPlugin plugin = server.loadPlugin(MyPlugin.class);
 *
 *         // Simulate full game flow
 *         MockPlayer player1 = server.addPlayer("Player1");
 *         MockPlayer player2 = server.addPlayer("Player2");
 *
 *         player1.performCommand("game create arena");
 *         player2.performCommand("game join arena");
 *
 *         server.advanceTicks(20);
 *
 *         // ... test complete flow
 *
 *         server.stop();
 *     }
 * }
 * }</pre>
 *
 * <p>To run only integration tests:
 * <pre>
 * ./gradlew test --tests "*" -Pgroups=integration
 * </pre>
 *
 * <p>To exclude integration tests:
 * <pre>
 * ./gradlew test --tests "*" -PexcludeGroups=integration
 * </pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Tag("integration")
@ExtendWith(UnifiedTestExtension.class)
public @interface IntegrationTest {

    /**
     * Description of what this integration test verifies.
     *
     * @return the description
     */
    String value() default "";

    /**
     * Timeout in seconds for the entire test class.
     *
     * @return the timeout in seconds
     */
    int timeout() default 60;
}
