/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.ConditionalGroup;
import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Event fired when a player exits a conditional group.
 *
 * <p>This event is not cancellable - the player has already left the group
 * when this event fires.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onGroupExit(ConditionalGroupExitEvent event) {
 *     ConditionalGroup group = event.getGroup();
 *
 *     if (group.getName().equals("night_vip")) {
 *         // Log the event
 *         logger.info(event.getPlayer().getName() + " left night VIP group");
 *
 *         // Perform cleanup
 *         removeVipPerks(event.getPlayer());
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionalGroup
 * @see ConditionalGroupEnterEvent
 */
public class ConditionalGroupExitEvent extends UnifiedEvent {

    private final UnifiedPlayer player;
    private final ConditionalGroup group;
    private final ExitReason reason;

    /**
     * Constructs a new group exit event.
     *
     * @param player the player exiting the group
     * @param group  the group being exited
     * @param reason the reason for exiting
     */
    public ConditionalGroupExitEvent(
            @NotNull UnifiedPlayer player,
            @NotNull ConditionalGroup group,
            @NotNull ExitReason reason
    ) {
        super();
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
    }

    /**
     * Returns the player exiting the group.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the group being exited.
     *
     * @return the group
     * @since 1.0.0
     */
    @NotNull
    public ConditionalGroup getGroup() {
        return group;
    }

    /**
     * Returns the name of the group.
     *
     * @return the group name
     * @since 1.0.0
     */
    @NotNull
    public String getGroupName() {
        return group.getName();
    }

    /**
     * Returns the reason the player exited the group.
     *
     * @return the exit reason
     * @since 1.0.0
     */
    @NotNull
    public ExitReason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ConditionalGroupExitEvent[player=" + player.getName() +
                ", group=" + group.getName() +
                ", reason=" + reason + "]";
    }

    /**
     * Reasons for exiting a conditional group.
     *
     * @since 1.0.0
     */
    public enum ExitReason {
        /**
         * The condition is no longer met.
         */
        CONDITION_NO_LONGER_MET,

        /**
         * The player disconnected.
         */
        PLAYER_DISCONNECT,

        /**
         * The group was unregistered.
         */
        GROUP_UNREGISTERED,

        /**
         * Manually removed by plugin.
         */
        FORCED_REMOVAL,

        /**
         * Server is shutting down.
         */
        SERVER_SHUTDOWN
    }
}
