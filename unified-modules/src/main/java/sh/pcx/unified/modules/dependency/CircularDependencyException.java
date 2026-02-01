/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.dependency;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when a circular dependency is detected between modules.
 *
 * <p>Circular dependencies occur when module A depends on module B, which
 * depends on module C, which depends on module A (or any similar cycle).
 * Such configurations cannot be resolved and prevent module loading.
 *
 * <h2>Example Circular Dependency</h2>
 * <pre>
 *    ┌─────────────┐
 *    │   ModuleA   │
 *    │ depends on: │
 *    │   ModuleB   │
 *    └──────┬──────┘
 *           │
 *           ▼
 *    ┌─────────────┐
 *    │   ModuleB   │
 *    │ depends on: │
 *    │   ModuleC   │
 *    └──────┬──────┘
 *           │
 *           ▼
 *    ┌─────────────┐
 *    │   ModuleC   │
 *    │ depends on: │──────┐
 *    │   ModuleA   │      │ Circular!
 *    └─────────────┘      │
 *           ▲             │
 *           └─────────────┘
 * </pre>
 *
 * <h2>Resolution</h2>
 * <p>To resolve circular dependencies:
 * <ul>
 *   <li>Refactor to remove the cycle</li>
 *   <li>Extract shared functionality to a separate module</li>
 *   <li>Use soft dependencies where possible</li>
 *   <li>Use interfaces and dependency injection instead of direct dependencies</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try {
 *     moduleManager.registerAll();
 * } catch (CircularDependencyException e) {
 *     logger.error("Circular dependency detected!");
 *     logger.error("Cycle: " + String.join(" -> ", e.getCycle()));
 *     // Handle gracefully
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see DependencyResolver
 * @see DependencyGraph
 */
public class CircularDependencyException extends RuntimeException {

    private final List<String> cycle;

    /**
     * Constructs a circular dependency exception with the detected cycle.
     *
     * @param cycle the list of module names forming the cycle
     */
    public CircularDependencyException(@NotNull List<String> cycle) {
        super(formatMessage(cycle));
        this.cycle = Collections.unmodifiableList(cycle);
    }

    /**
     * Constructs a circular dependency exception with a message and cycle.
     *
     * @param message the exception message
     * @param cycle   the list of module names forming the cycle
     */
    public CircularDependencyException(@NotNull String message, @NotNull List<String> cycle) {
        super(message);
        this.cycle = Collections.unmodifiableList(cycle);
    }

    /**
     * Returns the list of module names forming the circular dependency.
     *
     * <p>The cycle is represented as a path from the first module back to itself.
     * For example: ["ModuleA", "ModuleB", "ModuleC", "ModuleA"]
     *
     * @return the cycle as an unmodifiable list
     */
    @NotNull
    public List<String> getCycle() {
        return cycle;
    }

    /**
     * Returns the first module in the cycle.
     *
     * @return the first module name
     */
    @NotNull
    public String getFirstModule() {
        return cycle.isEmpty() ? "Unknown" : cycle.get(0);
    }

    /**
     * Returns the number of modules in the cycle.
     *
     * @return the cycle length
     */
    public int getCycleLength() {
        return cycle.size() > 0 ? cycle.size() - 1 : 0;
    }

    /**
     * Returns a formatted representation of the cycle.
     *
     * @return the cycle as "A -> B -> C -> A"
     */
    @NotNull
    public String getCycleString() {
        return String.join(" -> ", cycle);
    }

    /**
     * Formats the exception message from a cycle.
     *
     * @param cycle the cycle list
     * @return the formatted message
     */
    private static String formatMessage(List<String> cycle) {
        if (cycle == null || cycle.isEmpty()) {
            return "Circular dependency detected";
        }
        return "Circular dependency detected: " + String.join(" -> ", cycle);
    }

    @Override
    public String toString() {
        return "CircularDependencyException{cycle=" + getCycleString() + "}";
    }
}
