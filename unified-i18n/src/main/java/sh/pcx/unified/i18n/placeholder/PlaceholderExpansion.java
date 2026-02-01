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
 * Marks a class as a placeholder expansion provider.
 *
 * <p>Classes annotated with {@code @PlaceholderExpansion} are automatically discovered
 * and registered with the {@link PlaceholderRegistry}. Each expansion provides a set
 * of placeholders under a common identifier prefix.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @PlaceholderExpansion(
 *     identifier = "myplugin",
 *     author = "MyName",
 *     version = "1.0.0"
 * )
 * public class MyPlaceholders {
 *
 *     @Placeholder("level")
 *     public String getLevel(UnifiedPlayer player) {
 *         return String.valueOf(player.getLevel());
 *     }
 *
 *     @Placeholder("balance")
 *     public String getBalance(UnifiedPlayer player) {
 *         return formatBalance(economy.getBalance(player));
 *     }
 * }
 * }</pre>
 *
 * <p>The placeholders above would be accessed as:
 * <ul>
 *   <li>{@code %myplugin_level%} - Returns the player's level</li>
 *   <li>{@code %myplugin_balance%} - Returns the player's balance</li>
 * </ul>
 *
 * <h2>PAPI Compatibility</h2>
 * <p>Expansions are automatically bridged to PlaceholderAPI when available,
 * allowing them to be used by other plugins that depend on PAPI.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Placeholder
 * @see PlaceholderRegistry
 * @see PAPIBridge
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PlaceholderExpansion {

    /**
     * The unique identifier for this expansion.
     *
     * <p>This identifier is used as the prefix for all placeholders in this expansion.
     * For example, an identifier of "server" would create placeholders like
     * {@code %server_name%}, {@code %server_tps%}, etc.
     *
     * <p>The identifier should be lowercase and contain only alphanumeric characters
     * and underscores.
     *
     * @return the expansion identifier
     */
    String identifier();

    /**
     * The author of this expansion.
     *
     * <p>This is primarily used for informational purposes and PAPI registration.
     *
     * @return the author name
     */
    String author() default "";

    /**
     * The version of this expansion.
     *
     * <p>Used for tracking and PAPI registration.
     *
     * @return the version string
     */
    String version() default "1.0.0";

    /**
     * A description of what this expansion provides.
     *
     * @return the expansion description
     */
    String description() default "";

    /**
     * Whether this expansion requires a player context.
     *
     * <p>If {@code true}, placeholders in this expansion will only be resolved
     * when a player is provided. If {@code false}, placeholders can be resolved
     * without a player context (server-wide placeholders).
     *
     * @return {@code true} if player context is required
     */
    boolean requiresPlayer() default true;

    /**
     * Whether this expansion should be registered with PlaceholderAPI.
     *
     * <p>Set to {@code false} if this expansion should only be available
     * through the UnifiedPlugin placeholder system.
     *
     * @return {@code true} to register with PAPI (default)
     */
    boolean registerWithPAPI() default true;

    /**
     * The priority of this expansion.
     *
     * <p>Higher priority expansions are resolved first when multiple expansions
     * provide the same placeholder identifier.
     *
     * @return the expansion priority (higher = first)
     */
    int priority() default 0;
}
