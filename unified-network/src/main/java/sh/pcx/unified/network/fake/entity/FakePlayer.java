/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.fake.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a client-side fake player (NPC).
 *
 * <p>Fake players are fully rendered player entities with skins, equipment,
 * and animations. They are useful for creating NPCs, holograms with player
 * models, and other interactive elements.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create an NPC
 * FakePlayer npc = fakeEntities.spawnPlayer(player, location)
 *     .name("Guide")
 *     .skin(skinTexture, skinSignature)
 *     .listed(false)  // Don't show in tab
 *     .build();
 *
 * // Play animations
 * npc.playAnimation(EntityAnimation.SWING_MAIN_ARM);
 *
 * // Set equipment
 * npc.setEquipment(EquipmentSlot.HAND, diamondSword);
 * npc.setEquipment(EquipmentSlot.HEAD, helmet);
 *
 * // Look at something
 * npc.lookAt(targetLocation);
 *
 * // Handle clicks
 * npc.onClick(event -> {
 *     event.getPlayer().sendMessage("Hello!");
 * });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see FakeEntity
 * @see FakeEntityService
 */
public interface FakePlayer extends FakeEntity {

    /**
     * Returns the display name of this NPC.
     *
     * @return the NPC name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Sets the display name of this NPC.
     *
     * @param name the new name
     * @since 1.0.0
     */
    void setName(@NotNull String name);

    /**
     * Returns the skin texture value.
     *
     * @return the texture value, or null if default skin
     * @since 1.0.0
     */
    @Nullable
    String getSkinTexture();

    /**
     * Returns the skin texture signature.
     *
     * @return the signature, or null if default skin
     * @since 1.0.0
     */
    @Nullable
    String getSkinSignature();

    /**
     * Sets the skin of this NPC.
     *
     * @param texture   the texture value (Base64 encoded)
     * @param signature the texture signature
     * @since 1.0.0
     */
    void setSkin(@NotNull String texture, @NotNull String signature);

    /**
     * Sets the skin from another player.
     *
     * @param playerName the player name to copy skin from
     * @since 1.0.0
     */
    void setSkinFromPlayer(@NotNull String playerName);

    /**
     * Clears the skin, reverting to default Steve/Alex.
     *
     * @since 1.0.0
     */
    void clearSkin();

    /**
     * Checks if this NPC is listed in the tab list.
     *
     * @return true if listed
     * @since 1.0.0
     */
    boolean isListed();

    /**
     * Sets whether this NPC is listed in the tab list.
     *
     * @param listed true to show in tab list
     * @since 1.0.0
     */
    void setListed(boolean listed);

    /**
     * Sets the equipment in a slot.
     *
     * @param slot the equipment slot
     * @param item the item to equip (platform ItemStack)
     * @since 1.0.0
     */
    void setEquipment(@NotNull EquipmentSlot slot, @Nullable Object item);

    /**
     * Gets the equipment in a slot.
     *
     * @param <T>  the ItemStack type
     * @param slot the equipment slot
     * @return the item, or null if empty
     * @since 1.0.0
     */
    @Nullable
    <T> T getEquipment(@NotNull EquipmentSlot slot);

    /**
     * Clears all equipment.
     *
     * @since 1.0.0
     */
    void clearEquipment();

    /**
     * Makes this NPC look at a location.
     *
     * @param location the location to look at
     * @since 1.0.0
     */
    void lookAt(@NotNull Object location);

    /**
     * Makes this NPC look at an entity.
     *
     * @param entityId the entity ID to look at
     * @since 1.0.0
     */
    void lookAtEntity(int entityId);

    /**
     * Swings the main arm.
     *
     * @since 1.0.0
     */
    default void swingArm() {
        playAnimation(EntityAnimation.SWING_MAIN_ARM);
    }

    /**
     * Swings the off hand.
     *
     * @since 1.0.0
     */
    default void swingOffHand() {
        playAnimation(EntityAnimation.SWING_OFF_HAND);
    }

    /**
     * Plays the hurt animation.
     *
     * @since 1.0.0
     */
    default void hurt() {
        playAnimation(EntityAnimation.HURT);
    }

    /**
     * Sets the game mode appearance.
     *
     * <p>This affects how the NPC appears in the tab list.
     *
     * @param gameMode the game mode (0=survival, 1=creative, 2=adventure, 3=spectator)
     * @since 1.0.0
     */
    void setGameMode(int gameMode);

    /**
     * Sets the latency bar appearance in tab list.
     *
     * @param latency the latency in milliseconds
     * @since 1.0.0
     */
    void setLatency(int latency);

    /**
     * Sets displayed skin parts.
     *
     * @param parts the skin part flags
     * @since 1.0.0
     */
    void setSkinParts(@NotNull SkinParts parts);

    /**
     * Equipment slot enumeration.
     *
     * @since 1.0.0
     */
    enum EquipmentSlot {
        HAND,
        OFF_HAND,
        FEET,
        LEGS,
        CHEST,
        HEAD
    }

    /**
     * Skin parts configuration.
     *
     * @since 1.0.0
     */
    interface SkinParts {
        /**
         * Creates a default skin parts configuration with all parts enabled.
         *
         * @return the default skin parts
         */
        static SkinParts all() {
            return new SkinParts() {
                @Override
                public boolean hasCape() { return true; }
                @Override
                public boolean hasJacket() { return true; }
                @Override
                public boolean hasLeftSleeve() { return true; }
                @Override
                public boolean hasRightSleeve() { return true; }
                @Override
                public boolean hasLeftPants() { return true; }
                @Override
                public boolean hasRightPants() { return true; }
                @Override
                public boolean hasHat() { return true; }
            };
        }

        /**
         * Creates a skin parts configuration with no parts.
         *
         * @return empty skin parts
         */
        static SkinParts none() {
            return new SkinParts() {
                @Override
                public boolean hasCape() { return false; }
                @Override
                public boolean hasJacket() { return false; }
                @Override
                public boolean hasLeftSleeve() { return false; }
                @Override
                public boolean hasRightSleeve() { return false; }
                @Override
                public boolean hasLeftPants() { return false; }
                @Override
                public boolean hasRightPants() { return false; }
                @Override
                public boolean hasHat() { return false; }
            };
        }

        boolean hasCape();
        boolean hasJacket();
        boolean hasLeftSleeve();
        boolean hasRightSleeve();
        boolean hasLeftPants();
        boolean hasRightPants();
        boolean hasHat();

        /**
         * Converts to a byte flag value.
         *
         * @return the flag byte
         */
        default byte toByte() {
            int flags = 0;
            if (hasCape()) flags |= 0x01;
            if (hasJacket()) flags |= 0x02;
            if (hasLeftSleeve()) flags |= 0x04;
            if (hasRightSleeve()) flags |= 0x08;
            if (hasLeftPants()) flags |= 0x10;
            if (hasRightPants()) flags |= 0x20;
            if (hasHat()) flags |= 0x40;
            return (byte) flags;
        }
    }
}
