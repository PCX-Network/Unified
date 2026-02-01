/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an economy account for a player or entity.
 *
 * <p>An account holds balances for one or more currencies and provides
 * methods for checking and modifying those balances. Each player has
 * a single account that can hold multiple currency balances.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Optional<Account> optAccount = economy.getAccount(player.getUniqueId());
 * optAccount.ifPresent(account -> {
 *     // Get balance
 *     BigDecimal balance = account.getBalance();
 *
 *     // Check if they can afford something
 *     if (account.has(BigDecimal.valueOf(100))) {
 *         // Withdraw funds
 *         account.withdraw(BigDecimal.valueOf(100), "Shop purchase");
 *     }
 *
 *     // Get balance in another currency
 *     BigDecimal gems = account.getBalance("gems");
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Account implementations should be thread-safe. Balance modifications
 * should be atomic to prevent race conditions.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyService
 * @see Transaction
 */
public interface Account {

    /**
     * Returns the unique identifier of the account owner.
     *
     * <p>For player accounts, this is the player's UUID.
     *
     * @return the owner's UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getOwnerId();

    /**
     * Returns the display name of the account owner.
     *
     * <p>For player accounts, this is typically the player's username.
     *
     * @return the owner's display name
     * @since 1.0.0
     */
    @NotNull
    String getOwnerName();

    /**
     * Returns when this account was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getCreatedAt();

    /**
     * Returns when this account was last modified.
     *
     * @return the last modification timestamp
     * @since 1.0.0
     */
    @NotNull
    Instant getLastModified();

    // ========================================================================
    // Balance Operations (Default Currency)
    // ========================================================================

    /**
     * Returns the balance in the default currency.
     *
     * @return the account balance
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getBalance();

    /**
     * Checks if the account has at least the specified amount in the default currency.
     *
     * @param amount the amount to check
     * @return true if the balance is sufficient
     * @since 1.0.0
     */
    boolean has(@NotNull BigDecimal amount);

    /**
     * Deposits an amount into this account in the default currency.
     *
     * @param amount the amount to deposit
     * @param reason the reason for the deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult deposit(@NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Deposits an amount into this account.
     *
     * @param amount the amount to deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    default TransactionResult deposit(@NotNull BigDecimal amount) {
        return deposit(amount, null);
    }

    /**
     * Withdraws an amount from this account in the default currency.
     *
     * @param amount the amount to withdraw
     * @param reason the reason for the withdrawal
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult withdraw(@NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Withdraws an amount from this account.
     *
     * @param amount the amount to withdraw
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    default TransactionResult withdraw(@NotNull BigDecimal amount) {
        return withdraw(amount, null);
    }

    /**
     * Sets the balance in the default currency.
     *
     * @param amount the new balance
     * @param reason the reason for setting the balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult setBalance(@NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Sets the balance in the default currency.
     *
     * @param amount the new balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    default TransactionResult setBalance(@NotNull BigDecimal amount) {
        return setBalance(amount, null);
    }

    // ========================================================================
    // Balance Operations (Specific Currency)
    // ========================================================================

    /**
     * Returns the balance in a specific currency.
     *
     * @param currencyId the currency identifier
     * @return the balance in that currency
     * @since 1.0.0
     */
    @NotNull
    BigDecimal getBalance(@NotNull String currencyId);

    /**
     * Checks if the account has at least the specified amount in a currency.
     *
     * @param amount     the amount to check
     * @param currencyId the currency identifier
     * @return true if the balance is sufficient
     * @since 1.0.0
     */
    boolean has(@NotNull BigDecimal amount, @NotNull String currencyId);

    /**
     * Deposits an amount into this account in a specific currency.
     *
     * @param amount     the amount to deposit
     * @param currencyId the currency identifier
     * @param reason     the reason for the deposit
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult deposit(@NotNull BigDecimal amount, @NotNull String currencyId, @Nullable String reason);

    /**
     * Withdraws an amount from this account in a specific currency.
     *
     * @param amount     the amount to withdraw
     * @param currencyId the currency identifier
     * @param reason     the reason for the withdrawal
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult withdraw(@NotNull BigDecimal amount, @NotNull String currencyId, @Nullable String reason);

    /**
     * Sets the balance in a specific currency.
     *
     * @param amount     the new balance
     * @param currencyId the currency identifier
     * @param reason     the reason for setting the balance
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult setBalance(@NotNull BigDecimal amount, @NotNull String currencyId, @Nullable String reason);

    // ========================================================================
    // Multi-Currency Support
    // ========================================================================

    /**
     * Returns all currency balances for this account.
     *
     * @return an unmodifiable map of currency identifier to balance
     * @since 1.0.0
     */
    @NotNull
    Map<String, BigDecimal> getAllBalances();

    /**
     * Checks if this account has a balance in the specified currency.
     *
     * <p>Returns true even if the balance is zero. Returns false only if
     * no balance record exists for that currency.
     *
     * @param currencyId the currency identifier
     * @return true if a balance exists for that currency
     * @since 1.0.0
     */
    boolean hasCurrency(@NotNull String currencyId);

    // ========================================================================
    // Transfer Operations
    // ========================================================================

    /**
     * Transfers funds from this account to another.
     *
     * @param to     the destination account
     * @param amount the amount to transfer
     * @param reason the reason for the transfer
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult transferTo(@NotNull Account to, @NotNull BigDecimal amount, @Nullable String reason);

    /**
     * Transfers funds from this account to another in a specific currency.
     *
     * @param to         the destination account
     * @param amount     the amount to transfer
     * @param currencyId the currency identifier
     * @param reason     the reason for the transfer
     * @return the transaction result
     * @since 1.0.0
     */
    @NotNull
    TransactionResult transferTo(
            @NotNull Account to,
            @NotNull BigDecimal amount,
            @NotNull String currencyId,
            @Nullable String reason
    );

    // ========================================================================
    // Metadata
    // ========================================================================

    /**
     * Returns a custom metadata value stored on this account.
     *
     * @param key the metadata key
     * @return an Optional containing the value if present
     * @since 1.0.0
     */
    @NotNull
    Optional<String> getMetadata(@NotNull String key);

    /**
     * Sets a custom metadata value on this account.
     *
     * @param key   the metadata key
     * @param value the metadata value, or null to remove
     * @since 1.0.0
     */
    void setMetadata(@NotNull String key, @Nullable String value);

    /**
     * Removes a custom metadata value from this account.
     *
     * @param key the metadata key
     * @return true if the key was present and removed
     * @since 1.0.0
     */
    boolean removeMetadata(@NotNull String key);

    /**
     * Returns all metadata for this account.
     *
     * @return an unmodifiable map of metadata
     * @since 1.0.0
     */
    @NotNull
    Map<String, String> getAllMetadata();
}
