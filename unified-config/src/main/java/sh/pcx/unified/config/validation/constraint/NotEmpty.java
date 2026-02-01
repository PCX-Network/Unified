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
 * Validates that a value is not null or empty.
 *
 * <p>This constraint works with multiple types:</p>
 * <ul>
 *   <li><b>Strings:</b> Must not be null or empty ("")</li>
 *   <li><b>Collections:</b> Must not be null or have zero elements</li>
 *   <li><b>Maps:</b> Must not be null or have zero entries</li>
 *   <li><b>Arrays:</b> Must not be null or have zero length</li>
 *   <li><b>Other objects:</b> Must not be null</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class ServerConfig {
 *
 *     @NotEmpty
 *     private String serverName = "My Server";
 *
 *     @NotEmpty
 *     private List<String> allowedWorlds = List.of("world");
 *
 *     @NotEmpty
 *     private Map<String, Integer> permissions = Map.of("admin", 100);
 * }
 * }</pre>
 *
 * <h2>Custom Error Message</h2>
 * <pre>{@code
 * @NotEmpty(message = "Server name is required!")
 * private String serverName;
 * }</pre>
 *
 * <h2>Combining with Other Constraints</h2>
 * <pre>{@code
 * @NotEmpty
 * @MinLength(3)
 * @Pattern(regex = "^[a-zA-Z0-9_]+$")
 * private String username;
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Range
 * @see Pattern
 * @see MinLength
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotEmpty {

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
