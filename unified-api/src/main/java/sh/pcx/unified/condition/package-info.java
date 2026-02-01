/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Conditional System API for dynamic behavior based on permissions, time, placeholders, and custom conditions.
 *
 * <p>This package provides the public API interfaces for the condition system, including:
 * <ul>
 *   <li>{@link sh.pcx.unified.condition.Condition} - Base condition interface</li>
 *   <li>{@link sh.pcx.unified.condition.ConditionService} - Central service for condition management</li>
 *   <li>{@link sh.pcx.unified.condition.ConditionContext} - Context for condition evaluation</li>
 *   <li>{@link sh.pcx.unified.condition.ConditionResult} - Result of condition evaluation</li>
 *   <li>{@link sh.pcx.unified.condition.ConditionalGroup} - Group assignment based on conditions</li>
 *   <li>{@link sh.pcx.unified.condition.TemporaryCondition} - Time-limited conditions</li>
 *   <li>{@link sh.pcx.unified.condition.CronExpression} - Cron-based time expressions</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * @Inject
 * private ConditionService conditions;
 *
 * // Permission-based condition
 * Condition isVip = Condition.permission("group.vip");
 *
 * // Time-based condition (cron)
 * Condition isWeekend = Condition.cron("0 0 * * SAT,SUN");
 *
 * // Placeholder-based
 * Condition highLevel = Condition.placeholder("%player_level%")
 *     .greaterThan(50);
 *
 * // Combined conditions
 * Condition vipNightBonus = Condition.all(isVip, isNight);
 *
 * // Evaluate
 * boolean result = conditions.evaluate(player, isVip);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.condition;
