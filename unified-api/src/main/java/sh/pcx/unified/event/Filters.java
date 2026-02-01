/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link Filter} annotations.
 *
 * <p>This annotation is automatically used by the Java compiler when multiple
 * {@code @Filter} annotations are applied to the same method. It can also be
 * used explicitly for clarity.
 *
 * <h2>Implicit Usage</h2>
 * <pre>{@code
 * @EventHandler
 * @Filter(permission = "vip")
 * @Filter(world = "spawn")
 * public void onVipInSpawn(PlayerMoveEvent event) {
 *     // Automatically wrapped in @Filters
 * }
 * }</pre>
 *
 * <h2>Explicit Usage</h2>
 * <pre>{@code
 * @EventHandler
 * @Filters({
 *     @Filter(permission = "vip"),
 *     @Filter(world = "spawn")
 * })
 * public void onVipInSpawn(PlayerMoveEvent event) {
 *     // Explicitly wrapped in @Filters
 * }
 * }</pre>
 *
 * <p>Both forms are equivalent. All filters are evaluated using AND logic;
 * the handler is only invoked if ALL filters pass.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Filter
 * @see EventHandler
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filters {

    /**
     * The array of filters to apply.
     *
     * <p>All filters are evaluated using AND logic. The handler is only
     * invoked if every filter in this array passes.
     *
     * @return the filters to apply
     * @since 1.0.0
     */
    Filter[] value();
}
