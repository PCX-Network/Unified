/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.condition;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Factory interface for creating conditions from configuration.
 *
 * <p>Condition factories are registered with the {@link ConditionService}
 * to enable parsing of custom condition types from configuration files
 * or expression strings.</p>
 *
 * <h2>Example Implementation:</h2>
 * <pre>{@code
 * public class LevelConditionFactory implements ConditionFactory {
 *
 *     @Override
 *     public String getType() {
 *         return "level";
 *     }
 *
 *     @Override
 *     public Condition create(String value) {
 *         int level = Integer.parseInt(value);
 *         return new LevelCondition(level);
 *     }
 *
 *     @Override
 *     public Condition create(Map<String, Object> config) {
 *         int minLevel = (int) config.getOrDefault("min", 0);
 *         int maxLevel = (int) config.getOrDefault("max", Integer.MAX_VALUE);
 *         return new LevelRangeCondition(minLevel, maxLevel);
 *     }
 * }
 *
 * // Register the factory
 * conditionService.registerConditionType("level", new LevelConditionFactory());
 *
 * // Now these expressions work:
 * Condition c1 = Condition.expression("level:50");
 * Condition c2 = conditionService.parse(Map.of("type", "level", "min", 10, "max", 50));
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionService
 * @see Condition
 */
@FunctionalInterface
public interface ConditionFactory {

    /**
     * Creates a condition from a simple string value.
     *
     * <p>This is used when parsing expressions like {@code type:value}.</p>
     *
     * @param value the condition value/arguments
     * @return the created condition
     * @throws ConditionParseException if the value is invalid
     * @since 1.0.0
     */
    @NotNull
    Condition create(@NotNull String value);

    /**
     * Creates a condition from a configuration map.
     *
     * <p>The default implementation delegates to {@link #create(String)}
     * using the "value" key from the map.</p>
     *
     * @param config the configuration map
     * @return the created condition
     * @throws ConditionParseException if the configuration is invalid
     * @since 1.0.0
     */
    @NotNull
    default Condition create(@NotNull Map<String, Object> config) {
        Object value = config.get("value");
        if (value == null) {
            throw new ConditionParseException("Missing 'value' in condition config");
        }
        return create(String.valueOf(value));
    }

    /**
     * Returns the condition type this factory handles.
     *
     * @return the condition type identifier
     * @since 1.0.0
     */
    @NotNull
    default String getType() {
        String name = getClass().getSimpleName();
        if (name.endsWith("ConditionFactory")) {
            name = name.substring(0, name.length() - 16);
        } else if (name.endsWith("Factory")) {
            name = name.substring(0, name.length() - 7);
        }
        return name.toLowerCase();
    }

    /**
     * Returns a description of this condition type.
     *
     * @return the description
     * @since 1.0.0
     */
    @NotNull
    default String getDescription() {
        return "Condition type: " + getType();
    }

    /**
     * Returns usage examples for this condition type.
     *
     * @return usage examples
     * @since 1.0.0
     */
    @NotNull
    default String[] getExamples() {
        return new String[]{ getType() + ":value" };
    }
}
