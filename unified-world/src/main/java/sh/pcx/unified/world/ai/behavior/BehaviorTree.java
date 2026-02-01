package sh.pcx.unified.world.ai.behavior;

import sh.pcx.unified.world.ai.core.AIController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Represents a behavior tree for complex AI decision making.
 *
 * <p>A BehaviorTree is a hierarchical structure of nodes that define
 * AI behavior. It supports various node types including sequences,
 * selectors, decorators, and action nodes.</p>
 *
 * <h2>Node Types:</h2>
 * <ul>
 *   <li><b>Sequence:</b> Executes children in order, fails if any fails</li>
 *   <li><b>Selector:</b> Executes children until one succeeds</li>
 *   <li><b>Parallel:</b> Executes all children simultaneously</li>
 *   <li><b>Decorator:</b> Modifies the behavior of a child node</li>
 *   <li><b>Action:</b> Performs a specific action</li>
 *   <li><b>Condition:</b> Checks a condition</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * BehaviorTree tree = BehaviorTree.builder("guard")
 *     .selector()
 *         .sequence()
 *             .condition(ctx -> ctx.hasTarget())
 *             .action(Actions.attack())
 *         .end()
 *         .sequence()
 *             .condition(ctx -> ctx.shouldPatrol())
 *             .action(Actions.patrol())
 *         .end()
 *         .action(Actions.idle())
 *     .end()
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BehaviorTree {

    /**
     * Gets the name of this behavior tree.
     *
     * @return the tree name
     */
    @NotNull
    String getName();

    /**
     * Gets the root node of this tree.
     *
     * @return the root node
     */
    @NotNull
    BehaviorNode getRoot();

    /**
     * Ticks the behavior tree.
     *
     * @param controller the AI controller
     * @return the result of the tick
     */
    @NotNull
    NodeStatus tick(@NotNull AIController controller);

    /**
     * Resets the behavior tree to its initial state.
     */
    void reset();

    /**
     * Checks if the tree is currently running.
     *
     * @return true if running
     */
    boolean isRunning();

    /**
     * Gets a node by its ID.
     *
     * @param id the node ID
     * @return the node, or empty if not found
     */
    @NotNull
    Optional<BehaviorNode> getNode(@NotNull String id);

    /**
     * Creates a new behavior tree builder.
     *
     * @param name the tree name
     * @return the builder
     */
    @NotNull
    static Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Builder for creating behavior trees.
     */
    class Builder {
        private final String name;
        private BehaviorNode root;

        /**
         * Creates a new builder.
         *
         * @param name the tree name
         */
        public Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Starts a sequence node.
         *
         * @return this builder
         */
        @NotNull
        public Builder sequence() {
            // Implementation would create sequence node
            return this;
        }

        /**
         * Starts a selector node.
         *
         * @return this builder
         */
        @NotNull
        public Builder selector() {
            // Implementation would create selector node
            return this;
        }

        /**
         * Starts a parallel node.
         *
         * @return this builder
         */
        @NotNull
        public Builder parallel() {
            // Implementation would create parallel node
            return this;
        }

        /**
         * Adds a condition node.
         *
         * @param condition the condition predicate
         * @return this builder
         */
        @NotNull
        public Builder condition(@NotNull Predicate<BehaviorContext> condition) {
            // Implementation would create condition node
            return this;
        }

        /**
         * Adds an action node.
         *
         * @param action the action to execute
         * @return this builder
         */
        @NotNull
        public Builder action(@NotNull BehaviorAction action) {
            // Implementation would create action node
            return this;
        }

        /**
         * Ends the current composite node.
         *
         * @return this builder
         */
        @NotNull
        public Builder end() {
            // Implementation would end current composite
            return this;
        }

        /**
         * Adds a decorator node.
         *
         * @param decorator the decorator
         * @return this builder
         */
        @NotNull
        public Builder decorate(@NotNull BehaviorDecorator decorator) {
            // Implementation would add decorator
            return this;
        }

        /**
         * Builds the behavior tree.
         *
         * @return the behavior tree
         */
        @NotNull
        public BehaviorTree build() {
            return new DefaultBehaviorTree(name, root);
        }
    }

    /**
     * Node execution status.
     */
    enum NodeStatus {
        /**
         * Node is still running.
         */
        RUNNING,

        /**
         * Node completed successfully.
         */
        SUCCESS,

        /**
         * Node failed.
         */
        FAILURE
    }

    /**
     * Base interface for behavior tree nodes.
     */
    interface BehaviorNode {

        /**
         * Gets the node ID.
         *
         * @return the node ID
         */
        @NotNull
        String getId();

        /**
         * Ticks this node.
         *
         * @param context the behavior context
         * @return the node status
         */
        @NotNull
        NodeStatus tick(@NotNull BehaviorContext context);

        /**
         * Resets this node.
         */
        void reset();
    }

    /**
     * Context for behavior tree execution.
     */
    interface BehaviorContext {

        /**
         * Gets the AI controller.
         *
         * @return the controller
         */
        @NotNull
        AIController getController();

        /**
         * Gets a context value.
         *
         * @param key the value key
         * @param <T> the value type
         * @return the value, or null
         */
        @Nullable
        <T> T get(@NotNull String key);

        /**
         * Sets a context value.
         *
         * @param key the value key
         * @param value the value
         * @param <T> the value type
         */
        <T> void set(@NotNull String key, @NotNull T value);

        /**
         * Checks if the controller has a target.
         *
         * @return true if has target
         */
        boolean hasTarget();
    }

    /**
     * Interface for behavior actions.
     */
    interface BehaviorAction {

        /**
         * Executes the action.
         *
         * @param context the behavior context
         * @return the action result
         */
        @NotNull
        NodeStatus execute(@NotNull BehaviorContext context);
    }

    /**
     * Interface for behavior decorators.
     */
    interface BehaviorDecorator {

        /**
         * Decorates the child node's result.
         *
         * @param child the child node
         * @param context the behavior context
         * @return the decorated result
         */
        @NotNull
        NodeStatus decorate(@NotNull BehaviorNode child, @NotNull BehaviorContext context);
    }
}

/**
 * Default implementation of BehaviorTree.
 */
class DefaultBehaviorTree implements BehaviorTree {

    private final String name;
    private final BehaviorNode root;
    private boolean running;

    DefaultBehaviorTree(@NotNull String name, @Nullable BehaviorNode root) {
        this.name = name;
        this.root = root != null ? root : new EmptyNode();
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public BehaviorNode getRoot() {
        return root;
    }

    @Override
    @NotNull
    public NodeStatus tick(@NotNull AIController controller) {
        running = true;
        // Create context and tick root
        // Simplified implementation
        return NodeStatus.SUCCESS;
    }

    @Override
    public void reset() {
        running = false;
        root.reset();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    @NotNull
    public Optional<BehaviorNode> getNode(@NotNull String id) {
        // Would search tree for node
        return Optional.empty();
    }

    /**
     * Empty placeholder node.
     */
    private static class EmptyNode implements BehaviorNode {
        @Override
        @NotNull
        public String getId() {
            return "empty";
        }

        @Override
        @NotNull
        public NodeStatus tick(@NotNull BehaviorContext context) {
            return NodeStatus.SUCCESS;
        }

        @Override
        public void reset() {
            // No-op
        }
    }
}
