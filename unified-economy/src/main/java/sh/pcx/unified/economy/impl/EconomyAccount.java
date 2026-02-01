/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.impl;

import sh.pcx.unified.economy.Account;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.Transaction;
import sh.pcx.unified.economy.TransactionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link Account} with thread-safe balance operations.
 *
 * <p>This implementation uses a read-write lock to ensure atomic balance
 * modifications while allowing concurrent reads.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class EconomyAccount implements Account {

    private final UUID ownerId;
    private final String ownerName;
    private final Instant createdAt;
    private final EconomyService economyService;
    private final TransactionProcessor transactionProcessor;

    private final Map<String, BigDecimal> balances;
    private final Map<String, String> metadata;
    private final ReadWriteLock lock;

    private volatile Instant lastModified;

    /**
     * Creates a new economy account.
     *
     * @param ownerId              the owner's UUID
     * @param ownerName            the owner's name
     * @param economyService       the economy service
     * @param transactionProcessor the transaction processor
     * @since 1.0.0
     */
    public EconomyAccount(
            @NotNull UUID ownerId,
            @NotNull String ownerName,
            @NotNull EconomyService economyService,
            @NotNull TransactionProcessor transactionProcessor
    ) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName cannot be null");
        this.economyService = Objects.requireNonNull(economyService, "economyService cannot be null");
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor, "transactionProcessor cannot be null");

        this.createdAt = Instant.now();
        this.lastModified = this.createdAt;
        this.balances = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();

        // Initialize with starting balance for default currency
        Currency defaultCurrency = economyService.getDefaultCurrency();
        balances.put(defaultCurrency.getIdentifier(), defaultCurrency.getStartingBalance());
    }

    /**
     * Creates a new economy account with existing balances.
     *
     * @param ownerId              the owner's UUID
     * @param ownerName            the owner's name
     * @param createdAt            the creation timestamp
     * @param lastModified         the last modification timestamp
     * @param balances             the existing balances
     * @param metadata             the existing metadata
     * @param economyService       the economy service
     * @param transactionProcessor the transaction processor
     * @since 1.0.0
     */
    public EconomyAccount(
            @NotNull UUID ownerId,
            @NotNull String ownerName,
            @NotNull Instant createdAt,
            @NotNull Instant lastModified,
            @NotNull Map<String, BigDecimal> balances,
            @NotNull Map<String, String> metadata,
            @NotNull EconomyService economyService,
            @NotNull TransactionProcessor transactionProcessor
    ) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId cannot be null");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.lastModified = Objects.requireNonNull(lastModified, "lastModified cannot be null");
        this.economyService = Objects.requireNonNull(economyService, "economyService cannot be null");
        this.transactionProcessor = Objects.requireNonNull(transactionProcessor, "transactionProcessor cannot be null");

        this.balances = new ConcurrentHashMap<>(balances);
        this.metadata = new ConcurrentHashMap<>(metadata);
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    @NotNull
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    @NotNull
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    @NotNull
    public Instant getLastModified() {
        return lastModified;
    }

    // ========================================================================
    // Balance Operations (Default Currency)
    // ========================================================================

    @Override
    @NotNull
    public BigDecimal getBalance() {
        return getBalance(economyService.getDefaultCurrency().getIdentifier());
    }

    @Override
    public boolean has(@NotNull BigDecimal amount) {
        return has(amount, economyService.getDefaultCurrency().getIdentifier());
    }

    @Override
    @NotNull
    public TransactionResult deposit(@NotNull BigDecimal amount, @Nullable String reason) {
        return deposit(amount, economyService.getDefaultCurrency().getIdentifier(), reason);
    }

    @Override
    @NotNull
    public TransactionResult withdraw(@NotNull BigDecimal amount, @Nullable String reason) {
        return withdraw(amount, economyService.getDefaultCurrency().getIdentifier(), reason);
    }

    @Override
    @NotNull
    public TransactionResult setBalance(@NotNull BigDecimal amount, @Nullable String reason) {
        return setBalance(amount, economyService.getDefaultCurrency().getIdentifier(), reason);
    }

    // ========================================================================
    // Balance Operations (Specific Currency)
    // ========================================================================

    @Override
    @NotNull
    public BigDecimal getBalance(@NotNull String currencyId) {
        Objects.requireNonNull(currencyId, "currencyId cannot be null");
        lock.readLock().lock();
        try {
            return balances.getOrDefault(currencyId, BigDecimal.ZERO);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean has(@NotNull BigDecimal amount, @NotNull String currencyId) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");
        return getBalance(currencyId).compareTo(amount) >= 0;
    }

    @Override
    @NotNull
    public TransactionResult deposit(
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        if (amount.signum() < 0) {
            return TransactionResult.invalidAmount(amount, "Amount cannot be negative");
        }

        Optional<Currency> currencyOpt = economyService.getCurrency(currencyId);
        if (currencyOpt.isEmpty()) {
            return TransactionResult.builder()
                    .status(TransactionResult.Status.CURRENCY_NOT_FOUND)
                    .errorMessage("Currency not found: " + currencyId)
                    .build();
        }

        Currency currency = currencyOpt.get();
        amount = currency.round(amount);

        lock.writeLock().lock();
        try {
            BigDecimal oldBalance = balances.getOrDefault(currencyId, BigDecimal.ZERO);
            BigDecimal newBalance = oldBalance.add(amount);

            // Check max balance
            if (newBalance.compareTo(currency.getMaxBalance()) > 0) {
                return TransactionResult.maxBalanceExceeded(oldBalance, amount, currency.getMaxBalance(), currencyId);
            }

            // Update balance
            balances.put(currencyId, newBalance);
            lastModified = Instant.now();

            // Create transaction record
            Transaction transaction = Transaction.deposit(ownerId, currencyId, amount, oldBalance, newBalance, reason);
            transactionProcessor.recordTransaction(transaction);

            return TransactionResult.success(oldBalance, newBalance, currencyId, transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @NotNull
    public TransactionResult withdraw(
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        if (amount.signum() < 0) {
            return TransactionResult.invalidAmount(amount, "Amount cannot be negative");
        }

        Optional<Currency> currencyOpt = economyService.getCurrency(currencyId);
        if (currencyOpt.isEmpty()) {
            return TransactionResult.builder()
                    .status(TransactionResult.Status.CURRENCY_NOT_FOUND)
                    .errorMessage("Currency not found: " + currencyId)
                    .build();
        }

        Currency currency = currencyOpt.get();
        amount = currency.round(amount);

        lock.writeLock().lock();
        try {
            BigDecimal oldBalance = balances.getOrDefault(currencyId, BigDecimal.ZERO);
            BigDecimal newBalance = oldBalance.subtract(amount);

            // Check minimum balance
            if (newBalance.compareTo(currency.getMinBalance()) < 0) {
                return TransactionResult.insufficientFunds(oldBalance, amount, currencyId);
            }

            // Update balance
            balances.put(currencyId, newBalance);
            lastModified = Instant.now();

            // Create transaction record
            Transaction transaction = Transaction.withdraw(ownerId, currencyId, amount, oldBalance, newBalance, reason);
            transactionProcessor.recordTransaction(transaction);

            return TransactionResult.success(oldBalance, newBalance, currencyId, transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @NotNull
    public TransactionResult setBalance(
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        Optional<Currency> currencyOpt = economyService.getCurrency(currencyId);
        if (currencyOpt.isEmpty()) {
            return TransactionResult.builder()
                    .status(TransactionResult.Status.CURRENCY_NOT_FOUND)
                    .errorMessage("Currency not found: " + currencyId)
                    .build();
        }

        Currency currency = currencyOpt.get();

        // Validate the amount
        if (!currency.isValidBalance(amount)) {
            return TransactionResult.invalidAmount(amount,
                    "Amount must be between " + currency.getMinBalance() + " and " + currency.getMaxBalance());
        }

        amount = currency.round(amount);

        lock.writeLock().lock();
        try {
            BigDecimal oldBalance = balances.getOrDefault(currencyId, BigDecimal.ZERO);

            // Update balance
            balances.put(currencyId, amount);
            lastModified = Instant.now();

            // Create transaction record
            Transaction transaction = Transaction.builder()
                    .accountId(ownerId)
                    .currencyId(currencyId)
                    .type(Transaction.Type.SET)
                    .amount(amount)
                    .balanceBefore(oldBalance)
                    .balanceAfter(amount)
                    .reason(reason)
                    .build();
            transactionProcessor.recordTransaction(transaction);

            return TransactionResult.success(oldBalance, amount, currencyId, transaction);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========================================================================
    // Multi-Currency Support
    // ========================================================================

    @Override
    @NotNull
    public Map<String, BigDecimal> getAllBalances() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new ConcurrentHashMap<>(balances));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean hasCurrency(@NotNull String currencyId) {
        Objects.requireNonNull(currencyId, "currencyId cannot be null");
        lock.readLock().lock();
        try {
            return balances.containsKey(currencyId);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========================================================================
    // Transfer Operations
    // ========================================================================

    @Override
    @NotNull
    public TransactionResult transferTo(
            @NotNull Account to,
            @NotNull BigDecimal amount,
            @Nullable String reason
    ) {
        return transferTo(to, amount, economyService.getDefaultCurrency().getIdentifier(), reason);
    }

    @Override
    @NotNull
    public TransactionResult transferTo(
            @NotNull Account to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    ) {
        Objects.requireNonNull(to, "to cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        // Withdraw from this account
        TransactionResult withdrawResult = withdraw(amount, currencyId, reason);
        if (!withdrawResult.isSuccess()) {
            return withdrawResult;
        }

        // Deposit to target account
        TransactionResult depositResult = to.deposit(amount, currencyId, reason);
        if (!depositResult.isSuccess()) {
            // Rollback the withdrawal
            deposit(amount, currencyId, "Transfer rollback");
            return depositResult;
        }

        // Create transfer transaction record
        Transaction transferTransaction = Transaction.transfer(
                ownerId,
                to.getOwnerId(),
                currencyId,
                amount,
                withdrawResult.previousBalance(),
                withdrawResult.newBalance(),
                reason
        );
        transactionProcessor.recordTransaction(transferTransaction);

        return TransactionResult.success(
                withdrawResult.previousBalance(),
                withdrawResult.newBalance(),
                currencyId,
                transferTransaction
        );
    }

    // ========================================================================
    // Metadata
    // ========================================================================

    @Override
    @NotNull
    public Optional<String> getMetadata(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    public void setMetadata(@NotNull String key, @Nullable String value) {
        Objects.requireNonNull(key, "key cannot be null");
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
        lastModified = Instant.now();
    }

    @Override
    public boolean removeMetadata(@NotNull String key) {
        Objects.requireNonNull(key, "key cannot be null");
        boolean removed = metadata.remove(key) != null;
        if (removed) {
            lastModified = Instant.now();
        }
        return removed;
    }

    @Override
    @NotNull
    public Map<String, String> getAllMetadata() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(metadata));
    }

    /**
     * Sets the balance directly without creating a transaction record.
     *
     * <p>This method is intended for use by storage backends during loading.
     *
     * @param currencyId the currency identifier
     * @param balance    the balance to set
     * @since 1.0.0
     */
    public void setBalanceInternal(@NotNull String currencyId, @NotNull BigDecimal balance) {
        Objects.requireNonNull(currencyId, "currencyId cannot be null");
        Objects.requireNonNull(balance, "balance cannot be null");
        lock.writeLock().lock();
        try {
            balances.put(currencyId, balance);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "EconomyAccount{" +
                "ownerId=" + ownerId +
                ", ownerName='" + ownerName + '\'' +
                ", balances=" + balances +
                ", createdAt=" + createdAt +
                '}';
    }
}
