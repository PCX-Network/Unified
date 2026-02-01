/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the result of an economy transaction.
 *
 * <p>A TransactionResult contains information about whether the transaction
 * succeeded or failed, the new balance, and any error information. It also
 * includes a reference to the created {@link Transaction} record for auditing.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * TransactionResult result = economy.withdraw(player.getUniqueId(), BigDecimal.valueOf(100));
 *
 * if (result.isSuccess()) {
 *     player.sendMessage(Component.text("Success! New balance: " +
 *         economy.format(result.newBalance())));
 * } else {
 *     player.sendMessage(Component.text("Failed: " +
 *         result.errorMessage().orElse("Unknown error")));
 *
 *     // Check specific failure reasons
 *     if (result.status() == TransactionResult.Status.INSUFFICIENT_FUNDS) {
 *         player.sendMessage(Component.text("You need " +
 *             economy.format(result.requiredAmount().orElse(BigDecimal.ZERO))));
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>TransactionResult instances are immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyService
 * @see Transaction
 */
public final class TransactionResult {

    private final Status status;
    private final UUID transactionId;
    private final BigDecimal previousBalance;
    private final BigDecimal newBalance;
    private final BigDecimal requestedAmount;
    private final String currencyId;
    private final String errorMessage;
    private final Transaction transaction;

    private TransactionResult(@NotNull Builder builder) {
        this.status = builder.status;
        this.transactionId = builder.transactionId;
        this.previousBalance = builder.previousBalance;
        this.newBalance = builder.newBalance;
        this.requestedAmount = builder.requestedAmount;
        this.currencyId = builder.currencyId;
        this.errorMessage = builder.errorMessage;
        this.transaction = builder.transaction;
    }

    /**
     * Creates a successful transaction result.
     *
     * @param previousBalance the balance before the transaction
     * @param newBalance      the balance after the transaction
     * @param currencyId      the currency identifier
     * @param transaction     the transaction record
     * @return the successful result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult success(
            @NotNull BigDecimal previousBalance,
            @NotNull BigDecimal newBalance,
            @NotNull String currencyId,
            @NotNull Transaction transaction
    ) {
        return builder()
                .status(Status.SUCCESS)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .currencyId(currencyId)
                .transactionId(transaction.getTransactionId())
                .transaction(transaction)
                .build();
    }

    /**
     * Creates a failed transaction result due to insufficient funds.
     *
     * @param currentBalance  the current balance
     * @param requestedAmount the amount that was requested
     * @param currencyId      the currency identifier
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult insufficientFunds(
            @NotNull BigDecimal currentBalance,
            @NotNull BigDecimal requestedAmount,
            @NotNull String currencyId
    ) {
        return builder()
                .status(Status.INSUFFICIENT_FUNDS)
                .previousBalance(currentBalance)
                .newBalance(currentBalance)
                .requestedAmount(requestedAmount)
                .currencyId(currencyId)
                .errorMessage("Insufficient funds: has " + currentBalance + ", needs " + requestedAmount)
                .build();
    }

    /**
     * Creates a failed transaction result due to balance limit exceeded.
     *
     * @param currentBalance  the current balance
     * @param requestedAmount the amount that was requested
     * @param maxBalance      the maximum allowed balance
     * @param currencyId      the currency identifier
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult maxBalanceExceeded(
            @NotNull BigDecimal currentBalance,
            @NotNull BigDecimal requestedAmount,
            @NotNull BigDecimal maxBalance,
            @NotNull String currencyId
    ) {
        return builder()
                .status(Status.MAX_BALANCE_EXCEEDED)
                .previousBalance(currentBalance)
                .newBalance(currentBalance)
                .requestedAmount(requestedAmount)
                .currencyId(currencyId)
                .errorMessage("Maximum balance exceeded: would be " +
                        currentBalance.add(requestedAmount) + ", max is " + maxBalance)
                .build();
    }

    /**
     * Creates a failed transaction result due to account not found.
     *
     * @param accountId the account ID that was not found
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult accountNotFound(@NotNull UUID accountId) {
        return builder()
                .status(Status.ACCOUNT_NOT_FOUND)
                .errorMessage("Account not found: " + accountId)
                .build();
    }

    /**
     * Creates a failed transaction result due to invalid amount.
     *
     * @param amount the invalid amount
     * @param reason the reason it is invalid
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult invalidAmount(@NotNull BigDecimal amount, @NotNull String reason) {
        return builder()
                .status(Status.INVALID_AMOUNT)
                .requestedAmount(amount)
                .errorMessage("Invalid amount: " + reason)
                .build();
    }

    /**
     * Creates a failed transaction result due to an error.
     *
     * @param errorMessage the error message
     * @return the failure result
     * @since 1.0.0
     */
    @NotNull
    public static TransactionResult error(@NotNull String errorMessage) {
        return builder()
                .status(Status.ERROR)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether the transaction was successful.
     *
     * @return true if the transaction succeeded
     * @since 1.0.0
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * Returns whether the transaction failed.
     *
     * @return true if the transaction failed
     * @since 1.0.0
     */
    public boolean isFailure() {
        return status != Status.SUCCESS;
    }

    /**
     * Returns the transaction status.
     *
     * @return the status
     * @since 1.0.0
     */
    @NotNull
    public Status status() {
        return status;
    }

    /**
     * Returns the transaction ID if the transaction was successful.
     *
     * @return an Optional containing the transaction ID
     * @since 1.0.0
     */
    @NotNull
    public Optional<UUID> transactionId() {
        return Optional.ofNullable(transactionId);
    }

    /**
     * Returns the balance before the transaction.
     *
     * @return the previous balance, or zero if not applicable
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal previousBalance() {
        return previousBalance != null ? previousBalance : BigDecimal.ZERO;
    }

    /**
     * Returns the balance after the transaction.
     *
     * <p>For failed transactions, this equals the previous balance.
     *
     * @return the new balance, or zero if not applicable
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal newBalance() {
        return newBalance != null ? newBalance : BigDecimal.ZERO;
    }

    /**
     * Returns the amount that was requested in the transaction.
     *
     * @return an Optional containing the requested amount
     * @since 1.0.0
     */
    @NotNull
    public Optional<BigDecimal> requestedAmount() {
        return Optional.ofNullable(requestedAmount);
    }

    /**
     * Returns the currency identifier.
     *
     * @return an Optional containing the currency ID
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> currencyId() {
        return Optional.ofNullable(currencyId);
    }

    /**
     * Returns the error message if the transaction failed.
     *
     * @return an Optional containing the error message
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Returns the transaction record if the transaction was successful.
     *
     * @return an Optional containing the transaction
     * @since 1.0.0
     */
    @NotNull
    public Optional<Transaction> transaction() {
        return Optional.ofNullable(transaction);
    }

    /**
     * Returns the balance change from this transaction.
     *
     * @return the change in balance (positive for deposits, negative for withdrawals)
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal balanceChange() {
        return newBalance().subtract(previousBalance());
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "TransactionResult{SUCCESS, balance=" + newBalance + ", txId=" + transactionId + "}";
        } else {
            return "TransactionResult{" + status + ", error=" + errorMessage + "}";
        }
    }

    /**
     * The status of a transaction result.
     *
     * @since 1.0.0
     */
    public enum Status {
        /**
         * The transaction completed successfully.
         */
        SUCCESS,

        /**
         * The account does not have sufficient funds.
         */
        INSUFFICIENT_FUNDS,

        /**
         * The transaction would exceed the maximum balance.
         */
        MAX_BALANCE_EXCEEDED,

        /**
         * The transaction would go below the minimum balance.
         */
        MIN_BALANCE_EXCEEDED,

        /**
         * The specified account was not found.
         */
        ACCOUNT_NOT_FOUND,

        /**
         * The specified currency was not found.
         */
        CURRENCY_NOT_FOUND,

        /**
         * The amount specified is invalid (e.g., negative).
         */
        INVALID_AMOUNT,

        /**
         * The transaction was cancelled by an event handler.
         */
        CANCELLED,

        /**
         * A general error occurred during the transaction.
         */
        ERROR
    }

    /**
     * Builder for creating {@link TransactionResult} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private Status status;
        private UUID transactionId;
        private BigDecimal previousBalance;
        private BigDecimal newBalance;
        private BigDecimal requestedAmount;
        private String currencyId;
        private String errorMessage;
        private Transaction transaction;

        private Builder() {}

        /**
         * Sets the status.
         *
         * @param status the status
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder status(@NotNull Status status) {
            this.status = Objects.requireNonNull(status);
            return this;
        }

        /**
         * Sets the transaction ID.
         *
         * @param transactionId the transaction ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transactionId(@Nullable UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        /**
         * Sets the previous balance.
         *
         * @param previousBalance the previous balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder previousBalance(@Nullable BigDecimal previousBalance) {
            this.previousBalance = previousBalance;
            return this;
        }

        /**
         * Sets the new balance.
         *
         * @param newBalance the new balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder newBalance(@Nullable BigDecimal newBalance) {
            this.newBalance = newBalance;
            return this;
        }

        /**
         * Sets the requested amount.
         *
         * @param requestedAmount the requested amount
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder requestedAmount(@Nullable BigDecimal requestedAmount) {
            this.requestedAmount = requestedAmount;
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
        public Builder currencyId(@Nullable String currencyId) {
            this.currencyId = currencyId;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param errorMessage the error message
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder errorMessage(@Nullable String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Sets the transaction.
         *
         * @param transaction the transaction
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder transaction(@Nullable Transaction transaction) {
            this.transaction = transaction;
            return this;
        }

        /**
         * Builds the transaction result.
         *
         * @return the transaction result
         * @throws IllegalStateException if status is not set
         * @since 1.0.0
         */
        @NotNull
        public TransactionResult build() {
            Objects.requireNonNull(status, "status is required");
            return new TransactionResult(this);
        }
    }
}
