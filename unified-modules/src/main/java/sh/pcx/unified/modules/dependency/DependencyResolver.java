/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.dependency;

import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.annotation.ModulePriority;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves module dependencies and determines the correct load order.
 *
 * <p>The resolver builds a dependency graph from module annotations and
 * computes the topological order for loading modules. It ensures that:
 * <ul>
 *   <li>All required dependencies are loaded before a module</li>
 *   <li>Circular dependencies are detected and reported</li>
 *   <li>Load order respects module priorities</li>
 *   <li>Missing dependencies are identified</li>
 * </ul>
 *
 * <h2>Resolution Process</h2>
 * <ol>
 *   <li>Build dependency graph from module annotations</li>
 *   <li>Check for circular dependencies</li>
 *   <li>Validate all required dependencies exist</li>
 *   <li>Compute topological order</li>
 *   <li>Sort by priority within each dependency level</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DependencyResolver resolver = new DependencyResolver();
 *
 * // Add modules to resolve
 * resolver.addModule("Economy", economyModule.getClass());
 * resolver.addModule("PlayerData", playerDataModule.getClass());
 * resolver.addModule("BattlePass", battlePassModule.getClass());
 *
 * // Resolve and get load order
 * ResolutionResult result = resolver.resolve();
 *
 * if (result.isSuccess()) {
 *     List<String> loadOrder = result.getLoadOrder();
 *     for (String moduleName : loadOrder) {
 *         loadModule(moduleName);
 *     }
 * } else {
 *     for (String error : result.getErrors()) {
 *         logger.error(error);
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see DependencyGraph
 * @see CircularDependencyException
 */
public final class DependencyResolver {

    private final DependencyGraph graph;
    private final Map<String, ModuleInfo> modules;
    private final Set<String> softDependencies;

    /**
     * Creates a new dependency resolver.
     */
    public DependencyResolver() {
        this.graph = new DependencyGraph();
        this.modules = new HashMap<>();
        this.softDependencies = new HashSet<>();
    }

    /**
     * Adds a module to be resolved.
     *
     * @param name        the module name
     * @param moduleClass the module class with @Module annotation
     */
    public void addModule(@NotNull String name, @NotNull Class<?> moduleClass) {
        Objects.requireNonNull(name, "Module name cannot be null");
        Objects.requireNonNull(moduleClass, "Module class cannot be null");

        Module annotation = moduleClass.getAnnotation(Module.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                    "Class " + moduleClass.getName() + " is not annotated with @Module"
            );
        }

        ModuleInfo info = new ModuleInfo(
                name,
                moduleClass,
                Arrays.asList(annotation.dependencies()),
                Arrays.asList(annotation.softDependencies()),
                annotation.priority()
        );

        modules.put(name, info);
        graph.addNode(name);

        // Add hard dependencies to graph
        for (String dep : annotation.dependencies()) {
            graph.addEdge(name, dep);
        }

        // Track soft dependencies separately
        softDependencies.addAll(Arrays.asList(annotation.softDependencies()));
    }

    /**
     * Adds a module with explicit dependencies (no annotation parsing).
     *
     * @param name             the module name
     * @param dependencies     required dependencies
     * @param softDependencies optional dependencies
     * @param priority         the load priority
     */
    public void addModule(
            @NotNull String name,
            @NotNull List<String> dependencies,
            @NotNull List<String> softDependencies,
            @NotNull ModulePriority priority
    ) {
        ModuleInfo info = new ModuleInfo(name, null, dependencies, softDependencies, priority);
        modules.put(name, info);
        graph.addNode(name);

        for (String dep : dependencies) {
            graph.addEdge(name, dep);
        }

        this.softDependencies.addAll(softDependencies);
    }

    /**
     * Resolves dependencies and returns the result.
     *
     * @return the resolution result
     */
    @NotNull
    public ResolutionResult resolve() {
        List<String> errors = new ArrayList<>();

        // Check for missing hard dependencies
        for (ModuleInfo module : modules.values()) {
            for (String dep : module.dependencies) {
                if (!modules.containsKey(dep)) {
                    errors.add(String.format(
                            "Module '%s' requires missing dependency '%s'",
                            module.name, dep
                    ));
                }
            }
        }

        // Check for circular dependencies
        Optional<List<String>> cycle = graph.detectCycle();
        if (cycle.isPresent()) {
            errors.add("Circular dependency detected: " + String.join(" -> ", cycle.get()));
            return new ResolutionResult(Collections.emptyList(), errors, cycle.get());
        }

        if (!errors.isEmpty()) {
            return new ResolutionResult(Collections.emptyList(), errors, null);
        }

        // Get topological order
        List<String> order = graph.getTopologicalOrder();

        // Sort by priority within each dependency level
        order = sortByPriority(order);

        // Report available soft dependencies
        List<String> availableSoft = softDependencies.stream()
                .filter(modules::containsKey)
                .collect(Collectors.toList());

        List<String> missingSoft = softDependencies.stream()
                .filter(dep -> !modules.containsKey(dep))
                .collect(Collectors.toList());

        if (!missingSoft.isEmpty()) {
            // Not an error, just informational
        }

        return new ResolutionResult(order, errors, null);
    }

    /**
     * Sorts modules by priority while maintaining topological order.
     *
     * @param topologicalOrder the base topological order
     * @return the priority-sorted order
     */
    private List<String> sortByPriority(List<String> topologicalOrder) {
        // Group modules by their depth in the dependency graph
        Map<String, Integer> depths = calculateDepths();

        // Sort by depth first, then by priority (reversed because higher = first)
        return topologicalOrder.stream()
                .sorted((a, b) -> {
                    int depthCompare = Integer.compare(depths.getOrDefault(a, 0), depths.getOrDefault(b, 0));
                    if (depthCompare != 0) {
                        return depthCompare;
                    }
                    ModuleInfo infoA = modules.get(a);
                    ModuleInfo infoB = modules.get(b);
                    int priorityA = infoA != null ? infoA.priority.getValue() : ModulePriority.NORMAL.getValue();
                    int priorityB = infoB != null ? infoB.priority.getValue() : ModulePriority.NORMAL.getValue();
                    return Integer.compare(priorityB, priorityA); // Reversed for higher first
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculates the depth of each node in the dependency graph.
     *
     * @return map of module name to depth
     */
    private Map<String, Integer> calculateDepths() {
        Map<String, Integer> depths = new HashMap<>();

        for (String node : modules.keySet()) {
            calculateDepth(node, depths);
        }

        return depths;
    }

    /**
     * Recursively calculates depth for a single node.
     */
    private int calculateDepth(String node, Map<String, Integer> depths) {
        if (depths.containsKey(node)) {
            return depths.get(node);
        }

        Set<String> deps = graph.getDependencies(node);
        if (deps.isEmpty()) {
            depths.put(node, 0);
            return 0;
        }

        int maxDepth = 0;
        for (String dep : deps) {
            if (modules.containsKey(dep)) {
                maxDepth = Math.max(maxDepth, calculateDepth(dep, depths) + 1);
            }
        }

        depths.put(node, maxDepth);
        return maxDepth;
    }

    /**
     * Returns the dependency graph.
     *
     * @return the dependency graph
     */
    @NotNull
    public DependencyGraph getGraph() {
        return graph;
    }

    /**
     * Returns the number of modules.
     *
     * @return the module count
     */
    public int getModuleCount() {
        return modules.size();
    }

    /**
     * Checks if a module exists.
     *
     * @param name the module name
     * @return {@code true} if the module exists
     */
    public boolean hasModule(@NotNull String name) {
        return modules.containsKey(name);
    }

    /**
     * Clears all modules from the resolver.
     */
    public void clear() {
        graph.clear();
        modules.clear();
        softDependencies.clear();
    }

    /**
     * Internal class to hold module information.
     */
    private static class ModuleInfo {
        final String name;
        final Class<?> moduleClass;
        final List<String> dependencies;
        final List<String> softDependencies;
        final ModulePriority priority;

        ModuleInfo(
                String name,
                Class<?> moduleClass,
                List<String> dependencies,
                List<String> softDependencies,
                ModulePriority priority
        ) {
            this.name = name;
            this.moduleClass = moduleClass;
            this.dependencies = dependencies;
            this.softDependencies = softDependencies;
            this.priority = priority;
        }
    }

    /**
     * The result of dependency resolution.
     */
    public static final class ResolutionResult {
        private final List<String> loadOrder;
        private final List<String> errors;
        private final List<String> cycle;

        /**
         * Creates a resolution result.
         *
         * @param loadOrder the resolved load order
         * @param errors    any errors encountered
         * @param cycle     the cycle if one was detected
         */
        public ResolutionResult(
                @NotNull List<String> loadOrder,
                @NotNull List<String> errors,
                List<String> cycle
        ) {
            this.loadOrder = Collections.unmodifiableList(loadOrder);
            this.errors = Collections.unmodifiableList(errors);
            this.cycle = cycle != null ? Collections.unmodifiableList(cycle) : null;
        }

        /**
         * Returns whether resolution was successful.
         *
         * @return {@code true} if no errors occurred
         */
        public boolean isSuccess() {
            return errors.isEmpty();
        }

        /**
         * Returns the resolved load order.
         *
         * @return the load order (empty if resolution failed)
         */
        @NotNull
        public List<String> getLoadOrder() {
            return loadOrder;
        }

        /**
         * Returns any errors encountered during resolution.
         *
         * @return the list of error messages
         */
        @NotNull
        public List<String> getErrors() {
            return errors;
        }

        /**
         * Returns whether a circular dependency was detected.
         *
         * @return {@code true} if a cycle exists
         */
        public boolean hasCycle() {
            return cycle != null;
        }

        /**
         * Returns the circular dependency cycle if detected.
         *
         * @return the cycle, or null if none
         */
        public List<String> getCycle() {
            return cycle;
        }

        @Override
        public String toString() {
            if (isSuccess()) {
                return "ResolutionResult{success=true, loadOrder=" + loadOrder + "}";
            } else {
                return "ResolutionResult{success=false, errors=" + errors + "}";
            }
        }
    }
}
