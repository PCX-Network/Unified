/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a completed economy transaction.
 *
 * <p>Transactions are immutable records of economy operations including
 * deposits, withdrawals, and transfers. They serve as an audit trail
 * for all balance modifications.
 *
 * <h2>Transaction Types</h2>
 * <ul>
 *   <li>{@link Type#DEPOSIT} - Money added to an account</li>
 *   <li>{@link Type#WITHDRAW} - Money removed from an account</li>
 *   <li>{@link Type#TRANSFER} - Money moved between accounts</li>
 *   <li>{@link Type#SET} - Balance explicitly set to a value</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Get transaction history
 * List<Transaction> history = economy.getTransactionHistory(player.getUniqueId(), 10);
 * for (Transaction tx : history) {
 *     String type = tx.getType().name();
 *     String amount = economy.format(tx.getAmount());
 *     String time = tx.getTimestamp().toString();
 *     player.sendMessage(Component.text(
 *         String.format("[%s] %s: %s", time, type, amount)
 *     ));
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Transaction instances are immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyService
 * @see TransactionResult
 */
public final class Transaction {

    private final UUID transactionId;
    private final UUID accountId;
    private final UUID targetAccountId;
    private final String currencyId;
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final String reason;
    private final String source;
    private final Instant timestamp;

    private Transaction(@NotNull Builder builder) {
        this.transactionId = builder.transactionId;
        this.accountId = builder.accountId;
        this.targetAccountId = builder.targetAccountId;
        this.currencyId = builder.currencyId;
        this.type = builder.type;
        this.amount = builder.amount;
        this.balanceBefore = builder.balanceBefore;
        this.balanceAfter = builder.balanceAfter;
        this.reason = builder.reason;
        this.source = builder.source;
        this.timestamp = builder.timestamp;
    }

    /**
     * Creates a new transaction builder.
     *
     * @return a new builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a deposit transaction.
     *
     * @param accountId     the account receiving the deposit
     * @param currencyId    the currency identifier
     * @param amount        the amount deposited
     * @param balanceBefore the balance before the transaction
     * @param balanceAfter  the balance after the transaction
     * @param reason        the reason for the deposit
     * @return the transaction
     * @since 1.0.0
     */
    @NotNull
    public static Transaction deposit(
            @NotNull UUID accountId,
            @NotNull String currencyId,
            @NotNull BigDecimal amount,
            @NotNull BigDecimal balanceBefore,
            @NotNull BigDecimal balanceAfter,
            @Nullable String reason
    ) {
        return builder()
                .accountId(accountId)
                .currencyId(currencyId)
                .type(Type.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .build();
    }

    /**
     * Creates a withdrawal transaction.
     *
     * @param accountId     the account from which funds are withdrawn
     * @param currencyId    the currency identifier
     * @param amount        the amount withdrawn
     * @param balanceBefore the balance before the transaction
     * @param balanceAfter  the balance after the transaction
     * @param reason        the reason for the withdrawal
     * @return the transaction
     * @since 1.0.0
     */
    @NotNull
    public static Transaction withdraw(
            @NotNull UUID accountId,
            @NotNull String currencyId,
            @NotNull BigDecimal amount,
            @NotNull BigDecimal balanceBefore,
            @NotNull BigDecimal balanceAfter,
            @Nullable String reason
    ) {
        return builder()
                .accountId(accountId)
                .currencyId(currencyId)
                .type(Type.WITHDRAW)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .build();
    }

    /**
     * Creates a transfer transaction.
     *
     * @param fromAccountId the source account
     * @param toAccountId   the destination account
     * @param currencyId    the currency identifier
     * @param amount        the amount transferred
     * @param balanceBefore the source account balance before
     * @param balanceAfter  the source account balance after
     * @param reason        the reason for the transfer
     * @return the transaction
     * @since 1.0.0
     */
    @NotNull
    public static Transaction transfer(
            @NotNull UUID fromAccountId,
            @NotNull UUID toAccountId,
            @NotNull String currencyId,
            @NotNull BigDecimal amount,
            @NotNull BigDecimal balanceBefore,
            @NotNull BigDecimal balanceAfter,
            @Nullable String reason
    ) {
        return builder()
                .accountId(fromAccountId)
                .targetAccountId(toAccountId)
                .currencyId(currencyId)
                .type(Type.TRANSFER)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reason(reason)
                .build();
    }

    /**
     * Returns the unique identifier for this transaction.
     *
     * @return the transaction ID
     * @since 1.0.0
     */
    @NotNull
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * Returns the account ID involved in this transaction.
     *
     * <p>For deposits and withdrawals, this is the affected account.
     * For transfers, this is the source account.
     *
     * @return the account ID
     * @since 1.0.0
     */
    @NotNull
    public UUID getAccountId() {
        return accountId;
    }

    /**
     * Returns the target account ID for transfer transactions.
     *
     * @return an Optional containing the target account ID for transfers
     * @since 1.0.0
     */
    @NotNull
    public Optional<UUID> getTargetAccountId() {
        return Optional.ofNullable(targetAccountId);
    }

    /**
     * Returns the currency identifier for this transaction.
     *
     * @return the currency ID
     * @since 1.0.0
     */
    @NotNull
    public String getCurrencyId() {
        return currencyId;
    }

    /**
     * Returns the type of this transaction.
     *
     * @return the transaction type
     * @since 1.0.0
     */
    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Returns the amount involved in this transaction.
     *
     * <p>This is always a positive value representing the magnitude of the change.
     *
     * @return the transaction amount
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the account balance before this transaction.
     *
     * @return the balance before
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    /**
     * Returns the account balance after this transaction.
     *
     * @return the balance after
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    /**
     * Returns the net change in balance from this transaction.
     *
     * <p>Positive for deposits, negative for withdrawals.
     *
     * @return the balance change
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getBalanceChange() {
        return balanceAfter.subtract(balanceBefore);
    }

    /**
     * Returns the reason for this transaction.
     *
     * @return an Optional containing the reason if provided
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * Returns the source/initiator of this transaction.
     *
     * <p>This could be a plugin name, "console", or a player UUID.
     *
     * @return an Optional containing the source if known
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getSource() {
        return Optional.ofNullable(source);
    }

    /**
     * Returns when this transaction occurred.
     *
     * @return the transaction timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this is a deposit transaction.
     *
     * @return true if this is a deposit
     * @since 1.0.0
     */
    public boolean isDeposit() {
        return type == Type.DEPOSIT;
    }

    /**
     * Checks if this is a withdrawal transaction.
     *
     * @return true if this is a withdrawal
     * @since 1.0.0
     */
    public boolean isWithdrawal() {
        return type == Type.WITHDRAW;
    }

    /**
     * Checks if this is a transfer transaction.
     *
     * @return true if this is a transfer
     * @since 1.0.0
     */
    public boolean isTransfer() {
        return type == Type.TRANSFER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return transactionId.equals(that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + transactionId +
                ", type=" + type +
                ", account=" + accountId +
                ", amount=" + amount +
                ", currency=" + currencyId +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * The type of economy transaction.
     *
     * @since 1.0.0
     */
    public enum Type {
        /**
         * Money deposited into an account.
         */
        DEPOSIT,

        /**
         * Money withdrawn from an account.
         */
        WITHDRAW,

        /**
         * Money transferred between accounts.
         */
        TRANSFER,

        /**
         * Balance explicitly set to a specific value.
         */
        SET
    }

    /**
     * Builder for creating {@link Transaction} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private UUID transactionId;
        private UUID accountId;
        private UUID targetAccountId;
        private String currencyId;
        private Type type;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String reason;
        private String source;
        private Instant timestamp;

        private Builder() {
            this.transactionId = UUID.randomUUID();
            this.timestamp = Instant.now();
        }

        /**
         * Sets the transaction ID.
         *
         * @param transactionId the transaction ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transactionId(@NotNull UUID transactionId) {
            this.transactionId = Objects.requireNonNull(transactionId);
            return this;
        }

        /**
         * Sets the account ID.
         *
         * @param accountId the account ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder accountId(@NotNull UUID accountId) {
            this.accountId = Objects.requireNonNull(accountId);
            return this;
        }

        /**
         * Sets the target account ID for transfers.
         *
         * @param targetAccountId the target account ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder targetAccountId(@Nullable UUID targetAccountId) {
            this.targetAccountId = targetAccountId;
            return this;
        }

        /**
         * Sets the currency ID.
         *
         * @param currencyId the currency ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder currencyId(@NotNull String currencyId) {
            this.currencyId = Objects.requireNonNull(currencyId);
            return this;
        }

        /**
         * Sets the transaction type.
         *
         * @param type the transaction type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder type(@NotNull Type type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        /**
         * Sets the transaction amount.
         *
         * @param amount the amount
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder amount(@NotNull BigDecimal amount) {
            this.amount = Objects.requireNonNull(amount);
            return this;
        }

        /**
         * Sets the balance before the transaction.
         *
         * @param balanceBefore the balance before
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder balanceBefore(@NotNull BigDecimal balanceBefore) {
            this.balanceBefore = Objects.requireNonNull(balanceBefore);
            return this;
        }

        /**
         * Sets the balance after the transaction.
         *
         * @param balanceAfter the balance after
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder balanceAfter(@NotNull BigDecimal balanceAfter) {
            this.balanceAfter = Objects.requireNonNull(balanceAfter);
            return this;
        }

        /**
         * Sets the transaction reason.
         *
         * @param reason the reason
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder reason(@Nullable String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Sets the transaction source.
         *
         * @param source the source
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder source(@Nullable String source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the transaction timestamp.
         *
         * @param timestamp the timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder timestamp(@NotNull Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp);
            return this;
        }

        /**
         * Builds the transaction.
         *
         * @return the new transaction instance
         * @throws IllegalStateException if required fields are missing
         * @since 1.0.0
         */
        @NotNull
        public Transaction build() {
            Objects.requireNonNull(accountId, "accountId is required");
            Objects.requireNonNull(currencyId, "currencyId is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(amount, "amount is required");
            Objects.requireNonNull(balanceBefore, "balanceBefore is required");
            Objects.requireNonNull(balanceAfter, "balanceAfter is required");
            return new Transaction(this);
        }
    }
}
