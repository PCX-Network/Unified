/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.config.advanced;

import sh.pcx.unified.config.ConfigException;
import sh.pcx.unified.config.format.ConfigFormat;
import sh.pcx.unified.config.format.ConfigLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Manages configuration profiles for different environments.
 *
 * <p>ConfigProfile enables environment-specific configurations by loading
 * base settings and then merging profile-specific overrides. Common
 * profiles include development, staging, and production.</p>
 *
 * <h2>Profile Resolution</h2>
 * <p>Profiles are resolved in the following order:</p>
 * <ol>
 *   <li>{@code config.yml} - Base configuration</li>
 *   <li>{@code config-{profile}.yml} - Profile-specific overrides</li>
 *   <li>Environment variables - Highest priority overrides</li>
 * </ol>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create profile manager
 * ConfigProfile profile = new ConfigProfile("prod");
 *
 * // Load with profile
 * PluginConfig config = profile.load(
 *     PluginConfig.class,
 *     configDir,
 *     "config",
 *     configLoader
 * );
 *
 * // Files used:
 * // - config.yml (base)
 * // - config-prod.yml (overrides)
 * }</pre>
 *
 * <h2>Profile from Environment</h2>
 * <pre>{@code
 * // Get profile from UNIFIED_PROFILE or CONFIG_PROFILE env var
 * ConfigProfile profile = ConfigProfile.fromEnvironment();
 *
 * // Or with custom env var
 * ConfigProfile profile = ConfigProfile.fromEnvironment("MY_PROFILE_VAR");
 * }</pre>
 *
 * <h2>Multiple Profiles</h2>
 * <pre>{@code
 * // Load with multiple profiles (applied in order)
 * ConfigProfile profile = new ConfigProfile("production", "us-east");
 *
 * // Files used:
 * // - config.yml
 * // - config-production.yml
 * // - config-us-east.yml
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class ConfigProfile {

    /**
     * Standard profile names.
     */
    public static final String DEVELOPMENT = "dev";
    public static final String STAGING = "staging";
    public static final String PRODUCTION = "prod";
    public static final String TEST = "test";

    /**
     * Environment variable names for profile.
     */
    private static final List<String> PROFILE_ENV_VARS = List.of(
            "UNIFIED_PROFILE",
            "CONFIG_PROFILE",
            "SPRING_PROFILES_ACTIVE",
            "ENVIRONMENT"
    );

    private final List<String> activeProfiles;

    /**
     * Creates a ConfigProfile with no active profiles.
     */
    public ConfigProfile() {
        this.activeProfiles = List.of();
    }

    /**
     * Creates a ConfigProfile with active profiles.
     *
     * @param profiles the active profile names
     */
    public ConfigProfile(@NotNull String... profiles) {
        this.activeProfiles = Arrays.asList(profiles);
    }

    /**
     * Creates a ConfigProfile from a list of profiles.
     *
     * @param profiles the active profile names
     */
    public ConfigProfile(@NotNull List<String> profiles) {
        this.activeProfiles = List.copyOf(profiles);
    }

    /**
     * Creates a ConfigProfile from environment variables.
     *
     * <p>Checks the following environment variables in order:
     * UNIFIED_PROFILE, CONFIG_PROFILE, SPRING_PROFILES_ACTIVE, ENVIRONMENT</p>
     *
     * @return the config profile
     * @since 1.0.0
     */
    @NotNull
    public static ConfigProfile fromEnvironment() {
        for (String envVar : PROFILE_ENV_VARS) {
            String value = System.getenv(envVar);
            if (value != null && !value.isEmpty()) {
                return fromString(value);
            }
        }
        return new ConfigProfile();
    }

    /**
     * Creates a ConfigProfile from a specific environment variable.
     *
     * @param envVar the environment variable name
     * @return the config profile
     * @since 1.0.0
     */
    @NotNull
    public static ConfigProfile fromEnvironment(@NotNull String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            return fromString(value);
        }
        return new ConfigProfile();
    }

    /**
     * Creates a ConfigProfile from a comma-separated string.
     *
     * @param profiles the profiles string (e.g., "dev,local")
     * @return the config profile
     * @since 1.0.0
     */
    @NotNull
    public static ConfigProfile fromString(@NotNull String profiles) {
        String[] parts = profiles.split("[,;\\s]+");
        return new ConfigProfile(Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new));
    }

    /**
     * Creates a development profile.
     *
     * @return the development profile
     * @since 1.0.0
     */
    @NotNull
    public static ConfigProfile development() {
        return new ConfigProfile(DEVELOPMENT);
    }

    /**
     * Creates a production profile.
     *
     * @return the production profile
     * @since 1.0.0
     */
    @NotNull
    public static ConfigProfile production() {
        return new ConfigProfile(PRODUCTION);
    }

    /**
     * Gets the active profiles.
     *
     * @return unmodifiable list of active profiles
     * @since 1.0.0
     */
    @NotNull
    public List<String> getActiveProfiles() {
        return activeProfiles;
    }

    /**
     * Checks if any profiles are active.
     *
     * @return true if at least one profile is active
     * @since 1.0.0
     */
    public boolean hasActiveProfiles() {
        return !activeProfiles.isEmpty();
    }

    /**
     * Checks if a specific profile is active.
     *
     * @param profile the profile to check
     * @return true if the profile is active
     * @since 1.0.0
     */
    public boolean isActive(@NotNull String profile) {
        return activeProfiles.contains(profile);
    }

    /**
     * Checks if the development profile is active.
     *
     * @return true if dev profile is active
     * @since 1.0.0
     */
    public boolean isDevelopment() {
        return isActive(DEVELOPMENT) || isActive("development");
    }

    /**
     * Checks if the production profile is active.
     *
     * @return true if prod profile is active
     * @since 1.0.0
     */
    public boolean isProduction() {
        return isActive(PRODUCTION) || isActive("production");
    }

    /**
     * Loads a configuration with profile overrides.
     *
     * @param type the configuration class type
     * @param directory the configuration directory
     * @param baseName the base configuration name (without extension)
     * @param loader the config loader
     * @param <T> the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public <T> T load(
            @NotNull Class<T> type,
            @NotNull Path directory,
            @NotNull String baseName,
            @NotNull ConfigLoader loader
    ) {
        return load(type, directory, baseName, ConfigFormat.YAML, loader);
    }

    /**
     * Loads a configuration with profile overrides.
     *
     * @param type the configuration class type
     * @param directory the configuration directory
     * @param baseName the base configuration name
     * @param format the configuration format
     * @param loader the config loader
     * @param <T> the configuration type
     * @return the loaded configuration
     * @throws ConfigException if loading fails
     * @since 1.0.0
     */
    @NotNull
    public <T> T load(
            @NotNull Class<T> type,
            @NotNull Path directory,
            @NotNull String baseName,
            @NotNull ConfigFormat format,
            @NotNull ConfigLoader loader
    ) {
        String extension = format.getPrimaryExtension();

        // Load base configuration
        Path basePath = directory.resolve(baseName + "." + extension);
        if (!Files.exists(basePath)) {
            throw ConfigException.fileNotFound(basePath);
        }

        ConfigurationNode baseNode = loader.load(basePath, format);

        // Merge profile configurations
        for (String profile : activeProfiles) {
            Path profilePath = directory.resolve(baseName + "-" + profile + "." + extension);
            if (Files.exists(profilePath)) {
                ConfigurationNode profileNode = loader.load(profilePath, format);
                mergeNodes(baseNode, profileNode);
            }
        }

        // Deserialize to object
        try {
            T result = baseNode.get(type);
            if (result == null) {
                throw new ConfigException("Failed to deserialize configuration", basePath);
            }
            return result;
        } catch (Exception e) {
            if (e instanceof ConfigException) {
                throw (ConfigException) e;
            }
            throw ConfigException.serializationError(basePath, "", e);
        }
    }

    /**
     * Gets all configuration files for this profile.
     *
     * @param directory the configuration directory
     * @param baseName the base configuration name
     * @param format the configuration format
     * @return list of existing configuration files
     * @since 1.0.0
     */
    @NotNull
    public List<Path> getConfigFiles(
            @NotNull Path directory,
            @NotNull String baseName,
            @NotNull ConfigFormat format
    ) {
        String extension = format.getPrimaryExtension();
        java.util.ArrayList<Path> files = new java.util.ArrayList<>();

        // Base file
        Path basePath = directory.resolve(baseName + "." + extension);
        if (Files.exists(basePath)) {
            files.add(basePath);
        }

        // Profile files
        for (String profile : activeProfiles) {
            Path profilePath = directory.resolve(baseName + "-" + profile + "." + extension);
            if (Files.exists(profilePath)) {
                files.add(profilePath);
            }
        }

        return files;
    }

    /**
     * Creates a new ConfigProfile with additional profiles.
     *
     * @param profiles the profiles to add
     * @return a new ConfigProfile
     * @since 1.0.0
     */
    @NotNull
    public ConfigProfile with(@NotNull String... profiles) {
        Set<String> combined = new HashSet<>(activeProfiles);
        combined.addAll(Arrays.asList(profiles));
        return new ConfigProfile(combined.toArray(new String[0]));
    }

    /**
     * Merges nodes, with override values taking precedence.
     */
    private void mergeNodes(
            @NotNull ConfigurationNode base,
            @NotNull ConfigurationNode override
    ) {
        if (override.isMap()) {
            for (var entry : override.childrenMap().entrySet()) {
                ConfigurationNode baseChild = base.node(entry.getKey());
                ConfigurationNode overrideChild = entry.getValue();

                if (baseChild.isMap() && overrideChild.isMap()) {
                    mergeNodes(baseChild, overrideChild);
                } else {
                    try {
                        baseChild.set(overrideChild.raw());
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (!override.virtual()) {
            try {
                base.set(override.raw());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String toString() {
        if (activeProfiles.isEmpty()) {
            return "ConfigProfile{default}";
        }
        return "ConfigProfile{" + String.join(", ", activeProfiles) + "}";
    }
}
