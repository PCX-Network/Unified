/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.impl;

import sh.pcx.unified.economy.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for processing and storing transaction records.
 *
 * <p>This interface abstracts the storage and retrieval of transaction
 * history, allowing for different backend implementations.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface TransactionProcessor {

    /**
     * Records a transaction to the history.
     *
     * @param transaction the transaction to record
     * @since 1.0.0
     */
    void recordTransaction(@NotNull Transaction transaction);

    /**
     * Gets the transaction history for an account.
     *
     * @param accountId the account UUID
     * @param limit     the maximum number of transactions to return
     * @return a list of transactions, most recent first
     * @since 1.0.0
     */
    @NotNull
    List<Transaction> getHistory(@NotNull UUID accountId, int limit);

    /**
     * Gets the transaction history for an account in a specific currency.
     *
     * @param accountId  the account UUID
     * @param currencyId the currency identifier
     * @param limit      the maximum number of transactions to return
     * @return a list of transactions, most recent first
     * @since 1.0.0
     */
    @NotNull
    List<Transaction> getHistory(@NotNull UUID accountId, @NotNull String currencyId, int limit);

    /**
     * Gets a specific transaction by its ID.
     *
     * @param transactionId the transaction ID
     * @return an Optional containing the transaction if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Transaction> getTransaction(@NotNull UUID transactionId);

    /**
     * Clears old transactions beyond the retention period.
     *
     * @param olderThanDays the number of days to retain
     * @return the number of transactions cleared
     * @since 1.0.0
     */
    int clearOldTransactions(int olderThanDays);

    /**
     * Returns whether transaction history is enabled.
     *
     * @return true if transaction history is being recorded
     * @since 1.0.0
     */
    boolean isEnabled();
}
