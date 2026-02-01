/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import sh.pcx.unified.content.advancement.structure.AdvancementDisplay;
import sh.pcx.unified.content.advancement.structure.AdvancementParent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a custom advancement registered with the API.
 *
 * <p>CustomAdvancement encapsulates all properties of an advancement,
 * including display information, criteria, requirements, and rewards.
 *
 * <h2>Advancement Structure</h2>
 * <ul>
 *   <li><b>Key</b> - Unique namespaced identifier</li>
 *   <li><b>Display</b> - Visual properties (icon, title, frame)</li>
 *   <li><b>Parent</b> - Parent advancement (or root)</li>
 *   <li><b>Criteria</b> - Conditions that must be met</li>
 *   <li><b>Requirements</b> - How criteria combine (all, any, custom)</li>
 *   <li><b>Rewards</b> - Items, XP, commands given on completion</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AdvancementService
 * @see AdvancementBuilder
 */
public interface CustomAdvancement {

    /**
     * Returns the unique key for this advancement.
     *
     * @return the namespaced key (e.g., "myplugin:first_kill")
     * @since 1.0.0
     */
    @NotNull
    String getKey();

    /**
     * Returns the display properties for this advancement.
     *
     * @return the advancement display
     * @since 1.0.0
     */
    @NotNull
    AdvancementDisplay getDisplay();

    /**
     * Returns the parent of this advancement.
     *
     * @return an Optional containing the parent reference
     * @since 1.0.0
     */
    @NotNull
    Optional<AdvancementParent> getParent();

    /**
     * Checks if this advancement is a root (has no parent).
     *
     * @return true if this is a root advancement
     * @since 1.0.0
     */
    default boolean isRoot() {
        return getParent().map(AdvancementParent::isRoot).orElse(true);
    }

    /**
     * Returns all criteria for this advancement.
     *
     * @return an unmodifiable map of criterion name to trigger
     * @since 1.0.0
     */
    @NotNull
    Map<String, Trigger> getCriteria();

    /**
     * Returns the requirements for completing this advancement.
     *
     * <p>The outer list uses OR logic; the inner lists use AND logic.
     * Example: [[a, b], [c]] means (a AND b) OR c.
     *
     * @return the requirements structure
     * @since 1.0.0
     */
    @NotNull
    List<List<String>> getRequirements();

    /**
     * Returns the reward for completing this advancement.
     *
     * @return an Optional containing the reward if set
     * @since 1.0.0
     */
    @NotNull
    Optional<AdvancementReward> getReward();

    /**
     * Checks if this advancement sends a toast notification.
     *
     * @return true if toast is shown on completion
     * @since 1.0.0
     */
    boolean showsToast();

    /**
     * Checks if this advancement is announced in chat.
     *
     * @return true if completion is announced
     * @since 1.0.0
     */
    boolean announcesToChat();

    /**
     * Checks if this advancement is hidden until completed.
     *
     * @return true if hidden
     * @since 1.0.0
     */
    boolean isHidden();

    /**
     * Creates a builder pre-populated with this advancement's values.
     *
     * @return a new AdvancementBuilder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder toBuilder();
}
