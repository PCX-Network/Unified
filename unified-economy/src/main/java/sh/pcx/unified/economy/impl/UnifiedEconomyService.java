/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.impl;

import sh.pcx.unified.economy.*;
import sh.pcx.unified.economy.event.AccountCreateEvent;
import sh.pcx.unified.economy.event.BalanceChangeEvent;
import sh.pcx.unified.economy.event.TransferEvent;
import sh.pcx.unified.economy.storage.EconomyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Primary implementation of {@link EconomyService}.
 *
 * <p>This implementation provides:
 * <ul>
 *   <li>Multi-currency support</li>
 *   <li>Async operations with CompletableFuture</li>
 *   <li>Transaction history</li>
 *   <li>Event firing for balance changes</li>
 *   <li>Leaderboard caching</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class UnifiedEconomyService implements EconomyService {

    private static final Logger LOGGER = Logger.getLogger(UnifiedEconomyService.class.getName());

    private final Map<String, Currency> currencies;
    private final Map<UUID, EconomyAccount> accounts;
    private final TransactionProcessor transactionProcessor;
    private final EconomyStorage storage;
    private final Consumer<Object> eventPublisher;

    private Currency defaultCurrency;
    private volatile List<BalanceEntry> cachedLeaderboard;
    private volatile long leaderboardCacheTime;
    private static final long LEADERBOARD_CACHE_DURATION_MS = 60_000; // 1 minute

    /**
     * Creates a new economy service with in-memory storage.
     *
     * @since 1.0.0
     */
    public UnifiedEconomyService() {
        this(null, null);
    }

    /**
     * Creates a new economy service.
     *
     * @param storage        the storage backend (null for in-memory)
     * @param eventPublisher the event publisher (null to disable events)
     * @since 1.0.0
     */
    public UnifiedEconomyService(
            @Nullable EconomyStorage storage,
            @Nullable Consumer<Object> eventPublisher
    ) {
        this.currencies = new ConcurrentHashMap<>();
        this.accounts = new ConcurrentHashMap<>();
        this.transactionProcessor = new InMemoryTransactionProcessor();
        this.storage = storage;
        this.eventPublisher = eventPublisher;

        // Register default currency
        this.defaultCurrency = Currency.createDefault("default");
        currencies.put(defaultCurrency.getIdentifier(), defaultCurrency);
    }

    // ========================================================================
    // Currency Management
    // ========================================================================

    @Override
    @NotNull
    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    @Override
    @NotNull
    public Optional<Currency> getCurrency(@NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return Optional.ofNullable(currencies.get(identifier.toLowerCase()));
    }

    @Override
    @NotNull
    public Collection<Currency> getCurrencies() {
        return Collections.unmodifiableCollection(currencies.values());
    }

    @Override
    public void registerCurrency(@NotNull Currency currency) {
        Objects.requireNonNull(currency, "currency cannot be null");
        String id = currency.getIdentifier().toLowerCase();

        if (currencies.containsKey(id)) {
            throw new IllegalArgumentException("Currency already registered: " + id);
        }

        currencies.put(id, currency);

        if (currency.isDefaultCurrency()) {
            defaultCurrency = currency;
        }

        LOGGER.info("Registered currency: " + currency);
    }

    @Override
    public boolean unregisterCurrency(@NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        String id = identifier.toLowerCase();

        if (defaultCurrency.getIdentifier().equals(id)) {
            LOGGER.warning("Cannot unregister default currency: " + id);
            return false;
        }

        Currency removed = currencies.remove(id);
        if (removed != null) {
            LOGGER.info("Unregistered currency: " + removed);
            return true;
        }
        return false;
    }

    // ========================================================================
    // Account Management
    // ========================================================================

    @Override
    public boolean hasAccount(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return accounts.containsKey(playerId);
    }

    @Override
    public boolean hasAccount(@NotNull UUID playerId, @NotNull String currencyId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        return account != null && account.hasCurrency(currencyId);
    }

    @Override
    @NotNull
    public CompletableFuture<Account> createAccount(@NotNull UUID playerId, @NotNull String playerName) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");

        return CompletableFuture.supplyAsync(() -> {
            // Check if account already exists
            EconomyAccount existing = accounts.get(playerId);
            if (existing != null) {
                return existing;
            }

            // Fire event
            AccountCreateEvent event = new AccountCreateEvent(
                    playerId,
                    playerName,
                    defaultCurrency.getIdentifier(),
                    defaultCurrency.getStartingBalance()
            );

            if (eventPublisher != null) {
                eventPublisher.accept(event);
                if (event.isCancelled()) {
                    throw new IllegalStateException("Account creation was cancelled");
                }
            }

            // Create account
            EconomyAccount account = new EconomyAccount(
                    playerId,
                    playerName,
                    this,
                    transactionProcessor
            );

            // Initialize with modified starting balance if changed by event
            if (!event.getStartingBalance().equals(defaultCurrency.getStartingBalance())) {
                account.setBalanceInternal(defaultCurrency.getIdentifier(), event.getStartingBalance());
            }

            accounts.put(playerId, account);
            invalidateLeaderboardCache();

            LOGGER.fine("Created account for " + playerName + " (" + playerId + ")");

            return account;
        });
    }

    @Override
    @NotNull
    public Optional<Account> getAccount(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return Optional.ofNullable(accounts.get(playerId));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Account>> getAccountAsync(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(getAccount(playerId));
    }

    // ========================================================================
    // Balance Operations (Default Currency)
    // ========================================================================

    @Override
    @NotNull
    public BigDecimal getBalance(@NotNull UUID playerId) {
        return getBalance(playerId, defaultCurrency.getIdentifier());
    }

    @Override
    @NotNull
    public CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID playerId) {
        return CompletableFuture.completedFuture(getBalance(playerId));
    }

    @Override
    public boolean has(@NotNull UUID playerId, @NotNull BigDecimal amount) {
        return has(playerId, amount, defaultCurrency.getIdentifier());
    }

    @Override
    @NotNull
    public TransactionResult deposit(@NotNull UUID playerId, @NotNull BigDecimal amount) {
        return deposit(playerId, amount, null);
    }

    @Override
    @NotNull
    public TransactionResult deposit(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason) {
        return deposit(playerId, amount, defaultCurrency.getIdentifier(), reason);
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> depositAsync(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @Nullable String reason
    ) {
        return CompletableFuture.supplyAsync(() -> deposit(playerId, amount, reason));
    }

    @Override
    @NotNull
    public TransactionResult withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount) {
        return withdraw(playerId, amount, null);
    }

    @Override
    @NotNull
    public TransactionResult withdraw(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason) {
        return withdraw(playerId, amount, defaultCurrency.getIdentifier(), reason);
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> withdrawAsync(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @Nullable String reason
    ) {
        return CompletableFuture.supplyAsync(() -> withdraw(playerId, amount, reason));
    }

    @Override
    @NotNull
    public TransactionResult setBalance(@NotNull UUID playerId, @NotNull BigDecimal amount) {
        return setBalance(playerId, amount, null);
    }

    @Override
    @NotNull
    public TransactionResult setBalance(@NotNull UUID playerId, @NotNull BigDecimal amount, @Nullable String reason) {
        return setBalance(playerId, amount, defaultCurrency.getIdentifier(), reason);
    }

    // ========================================================================
    // Balance Operations (Specific Currency)
    // ========================================================================

    @Override
    @NotNull
    public BigDecimal getBalance(@NotNull UUID playerId, @NotNull String currencyId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null) {
            return BigDecimal.ZERO;
        }
        return account.getBalance(currencyId);
    }

    @Override
    @NotNull
    public CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID playerId, @NotNull String currencyId) {
        return CompletableFuture.completedFuture(getBalance(playerId, currencyId));
    }

    @Override
    public boolean has(@NotNull UUID playerId, @NotNull BigDecimal amount, @NotNull String currencyId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null) {
            return false;
        }
        return account.has(amount, currencyId);
    }

    @Override
    @NotNull
    public TransactionResult deposit(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null) {
            return TransactionResult.accountNotFound(playerId);
        }

        // Fire event
        if (eventPublisher != null) {
            BalanceChangeEvent event = new BalanceChangeEvent(
                    playerId,
                    currencyId,
                    Transaction.Type.DEPOSIT,
                    account.getBalance(currencyId),
                    amount,
                    reason
            );
            eventPublisher.accept(event);

            if (event.isCancelled()) {
                return TransactionResult.builder()
                        .status(TransactionResult.Status.CANCELLED)
                        .errorMessage("Transaction cancelled by event handler")
                        .build();
            }

            amount = event.getAmount();
        }

        TransactionResult result = account.deposit(amount, currencyId, reason);
        if (result.isSuccess()) {
            invalidateLeaderboardCache();
        }
        return result;
    }

    @Override
    @NotNull
    public TransactionResult withdraw(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null) {
            return TransactionResult.accountNotFound(playerId);
        }

        // Fire event
        if (eventPublisher != null) {
            BalanceChangeEvent event = new BalanceChangeEvent(
                    playerId,
                    currencyId,
                    Transaction.Type.WITHDRAW,
                    account.getBalance(currencyId),
                    amount,
                    reason
            );
            eventPublisher.accept(event);

            if (event.isCancelled()) {
                return TransactionResult.builder()
                        .status(TransactionResult.Status.CANCELLED)
                        .errorMessage("Transaction cancelled by event handler")
                        .build();
            }

            amount = event.getAmount();
        }

        TransactionResult result = account.withdraw(amount, currencyId, reason);
        if (result.isSuccess()) {
            invalidateLeaderboardCache();
        }
        return result;
    }

    @Override
    @NotNull
    public TransactionResult setBalance(
            @NotNull UUID playerId,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null) {
            return TransactionResult.accountNotFound(playerId);
        }

        // Fire event
        if (eventPublisher != null) {
            BalanceChangeEvent event = new BalanceChangeEvent(
                    playerId,
                    currencyId,
                    Transaction.Type.SET,
                    account.getBalance(currencyId),
                    amount,
                    reason
            );
            eventPublisher.accept(event);

            if (event.isCancelled()) {
                return TransactionResult.builder()
                        .status(TransactionResult.Status.CANCELLED)
                        .errorMessage("Transaction cancelled by event handler")
                        .build();
            }

            amount = event.getAmount();
        }

        TransactionResult result = account.setBalance(amount, currencyId, reason);
        if (result.isSuccess()) {
            invalidateLeaderboardCache();
        }
        return result;
    }

    // ========================================================================
    // Transfer Operations
    // ========================================================================

    @Override
    @NotNull
    public TransactionResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount) {
        return transfer(from, to, amount, null);
    }

    @Override
    @NotNull
    public TransactionResult transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @Nullable String reason
    ) {
        return transfer(from, to, amount, defaultCurrency.getIdentifier(), reason);
    }

    @Override
    @NotNull
    public TransactionResult transfer(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(from, "from cannot be null");
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        if (from.equals(to)) {
            return TransactionResult.invalidAmount(amount, "Cannot transfer to self");
        }

        EconomyAccount fromAccount = accounts.get(from);
        EconomyAccount toAccount = accounts.get(to);

        if (fromAccount == null) {
            return TransactionResult.accountNotFound(from);
        }
        if (toAccount == null) {
            return TransactionResult.accountNotFound(to);
        }

        // Fire event
        if (eventPublisher != null) {
            TransferEvent event = new TransferEvent(
                    from,
                    to,
                    currencyId,
                    amount,
                    fromAccount.getBalance(currencyId),
                    toAccount.getBalance(currencyId),
                    reason
            );
            eventPublisher.accept(event);

            if (event.isCancelled()) {
                return TransactionResult.builder()
                        .status(TransactionResult.Status.CANCELLED)
                        .errorMessage("Transfer cancelled by event handler")
                        .build();
            }

            amount = event.getAmount();
        }

        TransactionResult result = fromAccount.transferTo(toAccount, amount, currencyId, reason);
        if (result.isSuccess()) {
            invalidateLeaderboardCache();
        }
        return result;
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> transferAsync(
            @NotNull UUID from,
            @NotNull UUID to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        return CompletableFuture.supplyAsync(() -> transfer(from, to, amount, currencyId, reason));
    }

    // ========================================================================
    // Transaction History
    // ========================================================================

    @Override
    @NotNull
    public List<Transaction> getTransactionHistory(@NotNull UUID playerId, int limit) {
        return transactionProcessor.getHistory(playerId, limit);
    }

    @Override
    @NotNull
    public CompletableFuture<List<Transaction>> getTransactionHistoryAsync(@NotNull UUID playerId, int limit) {
        return CompletableFuture.completedFuture(getTransactionHistory(playerId, limit));
    }

    @Override
    @NotNull
    public List<Transaction> getTransactionHistory(@NotNull UUID playerId, @NotNull String currencyId, int limit) {
        return transactionProcessor.getHistory(playerId, currencyId, limit);
    }

    @Override
    @NotNull
    public Optional<Transaction> getTransaction(@NotNull UUID transactionId) {
        return transactionProcessor.getTransaction(transactionId);
    }

    // ========================================================================
    // Leaderboard (Baltop)
    // ========================================================================

    @Override
    @NotNull
    public List<BalanceEntry> getTopBalances(int limit) {
        return getTopBalances(defaultCurrency.getIdentifier(), limit);
    }

    @Override
    @NotNull
    public CompletableFuture<List<BalanceEntry>> getTopBalancesAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> getTopBalances(limit));
    }

    @Override
    @NotNull
    public List<BalanceEntry> getTopBalances(@NotNull String currencyId, int limit) {
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        // Check cache for default currency
        if (currencyId.equals(defaultCurrency.getIdentifier())) {
            long now = System.currentTimeMillis();
            if (cachedLeaderboard != null && (now - leaderboardCacheTime) < LEADERBOARD_CACHE_DURATION_MS) {
                return cachedLeaderboard.stream().limit(limit).toList();
            }
        }

        List<EconomyAccount> sortedAccounts = accounts.values().stream()
                .filter(acc -> acc.hasCurrency(currencyId))
                .sorted((a, b) -> b.getBalance(currencyId).compareTo(a.getBalance(currencyId)))
                .limit(limit)
                .toList();

        List<BalanceEntry> result = new ArrayList<>();
        for (int i = 0; i < sortedAccounts.size(); i++) {
            EconomyAccount account = sortedAccounts.get(i);
            result.add(new BalanceEntry(
                    account.getOwnerId(),
                    account.getOwnerName(),
                    account.getBalance(currencyId),
                    currencyId,
                    i + 1
            ));
        }

        // Update cache for default currency
        if (currencyId.equals(defaultCurrency.getIdentifier())) {
            cachedLeaderboard = result;
            leaderboardCacheTime = System.currentTimeMillis();
        }

        return result;
    }

    @Override
    @NotNull
    public CompletableFuture<List<BalanceEntry>> getTopBalancesAsync(@NotNull String currencyId, int limit) {
        return CompletableFuture.supplyAsync(() -> getTopBalances(currencyId, limit));
    }

    @Override
    public int getBalanceRank(@NotNull UUID playerId) {
        return getBalanceRank(playerId, defaultCurrency.getIdentifier());
    }

    @Override
    public int getBalanceRank(@NotNull UUID playerId, @NotNull String currencyId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        EconomyAccount account = accounts.get(playerId);
        if (account == null || !account.hasCurrency(currencyId)) {
            return -1;
        }

        BigDecimal playerBalance = account.getBalance(currencyId);
        int rank = 1;

        for (EconomyAccount other : accounts.values()) {
            if (!other.getOwnerId().equals(playerId) && other.hasCurrency(currencyId)) {
                if (other.getBalance(currencyId).compareTo(playerBalance) > 0) {
                    rank++;
                }
            }
        }

        return rank;
    }

    // ========================================================================
    // Formatting
    // ========================================================================

    @Override
    @NotNull
    public String format(@NotNull BigDecimal amount) {
        return defaultCurrency.format(amount);
    }

    @Override
    @NotNull
    public String format(@NotNull BigDecimal amount, @NotNull String currencyId) {
        Currency currency = getCurrencyOrThrow(currencyId);
        return currency.format(amount);
    }

    @Override
    @NotNull
    public String formatWithName(@NotNull BigDecimal amount) {
        return defaultCurrency.formatWithName(amount);
    }

    @Override
    @NotNull
    public String formatWithName(@NotNull BigDecimal amount, @NotNull String currencyId) {
        Currency currency = getCurrencyOrThrow(currencyId);
        return currency.formatWithName(amount);
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    @Override
    public boolean supportsMultiCurrency() {
        return true;
    }

    @Override
    public boolean supportsTransactionHistory() {
        return transactionProcessor.isEnabled();
    }

    @Override
    @NotNull
    public BigDecimal getTotalSupply() {
        return getTotalSupply(defaultCurrency.getIdentifier());
    }

    @Override
    @NotNull
    public BigDecimal getTotalSupply(@NotNull String currencyId) {
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        return accounts.values().stream()
                .filter(acc -> acc.hasCurrency(currencyId))
                .map(acc -> acc.getBalance(currencyId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getAccountCount() {
        return accounts.size();
    }

    @Override
    @NotNull
    public String getServiceName() {
        return "UnifiedEconomyService";
    }

    /**
     * Gets an account, creating one if it doesn't exist.
     *
     * @param playerId   the player UUID
     * @param playerName the player name
     * @return the account
     * @since 1.0.0
     */
    @NotNull
    public EconomyAccount getOrCreateAccount(@NotNull UUID playerId, @NotNull String playerName) {
        return accounts.computeIfAbsent(playerId, id -> {
            EconomyAccount account = new EconomyAccount(id, playerName, this, transactionProcessor);
            invalidateLeaderboardCache();
            return account;
        });
    }

    /**
     * Invalidates the leaderboard cache.
     */
    private void invalidateLeaderboardCache() {
        cachedLeaderboard = null;
        leaderboardCacheTime = 0;
    }

    /**
     * Returns the transaction processor.
     *
     * @return the transaction processor
     * @since 1.0.0
     */
    @NotNull
    public TransactionProcessor getTransactionProcessor() {
        return transactionProcessor;
    }
}
