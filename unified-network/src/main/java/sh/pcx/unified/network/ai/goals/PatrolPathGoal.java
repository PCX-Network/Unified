/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.ai.goals;

import sh.pcx.unified.network.ai.goal.AIGoal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

/**
 * AI goal that makes an entity patrol between waypoints.
 *
 * <p>This goal moves the entity along a path of patrol points,
 * optionally waiting at each point.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * List<Location> patrolPoints = List.of(point1, point2, point3);
 *
 * controller.addGoal(2, new PatrolPathGoal(entity, patrolPoints)
 *     .speed(1.0)
 *     .waitTime(Duration.ofSeconds(5))
 *     .loop(true)
 *     .reverseOnEnd(false)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AIGoal
 */
public class PatrolPathGoal extends AIGoal {

    private final List<?> patrolPoints;
    private double speed = 1.0;
    private Duration waitTime = Duration.ZERO;
    private boolean loop = true;
    private boolean reverseOnEnd = false;
    private double arrivalRadius = 1.5;

    private int currentIndex = 0;
    private int waitTicks = 0;
    private boolean waiting = false;
    private boolean forward = true;

    /**
     * Creates a patrol path goal.
     *
     * @param entity       the entity that will patrol
     * @param patrolPoints the list of patrol locations
     */
    public PatrolPathGoal(@NotNull Object entity, @NotNull List<?> patrolPoints) {
        super(entity);
        this.patrolPoints = patrolPoints;
        setFlags(GoalFlag.MOVE);
    }

    /**
     * Sets the movement speed modifier.
     *
     * @param speed the speed modifier
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the wait time at each patrol point.
     *
     * @param waitTime the time to wait
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal waitTime(@NotNull Duration waitTime) {
        this.waitTime = waitTime;
        return this;
    }

    /**
     * Sets the wait time in ticks at each patrol point.
     *
     * @param ticks the wait time in ticks
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal waitTicks(int ticks) {
        this.waitTime = Duration.ofMillis(ticks * 50L);
        return this;
    }

    /**
     * Sets whether the patrol loops.
     *
     * @param loop true to loop
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal loop(boolean loop) {
        this.loop = loop;
        return this;
    }

    /**
     * Sets whether to reverse direction at the end.
     *
     * <p>When true, the entity patrols back and forth.
     * When false, the entity returns to the start.
     *
     * @param reverse true to reverse
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal reverseOnEnd(boolean reverse) {
        this.reverseOnEnd = reverse;
        return this;
    }

    /**
     * Sets the arrival radius.
     *
     * <p>The entity is considered to have arrived when within this distance.
     *
     * @param radius the arrival radius
     * @return this goal for chaining
     */
    @NotNull
    public PatrolPathGoal arrivalRadius(double radius) {
        this.arrivalRadius = radius;
        return this;
    }

    /**
     * Gets the patrol points.
     *
     * @param <T> the location type
     * @return the patrol points
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> List<T> getPatrolPoints() {
        return (List<T>) patrolPoints;
    }

    /**
     * Gets the current patrol index.
     *
     * @return the current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Sets the current patrol index.
     *
     * @param index the new index
     */
    public void setCurrentIndex(int index) {
        this.currentIndex = Math.max(0, Math.min(index, patrolPoints.size() - 1));
    }

    /**
     * Gets the current target location.
     *
     * @param <T> the location type
     * @return the current target
     */
    @SuppressWarnings("unchecked")
    public <T> T getCurrentTarget() {
        return (T) patrolPoints.get(currentIndex);
    }

    @Override
    public boolean canStart() {
        return !patrolPoints.isEmpty();
    }

    @Override
    public boolean canContinue() {
        if (patrolPoints.isEmpty()) {
            return false;
        }

        // If looping, always continue
        if (loop) {
            return true;
        }

        // If not looping, continue until we've completed the path
        return currentIndex < patrolPoints.size() - 1 || waiting;
    }

    @Override
    public void start() {
        waiting = false;
        waitTicks = 0;
        moveToCurrentPoint();
    }

    @Override
    public void tick() {
        if (waiting) {
            if (--waitTicks <= 0) {
                waiting = false;
                advanceToNextPoint();
                moveToCurrentPoint();
            }
            return;
        }

        // Check if we've arrived at the current point
        if (hasArrivedAtCurrentPoint()) {
            if (!waitTime.isZero()) {
                waiting = true;
                waitTicks = (int) (waitTime.toMillis() / 50);
                getNavigation().stop();
            } else {
                advanceToNextPoint();
                moveToCurrentPoint();
            }
        }

        // Recalculate path if stuck
        if (getNavigation().isStuck()) {
            moveToCurrentPoint();
        }
    }

    @Override
    public void stop() {
        getNavigation().stop();
    }

    private void moveToCurrentPoint() {
        if (!patrolPoints.isEmpty() && currentIndex >= 0 && currentIndex < patrolPoints.size()) {
            getNavigation().moveTo(patrolPoints.get(currentIndex), speed);
        }
    }

    private void advanceToNextPoint() {
        if (reverseOnEnd) {
            // Ping-pong pattern
            if (forward) {
                currentIndex++;
                if (currentIndex >= patrolPoints.size()) {
                    currentIndex = patrolPoints.size() - 2;
                    forward = false;
                }
            } else {
                currentIndex--;
                if (currentIndex < 0) {
                    currentIndex = 1;
                    forward = true;
                }
            }
        } else {
            // Loop pattern
            currentIndex++;
            if (currentIndex >= patrolPoints.size()) {
                currentIndex = 0;
            }
        }
    }

    private boolean hasArrivedAtCurrentPoint() {
        if (patrolPoints.isEmpty() || currentIndex >= patrolPoints.size()) {
            return false;
        }

        // Implementation would calculate distance to current point
        return getNavigation().isDone() || !getNavigation().isNavigating();
    }
}
