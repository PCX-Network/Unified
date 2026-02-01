/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.event;

import sh.pcx.unified.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Event fired when a new economy account is created.
 *
 * <p>This event is fired before the account is created and is cancellable.
 * Cancelling this event prevents the account from being created.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onAccountCreate(AccountCreateEvent event) {
 *     // Modify starting balance for VIPs
 *     if (isVIP(event.getPlayerId())) {
 *         event.setStartingBalance(BigDecimal.valueOf(10000));
 *     }
 *
 *     // Block account creation for banned players
 *     if (isBanned(event.getPlayerId())) {
 *         event.setCancelled(true);
 *     }
 *
 *     // Log new accounts
 *     logger.info("New economy account: {} ({})",
 *         event.getPlayerName(), event.getPlayerId());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyEvent
 */
public class AccountCreateEvent extends EconomyEvent implements Cancellable {

    private final String playerName;
    private BigDecimal startingBalance;
    private boolean cancelled;

    /**
     * Constructs a new account create event.
     *
     * @param playerId        the player's UUID
     * @param playerName      the player's name
     * @param currencyId      the currency identifier
     * @param startingBalance the starting balance
     * @since 1.0.0
     */
    public AccountCreateEvent(
            @NotNull UUID playerId,
            @NotNull String playerName,
            @NotNull String currencyId,
            @NotNull BigDecimal startingBalance
    ) {
        super(playerId, currencyId);
        this.playerName = Objects.requireNonNull(playerName, "playerName cannot be null");
        this.startingBalance = Objects.requireNonNull(startingBalance, "startingBalance cannot be null");
        this.cancelled = false;
    }

    /**
     * Returns the name of the player the account is being created for.
     *
     * @return the player's name
     * @since 1.0.0
     */
    @NotNull
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the starting balance for the new account.
     *
     * @return the starting balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    /**
     * Sets the starting balance for the new account.
     *
     * @param startingBalance the starting balance
     * @throws IllegalArgumentException if startingBalance is negative
     * @since 1.0.0
     */
    public void setStartingBalance(@NotNull BigDecimal startingBalance) {
        Objects.requireNonNull(startingBalance, "startingBalance cannot be null");
        if (startingBalance.signum() < 0) {
            throw new IllegalArgumentException("startingBalance cannot be negative");
        }
        this.startingBalance = startingBalance;
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
        return "AccountCreateEvent{" +
                "player=" + getPlayerId() +
                ", name=" + playerName +
                ", currency=" + getCurrencyId() +
                ", startingBalance=" + startingBalance +
                ", cancelled=" + cancelled +
                '}';
    }
}
