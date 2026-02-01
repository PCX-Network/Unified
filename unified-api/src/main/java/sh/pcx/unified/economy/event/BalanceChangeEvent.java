/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.event;

import sh.pcx.unified.economy.Transaction;
import sh.pcx.unified.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Event fired when a player's balance changes.
 *
 * <p>This event is fired for deposits, withdrawals, and balance sets.
 * It is cancellable, allowing plugins to prevent balance modifications.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @EventHandler
 * public void onBalanceChange(BalanceChangeEvent event) {
 *     // Log all balance changes
 *     logger.info("{} balance changed: {} -> {} ({})",
 *         event.getPlayerId(),
 *         event.getOldBalance(),
 *         event.getNewBalance(),
 *         event.getType()
 *     );
 *
 *     // Prevent negative balance changes for VIPs
 *     if (event.getType() == Transaction.Type.WITHDRAW) {
 *         if (isVIP(event.getPlayerId()) && event.getNewBalance().signum() < 0) {
 *             event.setCancelled(true);
 *         }
 *     }
 *
 *     // Modify the amount
 *     if (isDoubleXPActive()) {
 *         event.setAmount(event.getAmount().multiply(BigDecimal.valueOf(2)));
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This event may be fired asynchronously depending on the economy implementation.
 * Check {@link #isAsync()} before modifying game state.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyEvent
 * @see TransferEvent
 */
public class BalanceChangeEvent extends EconomyEvent implements Cancellable {

    private final Transaction.Type type;
    private final BigDecimal oldBalance;
    private BigDecimal amount;
    private final String reason;
    private boolean cancelled;

    /**
     * Constructs a new balance change event.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @param type       the type of transaction
     * @param oldBalance the balance before the change
     * @param amount     the amount being added/removed
     * @param reason     the reason for the change
     * @since 1.0.0
     */
    public BalanceChangeEvent(
            @NotNull UUID playerId,
            @NotNull String currencyId,
            @NotNull Transaction.Type type,
            @NotNull BigDecimal oldBalance,
            @NotNull BigDecimal amount,
            @Nullable String reason
    ) {
        super(playerId, currencyId);
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.oldBalance = Objects.requireNonNull(oldBalance, "oldBalance cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.reason = reason;
        this.cancelled = false;
    }

    /**
     * Returns the type of balance change.
     *
     * @return the transaction type
     * @since 1.0.0
     */
    @NotNull
    public Transaction.Type getType() {
        return type;
    }

    /**
     * Returns the balance before the change.
     *
     * @return the old balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getOldBalance() {
        return oldBalance;
    }

    /**
     * Returns the amount being changed.
     *
     * <p>For deposits, this is added to the balance.
     * For withdrawals, this is subtracted from the balance.
     *
     * @return the amount
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the amount to be changed.
     *
     * <p>Modifying this value allows plugins to adjust the transaction amount.
     *
     * @param amount the new amount (must be positive)
     * @throws IllegalArgumentException if amount is negative
     * @since 1.0.0
     */
    public void setAmount(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
        this.amount = amount;
    }

    /**
     * Returns the new balance after the change.
     *
     * <p>This is calculated based on the current amount, which may
     * have been modified by event handlers.
     *
     * @return the new balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getNewBalance() {
        return switch (type) {
            case DEPOSIT -> oldBalance.add(amount);
            case WITHDRAW -> oldBalance.subtract(amount);
            case SET -> amount;
            case TRANSFER -> oldBalance.subtract(amount);
        };
    }

    /**
     * Returns the net change in balance.
     *
     * @return the change (positive for deposits, negative for withdrawals)
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getChange() {
        return getNewBalance().subtract(oldBalance);
    }

    /**
     * Returns the reason for this balance change.
     *
     * @return an Optional containing the reason
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Checks if this is a deposit.
     *
     * @return true if this is a deposit
     * @since 1.0.0
     */
    public boolean isDeposit() {
        return type == Transaction.Type.DEPOSIT;
    }

    /**
     * Checks if this is a withdrawal.
     *
     * @return true if this is a withdrawal
     * @since 1.0.0
     */
    public boolean isWithdrawal() {
        return type == Transaction.Type.WITHDRAW;
    }

    /**
     * Checks if this is a balance set.
     *
     * @return true if this is a balance set
     * @since 1.0.0
     */
    public boolean isSet() {
        return type == Transaction.Type.SET;
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
        return "BalanceChangeEvent{" +
                "player=" + getPlayerId() +
                ", type=" + type +
                ", oldBalance=" + oldBalance +
                ", amount=" + amount +
                ", newBalance=" + getNewBalance() +
                ", currency=" + getCurrencyId() +
                ", cancelled=" + cancelled +
                '}';
    }
}
