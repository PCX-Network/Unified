/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.impl;

import sh.pcx.unified.economy.Transaction;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of {@link TransactionProcessor}.
 *
 * <p>This implementation stores transactions in memory and is suitable for
 * testing or servers that don't require persistent transaction history.
 *
 * <p>Transactions are automatically pruned based on a maximum count per account.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class InMemoryTransactionProcessor implements TransactionProcessor {

    private static final int DEFAULT_MAX_TRANSACTIONS_PER_ACCOUNT = 1000;

    private final Map<UUID, List<Transaction>> transactionsByAccount;
    private final Map<UUID, Transaction> transactionsById;
    private final int maxTransactionsPerAccount;
    private final boolean enabled;

    /**
     * Creates a new in-memory transaction processor.
     *
     * @since 1.0.0
     */
    public InMemoryTransactionProcessor() {
        this(DEFAULT_MAX_TRANSACTIONS_PER_ACCOUNT, true);
    }

    /**
     * Creates a new in-memory transaction processor with custom settings.
     *
     * @param maxTransactionsPerAccount the maximum transactions to keep per account
     * @param enabled                   whether to enable transaction recording
     * @since 1.0.0
     */
    public InMemoryTransactionProcessor(int maxTransactionsPerAccount, boolean enabled) {
        this.maxTransactionsPerAccount = maxTransactionsPerAccount;
        this.enabled = enabled;
        this.transactionsByAccount = new ConcurrentHashMap<>();
        this.transactionsById = new ConcurrentHashMap<>();
    }

    @Override
    public void recordTransaction(@NotNull Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction cannot be null");

        if (!enabled) {
            return;
        }

        // Store by ID
        transactionsById.put(transaction.getTransactionId(), transaction);

        // Store by account
        List<Transaction> accountTransactions = transactionsByAccount.computeIfAbsent(
                transaction.getAccountId(),
                k -> new CopyOnWriteArrayList<>()
        );

        accountTransactions.addFirst(transaction);

        // Prune old transactions if needed
        while (accountTransactions.size() > maxTransactionsPerAccount) {
            Transaction removed = accountTransactions.removeLast();
            transactionsById.remove(removed.getTransactionId());
        }

        // Also store for target account in transfers
        transaction.getTargetAccountId().ifPresent(targetId -> {
            List<Transaction> targetTransactions = transactionsByAccount.computeIfAbsent(
                    targetId,
                    k -> new CopyOnWriteArrayList<>()
            );
            targetTransactions.addFirst(transaction);

            while (targetTransactions.size() > maxTransactionsPerAccount) {
                targetTransactions.removeLast();
            }
        });
    }

    @Override
    @NotNull
    public List<Transaction> getHistory(@NotNull UUID accountId, int limit) {
        Objects.requireNonNull(accountId, "accountId cannot be null");

        if (!enabled) {
            return Collections.emptyList();
        }

        List<Transaction> transactions = transactionsByAccount.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        int count = Math.min(limit, transactions.size());
        return new ArrayList<>(transactions.subList(0, count));
    }

    @Override
    @NotNull
    public List<Transaction> getHistory(@NotNull UUID accountId, @NotNull String currencyId, int limit) {
        Objects.requireNonNull(accountId, "accountId cannot be null");
        Objects.requireNonNull(currencyId, "currencyId cannot be null");

        if (!enabled) {
            return Collections.emptyList();
        }

        List<Transaction> transactions = transactionsByAccount.get(accountId);
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        return transactions.stream()
                .filter(tx -> tx.getCurrencyId().equals(currencyId))
                .limit(limit)
                .toList();
    }

    @Override
    @NotNull
    public Optional<Transaction> getTransaction(@NotNull UUID transactionId) {
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        return Optional.ofNullable(transactionsById.get(transactionId));
    }

    @Override
    public int clearOldTransactions(int olderThanDays) {
        if (!enabled) {
            return 0;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(olderThanDays));
        int cleared = 0;

        for (List<Transaction> transactions : transactionsByAccount.values()) {
            int sizeBefore = transactions.size();
            transactions.removeIf(tx -> tx.getTimestamp().isBefore(cutoff));
            cleared += sizeBefore - transactions.size();
        }

        // Also remove from ID map
        transactionsById.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp().isBefore(cutoff));

        return cleared;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the total number of transactions stored.
     *
     * @return the transaction count
     * @since 1.0.0
     */
    public int getTransactionCount() {
        return transactionsById.size();
    }

    /**
     * Clears all stored transactions.
     *
     * @since 1.0.0
     */
    public void clear() {
        transactionsByAccount.clear();
        transactionsById.clear();
    }
}
