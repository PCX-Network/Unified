/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.annotation;

import sh.pcx.unified.config.validation.ValidationConstraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies a validation constraint to a configuration field.
 *
 * <p>This annotation specifies custom validators that are applied when
 * loading or validating a configuration. Multiple validators can be
 * applied to the same field using the repeatable annotation pattern.</p>
 *
 * <h2>Using Built-in Constraints</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class GameConfig {
 *
 *     // Use the @Range constraint directly
 *     @Range(min = 1, max = 100)
 *     private int maxPlayers = 16;
 *
 *     // Use the @NotEmpty constraint directly
 *     @NotEmpty
 *     private String serverName = "My Server";
 *
 *     // Use the @Pattern constraint directly
 *     @Pattern(regex = "^[a-zA-Z0-9_-]+$", message = "Invalid arena name format")
 *     private String arenaName = "default";
 * }
 * }</pre>
 *
 * <h2>Using Custom Constraints</h2>
 * <pre>{@code
 * // Define a custom validator
 * public class PortValidator implements ValidationConstraint<Integer> {
 *
 *     @Override
 *     public boolean isValid(Integer value, String path) {
 *         return value != null && value >= 1024 && value <= 65535;
 *     }
 *
 *     @Override
 *     public String getMessage(Integer value, String path) {
 *         return "Port must be between 1024 and 65535, got: " + value;
 *     }
 * }
 *
 * // Use the custom validator
 * @ConfigSerializable
 * public class ServerConfig {
 *
 *     @ConfigValidate(PortValidator.class)
 *     private int port = 25565;
 * }
 * }</pre>
 *
 * <h2>Multiple Constraints</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class PlayerConfig {
 *
 *     @ConfigValidate(NotNullValidator.class)
 *     @ConfigValidate(LengthValidator.class)
 *     @MinLength(3)
 *     private String username;
 *
 *     // Or use the container annotation
 *     @ConfigValidate.List({
 *         @ConfigValidate(NotNullValidator.class),
 *         @ConfigValidate(RangeValidator.class)
 *     })
 *     @Range(min = 1, max = 1000)
 *     private int score = 0;
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ValidationConstraint
 * @see sh.pcx.unified.config.validation.ConfigValidator
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ConfigValidate.List.class)
public @interface ConfigValidate {

    /**
     * The constraint class to apply.
     *
     * <p>The class must implement {@link ValidationConstraint} and have
     * a no-arg constructor.</p>
     *
     * @return the constraint class
     * @since 1.0.0
     */
    Class<? extends ValidationConstraint<?>> value();

    /**
     * Optional custom error message.
     *
     * <p>If specified, overrides the constraint's default message.</p>
     *
     * @return the custom error message
     * @since 1.0.0
     */
    String message() default "";

    /**
     * The validation groups this constraint belongs to.
     *
     * <p>Allows selective validation of different constraint groups.</p>
     *
     * @return array of group names
     * @since 1.0.0
     */
    String[] groups() default {};

    /**
     * Container annotation for multiple @ConfigValidate annotations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface List {
        /**
         * The array of validation constraints.
         *
         * @return the constraints
         */
        ConfigValidate[] value();
    }
}
