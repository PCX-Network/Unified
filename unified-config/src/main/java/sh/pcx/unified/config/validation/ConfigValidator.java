/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.validation;

import sh.pcx.unified.config.annotation.ConfigValidate;
import sh.pcx.unified.config.validation.constraint.MinLength;
import sh.pcx.unified.config.validation.constraint.NotEmpty;
import sh.pcx.unified.config.validation.constraint.Pattern;
import sh.pcx.unified.config.validation.constraint.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates configuration objects against defined constraints.
 *
 * <p>ConfigValidator scans configuration classes for validation annotations
 * and applies the corresponding constraints. It supports both built-in
 * constraints ({@link Range}, {@link NotEmpty}, {@link Pattern}, {@link MinLength})
 * and custom constraints via {@link ConfigValidate}.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * ConfigValidator validator = new ConfigValidator();
 *
 * // Validate a configuration
 * ValidationResult result = validator.validate(config);
 *
 * if (result.hasErrors()) {
 *     result.getErrors().forEach(error ->
 *         logger.warning(error.getFullMessage())
 *     );
 * }
 * }</pre>
 *
 * <h2>Registering Custom Constraints</h2>
 * <pre>{@code
 * // Register a constraint for a specific annotation
 * validator.registerConstraint(MyConstraint.class, (annotation, value, path) -> {
 *     MyConstraint mc = (MyConstraint) annotation;
 *     if (!isValid(value, mc)) {
 *         return ValidationResult.failure("Invalid value at " + path, path);
 *     }
 *     return ValidationResult.success();
 * });
 * }</pre>
 *
 * <h2>Validation Groups</h2>
 * <pre>{@code
 * // Validate only specific groups
 * ValidationResult result = validator.validate(config, "production");
 *
 * // Validate all groups
 * ValidationResult result = validator.validate(config);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ValidationConstraint
 * @see ValidationResult
 */
public class ConfigValidator {

    private final Map<Class<? extends Annotation>, AnnotationConstraintHandler<?>> handlers;
    private final Map<Class<?>, List<FieldValidator>> validatorCache;

    /**
     * Creates a new ConfigValidator with built-in constraints registered.
     */
    public ConfigValidator() {
        this.handlers = new ConcurrentHashMap<>();
        this.validatorCache = new ConcurrentHashMap<>();
        registerBuiltInConstraints();
    }

    /**
     * Registers built-in constraint handlers.
     */
    private void registerBuiltInConstraints() {
        // Range constraint
        registerConstraint(Range.class, (annotation, value, path) -> {
            if (value == null) {
                return ValidationResult.success();
            }
            if (!(value instanceof Number)) {
                return ValidationResult.failure(
                        "Expected a number for @Range validation, got: " + value.getClass().getSimpleName(),
                        path
                );
            }

            double numValue = ((Number) value).doubleValue();
            double min = annotation.min();
            double max = annotation.max();

            if (numValue < min || numValue > max) {
                String message = annotation.message().isEmpty()
                        ? String.format("Value must be between %.2f and %.2f, got: %.2f", min, max, numValue)
                        : annotation.message();
                return ValidationResult.failure(message, path);
            }
            return ValidationResult.success();
        });

        // NotEmpty constraint
        registerConstraint(NotEmpty.class, (annotation, value, path) -> {
            boolean empty = value == null ||
                    (value instanceof String && ((String) value).isEmpty()) ||
                    (value instanceof Collection && ((Collection<?>) value).isEmpty()) ||
                    (value instanceof Map && ((Map<?, ?>) value).isEmpty()) ||
                    (value.getClass().isArray() && java.lang.reflect.Array.getLength(value) == 0);

            if (empty) {
                String message = annotation.message().isEmpty()
                        ? "Value must not be empty"
                        : annotation.message();
                return ValidationResult.failure(message, path);
            }
            return ValidationResult.success();
        });

        // Pattern constraint
        registerConstraint(Pattern.class, (annotation, value, path) -> {
            if (value == null) {
                return ValidationResult.success();
            }

            String strValue = String.valueOf(value);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    annotation.regex(),
                    annotation.flags()
            );

            if (!pattern.matcher(strValue).matches()) {
                String message = annotation.message().isEmpty()
                        ? String.format("Value does not match pattern '%s': %s", annotation.regex(), strValue)
                        : annotation.message();
                return ValidationResult.failure(message, path);
            }
            return ValidationResult.success();
        });

        // MinLength constraint
        registerConstraint(MinLength.class, (annotation, value, path) -> {
            if (value == null) {
                if (annotation.value() > 0) {
                    return ValidationResult.failure("Value is null but minimum length is " + annotation.value(), path);
                }
                return ValidationResult.success();
            }

            int length;
            if (value instanceof String) {
                length = ((String) value).length();
            } else if (value instanceof Collection) {
                length = ((Collection<?>) value).size();
            } else if (value instanceof Map) {
                length = ((Map<?, ?>) value).size();
            } else if (value.getClass().isArray()) {
                length = java.lang.reflect.Array.getLength(value);
            } else {
                return ValidationResult.success();
            }

            if (length < annotation.value()) {
                String message = annotation.message().isEmpty()
                        ? String.format("Length must be at least %d, got: %d", annotation.value(), length)
                        : annotation.message();
                return ValidationResult.failure(message, path);
            }
            return ValidationResult.success();
        });
    }

    /**
     * Registers a constraint handler for an annotation type.
     *
     * @param annotationType the annotation type
     * @param handler the constraint handler
     * @param <A> the annotation type
     * @since 1.0.0
     */
    public <A extends Annotation> void registerConstraint(
            @NotNull Class<A> annotationType,
            @NotNull AnnotationConstraintHandler<A> handler
    ) {
        handlers.put(annotationType, handler);
        validatorCache.clear(); // Clear cache when handlers change
    }

    /**
     * Validates a configuration object.
     *
     * @param config the configuration to validate
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult validate(@NotNull Object config) {
        return validate(config, (String[]) null);
    }

    /**
     * Validates a configuration object for specific groups.
     *
     * @param config the configuration to validate
     * @param groups the validation groups to check
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult validate(@NotNull Object config, @Nullable String... groups) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        validateObject(config, "", errors, groups);
        return ValidationResult.of(errors);
    }

    /**
     * Validates a single field value.
     *
     * @param value the value to validate
     * @param field the field definition
     * @param path the path to the field
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    public ValidationResult validateField(
            @Nullable Object value,
            @NotNull Field field,
            @NotNull String path
    ) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        validateFieldValue(value, field, path, errors, null);
        return ValidationResult.of(errors);
    }

    /**
     * Recursively validates an object and its nested fields.
     */
    private void validateObject(
            @NotNull Object config,
            @NotNull String basePath,
            @NotNull List<ValidationResult.ValidationError> errors,
            @Nullable String[] groups
    ) {
        Class<?> clazz = config.getClass();

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);

            String fieldPath = basePath.isEmpty() ? field.getName() : basePath + "." + field.getName();

            try {
                Object value = field.get(config);

                // Validate the field
                validateFieldValue(value, field, fieldPath, errors, groups);

                // Recursively validate nested objects
                if (value != null && isConfigSerializable(value.getClass())) {
                    validateObject(value, fieldPath, errors, groups);
                }

                // Validate collection elements
                if (value instanceof Collection<?> collection) {
                    int index = 0;
                    for (Object element : collection) {
                        if (element != null && isConfigSerializable(element.getClass())) {
                            validateObject(element, fieldPath + "[" + index + "]", errors, groups);
                        }
                        index++;
                    }
                }

                // Validate map values
                if (value instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        Object mapValue = entry.getValue();
                        if (mapValue != null && isConfigSerializable(mapValue.getClass())) {
                            validateObject(mapValue, fieldPath + "." + entry.getKey(), errors, groups);
                        }
                    }
                }

            } catch (IllegalAccessException e) {
                errors.add(new ValidationResult.ValidationError(
                        "Failed to access field: " + e.getMessage(),
                        fieldPath
                ));
            }
        }
    }

    /**
     * Validates a single field's value against its constraints.
     */
    private void validateFieldValue(
            @Nullable Object value,
            @NotNull Field field,
            @NotNull String path,
            @NotNull List<ValidationResult.ValidationError> errors,
            @Nullable String[] groups
    ) {
        for (Annotation annotation : field.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            // Check if we have a handler for this annotation
            @SuppressWarnings("unchecked")
            AnnotationConstraintHandler<Annotation> handler =
                    (AnnotationConstraintHandler<Annotation>) handlers.get(annotationType);

            if (handler != null) {
                // Check if groups match
                if (groups != null && !matchesGroups(annotation, groups)) {
                    continue;
                }

                ValidationResult result = handler.validate(annotation, value, path);
                errors.addAll(result.getErrors());
            }

            // Handle @ConfigValidate annotation
            if (annotation instanceof ConfigValidate validate) {
                if (groups != null && !matchesGroups(validate.groups(), groups)) {
                    continue;
                }

                try {
                    ValidationConstraint<?> constraint = validate.value().getDeclaredConstructor().newInstance();
                    @SuppressWarnings("unchecked")
                    ValidationConstraint<Object> typedConstraint = (ValidationConstraint<Object>) constraint;

                    if (!typedConstraint.isValid(value, path)) {
                        String message = validate.message().isEmpty()
                                ? typedConstraint.getMessage(value, path)
                                : validate.message();
                        errors.add(new ValidationResult.ValidationError(message, path));
                    }
                } catch (Exception e) {
                    errors.add(new ValidationResult.ValidationError(
                            "Failed to instantiate validator " + validate.value().getName() + ": " + e.getMessage(),
                            path
                    ));
                }
            }

            // Handle @ConfigValidate.List (multiple validators)
            if (annotation instanceof ConfigValidate.List validateList) {
                for (ConfigValidate validate : validateList.value()) {
                    if (groups != null && !matchesGroups(validate.groups(), groups)) {
                        continue;
                    }

                    try {
                        ValidationConstraint<?> constraint = validate.value().getDeclaredConstructor().newInstance();
                        @SuppressWarnings("unchecked")
                        ValidationConstraint<Object> typedConstraint = (ValidationConstraint<Object>) constraint;

                        if (!typedConstraint.isValid(value, path)) {
                            String message = validate.message().isEmpty()
                                    ? typedConstraint.getMessage(value, path)
                                    : validate.message();
                            errors.add(new ValidationResult.ValidationError(message, path));
                        }
                    } catch (Exception e) {
                        errors.add(new ValidationResult.ValidationError(
                                "Failed to instantiate validator: " + e.getMessage(),
                                path
                        ));
                    }
                }
            }
        }
    }

    /**
     * Checks if a validation group matches.
     */
    private boolean matchesGroups(@NotNull Annotation annotation, @NotNull String[] groups) {
        try {
            java.lang.reflect.Method groupsMethod = annotation.annotationType().getMethod("groups");
            String[] annotationGroups = (String[]) groupsMethod.invoke(annotation);
            return matchesGroups(annotationGroups, groups);
        } catch (Exception e) {
            return true; // No groups method, always matches
        }
    }

    /**
     * Checks if annotation groups match requested groups.
     */
    private boolean matchesGroups(@NotNull String[] annotationGroups, @NotNull String[] requestedGroups) {
        if (annotationGroups.length == 0) {
            return true; // No groups specified, matches all
        }

        for (String annotationGroup : annotationGroups) {
            for (String requestedGroup : requestedGroups) {
                if (annotationGroup.equals(requestedGroup)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets all fields including from superclasses.
     */
    @NotNull
    private List<Field> getAllFields(@NotNull Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     * Checks if a class is marked as configuration serializable.
     */
    private boolean isConfigSerializable(@NotNull Class<?> clazz) {
        return clazz.isAnnotationPresent(sh.pcx.unified.config.annotation.ConfigSerializable.class) ||
                clazz.isAnnotationPresent(org.spongepowered.configurate.objectmapping.ConfigSerializable.class);
    }

    /**
     * Functional interface for annotation-based constraint validation.
     *
     * @param <A> the annotation type
     */
    @FunctionalInterface
    public interface AnnotationConstraintHandler<A extends Annotation> {

        /**
         * Validates a value against the annotation constraint.
         *
         * @param annotation the constraint annotation
         * @param value the value to validate
         * @param path the path to the value
         * @return the validation result
         */
        @NotNull
        ValidationResult validate(@NotNull A annotation, @Nullable Object value, @NotNull String path);
    }

    /**
     * Internal field validator holder.
     */
    private record FieldValidator(Field field, List<AnnotationConstraintHandler<?>> handlers) {
    }
}
