/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement;

import sh.pcx.unified.content.advancement.structure.AdvancementDisplay;
import sh.pcx.unified.content.advancement.structure.AdvancementParent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fluent builder for creating custom advancements.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Root advancement (creates new tab)
 * CustomAdvancement root = advancements.create("myplugin:root")
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:nether_star")
 *         .title(Component.text("My Adventures"))
 *         .description(Component.text("Begin your journey"))
 *         .frame(AdvancementFrame.TASK)
 *         .background("minecraft:textures/gui/advancements/backgrounds/stone.png")
 *         .showToast(true)
 *         .build())
 *     .criteria("start", Trigger.impossible())
 *     .register();
 *
 * // Child advancement with progress
 * CustomAdvancement hunter = advancements.create("myplugin:monster_hunter")
 *     .parent(root)
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:diamond_sword")
 *         .title(Component.text("Monster Hunter"))
 *         .description(Component.text("Kill 100 monsters"))
 *         .frame(AdvancementFrame.GOAL)
 *         .build())
 *     .criteria("kills", Trigger.playerKilledEntity(
 *         EntityPredicate.category("monster")).count(100))
 *     .reward(AdvancementReward.builder()
 *         .experience(500)
 *         .item(ItemBuilder.of("minecraft:diamond").amount(10).build())
 *         .build())
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see CustomAdvancement
 * @see AdvancementService
 */
public interface AdvancementBuilder {

    /**
     * Sets the display properties for this advancement.
     *
     * @param display the display configuration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder display(@NotNull AdvancementDisplay display);

    /**
     * Sets the parent advancement.
     *
     * @param parent the parent reference
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder parent(@NotNull AdvancementParent parent);

    /**
     * Sets the parent to a custom advancement.
     *
     * @param parent the parent advancement
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default AdvancementBuilder parent(@NotNull CustomAdvancement parent) {
        return parent(AdvancementParent.of(parent));
    }

    /**
     * Sets the parent to a vanilla advancement by key.
     *
     * @param vanillaKey the vanilla advancement key
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    default AdvancementBuilder parentVanilla(@NotNull String vanillaKey) {
        return parent(AdvancementParent.ofVanilla(vanillaKey));
    }

    /**
     * Adds a criterion with a trigger.
     *
     * @param name    the criterion name
     * @param trigger the trigger condition
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder criteria(@NotNull String name, @NotNull Trigger trigger);

    /**
     * Sets the requirement mode to require all criteria.
     *
     * <p>All criteria must be completed for the advancement.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder requireAll();

    /**
     * Sets the requirement mode to require any criterion.
     *
     * <p>Only one criterion needs to be completed.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder requireAny();

    /**
     * Sets custom requirements.
     *
     * <p>The outer list uses OR logic; inner lists use AND logic.
     * Example: [[a, b], [c]] means (a AND b) OR c.
     *
     * @param requirements the requirements structure
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder requirements(@NotNull List<List<String>> requirements);

    /**
     * Sets the reward for completing this advancement.
     *
     * @param reward the reward configuration
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder reward(@NotNull AdvancementReward reward);

    /**
     * Sets whether to show a toast notification on completion.
     *
     * @param showToast true to show toast (default: from display)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder showToast(boolean showToast);

    /**
     * Sets whether to announce completion in chat.
     *
     * @param announce true to announce (default: from display/frame)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder announceToChat(boolean announce);

    /**
     * Sets whether this advancement is hidden until completed.
     *
     * @param hidden true to hide (default: from display)
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    AdvancementBuilder hidden(boolean hidden);

    /**
     * Builds the advancement without registering.
     *
     * @return the constructed CustomAdvancement
     * @throws IllegalStateException if required fields are missing
     * @since 1.0.0
     */
    @NotNull
    CustomAdvancement build();

    /**
     * Builds and registers the advancement.
     *
     * @return the registered CustomAdvancement
     * @throws IllegalStateException if required fields are missing
     * @since 1.0.0
     */
    @NotNull
    CustomAdvancement register();
}
