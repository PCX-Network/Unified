/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.vault;

import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.TransactionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridge between UnifiedEconomy and Vault's Economy interface.
 *
 * <p>This class provides Vault compatibility by implementing Vault's Economy
 * interface and delegating to the UnifiedEconomy service. This allows plugins
 * that use Vault to work seamlessly with UnifiedEconomy.
 *
 * <h2>Registration</h2>
 * <pre>{@code
 * // Register with Vault
 * VaultBridge bridge = new VaultBridge(economyService, plugin);
 * bridge.register();
 *
 * // Unregister when done
 * bridge.unregister();
 * }</pre>
 *
 * <h2>Compatibility Notes</h2>
 * <ul>
 *   <li>Bank accounts are not supported (returns failure)</li>
 *   <li>Only the default currency is exposed through Vault</li>
 *   <li>Player name lookups use cached data</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class VaultBridge {

    private static final Logger LOGGER = Logger.getLogger(VaultBridge.class.getName());

    private final EconomyService economyService;
    private final String pluginName;
    private final PlayerNameResolver nameResolver;

    private boolean registered;

    /**
     * Creates a new Vault bridge.
     *
     * @param economyService the economy service to bridge
     * @param pluginName     the plugin name for Vault registration
     * @param nameResolver   the player name resolver
     * @since 1.0.0
     */
    public VaultBridge(
            @NotNull EconomyService economyService,
            @NotNull String pluginName,
            @NotNull PlayerNameResolver nameResolver
    ) {
        this.economyService = economyService;
        this.pluginName = pluginName;
        this.nameResolver = nameResolver;
        this.registered = false;
    }

    /**
     * Checks if the economy is enabled.
     *
     * @return true if the economy is available
     */
    public boolean isEnabled() {
        return economyService.isAvailable();
    }

    /**
     * Gets the name of this economy implementation.
     *
     * @return the economy name
     */
    @NotNull
    public String getName() {
        return "UnifiedEconomy";
    }

    /**
     * Returns whether this economy supports banks.
     *
     * @return false (banks are not supported)
     */
    public boolean hasBankSupport() {
        return false;
    }

    /**
     * Gets the number of decimal places used.
     *
     * @return the number of decimal places
     */
    public int fractionalDigits() {
        return economyService.getDefaultCurrency().getDecimals();
    }

    /**
     * Formats an amount using the currency format.
     *
     * @param amount the amount to format
     * @return the formatted string
     */
    @NotNull
    public String format(double amount) {
        return economyService.format(BigDecimal.valueOf(amount));
    }

    /**
     * Gets the singular currency name.
     *
     * @return the singular name
     */
    @NotNull
    public String currencyNameSingular() {
        return economyService.getDefaultCurrency().getNameSingular();
    }

    /**
     * Gets the plural currency name.
     *
     * @return the plural name
     */
    @NotNull
    public String currencyNamePlural() {
        return economyService.getDefaultCurrency().getNamePlural();
    }

    // ========================================================================
    // Account Operations
    // ========================================================================

    /**
     * Checks if a player has an account.
     *
     * @param playerId the player UUID
     * @return true if the player has an account
     */
    public boolean hasAccount(@NotNull UUID playerId) {
        return economyService.hasAccount(playerId);
    }

    /**
     * Checks if a player has an account (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @return true if the player has an account
     */
    public boolean hasAccount(@NotNull UUID playerId, @Nullable String worldName) {
        return hasAccount(playerId);
    }

    /**
     * Gets the balance for a player.
     *
     * @param playerId the player UUID
     * @return the balance
     */
    public double getBalance(@NotNull UUID playerId) {
        return economyService.getBalance(playerId).doubleValue();
    }

    /**
     * Gets the balance for a player (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @return the balance
     */
    public double getBalance(@NotNull UUID playerId, @Nullable String worldName) {
        return getBalance(playerId);
    }

    /**
     * Checks if a player has at least a certain amount.
     *
     * @param playerId the player UUID
     * @param amount   the amount to check
     * @return true if the player has enough
     */
    public boolean has(@NotNull UUID playerId, double amount) {
        return economyService.has(playerId, BigDecimal.valueOf(amount));
    }

    /**
     * Checks if a player has at least a certain amount (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @param amount    the amount to check
     * @return true if the player has enough
     */
    public boolean has(@NotNull UUID playerId, @Nullable String worldName, double amount) {
        return has(playerId, amount);
    }

    /**
     * Withdraws from a player's account.
     *
     * @param playerId the player UUID
     * @param amount   the amount to withdraw
     * @return the transaction response
     */
    @NotNull
    public VaultResponse withdraw(@NotNull UUID playerId, double amount) {
        TransactionResult result = economyService.withdraw(playerId, BigDecimal.valueOf(amount));
        return toVaultResponse(result);
    }

    /**
     * Withdraws from a player's account (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @param amount    the amount to withdraw
     * @return the transaction response
     */
    @NotNull
    public VaultResponse withdraw(@NotNull UUID playerId, @Nullable String worldName, double amount) {
        return withdraw(playerId, amount);
    }

    /**
     * Deposits to a player's account.
     *
     * @param playerId the player UUID
     * @param amount   the amount to deposit
     * @return the transaction response
     */
    @NotNull
    public VaultResponse deposit(@NotNull UUID playerId, double amount) {
        TransactionResult result = economyService.deposit(playerId, BigDecimal.valueOf(amount));
        return toVaultResponse(result);
    }

    /**
     * Deposits to a player's account (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @param amount    the amount to deposit
     * @return the transaction response
     */
    @NotNull
    public VaultResponse deposit(@NotNull UUID playerId, @Nullable String worldName, double amount) {
        return deposit(playerId, amount);
    }

    /**
     * Creates an account for a player.
     *
     * @param playerId the player UUID
     * @return true if the account was created
     */
    public boolean createAccount(@NotNull UUID playerId) {
        if (economyService.hasAccount(playerId)) {
            return false;
        }
        String name = nameResolver.resolveName(playerId);
        economyService.createAccount(playerId, name);
        return true;
    }

    /**
     * Creates an account for a player (with world parameter, ignored).
     *
     * @param playerId  the player UUID
     * @param worldName the world name (ignored)
     * @return true if the account was created
     */
    public boolean createAccount(@NotNull UUID playerId, @Nullable String worldName) {
        return createAccount(playerId);
    }

    // ========================================================================
    // Bank Operations (Not Supported)
    // ========================================================================

    /**
     * Creates a bank (not supported).
     *
     * @param name    the bank name
     * @param ownerId the owner UUID
     * @return failure response
     */
    @NotNull
    public VaultResponse createBank(@NotNull String name, @NotNull UUID ownerId) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Deletes a bank (not supported).
     *
     * @param name the bank name
     * @return failure response
     */
    @NotNull
    public VaultResponse deleteBank(@NotNull String name) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Gets a bank's balance (not supported).
     *
     * @param name the bank name
     * @return failure response
     */
    @NotNull
    public VaultResponse bankBalance(@NotNull String name) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Checks if a bank has enough (not supported).
     *
     * @param name   the bank name
     * @param amount the amount
     * @return failure response
     */
    @NotNull
    public VaultResponse bankHas(@NotNull String name, double amount) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Withdraws from a bank (not supported).
     *
     * @param name   the bank name
     * @param amount the amount
     * @return failure response
     */
    @NotNull
    public VaultResponse bankWithdraw(@NotNull String name, double amount) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Deposits to a bank (not supported).
     *
     * @param name   the bank name
     * @param amount the amount
     * @return failure response
     */
    @NotNull
    public VaultResponse bankDeposit(@NotNull String name, double amount) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Checks if a player is a bank owner (not supported).
     *
     * @param name     the bank name
     * @param playerId the player UUID
     * @return failure response
     */
    @NotNull
    public VaultResponse isBankOwner(@NotNull String name, @NotNull UUID playerId) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Checks if a player is a bank member (not supported).
     *
     * @param name     the bank name
     * @param playerId the player UUID
     * @return failure response
     */
    @NotNull
    public VaultResponse isBankMember(@NotNull String name, @NotNull UUID playerId) {
        return VaultResponse.failure("Banks are not supported");
    }

    /**
     * Gets all bank names (not supported).
     *
     * @return empty list
     */
    @NotNull
    public List<String> getBanks() {
        return List.of();
    }

    // ========================================================================
    // Registration
    // ========================================================================

    /**
     * Registers this bridge with Vault.
     *
     * <p>This method should be called during plugin enable. The actual
     * Vault registration code is platform-specific and should be
     * implemented in a subclass.
     *
     * @return true if registration was successful
     * @since 1.0.0
     */
    public boolean register() {
        if (registered) {
            LOGGER.warning("VaultBridge is already registered");
            return false;
        }

        // Subclasses should override this to perform actual Vault registration
        LOGGER.info("VaultBridge registered for " + pluginName);
        registered = true;
        return true;
    }

    /**
     * Unregisters this bridge from Vault.
     *
     * <p>This method should be called during plugin disable.
     *
     * @return true if unregistration was successful
     * @since 1.0.0
     */
    public boolean unregister() {
        if (!registered) {
            return false;
        }

        // Subclasses should override this to perform actual Vault unregistration
        LOGGER.info("VaultBridge unregistered for " + pluginName);
        registered = false;
        return true;
    }

    /**
     * Returns whether this bridge is registered with Vault.
     *
     * @return true if registered
     * @since 1.0.0
     */
    public boolean isRegistered() {
        return registered;
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Converts a TransactionResult to a VaultResponse.
     *
     * @param result the transaction result
     * @return the vault response
     */
    @NotNull
    private VaultResponse toVaultResponse(@NotNull TransactionResult result) {
        if (result.isSuccess()) {
            return VaultResponse.success(
                    result.newBalance().doubleValue(),
                    "Transaction successful"
            );
        } else {
            return VaultResponse.failure(result.errorMessage().orElse("Transaction failed"));
        }
    }

    /**
     * Response object for Vault operations.
     *
     * @since 1.0.0
     */
    public record VaultResponse(
            boolean success,
            double balance,
            @NotNull String message
    ) {

        /**
         * Creates a successful response.
         *
         * @param balance the new balance
         * @param message the success message
         * @return the response
         */
        @NotNull
        public static VaultResponse success(double balance, @NotNull String message) {
            return new VaultResponse(true, balance, message);
        }

        /**
         * Creates a failure response.
         *
         * @param message the error message
         * @return the response
         */
        @NotNull
        public static VaultResponse failure(@NotNull String message) {
            return new VaultResponse(false, 0.0, message);
        }
    }

    /**
     * Interface for resolving player names from UUIDs.
     *
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface PlayerNameResolver {

        /**
         * Resolves a player name from their UUID.
         *
         * @param playerId the player UUID
         * @return the player name, or a fallback if not found
         */
        @NotNull
        String resolveName(@NotNull UUID playerId);
    }
}
