/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.validation.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a numeric value is within a specified range.
 *
 * <p>This constraint can be applied to any numeric field (int, long, float,
 * double, Integer, Long, Float, Double, etc.). Both minimum and maximum
 * bounds are inclusive.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class GameConfig {
 *
 *     @Range(min = 1, max = 100)
 *     private int maxPlayers = 16;
 *
 *     @Range(min = 0.0, max = 1.0)
 *     private double spawnChance = 0.5;
 *
 *     @Range(min = 1000, max = 60000)
 *     private long timeoutMs = 5000;
 * }
 * }</pre>
 *
 * <h2>Custom Error Message</h2>
 * <pre>{@code
 * @Range(min = 1, max = 10, message = "Difficulty must be between 1 and 10")
 * private int difficulty = 5;
 * }</pre>
 *
 * <h2>Unbounded Range</h2>
 * <pre>{@code
 * // Minimum only (no maximum)
 * @Range(min = 0)
 * private int positiveValue = 10;
 *
 * // Maximum only (no minimum)
 * @Range(max = 100)
 * private int percentValue = 50;
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see NotEmpty
 * @see Pattern
 * @see MinLength
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {

    /**
     * The minimum allowed value (inclusive).
     *
     * <p>Defaults to {@link Double#MIN_VALUE} for no minimum bound.</p>
     *
     * @return the minimum value
     * @since 1.0.0
     */
    double min() default Double.MIN_VALUE;

    /**
     * The maximum allowed value (inclusive).
     *
     * <p>Defaults to {@link Double#MAX_VALUE} for no maximum bound.</p>
     *
     * @return the maximum value
     * @since 1.0.0
     */
    double max() default Double.MAX_VALUE;

    /**
     * Custom error message.
     *
     * <p>If empty, a default message is generated.</p>
     *
     * @return the error message
     * @since 1.0.0
     */
    String message() default "";

    /**
     * Validation groups this constraint belongs to.
     *
     * @return the groups
     * @since 1.0.0
     */
    String[] groups() default {};
}
