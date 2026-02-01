/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.tablist;

import net.kyori.adventure.text.Component;
import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Service for manipulating the player tab list.
 *
 * <p>The TabListService allows customization of the tab list including
 * header/footer, custom entries, and player information updates.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private TabListService tabList;
 *
 * // Set header/footer
 * tabList.setHeaderFooter(player,
 *     Component.text("My Server", NamedTextColor.GOLD),
 *     Component.text("play.myserver.com", NamedTextColor.GRAY)
 * );
 *
 * // Add fake player to tab
 * tabList.addEntry(player, TabEntry.builder()
 *     .name("FakePlayer")
 *     .displayName(Component.text("[NPC] Guide", NamedTextColor.YELLOW))
 *     .gameMode(GameMode.SURVIVAL)
 *     .latency(50)
 *     .skin(skinTexture, skinSignature)
 *     .build()
 * );
 *
 * // Update entry
 * tabList.updateEntry(player, "Steve", entry -> {
 *     entry.setDisplayName(Component.text("[VIP] Steve", NamedTextColor.GOLD));
 *     entry.setLatency(100);
 * });
 *
 * // Remove entry
 * tabList.removeEntry(player, "FakePlayer");
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This service is thread-safe. Operations are queued and executed on
 * the appropriate thread.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TabEntry
 */
public interface TabListService extends Service {

    // =========================================================================
    // Header and Footer
    // =========================================================================

    /**
     * Sets the tab list header and footer for a player.
     *
     * @param player the player
     * @param header the header component
     * @param footer the footer component
     * @since 1.0.0
     */
    void setHeaderFooter(@NotNull Object player, @Nullable Component header, @Nullable Component footer);

    /**
     * Sets the tab list header for a player.
     *
     * @param player the player
     * @param header the header component
     * @since 1.0.0
     */
    void setHeader(@NotNull Object player, @Nullable Component header);

    /**
     * Sets the tab list footer for a player.
     *
     * @param player the player
     * @param footer the footer component
     * @since 1.0.0
     */
    void setFooter(@NotNull Object player, @Nullable Component footer);

    /**
     * Clears the header and footer for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clearHeaderFooter(@NotNull Object player);

    /**
     * Sets the header and footer for all players.
     *
     * @param header the header component
     * @param footer the footer component
     * @since 1.0.0
     */
    void setGlobalHeaderFooter(@Nullable Component header, @Nullable Component footer);

    // =========================================================================
    // Tab Entries
    // =========================================================================

    /**
     * Adds a tab entry for a player.
     *
     * @param player the player
     * @param entry  the tab entry
     * @since 1.0.0
     */
    void addEntry(@NotNull Object player, @NotNull TabEntry entry);

    /**
     * Adds multiple tab entries for a player.
     *
     * @param player  the player
     * @param entries the tab entries
     * @since 1.0.0
     */
    void addEntries(@NotNull Object player, @NotNull Collection<TabEntry> entries);

    /**
     * Removes a tab entry from a player's tab list.
     *
     * @param player the player
     * @param name   the entry name to remove
     * @since 1.0.0
     */
    void removeEntry(@NotNull Object player, @NotNull String name);

    /**
     * Removes a tab entry by UUID.
     *
     * @param player  the player
     * @param entryId the entry UUID
     * @since 1.0.0
     */
    void removeEntry(@NotNull Object player, @NotNull UUID entryId);

    /**
     * Updates an existing tab entry.
     *
     * @param player  the player
     * @param name    the entry name
     * @param updater the update function
     * @since 1.0.0
     */
    void updateEntry(@NotNull Object player, @NotNull String name, @NotNull Consumer<TabEntry> updater);

    /**
     * Updates an existing tab entry by UUID.
     *
     * @param player  the player
     * @param entryId the entry UUID
     * @param updater the update function
     * @since 1.0.0
     */
    void updateEntry(@NotNull Object player, @NotNull UUID entryId, @NotNull Consumer<TabEntry> updater);

    /**
     * Updates the display name of a tab entry.
     *
     * @param player      the player
     * @param name        the entry name
     * @param displayName the new display name
     * @since 1.0.0
     */
    void updateDisplayName(@NotNull Object player, @NotNull String name, @NotNull Component displayName);

    /**
     * Updates the latency of a tab entry.
     *
     * @param player  the player
     * @param name    the entry name
     * @param latency the latency in milliseconds
     * @since 1.0.0
     */
    void updateLatency(@NotNull Object player, @NotNull String name, int latency);

    /**
     * Updates the game mode of a tab entry.
     *
     * @param player   the player
     * @param name     the entry name
     * @param gameMode the game mode (0=survival, 1=creative, 2=adventure, 3=spectator)
     * @since 1.0.0
     */
    void updateGameMode(@NotNull Object player, @NotNull String name, int gameMode);

    /**
     * Checks if a player has a custom tab entry.
     *
     * @param player the player
     * @param name   the entry name
     * @return true if the entry exists
     * @since 1.0.0
     */
    boolean hasEntry(@NotNull Object player, @NotNull String name);

    /**
     * Gets all custom tab entries for a player.
     *
     * @param playerId the player's UUID
     * @return the custom entries
     * @since 1.0.0
     */
    @NotNull
    Collection<TabEntry> getCustomEntries(@NotNull UUID playerId);

    /**
     * Clears all custom tab entries for a player.
     *
     * @param player the player
     * @since 1.0.0
     */
    void clearCustomEntries(@NotNull Object player);

    // =========================================================================
    // Player Visibility
    // =========================================================================

    /**
     * Hides a player from another player's tab list.
     *
     * @param viewer the viewing player
     * @param target the target player to hide
     * @since 1.0.0
     */
    void hidePlayer(@NotNull Object viewer, @NotNull Object target);

    /**
     * Shows a previously hidden player in the tab list.
     *
     * @param viewer the viewing player
     * @param target the target player to show
     * @since 1.0.0
     */
    void showPlayer(@NotNull Object viewer, @NotNull Object target);

    /**
     * Checks if a player is hidden from another's tab list.
     *
     * @param viewerId the viewing player's UUID
     * @param targetId the target player's UUID
     * @return true if hidden
     * @since 1.0.0
     */
    boolean isHidden(@NotNull UUID viewerId, @NotNull UUID targetId);

    // =========================================================================
    // Sorting and Ordering
    // =========================================================================

    /**
     * Sets the sort priority for a player.
     *
     * <p>Lower priority values appear first in the tab list.
     *
     * @param player   the player
     * @param target   the target player or entry name
     * @param priority the sort priority
     * @since 1.0.0
     */
    void setSortPriority(@NotNull Object player, @NotNull String target, int priority);

    /**
     * Sets a prefix for tab list sorting.
     *
     * <p>This affects the internal sort order without changing display.
     *
     * @param player the player
     * @param target the target player or entry name
     * @param prefix the sort prefix
     * @since 1.0.0
     */
    void setSortPrefix(@NotNull Object player, @NotNull String target, @NotNull String prefix);

    // =========================================================================
    // Tab Entry Builder
    // =========================================================================

    /**
     * Creates a new tab entry builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static TabEntry.Builder entryBuilder() {
        return TabEntry.builder();
    }
}
