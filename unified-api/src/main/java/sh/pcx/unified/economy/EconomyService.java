/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import sh.pcx.unified.service.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for economy operations.
 *
 * <p>The EconomyService provides a unified API for managing player balances,
 * performing transactions, and handling multiple currencies. It is designed
 * to be compatible with Vault while providing extended functionality.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Multi-Currency:</b> Support for multiple currency types</li>
 *   <li><b>Async Operations:</b> Non-blocking balance operations</li>
 *   <li><b>Transaction History:</b> Complete audit trail of all transactions</li>
 *   <li><b>Atomic Transfers:</b> Safe transfers between accounts</li>
 *   <li><b>Leaderboards:</b> Balance rankings (baltop)</li>
 *   <li><b>Offline Support:</b> Modify balances for offline players</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private EconomyService economy;
 *
 * public void purchaseItem(UnifiedPlayer player, BigDecimal cost) {
 *     // Check balance
 *     if (!economy.has(player.getUniqueId(), cost)) {
 *         player.sendMessage(Component.text("Insufficient funds!"));
 *         return;
 *     }
 *
 *     // Withdraw funds
 *     TransactionResult result = economy.withdraw(
 *         player.getUniqueId(),
 *         cost,
 *         "Shop purchase"
 *     );
 *
 *     if (result.isSuccess()) {
 *         // Give item...
 *         player.sendMessage(Component.text("Purchased! New balance: " +
 *             economy.format(result.newBalance())));
 *     } else {
 *         player.sendMessage(Component.text("Transaction failed: " +
 *             result.errorMessage().orElse("Unknown error")));
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this interface are thread-safe. Async methods return
 * {@link CompletableFuture} for non-blocking operations.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Account
 * @see Transaction
 * @see TransactionResult
 * @see Currency
 */
public interface EconomyService extends Service {

    // ========================================================================
    // Currency Management
    // ========================================================================

    /**
     * Returns the default currency for this economy.
     *
     * <p>The default currency is used when no currency is explicitly specified
     * in balance and transaction operations.
     *
     * @return the default currency
     * @since 1.0.0
     */
    @NotNull
    Currency getDefaultCurrency();

    /**
     * Returns a currency by its identifier.
     *
     * @param identifier the currency identifier (e.g., "coins", "gems")
     * @return an Optional containing the currency if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Currency> getCurrency(@NotNull String identifier);

    /**
     * Returns a currency by its identifier or throws if not found.
     *
     * @param identifier the currency identifier
     * @return the currency
     * @throws IllegalArgumentException if the currency is not found
     * @since 1.0.0
     */
    @NotNull
    default Currency getCurrencyOrThrow(@NotNull String identifier) {
        return getCurrency(identifier).orElseThrow(() ->
                new IllegalArgumentException("Currency not found: " + identifier));
    }

    /**
     * Returns all registered currencies.
     *
     * @return an unmodifiable collection of all currencies
     * @since 1.0.0
     */
    @NotNull
    Collection<Currency> getCurrencies();

    /**
     * Registers a new currency.
     *
     * @param currency the currency to register
     * @throws IllegalArgumentException if a currency with the same identifier exists
     * @since 1.0.0
     */
    void registerCurrency(@NotNull Currency currency);

    /**
     * Unregisters a currency.
     *
     * <p>Note: The default currency cannot be unregistered.
     *
     * @param identifier the currency identifier
     * @return true if the currency was unregistered
     * @since 1.0.0
     */
    boolean unregisterCurrency(@NotNull String identifier);

    // ========================================================================
    // Account Management
    // ========================================================================

    /**
     * Checks if an account exists for the given player.
     *
     * @param playerId the player's UUID
     * @return true if an account exists
     * @since 1.0.0
     */
    boolean hasAccount(@NotNull UUID playerId);

    /**
     * Checks if an account exists for the given player in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @return true if an account exists for that currency
     * @since 1.0.0
     */
    boolean hasAccount(@NotNull UUID playerId, @NotNull String currencyId);

    /**
     * Creates an account for the given player if one does not exist.
     *
     * <p>The account is created with the default currency's starting balance.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's name (for display purposes)
     * @return a future that completes with the created or existing account
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Account> createAccount(@NotNull UUID playerId, @NotNull String playerName);

    /**
     * Gets the account for a player.
     *
     * @param playerId the player's UUID
     * @return an Optional containing the account if it exists
     * @since 1.0.0
     */
    @NotNull
    Optional<Account> getAccount(@NotNull UUID playerId);

    /**
     * Gets the account for a player asynchronously.
     *
     * @param playerId the player's UUID
     * @return a future that completes with the account if it exists
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<Optional<Account>> getAccountAsync(@NotNull UUID playerId);

    // ========================================================================
    // Balance Operations (Default Currency)
    // ========================================================================

    /**
     * Gets the balance for a player in the default currency.
     *
     * @param playerId the player's UUID
     * @return the player's balance
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getBalance(@NotNull UUID playerId);

    /**
     * Gets the balance for a player asynchronously.
     *
     * @param playerId the player's UUID
     * @return a future that completes with the balance
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID playerId);

    /**
     * Checks if a player has at least the specified amount.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to check
     * @return true if the player has sufficient funds
     * @since 1.0.0
     */
    boolean has(@NotNull UUID playerId, @NotNull BigDecimal amount);

    /**
     * Deposits an amount into a player's account.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult deposit(@NotNull UUID playerId, @NotNull BigDecimal amount);

    /**
     * Deposits an amount into a player's account with a reason.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to deposit
     * @param reason   the reason for the deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult deposit(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Deposits an amount asynchronously.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to deposit
     * @param reason   the reason for the deposit
     * @return a future that completes with the transaction result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransactionResult> depositAsync(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @Nullable String reason
    );

    /**
     * Withdraws an amount from a player's account.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to withdraw
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount);

    /**
     * Withdraws an amount from a player's account with a reason.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to withdraw
     * @param reason   the reason for the withdrawal
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Withdraws an amount asynchronously.
     *
     * @param playerId the player's UUID
     * @param amount   the amount to withdraw
     * @param reason   the reason for the withdrawal
     * @return a future that completes with the transaction result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransactionResult> withdrawAsync(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @Nullable String reason
    );

    /**
     * Sets the balance for a player.
     *
     * @param playerId the player's UUID
     * @param amount   the new balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult setBalance(@NotNull UUID playerId, @NotNull BigDecimal amount);

    /**
     * Sets the balance for a player with a reason.
     *
     * @param playerId the player's UUID
     * @param amount   the new balance
     * @param reason   the reason for setting the balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult setBalance(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason);

    // ========================================================================
    // Balance Operations (Specific Currency)
    // ========================================================================

    /**
     * Gets the balance for a player in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @return the player's balance in that currency
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getBalance(@NotNull UUID playerId, @NotNull String currencyId);

    /**
     * Gets the balance for a player in a specific currency asynchronously.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @return a future that completes with the balance
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID playerId, @NotNull String currencyId);

    /**
     * Checks if a player has at least the specified amount in a currency.
     *
     * @param playerId   the player's UUID
     * @param amount     the amount to check
     * @param currencyId the currency identifier
     * @return true if the player has sufficient funds
     * @since 1.0.0
     */
    boolean has(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currencyId);

    /**
     * Deposits an amount into a player's account in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param amount     the amount to deposit
     * @param currencyId the currency identifier
     * @param reason     the reason for the deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult deposit(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    /**
     * Withdraws an amount from a player's account in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param amount     the amount to withdraw
     * @param currencyId the currency identifier
     * @param reason     the reason for the withdrawal
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult withdraw(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    /**
     * Sets the balance for a player in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param amount     the new balance
     * @param currencyId the currency identifier
     * @param reason     the reason for setting the balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult setBalance(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    // ========================================================================
    // Transfer Operations
    // ========================================================================

    /**
     * Transfers funds between two players.
     *
     * <p>This operation is atomic - if the withdrawal fails, no deposit is made.
     *
     * @param from   the source player's UUID
     * @param to     the destination player's UUID
     * @param amount the amount to transfer
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount);

    /**
     * Transfers funds between two players with a reason.
     *
     * @param from   the source player's UUID
     * @param to     the destination player's UUID
     * @param amount the amount to transfer
     * @param reason the reason for the transfer
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @Nullable String reason
    );

    /**
     * Transfers funds between two players in a specific currency.
     *
     * @param from       the source player's UUID
     * @param to         the destination player's UUID
     * @param amount     the amount to transfer
     * @param currencyId the currency identifier
     * @param reason     the reason for the transfer
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    /**
     * Transfers funds asynchronously.
     *
     * @param from       the source player's UUID
     * @param to         the destination player's UUID
     * @param amount     the amount to transfer
     * @param currencyId the currency identifier
     * @param reason     the reason for the transfer
     * @return a future that completes with the transaction result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<TransactionResult> transferAsync(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    // ========================================================================
    // Transaction History
    // ========================================================================

    /**
     * Gets the transaction history for a player.
     *
     * @param playerId the player's UUID
     * @param limit    the maximum number of transactions to return
     * @return a list of transactions, most recent first
     * @since 1.0.0
     */
    @NotNull
    List<Transaction> getTransactionHistory(@NotNull UUID playerId, int limit);

    /**
     * Gets the transaction history for a player asynchronously.
     *
     * @param playerId the player's UUID
     * @param limit    the maximum number of transactions to return
     * @return a future that completes with the transaction list
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<Transaction>> getTransactionHistoryAsync(@NotNull UUID playerId, int limit);

    /**
     * Gets the transaction history for a player in a specific currency.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @param limit      the maximum number of transactions to return
     * @return a list of transactions, most recent first
     * @since 1.0.0
     */
    @NotNull
    List<Transaction> getTransactionHistory(@NotNull UUID playerId, @NotNull String currencyId, int limit);

    /**
     * Gets a specific transaction by its ID.
     *
     * @param transactionId the transaction ID
     * @return an Optional containing the transaction if found
     * @since 1.0.0
     */
    @NotNull
    Optional<Transaction> getTransaction(@NotNull UUID transactionId);

    // ========================================================================
    // Leaderboard (Baltop)
    // ========================================================================

    /**
     * Gets the top balances for the default currency.
     *
     * @param limit the maximum number of entries to return
     * @return a list of balance entries, highest first
     * @since 1.0.0
     */
    @NotNull
    List<BalanceEntry> getTopBalances(int limit);

    /**
     * Gets the top balances asynchronously.
     *
     * @param limit the maximum number of entries to return
     * @return a future that completes with the leaderboard entries
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<BalanceEntry>> getTopBalancesAsync(int limit);

    /**
     * Gets the top balances for a specific currency.
     *
     * @param currencyId the currency identifier
     * @param limit      the maximum number of entries to return
     * @return a list of balance entries, highest first
     * @since 1.0.0
     */
    @NotNull
    List<BalanceEntry> getTopBalances(@NotNull String currencyId, int limit);

    /**
     * Gets the top balances for a specific currency asynchronously.
     *
     * @param currencyId the currency identifier
     * @param limit      the maximum number of entries to return
     * @return a future that completes with the leaderboard entries
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<List<BalanceEntry>> getTopBalancesAsync(@NotNull String currencyId, int limit);

    /**
     * Gets a player's rank on the leaderboard.
     *
     * @param playerId the player's UUID
     * @return the player's rank (1-based), or -1 if not ranked
     * @since 1.0.0
     */
    int getBalanceRank(@NotNull UUID playerId);

    /**
     * Gets a player's rank on the leaderboard for a specific currency.
     *
     * @param playerId   the player's UUID
     * @param currencyId the currency identifier
     * @return the player's rank (1-based), or -1 if not ranked
     * @since 1.0.0
     */
    int getBalanceRank(@NotNull UUID playerId, @NotNull String currencyId);

    // ========================================================================
    // Formatting
    // ========================================================================

    /**
     * Formats an amount using the default currency's format settings.
     *
     * @param amount the amount to format
     * @return the formatted amount string
     * @since 1.0.0
     */
    @NotNull
    String format(@NotNull BigDecimal amount);

    /**
     * Formats an amount using a specific currency's format settings.
     *
     * @param amount     the amount to format
     * @param currencyId the currency identifier
     * @return the formatted amount string
     * @since 1.0.0
     */
    @NotNull
    String format(@NotNull BigDecimal amount, @NotNull String currencyId);

    /**
     * Formats an amount with the currency name.
     *
     * @param amount the amount to format
     * @return the formatted amount with currency name
     * @since 1.0.0
     */
    @NotNull
    String formatWithName(@NotNull BigDecimal amount);

    /**
     * Formats an amount with a specific currency's name.
     *
     * @param amount     the amount to format
     * @param currencyId the currency identifier
     * @return the formatted amount with currency name
     * @since 1.0.0
     */
    @NotNull
    String formatWithName(@NotNull BigDecimal amount, @NotNull String currencyId);

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Returns whether this economy supports multiple currencies.
     *
     * @return true if multi-currency is supported
     * @since 1.0.0
     */
    boolean supportsMultiCurrency();

    /**
     * Returns whether this economy supports transaction history.
     *
     * @return true if transaction history is supported
     * @since 1.0.0
     */
    boolean supportsTransactionHistory();

    /**
     * Returns the total money supply for the default currency.
     *
     * @return the total money in circulation
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getTotalSupply();

    /**
     * Returns the total money supply for a specific currency.
     *
     * @param currencyId the currency identifier
     * @return the total money in circulation for that currency
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getTotalSupply(@NotNull String currencyId);

    /**
     * Returns the total number of accounts.
     *
     * @return the number of economy accounts
     * @since 1.0.0
     */
    int getAccountCount();
}
