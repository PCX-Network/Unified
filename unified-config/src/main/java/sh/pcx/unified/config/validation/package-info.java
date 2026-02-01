/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Configuration validation framework with constraint annotations.
 *
 * <p>This package provides a validation system for configuration values,
 * including built-in constraints and support for custom validators.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.validation.ConfigValidator} - Validates config objects</li>
 *   <li>{@link sh.pcx.unified.config.validation.ValidationResult} - Result of validation</li>
 *   <li>{@link sh.pcx.unified.config.validation.ValidationConstraint} - Custom constraint interface</li>
 * </ul>
 *
 * <h2>Built-in Constraints</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.Range} - Numeric range</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.NotEmpty} - Non-empty values</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.Pattern} - Regex matching</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.MinLength} - Minimum length</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class ServerConfig {
 *     @NotEmpty
 *     private String name;
 *
 *     @Range(min = 1, max = 100)
 *     private int maxPlayers;
 *
 *     @Pattern(regex = "^[a-z]+$")
 *     private String mode;
 * }
 *
 * // Validate
 * ConfigValidator validator = new ConfigValidator();
 * ValidationResult result = validator.validate(config);
 * if (result.hasErrors()) {
 *     result.getErrors().forEach(System.out::println);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.validation;
