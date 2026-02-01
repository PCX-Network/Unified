/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goals;

import sh.pcx.unified.network.ai.goal.AIGoal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * AI goal that makes an entity follow a player.
 *
 * <p>This goal moves the entity towards a target player, maintaining
 * a specified minimum distance.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * controller.addGoal(1, new FollowPlayerGoal(entity, targetPlayer)
 *     .speed(1.2)
 *     .minDistance(2.0)
 *     .maxDistance(10.0)
 *     .teleportDistance(20.0)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIGoal
 */
public class FollowPlayerGoal extends AIGoal {

    private Object targetPlayer;
    private UUID targetPlayerId;
    private double speed = 1.0;
    private double minDistance = 2.0;
    private double maxDistance = 10.0;
    private double teleportDistance = 20.0;
    private boolean teleportEnabled = true;
    private int recalculatePath = 0;
    private int recalculateInterval = 10;

    /**
     * Creates a follow player goal.
     *
     * @param entity       the entity that will follow
     * @param targetPlayer the player to follow
     */
    public FollowPlayerGoal(@NotNull Object entity, @NotNull Object targetPlayer) {
        super(entity);
        this.targetPlayer = targetPlayer;
        setFlags(GoalFlag.MOVE, GoalFlag.LOOK);
    }

    /**
     * Creates a follow player goal with a player UUID.
     *
     * <p>The player will be looked up when the goal starts.
     *
     * @param entity         the entity that will follow
     * @param targetPlayerId the UUID of the player to follow
     */
    public FollowPlayerGoal(@NotNull Object entity, @NotNull UUID targetPlayerId) {
        super(entity);
        this.targetPlayerId = targetPlayerId;
        setFlags(GoalFlag.MOVE, GoalFlag.LOOK);
    }

    /**
     * Sets the movement speed modifier.
     *
     * @param speed the speed modifier
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the minimum following distance.
     *
     * <p>The entity will stop when within this distance.
     *
     * @param distance the minimum distance
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal minDistance(double distance) {
        this.minDistance = distance;
        return this;
    }

    /**
     * Sets the maximum following distance.
     *
     * <p>The entity starts following when the player exceeds this distance.
     *
     * @param distance the maximum distance
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal maxDistance(double distance) {
        this.maxDistance = distance;
        return this;
    }

    /**
     * Sets the teleport distance.
     *
     * <p>When the player exceeds this distance, the entity teleports.
     *
     * @param distance the teleport distance
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal teleportDistance(double distance) {
        this.teleportDistance = distance;
        return this;
    }

    /**
     * Sets whether teleporting is enabled.
     *
     * @param enabled true to enable teleporting
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal teleportEnabled(boolean enabled) {
        this.teleportEnabled = enabled;
        return this;
    }

    /**
     * Sets the path recalculation interval.
     *
     * @param ticks the interval in ticks
     * @return this goal for chaining
     */
    @NotNull
    public FollowPlayerGoal recalculateInterval(int ticks) {
        this.recalculateInterval = ticks;
        return this;
    }

    /**
     * Gets the target player.
     *
     * @param <T> the player type
     * @return the target player, or null if not found
     */
    @Nullable
    public <T> T getTargetPlayer() {
        return resolvePlayer();
    }

    /**
     * Gets the movement speed.
     *
     * @return the speed modifier
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the minimum distance.
     *
     * @return the minimum distance
     */
    public double getMinDistance() {
        return minDistance;
    }

    /**
     * Gets the maximum distance.
     *
     * @return the maximum distance
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    @Override
    public boolean canStart() {
        Object player = resolvePlayer();
        if (player == null) {
            return false;
        }

        double distance = getDistanceToPlayer(player);
        return distance > maxDistance;
    }

    @Override
    public boolean canContinue() {
        Object player = resolvePlayer();
        if (player == null) {
            return false;
        }

        double distance = getDistanceToPlayer(player);
        return distance > minDistance;
    }

    @Override
    public void start() {
        recalculatePath = 0;
    }

    @Override
    public void tick() {
        Object player = resolvePlayer();
        if (player == null) {
            return;
        }

        double distance = getDistanceToPlayer(player);

        // Check for teleport
        if (teleportEnabled && distance > teleportDistance) {
            teleportToPlayer(player);
            return;
        }

        // Look at player
        lookAtPlayer(player);

        // Recalculate path periodically
        if (--recalculatePath <= 0) {
            recalculatePath = recalculateInterval;
            getNavigation().moveTo(player, speed);
        }
    }

    @Override
    public void stop() {
        getNavigation().stop();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T resolvePlayer() {
        if (targetPlayer != null) {
            return (T) targetPlayer;
        }

        if (targetPlayerId != null) {
            // Implementation would look up the player by UUID
            return null;
        }

        return null;
    }

    private double getDistanceToPlayer(Object player) {
        // Implementation would calculate distance between entity and player
        return 0.0;
    }

    private void teleportToPlayer(Object player) {
        // Implementation would teleport the entity near the player
    }

    private void lookAtPlayer(Object player) {
        // Implementation would make the entity look at the player
    }
}
