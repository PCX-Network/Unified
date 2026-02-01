/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.placeholder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a placeholder handler within a {@link PlaceholderExpansion}.
 *
 * <p>Methods annotated with {@code @Placeholder} are automatically discovered and
 * registered to handle specific placeholder requests. The method signature determines
 * how the placeholder can be used.
 *
 * <h2>Method Signatures</h2>
 * <p>Placeholder methods can have various signatures:
 *
 * <h3>No Arguments (Server-wide)</h3>
 * <pre>{@code
 * @Placeholder("tps")
 * public String getTps() {
 *     return String.format("%.2f", server.getTPS());
 * }
 * // Usage: %server_tps%
 * }</pre>
 *
 * <h3>Player Context</h3>
 * <pre>{@code
 * @Placeholder("health")
 * public String getHealth(UnifiedPlayer player) {
 *     return String.format("%.1f", player.getHealth());
 * }
 * // Usage: %myplugin_health%
 * }</pre>
 *
 * <h3>With Parameters</h3>
 * <pre>{@code
 * @Placeholder("stat_")
 * public String getStat(UnifiedPlayer player, String statName) {
 *     return stats.getStat(player, statName);
 * }
 * // Usage: %myplugin_stat_kills%, %myplugin_stat_deaths%
 * }</pre>
 *
 * <h3>Full Context</h3>
 * <pre>{@code
 * @Placeholder("custom")
 * public String getCustom(PlaceholderContext context) {
 *     return context.getPlayer()
 *         .map(p -> "Hello, " + p.getName())
 *         .orElse("Hello, World");
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlaceholderExpansion
 * @see PlaceholderContext
 * @see Relational
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Placeholder {

    /**
     * The placeholder name or prefix.
     *
     * <p>If the name ends with an underscore, it acts as a prefix and the remainder
     * of the placeholder text is passed as a parameter to the method.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "name"} - Exact match for {@code %expansion_name%}</li>
     *   <li>{@code "stat_"} - Prefix match for {@code %expansion_stat_kills%}, etc.</li>
     * </ul>
     *
     * @return the placeholder name or prefix
     */
    String value();

    /**
     * A description of what this placeholder returns.
     *
     * <p>Used for documentation and help commands.
     *
     * @return the placeholder description
     */
    String description() default "";

    /**
     * Example usage of this placeholder.
     *
     * @return example usage strings
     */
    String[] examples() default {};

    /**
     * Whether this placeholder can be cached.
     *
     * <p>Set to {@code false} for placeholders that return dynamic values
     * that should not be cached (e.g., current time, random values).
     *
     * @return {@code true} if the result can be cached (default)
     */
    boolean cacheable() default true;

    /**
     * The cache time-to-live in milliseconds.
     *
     * <p>Only applies if {@link #cacheable()} is {@code true}.
     * A value of 0 uses the default TTL from the cache configuration.
     *
     * @return the TTL in milliseconds, or 0 for default
     */
    long cacheTTL() default 0;

    /**
     * Whether this placeholder should be computed asynchronously.
     *
     * <p>Set to {@code true} for placeholders that perform slow operations
     * (database queries, API calls, etc.) to prevent blocking the main thread.
     *
     * @return {@code true} if async computation is preferred
     */
    boolean async() default false;

    /**
     * The fallback value to return if resolution fails.
     *
     * @return the fallback value
     */
    String fallback() default "";
}
