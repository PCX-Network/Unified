/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.event.player;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.event.Cancellable;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Event fired when a player sends a chat message.
 *
 * <p>This event allows plugins to modify chat messages, change recipients,
 * format the output, or cancel the message entirely. On modern platforms,
 * this event is typically fired asynchronously.
 *
 * <h2>Platform Mapping</h2>
 * <table>
 *   <caption>Platform-specific event mapping</caption>
 *   <tr><th>Platform</th><th>Native Event</th></tr>
 *   <tr><td>Paper (1.19+)</td><td>{@code AsyncChatEvent}</td></tr>
 *   <tr><td>Spigot</td><td>{@code AsyncPlayerChatEvent}</td></tr>
 *   <tr><td>Sponge</td><td>{@code PlayerChatEvent}</td></tr>
 * </table>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onChat(PlayerChatEvent event) {
 *     UnifiedPlayer player = event.getPlayer();
 *     String message = event.getMessage();
 *
 *     // Filter bad words
 *     if (containsBadWords(message)) {
 *         event.setCancelled(true);
 *         player.sendMessage(Component.text("Watch your language!"));
 *         return;
 *     }
 *
 *     // Modify the message
 *     event.setMessage(message.toUpperCase());
 *
 *     // Custom formatting
 *     Component formatted = Component.text()
 *         .append(Component.text("[" + getPrefix(player) + "] "))
 *         .append(Component.text(player.getName() + ": "))
 *         .append(Component.text(event.getMessage()))
 *         .build();
 *     event.setFormattedMessage(formatted);
 *
 *     // Limit recipients
 *     event.getRecipients().removeIf(p ->
 *         !p.hasPermission("chat.see.all"));
 * }
 * }</pre>
 *
 * <h2>Async Considerations</h2>
 * <p>This event is typically fired asynchronously. Use caution when:
 * <ul>
 *   <li>Modifying game state (use scheduler to sync)</li>
 *   <li>Accessing mutable collections</li>
 *   <li>Interacting with non-thread-safe APIs</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see PlayerEvent
 * @see Cancellable
 */
public class PlayerChatEvent extends PlayerEvent implements Cancellable {

    private String message;
    private Component formattedMessage;
    private final Set<UnifiedPlayer> recipients;
    private boolean cancelled;

    /**
     * Constructs a new player chat event.
     *
     * @param player     the player who sent the message
     * @param message    the raw message content
     * @param recipients the initial set of message recipients
     * @since 1.0.0
     */
    public PlayerChatEvent(
            @NotNull UnifiedPlayer player,
            @NotNull String message,
            @NotNull Set<UnifiedPlayer> recipients
    ) {
        super(player, true); // Chat events are typically async
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.recipients = new HashSet<>(recipients);
        this.formattedMessage = null;
        this.cancelled = false;
    }

    /**
     * Constructs a new player chat event with default recipients (all online players).
     *
     * @param player  the player who sent the message
     * @param message the raw message content
     * @since 1.0.0
     */
    public PlayerChatEvent(@NotNull UnifiedPlayer player, @NotNull String message) {
        this(player, message, Collections.emptySet());
    }

    /**
     * Returns the raw message content.
     *
     * @return the message text
     * @since 1.0.0
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * Sets the raw message content.
     *
     * <p>This changes the message text but not the formatting. If a formatted
     * message has been set, you may need to update it as well.
     *
     * @param message the new message text
     * @throws NullPointerException if message is null
     * @since 1.0.0
     */
    public void setMessage(@NotNull String message) {
        this.message = Objects.requireNonNull(message, "message cannot be null");
    }

    /**
     * Returns the formatted message that will be sent.
     *
     * <p>If no formatted message has been explicitly set, this returns null
     * and the platform's default formatting will be used.
     *
     * @return the formatted message, or null for default formatting
     * @since 1.0.0
     */
    @Nullable
    public Component getFormattedMessage() {
        return formattedMessage;
    }

    /**
     * Sets the formatted message that will be sent.
     *
     * <p>This overrides the platform's default chat formatting. Set to null
     * to use the platform's default formatting.
     *
     * @param formattedMessage the formatted message, or null for default
     * @since 1.0.0
     */
    public void setFormattedMessage(@Nullable Component formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    /**
     * Checks if a custom formatted message has been set.
     *
     * @return true if a formatted message is set
     * @since 1.0.0
     */
    public boolean hasFormattedMessage() {
        return formattedMessage != null;
    }

    /**
     * Returns the set of players who will receive this message.
     *
     * <p>The returned set is mutable. Add or remove players to control
     * who receives the message.
     *
     * @return the mutable set of recipients
     * @since 1.0.0
     */
    @NotNull
    public Set<UnifiedPlayer> getRecipients() {
        return recipients;
    }

    /**
     * Adds a recipient to receive this message.
     *
     * @param player the player to add
     * @since 1.0.0
     */
    public void addRecipient(@NotNull UnifiedPlayer player) {
        recipients.add(player);
    }

    /**
     * Removes a recipient so they won't receive this message.
     *
     * @param player the player to remove
     * @return true if the player was a recipient
     * @since 1.0.0
     */
    public boolean removeRecipient(@NotNull UnifiedPlayer player) {
        return recipients.remove(player);
    }

    /**
     * Removes a recipient by UUID.
     *
     * @param playerId the UUID of the player to remove
     * @return true if a player was removed
     * @since 1.0.0
     */
    public boolean removeRecipient(@NotNull UUID playerId) {
        return recipients.removeIf(p -> p.getUniqueId().equals(playerId));
    }

    /**
     * Clears all recipients.
     *
     * <p>No players will receive the message after calling this method.
     *
     * @since 1.0.0
     */
    public void clearRecipients() {
        recipients.clear();
    }

    /**
     * Checks if a player is in the recipient list.
     *
     * @param player the player to check
     * @return true if the player will receive the message
     * @since 1.0.0
     */
    public boolean isRecipient(@NotNull UnifiedPlayer player) {
        return recipients.contains(player);
    }

    /**
     * Returns the number of recipients.
     *
     * @return the recipient count
     * @since 1.0.0
     */
    public int getRecipientCount() {
        return recipients.size();
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
        return "PlayerChatEvent[player=" + getPlayer().getName()
                + ", message=\"" + (message.length() > 20 ? message.substring(0, 20) + "..." : message) + "\""
                + ", recipients=" + recipients.size()
                + ", cancelled=" + cancelled
                + "]";
    }
}
