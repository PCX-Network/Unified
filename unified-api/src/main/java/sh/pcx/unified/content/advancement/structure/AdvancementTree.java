/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement.structure;

import sh.pcx.unified.content.advancement.CustomAdvancement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a tree structure of related advancements.
 *
 * <p>An AdvancementTree organizes advancements in a hierarchical structure,
 * with a single root advancement and any number of child advancements.
 * Trees correspond to tabs in the Minecraft advancement GUI.
 *
 * <h2>Tree Structure</h2>
 * <p>Each tree has exactly one root advancement which determines:
 * <ul>
 *   <li>The tab icon in the advancement GUI</li>
 *   <li>The background texture for the tab</li>
 *   <li>The namespace/category for all child advancements</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a new advancement tree
 * AdvancementTree tree = AdvancementTree.create("myplugin:quests");
 *
 * // Set the root advancement
 * CustomAdvancement root = advancementService.create("myplugin:quests/root")
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:book")
 *         .title(Component.text("Quest Journal"))
 *         .background("minecraft:textures/gui/advancements/backgrounds/stone.png")
 *         .build())
 *     .build();
 * tree.setRoot(root);
 *
 * // Add child advancements
 * CustomAdvancement quest1 = advancementService.create("myplugin:quests/first_quest")
 *     .parent(root)
 *     .build();
 * tree.add(quest1);
 *
 * // Get all advancements in the tree
 * Collection<CustomAdvancement> all = tree.getAll();
 *
 * // Find children of a specific advancement
 * List<CustomAdvancement> children = tree.getChildren(root);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 * @see AdvancementParent
 */
public final class AdvancementTree {

    private final String namespace;
    private final Map<String, CustomAdvancement> advancements;
    private final Map<String, Set<String>> childrenMap;
    private volatile CustomAdvancement root;

    /**
     * Private constructor - use {@link #create(String)} to create instances.
     */
    private AdvancementTree(String namespace) {
        this.namespace = Objects.requireNonNull(namespace, "Namespace cannot be null");
        this.advancements = new ConcurrentHashMap<>();
        this.childrenMap = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new advancement tree with the specified namespace.
     *
     * <p>The namespace should be a namespaced identifier that will be used
     * as the prefix for all advancements in this tree.
     *
     * @param namespace the tree namespace (e.g., "myplugin:quests")
     * @return a new AdvancementTree
     * @throws NullPointerException if namespace is null
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementTree create(@NotNull String namespace) {
        return new AdvancementTree(namespace);
    }

    /**
     * Returns the namespace of this tree.
     *
     * @return the tree namespace
     * @since 1.0.0
     */
    @NotNull
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the root advancement of this tree.
     *
     * @return an Optional containing the root advancement if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<CustomAdvancement> getRoot() {
        return Optional.ofNullable(root);
    }

    /**
     * Sets the root advancement of this tree.
     *
     * <p>The root advancement must have no parent (be a true root).
     * Setting a new root will replace any existing root.
     *
     * @param advancement the root advancement
     * @throws NullPointerException     if advancement is null
     * @throws IllegalArgumentException if advancement has a parent
     * @since 1.0.0
     */
    public void setRoot(@NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(advancement, "Root advancement cannot be null");
        if (advancement.getParent().isPresent() && !advancement.getParent().get().isRoot()) {
            throw new IllegalArgumentException("Root advancement cannot have a parent");
        }
        this.root = advancement;
        advancements.put(advancement.getKey(), advancement);
    }

    /**
     * Adds an advancement to this tree.
     *
     * <p>The advancement should have a parent that is already in this tree,
     * or be a root advancement.
     *
     * @param advancement the advancement to add
     * @throws NullPointerException if advancement is null
     * @since 1.0.0
     */
    public void add(@NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(advancement, "Advancement cannot be null");

        String key = advancement.getKey();
        advancements.put(key, advancement);

        // Update parent-child relationship
        advancement.getParent().flatMap(AdvancementParent::getKey).ifPresent(parentKey -> {
            childrenMap.computeIfAbsent(parentKey, k -> ConcurrentHashMap.newKeySet()).add(key);
        });
    }

    /**
     * Adds multiple advancements to this tree.
     *
     * @param advancements the advancements to add
     * @since 1.0.0
     */
    public void addAll(@NotNull Collection<CustomAdvancement> advancements) {
        advancements.forEach(this::add);
    }

    /**
     * Removes an advancement from this tree.
     *
     * <p>Removing an advancement does not remove its children;
     * they will become orphaned.
     *
     * @param key the advancement key
     * @return the removed advancement, or null if not found
     * @since 1.0.0
     */
    @Nullable
    public CustomAdvancement remove(@NotNull String key) {
        CustomAdvancement removed = advancements.remove(key);
        if (removed != null) {
            // Remove from parent's children list
            removed.getParent().flatMap(AdvancementParent::getKey).ifPresent(parentKey -> {
                Set<String> children = childrenMap.get(parentKey);
                if (children != null) {
                    children.remove(key);
                }
            });
            // Remove children mapping for this advancement
            childrenMap.remove(key);
            // Clear root if removed
            if (root != null && root.getKey().equals(key)) {
                root = null;
            }
        }
        return removed;
    }

    /**
     * Gets an advancement by its key.
     *
     * @param key the advancement key
     * @return an Optional containing the advancement if found
     * @since 1.0.0
     */
    @NotNull
    public Optional<CustomAdvancement> get(@NotNull String key) {
        return Optional.ofNullable(advancements.get(key));
    }

    /**
     * Checks if this tree contains an advancement with the given key.
     *
     * @param key the advancement key
     * @return true if the advancement exists in this tree
     * @since 1.0.0
     */
    public boolean contains(@NotNull String key) {
        return advancements.containsKey(key);
    }

    /**
     * Returns all advancements in this tree.
     *
     * @return an unmodifiable collection of all advancements
     * @since 1.0.0
     */
    @NotNull
    public Collection<CustomAdvancement> getAll() {
        return Collections.unmodifiableCollection(advancements.values());
    }

    /**
     * Returns all advancement keys in this tree.
     *
     * @return an unmodifiable set of all advancement keys
     * @since 1.0.0
     */
    @NotNull
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(advancements.keySet());
    }

    /**
     * Returns the direct children of an advancement.
     *
     * @param parent the parent advancement
     * @return a list of child advancements
     * @since 1.0.0
     */
    @NotNull
    public List<CustomAdvancement> getChildren(@NotNull CustomAdvancement parent) {
        return getChildren(parent.getKey());
    }

    /**
     * Returns the direct children of an advancement by key.
     *
     * @param parentKey the parent advancement key
     * @return a list of child advancements
     * @since 1.0.0
     */
    @NotNull
    public List<CustomAdvancement> getChildren(@NotNull String parentKey) {
        Set<String> childKeys = childrenMap.get(parentKey);
        if (childKeys == null || childKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return childKeys.stream()
                .map(advancements::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns all descendants of an advancement (children, grandchildren, etc.).
     *
     * @param parent the parent advancement
     * @return a list of all descendant advancements
     * @since 1.0.0
     */
    @NotNull
    public List<CustomAdvancement> getDescendants(@NotNull CustomAdvancement parent) {
        return getDescendants(parent.getKey());
    }

    /**
     * Returns all descendants of an advancement by key.
     *
     * @param parentKey the parent advancement key
     * @return a list of all descendant advancements
     * @since 1.0.0
     */
    @NotNull
    public List<CustomAdvancement> getDescendants(@NotNull String parentKey) {
        List<CustomAdvancement> descendants = new ArrayList<>();
        collectDescendants(parentKey, descendants);
        return descendants;
    }

    /**
     * Recursively collects all descendants.
     */
    private void collectDescendants(String parentKey, List<CustomAdvancement> result) {
        Set<String> childKeys = childrenMap.get(parentKey);
        if (childKeys == null) {
            return;
        }
        for (String childKey : childKeys) {
            CustomAdvancement child = advancements.get(childKey);
            if (child != null) {
                result.add(child);
                collectDescendants(childKey, result);
            }
        }
    }

    /**
     * Returns the path from root to the specified advancement.
     *
     * @param advancement the target advancement
     * @return a list representing the path from root to target
     * @since 1.0.0
     */
    @NotNull
    public List<CustomAdvancement> getPath(@NotNull CustomAdvancement advancement) {
        List<CustomAdvancement> path = new ArrayList<>();
        CustomAdvancement current = advancement;

        while (current != null) {
            path.add(0, current);
            current = current.getParent()
                    .flatMap(AdvancementParent::getCustomParent)
                    .orElse(null);
        }

        return path;
    }

    /**
     * Returns the depth of an advancement in the tree (0 for root).
     *
     * @param advancement the advancement
     * @return the depth level
     * @since 1.0.0
     */
    public int getDepth(@NotNull CustomAdvancement advancement) {
        int depth = 0;
        CustomAdvancement current = advancement;

        while (current.getParent().isPresent() && !current.getParent().get().isRoot()) {
            depth++;
            current = current.getParent()
                    .flatMap(AdvancementParent::getCustomParent)
                    .orElse(null);
            if (current == null) break;
        }

        return depth;
    }

    /**
     * Returns a stream of all advancements in this tree.
     *
     * @return a stream of advancements
     * @since 1.0.0
     */
    @NotNull
    public Stream<CustomAdvancement> stream() {
        return advancements.values().stream();
    }

    /**
     * Returns the number of advancements in this tree.
     *
     * @return the advancement count
     * @since 1.0.0
     */
    public int size() {
        return advancements.size();
    }

    /**
     * Checks if this tree is empty (has no advancements).
     *
     * @return true if the tree is empty
     * @since 1.0.0
     */
    public boolean isEmpty() {
        return advancements.isEmpty();
    }

    /**
     * Clears all advancements from this tree.
     *
     * @since 1.0.0
     */
    public void clear() {
        advancements.clear();
        childrenMap.clear();
        root = null;
    }

    /**
     * Validates the tree structure.
     *
     * <p>Checks that:
     * <ul>
     *   <li>All advancements have valid parents (in tree or vanilla)</li>
     *   <li>There are no circular references</li>
     *   <li>The root is properly set</li>
     * </ul>
     *
     * @return a list of validation errors, empty if valid
     * @since 1.0.0
     */
    @NotNull
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (root == null && !advancements.isEmpty()) {
            errors.add("Tree has advancements but no root is set");
        }

        for (CustomAdvancement advancement : advancements.values()) {
            advancement.getParent().ifPresent(parent -> {
                if (parent.isCustom()) {
                    String parentKey = parent.getCustomParent().map(CustomAdvancement::getKey).orElse(null);
                    if (parentKey != null && !advancements.containsKey(parentKey)) {
                        errors.add("Advancement '" + advancement.getKey() + "' has parent '" + parentKey + "' not in tree");
                    }
                }
            });
        }

        // Check for circular references
        for (CustomAdvancement advancement : advancements.values()) {
            Set<String> visited = new HashSet<>();
            CustomAdvancement current = advancement;
            while (current != null) {
                if (!visited.add(current.getKey())) {
                    errors.add("Circular reference detected involving '" + advancement.getKey() + "'");
                    break;
                }
                current = current.getParent()
                        .flatMap(AdvancementParent::getCustomParent)
                        .orElse(null);
            }
        }

        return errors;
    }

    @Override
    public String toString() {
        return "AdvancementTree{" +
               "namespace='" + namespace + '\'' +
               ", size=" + advancements.size() +
               ", root=" + (root != null ? root.getKey() : "null") +
               '}';
    }
}
