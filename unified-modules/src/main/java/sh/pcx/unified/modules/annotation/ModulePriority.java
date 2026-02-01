/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.annotation;

import java.util.Comparator;

/**
 * Defines the loading priority of a module within the UnifiedPlugin module system.
 *
 * <p>Priority determines the order in which modules are loaded after dependency
 * resolution. Modules with higher priority are loaded first within their
 * dependency level. This allows fine-grained control over initialization order
 * for modules that don't have explicit dependency relationships.
 *
 * <h2>Load Order</h2>
 * <ol>
 *   <li>Dependencies are resolved to determine the topological order</li>
 *   <li>Within each dependency level, modules are sorted by priority</li>
 *   <li>Higher priority modules (HIGHEST, HIGH) load before lower ones</li>
 *   <li>Modules with the same priority load in registration order</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Module(
 *     name = "CoreServices",
 *     priority = ModulePriority.HIGHEST
 * )
 * public class CoreServicesModule implements Initializable {
 *     // Loads first, provides core services to other modules
 * }
 *
 * @Module(
 *     name = "OptionalFeature",
 *     priority = ModulePriority.LOW,
 *     softDependencies = {"CoreServices"}
 * )
 * public class OptionalFeatureModule {
 *     // Loads later, after higher priority modules
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see Module#priority()
 */
public enum ModulePriority {

    /**
     * Lowest priority, loads last within its dependency level.
     *
     * <p>Use for optional features, cosmetic modules, or modules
     * that depend on many other modules being initialized first.
     */
    LOWEST(0),

    /**
     * Lower than normal priority.
     *
     * <p>Use for modules that should load after core modules
     * but before the final initialization phase.
     */
    LOW(1),

    /**
     * Default priority for most modules.
     *
     * <p>Use for standard feature modules that don't require
     * specific load ordering within their dependency level.
     */
    NORMAL(2),

    /**
     * Higher than normal priority.
     *
     * <p>Use for modules that provide services used by many
     * other modules but aren't core infrastructure.
     */
    HIGH(3),

    /**
     * Highest priority, loads first within its dependency level.
     *
     * <p>Use for core infrastructure modules that must initialize
     * before any dependent modules. Examples include database
     * connection pools, configuration loaders, and service registries.
     */
    HIGHEST(4);

    private final int value;

    /**
     * Constructs a priority level with the specified numeric value.
     *
     * @param value the numeric priority value (higher = loads first)
     */
    ModulePriority(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric value of this priority.
     *
     * <p>Higher values indicate higher priority (loads first).
     *
     * @return the priority value
     */
    public int getValue() {
        return value;
    }

    /**
     * Comparator for sorting priorities by their numeric values.
     *
     * <p>This comparator orders priorities by their numeric value in ascending order,
     * where higher numeric values indicate higher priority (loads first).
     *
     * <p>Example usage:
     * <pre>{@code
     * List<ModulePriority> priorities = Arrays.asList(ModulePriority.LOW, ModulePriority.HIGHEST);
     * priorities.sort(ModulePriority.BY_VALUE);
     * }</pre>
     */
    public static final Comparator<ModulePriority> BY_VALUE =
        Comparator.comparingInt(ModulePriority::getValue);
}
