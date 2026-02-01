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
 * Validates that a value has at least a minimum length/size.
 *
 * <p>This constraint works with multiple types:</p>
 * <ul>
 *   <li><b>Strings:</b> Checks character count</li>
 *   <li><b>Collections:</b> Checks element count</li>
 *   <li><b>Maps:</b> Checks entry count</li>
 *   <li><b>Arrays:</b> Checks array length</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class GameConfig {
 *
 *     // String must be at least 3 characters
 *     @MinLength(3)
 *     private String playerName = "Steve";
 *
 *     // List must have at least 1 element
 *     @MinLength(1)
 *     private List<String> enabledModes = List.of("survival");
 *
 *     // Map must have at least 2 entries
 *     @MinLength(2)
 *     private Map<String, Integer> teamSizes = Map.of("small", 2, "large", 8);
 * }
 * }</pre>
 *
 * <h2>Custom Error Message</h2>
 * <pre>{@code
 * @MinLength(value = 5, message = "Password must be at least 5 characters")
 * private String password;
 * }</pre>
 *
 * <h2>Combining with Other Constraints</h2>
 * <pre>{@code
 * @NotEmpty
 * @MinLength(3)
 * @Pattern(regex = "^[a-zA-Z]+$")
 * private String firstName;
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Range
 * @see NotEmpty
 * @see Pattern
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MinLength {

    /**
     * The minimum required length/size.
     *
     * @return the minimum length
     * @since 1.0.0
     */
    int value();

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
