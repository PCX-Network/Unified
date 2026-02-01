/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.advanced;

import sh.pcx.unified.config.ConfigException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies environment variable overrides to configuration objects.
 *
 * <p>EnvironmentOverrides supports two patterns for overriding configuration
 * values with environment variables:</p>
 * <ul>
 *   <li><b>String substitution:</b> Replace {@code ${ENV_VAR}} or
 *       {@code ${ENV_VAR:default}} in string values</li>
 *   <li><b>Path-based mapping:</b> Map environment variables to config paths
 *       (e.g., {@code DATABASE_HOST} maps to {@code database.host})</li>
 * </ul>
 *
 * <h2>String Substitution</h2>
 * <pre>{@code
 * // In config class
 * @ConfigDefault("${DATABASE_HOST:localhost}")
 * private String host;
 *
 * @ConfigDefault("${DATABASE_PORT:3306}")
 * private int port;
 *
 * // Apply substitutions
 * EnvironmentOverrides.substituteEnvVars(config);
 * }</pre>
 *
 * <h2>Path-Based Mapping</h2>
 * <pre>{@code
 * // Environment variables:
 * // MYAPP_DATABASE_HOST=production-db.example.com
 * // MYAPP_DATABASE_PORT=5432
 *
 * EnvironmentOverrides overrides = new EnvironmentOverrides("MYAPP");
 * overrides.apply(config);
 *
 * // config.database.host is now "production-db.example.com"
 * // config.database.port is now 5432
 * }</pre>
 *
 * <h2>Custom Mapping</h2>
 * <pre>{@code
 * EnvironmentOverrides overrides = new EnvironmentOverrides();
 * overrides.map("DB_URL", "database.url");
 * overrides.map("REDIS_HOST", "cache.host");
 * overrides.apply(config);
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class EnvironmentOverrides {

    /**
     * Pattern for ${ENV_VAR} or ${ENV_VAR:default} substitution.
     */
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private final String prefix;
    private final Map<String, String> customMappings;
    private final boolean caseSensitive;

    /**
     * Creates an EnvironmentOverrides without a prefix.
     */
    public EnvironmentOverrides() {
        this(null);
    }

    /**
     * Creates an EnvironmentOverrides with a prefix.
     *
     * <p>The prefix is prepended to config paths when looking up
     * environment variables. For example, with prefix "MYAPP",
     * the path "database.host" maps to "MYAPP_DATABASE_HOST".</p>
     *
     * @param prefix the environment variable prefix (may be null)
     */
    public EnvironmentOverrides(@Nullable String prefix) {
        this(prefix, false);
    }

    /**
     * Creates an EnvironmentOverrides with full configuration.
     *
     * @param prefix the environment variable prefix
     * @param caseSensitive whether env var lookups are case-sensitive
     */
    public EnvironmentOverrides(@Nullable String prefix, boolean caseSensitive) {
        this.prefix = prefix;
        this.customMappings = new HashMap<>();
        this.caseSensitive = caseSensitive;
    }

    /**
     * Adds a custom mapping from environment variable to config path.
     *
     * @param envVar the environment variable name
     * @param configPath the configuration path (dot-notation)
     * @return this for chaining
     * @since 1.0.0
     */
    @NotNull
    public EnvironmentOverrides map(@NotNull String envVar, @NotNull String configPath) {
        customMappings.put(envVar, configPath);
        return this;
    }

    /**
     * Applies environment overrides to a configuration object.
     *
     * @param config the configuration to modify
     * @param <T> the configuration type
     * @return the modified configuration
     * @since 1.0.0
     */
    @NotNull
    public <T> T apply(@NotNull T config) {
        // Apply custom mappings
        for (Map.Entry<String, String> mapping : customMappings.entrySet()) {
            String envValue = getEnvValue(mapping.getKey());
            if (envValue != null) {
                setValueByPath(config, mapping.getValue(), envValue);
            }
        }

        // Apply path-based overrides
        applyPathOverrides(config, "");

        // Apply string substitutions
        substituteFields(config);

        return config;
    }

    /**
     * Substitutes ${ENV_VAR} patterns in string fields.
     *
     * @param obj the object to process
     * @since 1.0.0
     */
    public static void substituteEnvVars(@NotNull Object obj) {
        substituteFields(obj);
    }

    /**
     * Substitutes environment variables in a string.
     *
     * @param input the input string
     * @return the string with substitutions applied
     * @since 1.0.0
     */
    @NotNull
    public static String substitute(@NotNull String input) {
        Matcher matcher = ENV_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String envVar = matcher.group(1);
            String defaultValue = matcher.group(2);

            String value = System.getenv(envVar);
            if (value == null) {
                value = defaultValue != null ? defaultValue : "";
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Checks if a string contains environment variable references.
     *
     * @param input the string to check
     * @return true if it contains ${...} patterns
     * @since 1.0.0
     */
    public static boolean hasEnvVars(@NotNull String input) {
        return ENV_PATTERN.matcher(input).find();
    }

    /**
     * Gets an environment variable value.
     *
     * @param name the variable name
     * @return the value, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public static String getEnvValue(@NotNull String name) {
        return System.getenv(name);
    }

    /**
     * Gets an environment variable with a default.
     *
     * @param name the variable name
     * @param defaultValue the default value
     * @return the value or default
     * @since 1.0.0
     */
    @NotNull
    public static String getEnvValue(@NotNull String name, @NotNull String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Converts a config path to an environment variable name.
     *
     * @param path the config path (e.g., "database.host")
     * @return the env var name (e.g., "DATABASE_HOST")
     * @since 1.0.0
     */
    @NotNull
    public String pathToEnvVar(@NotNull String path) {
        String envVar = path.replace('.', '_').toUpperCase();
        if (prefix != null && !prefix.isEmpty()) {
            envVar = prefix.toUpperCase() + "_" + envVar;
        }
        return envVar;
    }

    /**
     * Applies path-based environment overrides.
     */
    private void applyPathOverrides(@NotNull Object obj, @NotNull String basePath) {
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            String fieldPath = basePath.isEmpty() ? field.getName() : basePath + "." + field.getName();

            try {
                Object value = field.get(obj);

                // Check for environment override
                String envVar = pathToEnvVar(fieldPath);
                String envValue = getEnvValue(envVar);

                if (envValue != null) {
                    Object converted = convertValue(envValue, field.getType());
                    field.set(obj, converted);
                } else if (value != null && isConfigClass(value.getClass())) {
                    // Recurse into nested config objects
                    applyPathOverrides(value, fieldPath);
                }

            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
    }

    /**
     * Substitutes environment variables in string fields.
     */
    private static void substituteFields(@NotNull Object obj) {
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object value = field.get(obj);

                if (value instanceof String strValue) {
                    if (hasEnvVars(strValue)) {
                        field.set(obj, substitute(strValue));
                    }
                } else if (value != null && isConfigClass(value.getClass())) {
                    substituteFields(value);
                }

            } catch (IllegalAccessException e) {
                // Skip inaccessible fields
            }
        }
    }

    /**
     * Sets a value by dot-notation path.
     */
    private void setValueByPath(@NotNull Object obj, @NotNull String path, @NotNull String value) {
        String[] parts = path.split("\\.");
        Object current = obj;

        for (int i = 0; i < parts.length - 1; i++) {
            try {
                Field field = current.getClass().getDeclaredField(parts[i]);
                field.setAccessible(true);
                current = field.get(current);
                if (current == null) {
                    return; // Path doesn't exist
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return; // Path doesn't exist
            }
        }

        try {
            Field field = current.getClass().getDeclaredField(parts[parts.length - 1]);
            field.setAccessible(true);
            Object converted = convertValue(value, field.getType());
            field.set(current, converted);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Field doesn't exist or inaccessible
        }
    }

    /**
     * Converts a string value to the target type.
     */
    @Nullable
    private static Object convertValue(@NotNull String value, @NotNull Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, value.toUpperCase());
            return enumValue;
        }
        return value; // Return as string if unknown type
    }

    /**
     * Checks if a class is a configuration class.
     */
    private static boolean isConfigClass(@NotNull Class<?> clazz) {
        return clazz.isAnnotationPresent(sh.pcx.unified.config.annotation.ConfigSerializable.class) ||
                clazz.isAnnotationPresent(org.spongepowered.configurate.objectmapping.ConfigSerializable.class);
    }
}
