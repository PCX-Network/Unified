/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.event;

import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.event.UnifiedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all economy-related events.
 *
 * <p>This abstract class provides common functionality for economy events,
 * including the affected player and currency information.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BalanceChangeEvent
 * @see TransferEvent
 * @see AccountCreateEvent
 */
public abstract class EconomyEvent extends UnifiedEvent {

    private final UUID playerId;
    private final String currencyId;

    /**
     * Constructs a new economy event.
     *
     * @param playerId   the UUID of the affected player
     * @param currencyId the currency identifier
     * @since 1.0.0
     */
    protected EconomyEvent(@NotNull UUID playerId, @NotNull String currencyId) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId cannot be null");
    }

    /**
     * Constructs a new economy event with async flag.
     *
     * @param playerId   the UUID of the affected player
     * @param currencyId the currency identifier
     * @param async      whether this event is fired asynchronously
     * @since 1.0.0
     */
    protected EconomyEvent(@NotNull UUID playerId, @NotNull String currencyId, boolean async) {
        super(async);
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.currencyId = Objects.requireNonNull(currencyId, "currencyId cannot be null");
    }

    /**
     * Returns the UUID of the player involved in this event.
     *
     * @return the player's UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the currency identifier for this event.
     *
     * @return the currency ID
     * @since 1.0.0
     */
    @NotNull
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Checks if this event is for the default currency.
     *
     * @return true if this is for the default currency
     * @since 1.0.0
     */
    public boolean isDefaultCurrency() {
        return "default".equals(currencyId) || currencyId.isEmpty();
    }
}
