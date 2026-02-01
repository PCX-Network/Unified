/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import java.lang.annotation.*;

/**
 * Grants a permission to the test player.
 *
 * <p>Used in conjunction with {@link WithPlayer} to set up player permissions.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Test
 * @WithPlayer("Admin")
 * @WithPermission("myplugin.admin")
 * @WithPermission("myplugin.moderator")
 * void testWithPermissions(MockPlayer admin) {
 *     assertThat(admin.hasPermission("myplugin.admin")).isTrue();
 *     assertThat(admin.hasPermission("myplugin.moderator")).isTrue();
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(WithPermissions.class)
public @interface WithPermission {

    /**
     * The permission node to grant.
     *
     * @return the permission
     */
    String value();
}
