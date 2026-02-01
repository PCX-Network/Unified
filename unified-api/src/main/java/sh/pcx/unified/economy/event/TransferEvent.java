/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.event;

import sh.pcx.unified.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Event fired when money is transferred between players.
 *
 * <p>This event is fired before the transfer occurs and is cancellable.
 * Cancelling this event prevents the transfer from happening.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onTransfer(TransferEvent event) {
 *     // Tax transfers
 *     BigDecimal tax = event.getAmount().multiply(BigDecimal.valueOf(0.05));
 *     event.setAmount(event.getAmount().subtract(tax));
 *
 *     // Notify sender about tax
 *     UnifiedPlayer sender = UnifiedAPI.getPlayer(event.getFromPlayerId());
 *     sender.ifPresent(p -> p.sendMessage(
 *         Component.text("5% transfer tax applied: " + economy.format(tax))
 *     ));
 *
 *     // Block transfers to banned players
 *     if (isBanned(event.getToPlayerId())) {
 *         event.setCancelled(true);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see BalanceChangeEvent
 * @see EconomyEvent
 */
public class TransferEvent extends EconomyEvent implements Cancellable {

    private final UUID toPlayerId;
    private final BigDecimal fromBalance;
    private final BigDecimal toBalance;
    private BigDecimal amount;
    private final String reason;
    private boolean cancelled;

    /**
     * Constructs a new transfer event.
     *
     * @param fromPlayerId the sender's UUID
     * @param toPlayerId   the recipient's UUID
     * @param currencyId   the currency identifier
     * @param amount       the amount being transferred
     * @param fromBalance  the sender's balance before transfer
     * @param toBalance    the recipient's balance before transfer
     * @param reason       the reason for the transfer
     * @since 1.0.0
     */
    public TransferEvent(
            @NotNull UUID fromPlayerId,
            @NotNull UUID toPlayerId,
            @NotNull String currencyId,
            @NotNull BigDecimal amount,
            @NotNull BigDecimal fromBalance,
            @NotNull BigDecimal toBalance,
            @Nullable String reason
    ) {
        super(fromPlayerId, currencyId);
        this.toPlayerId = Objects.requireNonNull(toPlayerId, "toPlayerId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.fromBalance = Objects.requireNonNull(fromBalance, "fromBalance cannot be null");
        this.toBalance = Objects.requireNonNull(toBalance, "toBalance cannot be null");
        this.reason = reason;
        this.cancelled = false;
    }

    /**
     * Returns the UUID of the player sending the money.
     *
     * <p>This is an alias for {@link #getPlayerId()}.
     *
     * @return the sender's UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getFromPlayerId() {
        return getPlayerId();
    }

    /**
     * Returns the UUID of the player receiving the money.
     *
     * @return the recipient's UUID
     * @since 1.0.0
     */
    @NotNull
    public UUID getToPlayerId() {
        return toPlayerId;
    }

    /**
     * Returns the amount being transferred.
     *
     * @return the transfer amount
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the amount to be transferred.
     *
     * <p>This allows plugins to modify the transfer amount (e.g., for taxes).
     *
     * @param amount the new amount (must be positive)
     * @throws IllegalArgumentException if amount is not positive
     * @since 1.0.0
     */
    public void setAmount(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.amount = amount;
    }

    /**
     * Returns the sender's balance before the transfer.
     *
     * @return the sender's current balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getFromBalance() {
        return fromBalance;
    }

    /**
     * Returns the recipient's balance before the transfer.
     *
     * @return the recipient's current balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getToBalance() {
        return toBalance;
    }

    /**
     * Returns the sender's balance after the transfer.
     *
     * @return the sender's new balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getFromNewBalance() {
        return fromBalance.subtract(amount);
    }

    /**
     * Returns the recipient's balance after the transfer.
     *
     * @return the recipient's new balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getToNewBalance() {
        return toBalance.add(amount);
    }

    /**
     * Returns the reason for this transfer.
     *
     * @return an Optional containing the reason
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Checks if the sender has sufficient funds for the transfer.
     *
     * @return true if the sender can afford the transfer
     * @since 1.0.0
     */
    public boolean hasSufficientFunds() {
        return fromBalance.compareTo(amount) >= 0;
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
        return "TransferEvent{" +
                "from=" + getFromPlayerId() +
                ", to=" + toPlayerId +
                ", amount=" + amount +
                ", currency=" + getCurrencyId() +
                ", cancelled=" + cancelled +
                '}';
    }
}
