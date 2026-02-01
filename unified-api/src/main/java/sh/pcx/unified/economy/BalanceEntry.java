/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an entry in the balance leaderboard (baltop).
 *
 * <p>A BalanceEntry contains information about a player's rank, balance,
 * and identity for display on leaderboards.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * List<BalanceEntry> topBalances = economy.getTopBalances(10);
 * for (int i = 0; i < topBalances.size(); i++) {
 *     BalanceEntry entry = topBalances.get(i);
 *     player.sendMessage(Component.text(
 *         String.format("#%d: %s - %s",
 *             entry.rank(),
 *             entry.playerName(),
 *             economy.format(entry.balance())
 *         )
 *     ));
 * }
 * }</pre>
 *
 * @param playerId   the player's UUID
 * @param playerName the player's display name
 * @param balance    the player's balance
 * @param currencyId the currency identifier
 * @param rank       the player's rank (1-based)
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyService#getTopBalances(int)
 */
public record BalanceEntry(
        @NotNull UUID playerId,
        @NotNull String playerName,
        @NotNull BigDecimal balance,
        @NotNull String currencyId,
        int rank
) {

    /**
     * Validates the record parameters.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @param balance    the balance
     * @param currencyId the currency ID
     * @param rank       the rank
     */
    public BalanceEntry {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");
        Objects.requireNonNull(balance, "balance cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");
        if (rank < 1) {
            throw new IllegalArgumentException("rank must be at least 1");
        }
    }

    /**
     * Creates a new balance entry.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @param balance    the balance
     * @param currencyId the currency ID
     * @param rank       the rank (1-based)
     * @return the balance entry
     * @since 1.0.0
     */
    @NotNull
    public static BalanceEntry of(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull BigDecimal balance,
            @NotNull String currencyId,
            int rank
    ) {
        return new BalanceEntry(playerId, playerName, balance, currencyId, rank);
    }

    /**
     * Creates a new balance entry with the default currency.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name
     * @param balance    the balance
     * @param rank       the rank (1-based)
     * @return the balance entry
     * @since 1.0.0
     */
    @NotNull
    public static BalanceEntry of(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull BigDecimal balance,
            int rank
    ) {
        return new BalanceEntry(playerId, playerName, balance, "default", rank);
    }

    /**
     * Returns whether this entry is for a specific player.
     *
     * @param uuid the player UUID to check
     * @return true if this entry belongs to the player
     * @since 1.0.0
     */
    public boolean isPlayer(@NotNull UUID uuid) {
        return playerId.equals(uuid);
    }

    /**
     * Compares this entry to another by balance (descending order).
     *
     * @param other the other entry
     * @return comparison result
     * @since 1.0.0
     */
    public int compareByBalance(@NotNull BalanceEntry other) {
        return other.balance.compareTo(this.balance);
    }

    /**
     * Compares this entry to another by rank (ascending order).
     *
     * @param other the other entry
     * @return comparison result
     * @since 1.0.0
     */
    public int compareByRank(@NotNull BalanceEntry other) {
        return Integer.compare(this.rank, other.rank);
    }

    @Override
    public String toString() {
        return "BalanceEntry{rank=" + rank +
                ", player=" + playerName +
                ", balance=" + balance +
                ", currency=" + currencyId + "}";
    }
}
