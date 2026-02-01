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
 * Validates that a string value matches a regular expression pattern.
 *
 * <p>This constraint uses Java's {@link java.util.regex.Pattern} for
 * matching. The pattern must match the entire string (like using
 * {@code matches()}, not {@code find()}).</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class PlayerConfig {
 *
 *     // Alphanumeric with underscores
 *     @Pattern(regex = "^[a-zA-Z0-9_]+$")
 *     private String username = "Player1";
 *
 *     // Valid email format
 *     @Pattern(regex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")
 *     private String email = "player@example.com";
 *
 *     // Minecraft color code
 *     @Pattern(regex = "^&[0-9a-fk-or]$")
 *     private String chatColor = "&a";
 * }
 * }</pre>
 *
 * <h2>Custom Error Message</h2>
 * <pre>{@code
 * @Pattern(
 *     regex = "^[a-zA-Z0-9_]{3,16}$",
 *     message = "Username must be 3-16 characters, alphanumeric or underscore"
 * )
 * private String username;
 * }</pre>
 *
 * <h2>Pattern Flags</h2>
 * <pre>{@code
 * // Case-insensitive matching
 * @Pattern(
 *     regex = "^(yes|no|maybe)$",
 *     flags = java.util.regex.Pattern.CASE_INSENSITIVE
 * )
 * private String response = "Yes";
 * }</pre>
 *
 * <h2>Common Patterns</h2>
 * <ul>
 *   <li>Username: {@code ^[a-zA-Z0-9_]{3,16}$}</li>
 *   <li>IP Address: {@code ^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$}</li>
 *   <li>UUID: {@code ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$}</li>
 *   <li>Hex Color: {@code ^#[0-9a-fA-F]{6}$}</li>
 *   <li>URL Path: {@code ^/[a-zA-Z0-9/_-]*$}</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Range
 * @see NotEmpty
 * @see MinLength
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Pattern {

    /**
     * The regular expression pattern to match against.
     *
     * @return the regex pattern
     * @since 1.0.0
     */
    String regex();

    /**
     * Pattern matching flags.
     *
     * <p>Use constants from {@link java.util.regex.Pattern} such as
     * {@code Pattern.CASE_INSENSITIVE}, {@code Pattern.MULTILINE}, etc.</p>
     *
     * @return the pattern flags
     * @since 1.0.0
     */
    int flags() default 0;

    /**
     * Custom error message.
     *
     * <p>If empty, a default message is generated showing the pattern.</p>
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
