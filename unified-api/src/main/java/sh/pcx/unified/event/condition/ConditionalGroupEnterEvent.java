/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.condition;

import sh.pcx.unified.condition.ConditionalGroup;
import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.event.UnifiedEvent;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Event fired when a player enters a conditional group.
 *
 * <p>This event is cancellable. If cancelled, the player will not enter
 * the group and any on-enter actions will not be executed.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @EventHandler
 * public void onGroupEnter(ConditionalGroupEnterEvent event) {
 *     ConditionalGroup group = event.getGroup();
 *
 *     if (group.getName().equals("night_vip")) {
 *         // Log the event
 *         logger.info(event.getPlayer().getName() + " entered night VIP group");
 *
 *         // Optionally prevent entry
 *         if (someCondition) {
 *             event.setCancelled(true);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 * @see ConditionalGroup
 * @see ConditionalGroupExitEvent
 */
public class ConditionalGroupEnterEvent extends UnifiedEvent implements Cancellable {

    private final UnifiedPlayer player;
    private final ConditionalGroup group;
    private boolean cancelled;

    /**
     * Constructs a new group enter event.
     *
     * @param player the player entering the group
     * @param group  the group being entered
     */
    public ConditionalGroupEnterEvent(@NotNull UnifiedPlayer player, @NotNull ConditionalGroup group) {
        super();
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.cancelled = false;
    }

    /**
     * Returns the player entering the group.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    public UnifiedPlayer getPlayer() {
        return player;
    }

    /**
     * Returns the group being entered.
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public String toString() {
        return "ConditionalGroupEnterEvent[player=" + player.getName() +
                ", group=" + group.getName() +
                ", cancelled=" + cancelled + "]";
    }
}
