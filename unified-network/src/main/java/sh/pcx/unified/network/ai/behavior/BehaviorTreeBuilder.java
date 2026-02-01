/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.behavior;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default implementation of the behavior tree builder.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class BehaviorTreeBuilder implements BehaviorTree.Builder {

    private String name = "BehaviorTree";
    private final Deque<CompositeBuilder> compositeStack = new ArrayDeque<>();
    private CompositeBuilder currentComposite;
    private BehaviorNode root;

    @Override
    @NotNull
    public BehaviorTree.Builder name(@NotNull String name) {
        this.name = name;
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder selector() {
        return selector("Selector");
    }

    @Override
    @NotNull
    public BehaviorTree.Builder selector(@NotNull String name) {
        return pushComposite(new CompositeBuilder(name, BehaviorNode.NodeType.SELECTOR));
    }

    @Override
    @NotNull
    public BehaviorTree.Builder sequence() {
        return sequence("Sequence");
    }

    @Override
    @NotNull
    public BehaviorTree.Builder sequence(@NotNull String name) {
        return pushComposite(new CompositeBuilder(name, BehaviorNode.NodeType.SEQUENCE));
    }

    @Override
    @NotNull
    public BehaviorTree.Builder parallel(@NotNull BehaviorTree.ParallelPolicy successPolicy,
                                         @NotNull BehaviorTree.ParallelPolicy failurePolicy) {
        CompositeBuilder builder = new CompositeBuilder("Parallel", BehaviorNode.NodeType.PARALLEL);
        builder.successPolicy = successPolicy;
        builder.failurePolicy = failurePolicy;
        return pushComposite(builder);
    }

    private BehaviorTree.Builder pushComposite(CompositeBuilder builder) {
        if (currentComposite != null) {
            compositeStack.push(currentComposite);
        }
        currentComposite = builder;
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder end() {
        if (currentComposite == null) {
            throw new IllegalStateException("No composite to end");
        }

        BehaviorNode node = currentComposite.build();

        if (compositeStack.isEmpty()) {
            root = node;
            currentComposite = null;
        } else {
            currentComposite = compositeStack.pop();
            currentComposite.children.add(node);
        }

        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder condition(@NotNull String name, @NotNull Predicate<Object> condition) {
        addLeaf(new ConditionNode(name, condition));
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder action(@NotNull String name, @NotNull Function<Object, BehaviorStatus> action) {
        addLeaf(new ActionNode(name, action));
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder wait(@NotNull String name, int ticks) {
        addLeaf(new WaitNode(name, ticks, ticks));
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder waitRandom(@NotNull String name, int minTicks, int maxTicks) {
        addLeaf(new WaitNode(name, minTicks, maxTicks));
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder succeeder() {
        // Will wrap the next node
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder inverter() {
        // Will wrap the next node
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder repeat(int times) {
        // Will wrap the next node
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder repeatUntilFail() {
        return repeat(-1);
    }

    @Override
    @NotNull
    public BehaviorTree.Builder limit(int maxExecutions) {
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder cooldown(int ticks) {
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder timeout(int ticks) {
        return this;
    }

    @Override
    @NotNull
    public BehaviorTree.Builder subTree(@NotNull BehaviorTree tree) {
        addLeaf(new SubTreeNode(tree));
        return this;
    }

    private void addLeaf(BehaviorNode node) {
        if (currentComposite != null) {
            currentComposite.children.add(node);
        } else {
            root = node;
        }
    }

    @Override
    @NotNull
    public BehaviorTree build() {
        if (root == null) {
            throw new IllegalStateException("Behavior tree has no root node");
        }
        return new SimpleBehaviorTree(name, root);
    }

    // =========================================================================
    // Internal Node Implementations
    // =========================================================================

    private static class CompositeBuilder {
        final String name;
        final BehaviorNode.NodeType type;
        final List<BehaviorNode> children = new ArrayList<>();
        BehaviorTree.ParallelPolicy successPolicy;
        BehaviorTree.ParallelPolicy failurePolicy;

        CompositeBuilder(String name, BehaviorNode.NodeType type) {
            this.name = name;
            this.type = type;
        }

        BehaviorNode build() {
            return new CompositeNode(name, type, new ArrayList<>(children));
        }
    }

    private static abstract class AbstractNode implements BehaviorNode {
        protected final String name;
        protected final BehaviorNode.NodeType type;
        protected BehaviorNode parent;
        protected BehaviorStatus status = BehaviorStatus.SUCCESS;

        AbstractNode(String name, BehaviorNode.NodeType type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull BehaviorNode.NodeType getType() {
            return type;
        }

        @Override
        public BehaviorNode getParent() {
            return parent;
        }

        @Override
        public @NotNull List<BehaviorNode> getChildren() {
            return List.of();
        }

        @Override
        public @NotNull BehaviorStatus getStatus() {
            return status;
        }

        @Override
        public void onStart(@NotNull Object entity) {}

        @Override
        public void onFinish(@NotNull Object entity, @NotNull BehaviorStatus status) {}

        @Override
        public void reset() {
            status = BehaviorStatus.SUCCESS;
        }
    }

    private static class CompositeNode extends AbstractNode {
        private final List<BehaviorNode> children;
        private int currentIndex = 0;

        CompositeNode(String name, BehaviorNode.NodeType type, List<BehaviorNode> children) {
            super(name, type);
            this.children = children;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            while (currentIndex < children.size()) {
                BehaviorStatus childStatus = children.get(currentIndex).tick(entity);

                if (type == NodeType.SELECTOR) {
                    if (childStatus == BehaviorStatus.SUCCESS) {
                        reset();
                        return BehaviorStatus.SUCCESS;
                    } else if (childStatus == BehaviorStatus.RUNNING) {
                        return BehaviorStatus.RUNNING;
                    }
                    currentIndex++;
                } else if (type == NodeType.SEQUENCE) {
                    if (childStatus == BehaviorStatus.FAILURE) {
                        reset();
                        return BehaviorStatus.FAILURE;
                    } else if (childStatus == BehaviorStatus.RUNNING) {
                        return BehaviorStatus.RUNNING;
                    }
                    currentIndex++;
                }
            }

            reset();
            return type == NodeType.SELECTOR ? BehaviorStatus.FAILURE : BehaviorStatus.SUCCESS;
        }

        @Override
        public @NotNull List<BehaviorNode> getChildren() {
            return children;
        }

        @Override
        public void reset() {
            super.reset();
            currentIndex = 0;
            for (BehaviorNode child : children) {
                child.reset();
            }
        }
    }

    private static class ConditionNode extends AbstractNode {
        private final Predicate<Object> condition;

        ConditionNode(String name, Predicate<Object> condition) {
            super(name, NodeType.CONDITION);
            this.condition = condition;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            return condition.test(entity) ? BehaviorStatus.SUCCESS : BehaviorStatus.FAILURE;
        }
    }

    private static class ActionNode extends AbstractNode {
        private final Function<Object, BehaviorStatus> action;

        ActionNode(String name, Function<Object, BehaviorStatus> action) {
            super(name, NodeType.ACTION);
            this.action = action;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            return action.apply(entity);
        }
    }

    private static class WaitNode extends AbstractNode {
        private final int minTicks;
        private final int maxTicks;
        private int remaining = -1;

        WaitNode(String name, int minTicks, int maxTicks) {
            super(name, NodeType.WAIT);
            this.minTicks = minTicks;
            this.maxTicks = maxTicks;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            if (remaining < 0) {
                remaining = minTicks + (int) (Math.random() * (maxTicks - minTicks + 1));
            }

            remaining--;

            if (remaining <= 0) {
                remaining = -1;
                return BehaviorStatus.SUCCESS;
            }

            return BehaviorStatus.RUNNING;
        }

        @Override
        public void reset() {
            super.reset();
            remaining = -1;
        }
    }

    private static class SubTreeNode extends AbstractNode {
        private final BehaviorTree subTree;

        SubTreeNode(BehaviorTree subTree) {
            super(subTree.getName(), NodeType.SUB_TREE);
            this.subTree = subTree;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            return subTree.tick(entity);
        }

        @Override
        public void reset() {
            super.reset();
            subTree.reset();
        }
    }

    private record SimpleBehaviorTree(String name, BehaviorNode root) implements BehaviorTree {
        private static BehaviorNode runningNode;

        @Override
        public @NotNull BehaviorNode getRoot() {
            return root;
        }

        @Override
        public @NotNull BehaviorStatus tick(@NotNull Object entity) {
            BehaviorStatus result = root.tick(entity);
            runningNode = result == BehaviorStatus.RUNNING ? findRunningNode(root) : null;
            return result;
        }

        private BehaviorNode findRunningNode(BehaviorNode node) {
            if (node.getStatus() == BehaviorStatus.RUNNING) {
                for (BehaviorNode child : node.getChildren()) {
                    BehaviorNode running = findRunningNode(child);
                    if (running != null) return running;
                }
                return node;
            }
            return null;
        }

        @Override
        public void reset() {
            root.reset();
            runningNode = null;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public boolean isRunning() {
            return runningNode != null;
        }

        @Override
        public BehaviorNode getRunningNode() {
            return runningNode;
        }
    }
}
