/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.admin;

import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.core.ModuleRegistry;
import sh.pcx.unified.modules.lifecycle.ModuleState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Provides detailed information about a registered module.
 *
 * <p>This class aggregates information from a module's annotation and
 * runtime state to present a complete view of the module for admin
 * commands and monitoring tools.
 *
 * <h2>Information Provided</h2>
 * <ul>
 *   <li>Basic info: name, description, version, authors</li>
 *   <li>Dependencies: required and optional dependencies</li>
 *   <li>State: current state, load time, uptime</li>
 *   <li>Health: error messages if failed</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ModuleInfo info = ModuleInfo.from(moduleEntry);
 *
 * // Display in command
 * sender.sendMessage("Module: " + info.getName());
 * sender.sendMessage("Description: " + info.getDescription());
 * sender.sendMessage("Version: " + info.getVersion());
 * sender.sendMessage("State: " + info.getState());
 * sender.sendMessage("Load time: " + info.getLoadTimeMs() + "ms");
 *
 * if (info.hasError()) {
 *     sender.sendMessage("Error: " + info.getErrorMessage());
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleCommands
 */
public final class ModuleInfo {

    private final String name;
    private final String description;
    private final String version;
    private final List<String> authors;
    private final List<String> dependencies;
    private final List<String> softDependencies;
    private final ModuleState state;
    private final long loadTimeMs;
    private final long enableTimeMs;
    private final String errorMessage;
    private final Class<?> moduleClass;

    /**
     * Creates a new ModuleInfo.
     *
     * @param name             the module name
     * @param description      the module description
     * @param version          the module version
     * @param authors          the module authors
     * @param dependencies     required dependencies
     * @param softDependencies optional dependencies
     * @param state            the current state
     * @param loadTimeMs       time taken to load in milliseconds
     * @param enableTimeMs     timestamp when enabled
     * @param errorMessage     error message if failed
     * @param moduleClass      the module class
     */
    public ModuleInfo(
            @NotNull String name,
            @NotNull String description,
            @NotNull String version,
            @NotNull List<String> authors,
            @NotNull List<String> dependencies,
            @NotNull List<String> softDependencies,
            @NotNull ModuleState state,
            long loadTimeMs,
            long enableTimeMs,
            @Nullable String errorMessage,
            @NotNull Class<?> moduleClass
    ) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.version = Objects.requireNonNull(version);
        this.authors = List.copyOf(authors);
        this.dependencies = List.copyOf(dependencies);
        this.softDependencies = List.copyOf(softDependencies);
        this.state = Objects.requireNonNull(state);
        this.loadTimeMs = loadTimeMs;
        this.enableTimeMs = enableTimeMs;
        this.errorMessage = errorMessage;
        this.moduleClass = Objects.requireNonNull(moduleClass);
    }

    /**
     * Creates a ModuleInfo from a registry entry.
     *
     * @param entry the module entry
     * @return the module info
     */
    @NotNull
    public static ModuleInfo from(@NotNull ModuleRegistry.ModuleEntry entry) {
        Module annotation = entry.getAnnotation();

        return new ModuleInfo(
                entry.getName(),
                annotation != null ? annotation.description() : "",
                annotation != null ? annotation.version() : "1.0.0",
                annotation != null ? Arrays.asList(annotation.authors()) : List.of(),
                annotation != null ? Arrays.asList(annotation.dependencies()) : List.of(),
                annotation != null ? Arrays.asList(annotation.softDependencies()) : List.of(),
                entry.getState(),
                entry.getLoadTime(),
                entry.getEnableTime(),
                entry.getErrorMessage(),
                entry.getModuleClass()
        );
    }

    /**
     * Returns the module name.
     *
     * @return the name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the module description.
     *
     * @return the description
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Returns the module version.
     *
     * @return the version
     */
    @NotNull
    public String getVersion() {
        return version;
    }

    /**
     * Returns the module authors.
     *
     * @return the list of authors
     */
    @NotNull
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Returns the required dependencies.
     *
     * @return the list of dependencies
     */
    @NotNull
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the optional dependencies.
     *
     * @return the list of soft dependencies
     */
    @NotNull
    public List<String> getSoftDependencies() {
        return softDependencies;
    }

    /**
     * Returns the current state.
     *
     * @return the module state
     */
    @NotNull
    public ModuleState getState() {
        return state;
    }

    /**
     * Returns whether the module is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return state == ModuleState.ENABLED;
    }

    /**
     * Returns whether the module has failed.
     *
     * @return {@code true} if failed
     */
    public boolean hasFailed() {
        return state == ModuleState.FAILED;
    }

    /**
     * Returns whether there is an error message.
     *
     * @return {@code true} if there is an error
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * Returns the error message if the module failed.
     *
     * @return the error message, or null
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the time taken to load in milliseconds.
     *
     * @return the load time
     */
    public long getLoadTimeMs() {
        return loadTimeMs;
    }

    /**
     * Returns the timestamp when the module was enabled.
     *
     * @return the enable timestamp
     */
    public long getEnableTimeMs() {
        return enableTimeMs;
    }

    /**
     * Returns how long the module has been enabled.
     *
     * @return the uptime duration
     */
    @NotNull
    public Duration getUptime() {
        if (enableTimeMs <= 0 || state != ModuleState.ENABLED) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(System.currentTimeMillis() - enableTimeMs);
    }

    /**
     * Returns a formatted uptime string.
     *
     * @return the uptime as "Xh Ym Zs"
     */
    @NotNull
    public String getFormattedUptime() {
        Duration uptime = getUptime();
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Returns the module class.
     *
     * @return the class
     */
    @NotNull
    public Class<?> getModuleClass() {
        return moduleClass;
    }

    /**
     * Returns a formatted string representation for display.
     *
     * @return the formatted info
     */
    @NotNull
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(name).append(" v").append(version).append(" ===\n");

        if (!description.isEmpty()) {
            sb.append("Description: ").append(description).append("\n");
        }

        if (!authors.isEmpty()) {
            sb.append("Authors: ").append(String.join(", ", authors)).append("\n");
        }

        sb.append("State: ").append(state.getDisplayName()).append("\n");
        sb.append("Load time: ").append(loadTimeMs).append("ms\n");

        if (state == ModuleState.ENABLED) {
            sb.append("Uptime: ").append(getFormattedUptime()).append("\n");
        }

        if (!dependencies.isEmpty()) {
            sb.append("Dependencies: ").append(String.join(", ", dependencies)).append("\n");
        }

        if (!softDependencies.isEmpty()) {
            sb.append("Optional: ").append(String.join(", ", softDependencies)).append("\n");
        }

        if (hasError()) {
            sb.append("Error: ").append(errorMessage).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "ModuleInfo{name='" + name + "', state=" + state + ", version=" + version + "}";
    }
}
