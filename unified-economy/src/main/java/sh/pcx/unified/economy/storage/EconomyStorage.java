/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.storage;

import sh.pcx.unified.economy.Account;
import sh.pcx.unified.economy.BalanceEntry;
import sh.pcx.unified.economy.Transaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for economy data persistence.
 *
 * <p>This interface defines the contract for storing and retrieving economy
 * data. Implementations can use various backends such as:
 * <ul>
 *   <li>SQL databases (MySQL, PostgreSQL, SQLite)</li>
 *   <li>NoSQL databases (MongoDB, Redis)</li>
 *   <li>File-based storage (JSON, YAML)</li>
 * </ul>
 *
 * <h2>Implementation Guidelines</h2>
 * <ul>
 *   <li>All methods should be thread-safe</li>
 *   <li>Use async operations for I/O-bound work</li>
 *   <li>Handle connection failures gracefully</li>
 *   <li>Implement proper transaction support for atomicity</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public interface EconomyStorage {

    /**
     * Initializes the storage backend.
     *
     * <p>This method should create necessary tables, indexes, or file structures.
     *
     * @return a future that completes when initialization is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> initialize();

    /**
     * Shuts down the storage backend.
     *
     * <p>This method should flush any pending writes and close connections.
     *
     * @return a future that completes when shutdown is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> shutdown();

    // ========================================================================
    // Account Operations
    // ========================================================================

    /**
     * Loads an account from storage.
     *
     * @param playerId the player's UUID
     * @return a future that completes with the account if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Account>> loadAccount(@NotNull UUID playerId);

    /**
     * Saves an account to storage.
     *
     * @param account the account to save
     * @return a future that completes when the save is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAccount(@NotNull Account account);

    /**
     * Deletes an account from storage.
     *
     * @param playerId the player's UUID
     * @return a future that completes with true if the account was deleted
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> deleteAccount(@NotNull UUID playerId);

    /**
     * Checks if an account exists in storage.
     *
     * @param playerId the player's UUID
     * @return a future that completes with true if the account exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> accountExists(@NotNull UUID playerId);

    /**
     * Loads all accounts from storage.
     *
     * <p>This method is typically used during plugin startup.
     *
     * @return a future that completes with all accounts
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Account>> loadAllAccounts();

    /**
     * Saves all accounts to storage.
     *
     * <p>This method is typically used during plugin shutdown.
     *
     * @param accounts the accounts to save
     * @return a future that completes when all saves are done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveAllAccounts(@NotNull List<Account> accounts);

    // ========================================================================
    // Balance Operations
    // ========================================================================

    /**
     * Updates a player's balance for a specific currency.
     *
     * <p>This is an optimized method for updating just the balance without
     * rewriting the entire account.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @param newBalance the new balance
     * @return a future that completes when the update is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> updateBalance(
            @NotNull UUID playerId,
            @NotNull String currencyId,
            @NotNull BigDecimal newBalance
    );

    /**
     * Gets the top balances for leaderboards.
     *
     * @param currencyId the currency identifier
     * @param limit      the maximum number of entries
     * @return a future that completes with the top balances
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<BalanceEntry>> getTopBalances(@NotNull String currencyId, int limit);

    /**
     * Gets a player's rank in the leaderboard.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @return a future that completes with the rank (1-based), or -1 if not ranked
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> getBalanceRank(@NotNull UUID playerId, @NotNull String currencyId);

    /**
     * Gets the total money supply for a currency.
     *
     * @param currencyId the currency identifier
     * @return a future that completes with the total supply
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<BigDecimal> getTotalSupply(@NotNull String currencyId);

    /**
     * Gets the total number of accounts.
     *
     * @return a future that completes with the account count
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> getAccountCount();

    // ========================================================================
    // Transaction Operations
    // ========================================================================

    /**
     * Saves a transaction record.
     *
     * @param transaction the transaction to save
     * @return a future that completes when the save is done
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Void> saveTransaction(@NotNull Transaction transaction);

    /**
     * Loads transaction history for a player.
     *
     * @param playerId the player's UUID
     * @param limit    the maximum number of transactions
     * @return a future that completes with the transactions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Transaction>> loadTransactionHistory(@NotNull UUID playerId, int limit);

    /**
     * Loads transaction history for a player in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @param limit      the maximum number of transactions
     * @return a future that completes with the transactions
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Transaction>> loadTransactionHistory(
            @NotNull UUID playerId,
            @NotNull String currencyId,
            int limit
    );

    /**
     * Loads a specific transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return a future that completes with the transaction if found
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Transaction>> loadTransaction(@NotNull UUID transactionId);

    /**
     * Clears old transactions beyond the retention period.
     *
     * @param olderThanDays the number of days to retain
     * @return a future that completes with the number of transactions cleared
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Integer> clearOldTransactions(int olderThanDays);

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Returns the name of this storage backend.
     *
     * @return the storage name (e.g., "MySQL", "SQLite", "MongoDB")
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns whether the storage backend is connected and ready.
     *
     * @return true if the storage is available
     * @since 1.0.0
     */
    boolean isAvailable();

    /**
     * Performs a health check on the storage backend.
     *
     * @return a future that completes with true if healthy
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Boolean> healthCheck();
}
