/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.world.condition;

import sh.pcx.unified.condition.ConditionContext;
import sh.pcx.unified.condition.ConditionResult;
import sh.pcx.unified.condition.PlaceholderCondition;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Implementation of {@link PlaceholderCondition} that integrates with placeholder providers.
 *
 * <p>This implementation can be configured with a custom placeholder resolver that
 * integrates with PlaceholderAPI or other placeholder systems.</p>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PlaceholderConditionImpl implements PlaceholderCondition {

    private final String placeholder;
    private final ComparisonOperator operator;
    private final Object value;
    private final BiFunction<UnifiedPlayer, String, String> placeholderResolver;

    private static volatile BiFunction<UnifiedPlayer, String, String> defaultResolver = null;

    /**
     * Creates a new placeholder condition.
     *
     * @param placeholder the placeholder string
     * @param operator    the comparison operator
     * @param value       the value to compare against
     */
    public PlaceholderConditionImpl(
            @NotNull String placeholder,
            @NotNull ComparisonOperator operator,
            @NotNull Object value
    ) {
        this(placeholder, operator, value, null);
    }

    /**
     * Creates a new placeholder condition with a custom resolver.
     *
     * @param placeholder         the placeholder string
     * @param operator            the comparison operator
     * @param value               the value to compare against
     * @param placeholderResolver the function that resolves placeholders
     */
    public PlaceholderConditionImpl(
            @NotNull String placeholder,
            @NotNull ComparisonOperator operator,
            @NotNull Object value,
            @Nullable BiFunction<UnifiedPlayer, String, String> placeholderResolver
    ) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder cannot be null");
        this.operator = Objects.requireNonNull(operator, "operator cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.placeholderResolver = placeholderResolver;
    }

    @Override
    public @NotNull String getPlaceholder() {
        return placeholder;
    }

    @Override
    public @NotNull ComparisonOperator getOperator() {
        return operator;
    }

    @Override
    public @NotNull Object getValue() {
        return value;
    }

    @Override
    public @NotNull ConditionResult evaluate(@NotNull ConditionContext context) {
        if (!context.hasPlayer()) {
            return ConditionResult.failure("No player in context for placeholder resolution");
        }

        UnifiedPlayer player = context.getPlayer().orElseThrow();
        BiFunction<UnifiedPlayer, String, String> resolver = getActiveResolver();

        if (resolver == null) {
            return ConditionResult.failure("Placeholder resolution not available - no placeholder provider configured");
        }

        try {
            String resolved = resolver.apply(player, placeholder);
            if (resolved == null) {
                return ConditionResult.failure("Placeholder '" + placeholder + "' resolved to null");
            }

            boolean matches = operator.compare(resolved, value);
            if (matches) {
                return ConditionResult.success(placeholder + " " + operator.getSymbol() + " " + value + " (actual: " + resolved + ")");
            }
            return ConditionResult.failure(placeholder + " " + operator.getSymbol() + " " + value + " failed (actual: " + resolved + ")");
        } catch (Exception e) {
            return ConditionResult.failure("Placeholder resolution error: " + e.getMessage());
        }
    }

    private BiFunction<UnifiedPlayer, String, String> getActiveResolver() {
        if (placeholderResolver != null) {
            return placeholderResolver;
        }
        return defaultResolver;
    }

    /**
     * Sets the default placeholder resolver used by all PlaceholderConditionImpl instances
     * that don't have a custom resolver.
     *
     * <p>This should be called during plugin initialization to integrate with
     * the placeholder provider being used on the server.</p>
     *
     * <h2>Example PlaceholderAPI Integration:</h2>
     * <pre>{@code
     * PlaceholderConditionImpl.setDefaultResolver((player, placeholder) -> {
     *     Player bukkitPlayer = player.getHandle();
     *     return PlaceholderAPI.setPlaceholders(bukkitPlayer, placeholder);
     * });
     * }</pre>
     *
     * @param resolver the placeholder resolver function, or null to disable
     * @since 1.0.0
     */
    public static void setDefaultResolver(@Nullable BiFunction<UnifiedPlayer, String, String> resolver) {
        defaultResolver = resolver;
    }

    /**
     * Returns whether a default placeholder resolver is configured.
     *
     * @return true if placeholder resolution is available
     * @since 1.0.0
     */
    public static boolean isPlaceholderResolutionAvailable() {
        return defaultResolver != null;
    }

    @Override
    public String toString() {
        return "PlaceholderCondition{" +
                "placeholder='" + placeholder + '\'' +
                ", operator=" + operator +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceholderConditionImpl that = (PlaceholderConditionImpl) o;
        return Objects.equals(placeholder, that.placeholder) &&
               operator == that.operator &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeholder, operator, value);
    }
}
