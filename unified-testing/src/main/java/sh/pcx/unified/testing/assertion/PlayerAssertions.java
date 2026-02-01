/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.assertion;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.testing.player.MockPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Fluent assertions for MockPlayer testing.
 *
 * <p>Provides a convenient API for asserting player state in tests.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockPlayer player = server.addPlayer("Steve");
 *
 * PlayerAssertions.assertThat(player)
 *     .hasName("Steve")
 *     .hasHealth(20.0)
 *     .hasGameMode(GameMode.SURVIVAL)
 *     .hasPermission("myplugin.use")
 *     .hasReceivedMessage("Welcome");
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class PlayerAssertions {

    private final MockPlayer player;

    private PlayerAssertions(@NotNull MockPlayer player) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
    }

    /**
     * Creates assertions for a player.
     *
     * @param player the player to assert on
     * @return the assertions
     */
    @NotNull
    public static PlayerAssertions assertThat(@NotNull MockPlayer player) {
        return new PlayerAssertions(player);
    }

    /**
     * Asserts that the player has the specified name.
     *
     * @param name the expected name
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasName(@NotNull String name) {
        if (!player.getPlayerName().equals(name)) {
            throw new AssertionError(
                "Expected player name to be '" + name + "' but was '" + player.getPlayerName() + "'"
            );
        }
        return this;
    }

    /**
     * Asserts that the player has the specified health.
     *
     * @param health the expected health
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasHealth(double health) {
        if (Math.abs(player.getHealth() - health) > 0.001) {
            throw new AssertionError(
                "Expected health to be " + health + " but was " + player.getHealth()
            );
        }
        return this;
    }

    /**
     * Asserts that the player's health is within a range.
     *
     * @param min the minimum health
     * @param max the maximum health
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasHealthBetween(double min, double max) {
        double health = player.getHealth();
        if (health < min || health > max) {
            throw new AssertionError(
                "Expected health between " + min + " and " + max + " but was " + health
            );
        }
        return this;
    }

    /**
     * Asserts that the player is alive.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isAlive() {
        if (player.getHealth() <= 0) {
            throw new AssertionError("Expected player to be alive but health was " + player.getHealth());
        }
        return this;
    }

    /**
     * Asserts that the player is dead.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isDead() {
        if (player.getHealth() > 0) {
            throw new AssertionError("Expected player to be dead but health was " + player.getHealth());
        }
        return this;
    }

    /**
     * Asserts that the player has the specified food level.
     *
     * @param foodLevel the expected food level
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasFoodLevel(int foodLevel) {
        if (player.getFoodLevel() != foodLevel) {
            throw new AssertionError(
                "Expected food level to be " + foodLevel + " but was " + player.getFoodLevel()
            );
        }
        return this;
    }

    /**
     * Asserts that the player has the specified game mode.
     *
     * @param gameMode the expected game mode
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasGameMode(@NotNull UnifiedPlayer.GameMode gameMode) {
        if (player.getGameMode() != gameMode) {
            throw new AssertionError(
                "Expected game mode to be " + gameMode + " but was " + player.getGameMode()
            );
        }
        return this;
    }

    /**
     * Asserts that the player is an operator.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isOp() {
        if (!player.isOp()) {
            throw new AssertionError("Expected player to be an operator");
        }
        return this;
    }

    /**
     * Asserts that the player is not an operator.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isNotOp() {
        if (player.isOp()) {
            throw new AssertionError("Expected player to not be an operator");
        }
        return this;
    }

    /**
     * Asserts that the player has a permission.
     *
     * @param permission the permission to check
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasPermission(@NotNull String permission) {
        if (!player.hasPermission(permission)) {
            throw new AssertionError("Expected player to have permission '" + permission + "'");
        }
        return this;
    }

    /**
     * Asserts that the player does not have a permission.
     *
     * @param permission the permission to check
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions doesNotHavePermission(@NotNull String permission) {
        if (player.hasPermission(permission)) {
            throw new AssertionError("Expected player to not have permission '" + permission + "'");
        }
        return this;
    }

    /**
     * Asserts that the player is online.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isOnline() {
        if (!player.isOnline()) {
            throw new AssertionError("Expected player to be online");
        }
        return this;
    }

    /**
     * Asserts that the player is offline.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isOffline() {
        if (player.isOnline()) {
            throw new AssertionError("Expected player to be offline");
        }
        return this;
    }

    /**
     * Asserts that the player is flying.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isFlying() {
        if (!player.isFlying()) {
            throw new AssertionError("Expected player to be flying");
        }
        return this;
    }

    /**
     * Asserts that the player is not flying.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isNotFlying() {
        if (player.isFlying()) {
            throw new AssertionError("Expected player to not be flying");
        }
        return this;
    }

    /**
     * Asserts that the player received a message containing the text.
     *
     * @param text the text to search for
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasReceivedMessage(@NotNull String text) {
        boolean found = player.getRawMessages().stream()
            .anyMatch(msg -> msg.contains(text));

        if (!found) {
            throw new AssertionError(
                "Expected player to receive message containing '" + text + "' but messages were: " +
                player.getRawMessages()
            );
        }
        return this;
    }

    /**
     * Asserts that the player has not received any message containing the text.
     *
     * @param text the text to search for
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasNotReceivedMessage(@NotNull String text) {
        boolean found = player.getRawMessages().stream()
            .anyMatch(msg -> msg.contains(text));

        if (found) {
            throw new AssertionError(
                "Expected player to not receive message containing '" + text + "'"
            );
        }
        return this;
    }

    /**
     * Asserts that the player has received exactly the specified number of messages.
     *
     * @param count the expected message count
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasReceivedMessageCount(int count) {
        int actual = player.getMessages().size();
        if (actual != count) {
            throw new AssertionError(
                "Expected player to receive " + count + " messages but received " + actual
            );
        }
        return this;
    }

    /**
     * Asserts that the player has received at least one message.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasReceivedMessages() {
        if (player.getMessages().isEmpty()) {
            throw new AssertionError("Expected player to have received messages");
        }
        return this;
    }

    /**
     * Asserts that the player has not received any messages.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasNotReceivedMessages() {
        if (!player.getMessages().isEmpty()) {
            throw new AssertionError(
                "Expected player to have no messages but received: " + player.getRawMessages()
            );
        }
        return this;
    }

    /**
     * Asserts that the player is in the specified world.
     *
     * @param worldName the world name
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions isInWorld(@NotNull String worldName) {
        if (!player.getWorld().getName().equals(worldName)) {
            throw new AssertionError(
                "Expected player to be in world '" + worldName + "' but was in '" +
                player.getWorld().getName() + "'"
            );
        }
        return this;
    }

    /**
     * Asserts that the player's inventory contains an item.
     *
     * @param itemType the item type
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions hasItem(@NotNull String itemType) {
        if (!player.getInventory().contains(itemType)) {
            throw new AssertionError("Expected inventory to contain '" + itemType + "'");
        }
        return this;
    }

    /**
     * Asserts that the player's inventory does not contain an item.
     *
     * @param itemType the item type
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions doesNotHaveItem(@NotNull String itemType) {
        if (player.getInventory().contains(itemType)) {
            throw new AssertionError("Expected inventory to not contain '" + itemType + "'");
        }
        return this;
    }

    /**
     * Asserts using a custom predicate.
     *
     * @param predicate   the predicate to test
     * @param description the description for the error message
     * @return this assertion for chaining
     */
    @NotNull
    public PlayerAssertions satisfies(
        @NotNull Predicate<MockPlayer> predicate,
        @NotNull String description
    ) {
        if (!predicate.test(player)) {
            throw new AssertionError("Expected player to " + description);
        }
        return this;
    }
}
