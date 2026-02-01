/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.dependency;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a directed graph of module dependencies.
 *
 * <p>This class maintains the dependency relationships between modules and
 * provides operations for querying and traversing the graph. It's used by
 * {@link DependencyResolver} to determine load order and detect cycles.
 *
 * <h2>Graph Structure</h2>
 * <pre>
 *     ┌─────────────┐     ┌─────────────┐
 *     │   Economy   │────▶│  PlayerData │
 *     └──────┬──────┘     └─────────────┘
 *            │                   ▲
 *            │                   │
 *     ┌──────▼──────┐     ┌──────┴──────┐
 *     │  BattlePass │────▶│  Cosmetics  │
 *     └─────────────┘     └─────────────┘
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DependencyGraph graph = new DependencyGraph();
 *
 * // Add nodes
 * graph.addNode("Economy");
 * graph.addNode("PlayerData");
 * graph.addNode("BattlePass");
 *
 * // Add edges (dependencies)
 * graph.addEdge("BattlePass", "Economy");
 * graph.addEdge("BattlePass", "PlayerData");
 *
 * // Query dependencies
 * Set<String> deps = graph.getDependencies("BattlePass");
 * // deps = ["Economy", "PlayerData"]
 *
 * // Query dependents
 * Set<String> dependents = graph.getDependents("Economy");
 * // dependents = ["BattlePass"]
 *
 * // Get topological order
 * List<String> loadOrder = graph.getTopologicalOrder();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see DependencyResolver
 * @see CircularDependencyException
 */
public final class DependencyGraph {

    /**
     * Map from module name to its dependencies (outgoing edges).
     */
    private final Map<String, Set<String>> dependencies;

    /**
     * Map from module name to modules that depend on it (incoming edges).
     */
    private final Map<String, Set<String>> dependents;

    /**
     * Set of all nodes in the graph.
     */
    private final Set<String> nodes;

    /**
     * Creates an empty dependency graph.
     */
    public DependencyGraph() {
        this.dependencies = new HashMap<>();
        this.dependents = new HashMap<>();
        this.nodes = new HashSet<>();
    }

    /**
     * Adds a node (module) to the graph.
     *
     * @param moduleName the name of the module
     */
    public void addNode(@NotNull String moduleName) {
        Objects.requireNonNull(moduleName, "Module name cannot be null");
        nodes.add(moduleName);
        dependencies.putIfAbsent(moduleName, new HashSet<>());
        dependents.putIfAbsent(moduleName, new HashSet<>());
    }

    /**
     * Adds an edge (dependency) from one module to another.
     *
     * <p>This indicates that {@code from} depends on {@code to},
     * meaning {@code to} must be loaded before {@code from}.
     *
     * @param from the dependent module
     * @param to   the dependency (required module)
     */
    public void addEdge(@NotNull String from, @NotNull String to) {
        Objects.requireNonNull(from, "From module cannot be null");
        Objects.requireNonNull(to, "To module cannot be null");

        addNode(from);
        addNode(to);

        dependencies.get(from).add(to);
        dependents.get(to).add(from);
    }

    /**
     * Removes a node and all its edges from the graph.
     *
     * @param moduleName the name of the module to remove
     */
    public void removeNode(@NotNull String moduleName) {
        if (!nodes.contains(moduleName)) {
            return;
        }

        // Remove outgoing edges
        Set<String> deps = dependencies.remove(moduleName);
        if (deps != null) {
            for (String dep : deps) {
                Set<String> depDependents = dependents.get(dep);
                if (depDependents != null) {
                    depDependents.remove(moduleName);
                }
            }
        }

        // Remove incoming edges
        Set<String> deps2 = dependents.remove(moduleName);
        if (deps2 != null) {
            for (String dependent : deps2) {
                Set<String> depDeps = dependencies.get(dependent);
                if (depDeps != null) {
                    depDeps.remove(moduleName);
                }
            }
        }

        nodes.remove(moduleName);
    }

    /**
     * Returns whether the graph contains the specified node.
     *
     * @param moduleName the module name
     * @return {@code true} if the node exists
     */
    public boolean containsNode(@NotNull String moduleName) {
        return nodes.contains(moduleName);
    }

    /**
     * Returns the direct dependencies of a module.
     *
     * @param moduleName the module name
     * @return the set of dependencies, or empty set if not found
     */
    @NotNull
    public Set<String> getDependencies(@NotNull String moduleName) {
        Set<String> deps = dependencies.get(moduleName);
        return deps != null ? Collections.unmodifiableSet(deps) : Collections.emptySet();
    }

    /**
     * Returns all dependencies of a module (transitive closure).
     *
     * @param moduleName the module name
     * @return all modules that this module depends on, directly or indirectly
     */
    @NotNull
    public Set<String> getAllDependencies(@NotNull String moduleName) {
        Set<String> result = new HashSet<>();
        collectDependencies(moduleName, result);
        return result;
    }

    /**
     * Recursively collects all dependencies.
     */
    private void collectDependencies(String moduleName, Set<String> collected) {
        for (String dep : getDependencies(moduleName)) {
            if (collected.add(dep)) {
                collectDependencies(dep, collected);
            }
        }
    }

    /**
     * Returns the modules that directly depend on this module.
     *
     * @param moduleName the module name
     * @return the set of dependents, or empty set if not found
     */
    @NotNull
    public Set<String> getDependents(@NotNull String moduleName) {
        Set<String> deps = dependents.get(moduleName);
        return deps != null ? Collections.unmodifiableSet(deps) : Collections.emptySet();
    }

    /**
     * Returns all modules that depend on this module (transitive closure).
     *
     * @param moduleName the module name
     * @return all modules that depend on this module, directly or indirectly
     */
    @NotNull
    public Set<String> getAllDependents(@NotNull String moduleName) {
        Set<String> result = new HashSet<>();
        collectDependents(moduleName, result);
        return result;
    }

    /**
     * Recursively collects all dependents.
     */
    private void collectDependents(String moduleName, Set<String> collected) {
        for (String dependent : getDependents(moduleName)) {
            if (collected.add(dependent)) {
                collectDependents(dependent, collected);
            }
        }
    }

    /**
     * Returns all nodes in the graph.
     *
     * @return an unmodifiable set of all module names
     */
    @NotNull
    public Set<String> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * Returns the number of nodes in the graph.
     *
     * @return the node count
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Returns whether the graph is empty.
     *
     * @return {@code true} if the graph has no nodes
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Returns the topological order of the graph (dependencies first).
     *
     * <p>This is the order in which modules should be loaded to satisfy
     * all dependencies. Modules with no dependencies come first.
     *
     * @return a list of module names in topological order
     * @throws CircularDependencyException if a cycle is detected
     */
    @NotNull
    public List<String> getTopologicalOrder() throws CircularDependencyException {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();

        for (String node : nodes) {
            if (!visited.contains(node)) {
                topologicalSort(node, visited, visiting, path, result);
            }
        }

        return result;
    }

    /**
     * Performs DFS for topological sort with cycle detection.
     */
    private void topologicalSort(
            String node,
            Set<String> visited,
            Set<String> visiting,
            Deque<String> path,
            List<String> result
    ) throws CircularDependencyException {
        if (visiting.contains(node)) {
            // Cycle detected - build cycle path
            List<String> cycle = new ArrayList<>();
            boolean inCycle = false;
            for (String n : path) {
                if (n.equals(node)) {
                    inCycle = true;
                }
                if (inCycle) {
                    cycle.add(n);
                }
            }
            cycle.add(node);
            throw new CircularDependencyException(cycle);
        }

        if (visited.contains(node)) {
            return;
        }

        visiting.add(node);
        path.addLast(node);

        for (String dep : getDependencies(node)) {
            topologicalSort(dep, visited, visiting, path, result);
        }

        path.removeLast();
        visiting.remove(node);
        visited.add(node);
        result.add(node);
    }

    /**
     * Detects if there is a cycle in the graph.
     *
     * @return an Optional containing the cycle if found, empty otherwise
     */
    @NotNull
    public Optional<List<String>> detectCycle() {
        try {
            getTopologicalOrder();
            return Optional.empty();
        } catch (CircularDependencyException e) {
            return Optional.of(e.getCycle());
        }
    }

    /**
     * Returns whether the graph has any cycles.
     *
     * @return {@code true} if a cycle exists
     */
    public boolean hasCycle() {
        return detectCycle().isPresent();
    }

    /**
     * Returns modules with no dependencies (roots).
     *
     * @return set of root modules
     */
    @NotNull
    public Set<String> getRoots() {
        Set<String> roots = new HashSet<>();
        for (String node : nodes) {
            if (getDependencies(node).isEmpty()) {
                roots.add(node);
            }
        }
        return roots;
    }

    /**
     * Returns modules with no dependents (leaves).
     *
     * @return set of leaf modules
     */
    @NotNull
    public Set<String> getLeaves() {
        Set<String> leaves = new HashSet<>();
        for (String node : nodes) {
            if (getDependents(node).isEmpty()) {
                leaves.add(node);
            }
        }
        return leaves;
    }

    /**
     * Clears all nodes and edges from the graph.
     */
    public void clear() {
        nodes.clear();
        dependencies.clear();
        dependents.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DependencyGraph{\n");
        for (String node : nodes) {
            Set<String> deps = getDependencies(node);
            sb.append("  ").append(node);
            if (!deps.isEmpty()) {
                sb.append(" -> ").append(deps);
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
