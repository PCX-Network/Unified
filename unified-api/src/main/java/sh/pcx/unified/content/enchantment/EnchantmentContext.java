/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.enchantment;

import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.world.UnifiedBlock;
import sh.pcx.unified.world.UnifiedLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Context objects provided to enchantment effect handlers.
 *
 * <p>EnchantmentContext provides type-safe access to event data relevant
 * to each enchantment trigger type. Each context type contains only the
 * data applicable to that specific trigger.
 *
 * <h2>Context Types</h2>
 * <ul>
 *   <li>{@link Hit} - Entity hit events</li>
 *   <li>{@link Damage} - Damage received events</li>
 *   <li>{@link BlockBreak} - Block breaking events</li>
 *   <li>{@link Experience} - Experience gain events</li>
 *   <li>{@link ItemUse} - Item usage events</li>
 *   <li>{@link Equipped} - Passive equipped effects</li>
 *   <li>{@link Shoot} - Projectile shooting events</li>
 *   <li>{@link ArrowHit} - Arrow impact events</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EnchantmentBuilder
 */
public sealed interface EnchantmentContext {

    /**
     * Returns the player involved in this context.
     *
     * @return the player
     * @since 1.0.0
     */
    @NotNull
    UnifiedPlayer getPlayer();

    /**
     * Returns the enchanted item.
     *
     * @return the item with the enchantment
     * @since 1.0.0
     */
    @NotNull
    UnifiedItemStack getItem();

    /**
     * Returns the enchantment that triggered this context.
     *
     * @return the custom enchantment
     * @since 1.0.0
     */
    @NotNull
    CustomEnchantment getEnchantment();

    /**
     * Context for entity hit events (melee attacks).
     *
     * @since 1.0.0
     */
    non-sealed interface Hit extends EnchantmentContext {

        /**
         * Returns the UUID of the damaged entity.
         *
         * @return the target entity UUID
         * @since 1.0.0
         */
        @NotNull
        UUID getTargetId();

        /**
         * Returns the type of the damaged entity.
         *
         * @return the entity type ID
         * @since 1.0.0
         */
        @NotNull
        String getTargetType();

        /**
         * Returns the target entity if it's a player.
         *
         * @return an Optional containing the target player
         * @since 1.0.0
         */
        @NotNull
        Optional<UnifiedPlayer> getTargetPlayer();

        /**
         * Returns the location of the hit.
         *
         * @return the hit location
         * @since 1.0.0
         */
        @NotNull
        UnifiedLocation getLocation();

        /**
         * Returns the base damage dealt.
         *
         * @return the damage amount
         * @since 1.0.0
         */
        double getDamage();

        /**
         * Sets the damage to be dealt.
         *
         * @param damage the new damage amount
         * @since 1.0.0
         */
        void setDamage(double damage);

        /**
         * Returns whether the attack was a critical hit.
         *
         * @return true if critical hit
         * @since 1.0.0
         */
        boolean isCritical();

        /**
         * Cancels the damage event.
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }

    /**
     * Context for damage received events.
     *
     * @since 1.0.0
     */
    non-sealed interface Damage extends EnchantmentContext {

        /**
         * Returns the damage cause.
         *
         * @return the damage cause name
         * @since 1.0.0
         */
        @NotNull
        String getCause();

        /**
         * Returns the UUID of the damager, if any.
         *
         * @return an Optional containing the damager UUID
         * @since 1.0.0
         */
        @NotNull
        Optional<UUID> getDamagerId();

        /**
         * Returns the damage amount.
         *
         * @return the damage
         * @since 1.0.0
         */
        double getDamage();

        /**
         * Sets the damage to be taken.
         *
         * @param damage the new damage amount
         * @since 1.0.0
         */
        void setDamage(double damage);

        /**
         * Returns the final damage after armor and effects.
         *
         * @return the final damage
         * @since 1.0.0
         */
        double getFinalDamage();

        /**
         * Cancels the damage event.
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }

    /**
     * Context for block breaking events.
     *
     * @since 1.0.0
     */
    non-sealed interface BlockBreak extends EnchantmentContext {

        /**
         * Returns the block being broken.
         *
         * @return the block
         * @since 1.0.0
         */
        @NotNull
        UnifiedBlock getBlock();

        /**
         * Returns the block type.
         *
         * @return the block type ID
         * @since 1.0.0
         */
        @NotNull
        String getBlockType();

        /**
         * Returns the experience to be dropped.
         *
         * @return the experience amount
         * @since 1.0.0
         */
        int getExpToDrop();

        /**
         * Sets the experience to be dropped.
         *
         * @param exp the experience amount
         * @since 1.0.0
         */
        void setExpToDrop(int exp);

        /**
         * Sets whether items should be dropped.
         *
         * @param dropItems true to drop items
         * @since 1.0.0
         */
        void setDropItems(boolean dropItems);

        /**
         * Returns whether items will be dropped.
         *
         * @return true if items will drop
         * @since 1.0.0
         */
        boolean willDropItems();

        /**
         * Cancels the break event.
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }

    /**
     * Context for experience gain events.
     *
     * @since 1.0.0
     */
    non-sealed interface Experience extends EnchantmentContext {

        /**
         * Returns the experience amount.
         *
         * @return the experience
         * @since 1.0.0
         */
        int getAmount();

        /**
         * Sets the experience amount.
         *
         * @param amount the new amount
         * @since 1.0.0
         */
        void setAmount(int amount);

        /**
         * Returns the source of the experience.
         *
         * @return the experience source
         * @since 1.0.0
         */
        @NotNull
        String getSource();
    }

    /**
     * Context for item use events (right-click).
     *
     * @since 1.0.0
     */
    non-sealed interface ItemUse extends EnchantmentContext {

        /**
         * Returns the use action type.
         *
         * @return the action type
         * @since 1.0.0
         */
        @NotNull
        String getAction();

        /**
         * Returns the block clicked, if any.
         *
         * @return an Optional containing the clicked block
         * @since 1.0.0
         */
        @NotNull
        Optional<UnifiedBlock> getClickedBlock();

        /**
         * Returns the block face clicked, if any.
         *
         * @return an Optional containing the block face
         * @since 1.0.0
         */
        @NotNull
        Optional<String> getBlockFace();

        /**
         * Cancels the use event.
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }

    /**
     * Context for passive effects while equipped.
     *
     * @since 1.0.0
     */
    non-sealed interface Equipped extends EnchantmentContext {

        /**
         * Returns the equipment slot.
         *
         * @return the slot name
         * @since 1.0.0
         */
        @NotNull
        String getSlot();

        /**
         * Returns how long the item has been equipped in ticks.
         *
         * @return the equipped duration in ticks
         * @since 1.0.0
         */
        long getEquippedTicks();
    }

    /**
     * Context for projectile shooting events.
     *
     * @since 1.0.0
     */
    non-sealed interface Shoot extends EnchantmentContext {

        /**
         * Returns the projectile type.
         *
         * @return the projectile type ID
         * @since 1.0.0
         */
        @NotNull
        String getProjectileType();

        /**
         * Returns the projectile UUID.
         *
         * @return the projectile UUID
         * @since 1.0.0
         */
        @NotNull
        UUID getProjectileId();

        /**
         * Returns the initial velocity/force.
         *
         * @return the force value
         * @since 1.0.0
         */
        float getForce();

        /**
         * Sets additional velocity multiplier.
         *
         * @param multiplier the velocity multiplier
         * @since 1.0.0
         */
        void setVelocityMultiplier(float multiplier);

        /**
         * Cancels the shoot event.
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }

    /**
     * Context for arrow hit events.
     *
     * @since 1.0.0
     */
    non-sealed interface ArrowHit extends EnchantmentContext {

        /**
         * Returns the hit entity UUID, if an entity was hit.
         *
         * @return an Optional containing the hit entity UUID
         * @since 1.0.0
         */
        @NotNull
        Optional<UUID> getHitEntityId();

        /**
         * Returns the hit entity type, if an entity was hit.
         *
         * @return an Optional containing the entity type ID
         * @since 1.0.0
         */
        @NotNull
        Optional<String> getHitEntityType();

        /**
         * Returns the hit block, if a block was hit.
         *
         * @return an Optional containing the hit block
         * @since 1.0.0
         */
        @NotNull
        Optional<UnifiedBlock> getHitBlock();

        /**
         * Returns the impact location.
         *
         * @return the hit location
         * @since 1.0.0
         */
        @NotNull
        UnifiedLocation getHitLocation();

        /**
         * Returns the projectile UUID.
         *
         * @return the projectile UUID
         * @since 1.0.0
         */
        @NotNull
        UUID getProjectileId();

        /**
         * Cancels the hit event (arrow passes through).
         *
         * @since 1.0.0
         */
        void cancel();

        /**
         * Returns whether the event is cancelled.
         *
         * @return true if cancelled
         * @since 1.0.0
         */
        boolean isCancelled();
    }
}
