/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Built-in validation constraint annotations.
 *
 * <p>This package provides commonly used validation constraints that can
 * be applied to configuration fields.</p>
 *
 * <h2>Available Constraints</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.Range} -
 *       Validates numeric values are within a range</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.NotEmpty} -
 *       Validates values are not null or empty</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.Pattern} -
 *       Validates strings match a regex pattern</li>
 *   <li>{@link sh.pcx.unified.config.validation.constraint.MinLength} -
 *       Validates minimum length/size</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @ConfigSerializable
 * public class GameConfig {
 *     @Range(min = 2, max = 100)
 *     private int maxPlayers = 16;
 *
 *     @NotEmpty
 *     @MinLength(3)
 *     private String gameName = "default";
 *
 *     @Pattern(regex = "^[a-z_]+$", message = "Mode must be lowercase with underscores")
 *     private String gameMode = "survival";
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.config.validation.constraint;
