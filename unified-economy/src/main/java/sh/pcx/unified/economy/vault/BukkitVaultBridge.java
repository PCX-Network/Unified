/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.TransactionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bukkit-specific implementation of the Vault economy bridge.
 *
 * <p>This class implements Vault's Economy interface directly and registers
 * with Bukkit's service manager for full Vault compatibility.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // During plugin enable
 * BukkitVaultBridge bridge = new BukkitVaultBridge(economyService, plugin);
 * bridge.register();
 *
 * // During plugin disable
 * bridge.unregister();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class BukkitVaultBridge implements Economy {

    private static final Logger LOGGER = Logger.getLogger(BukkitVaultBridge.class.getName());

    private final EconomyService economyService;
    private final Plugin plugin;
    private boolean registered;

    /**
     * Creates a new Bukkit Vault bridge.
     *
     * @param economyService the economy service to bridge
     * @param plugin         the plugin registering the economy
     * @since 1.0.0
     */
    public BukkitVaultBridge(@NotNull EconomyService economyService, @NotNull Plugin plugin) {
        this.economyService = Objects.requireNonNull(economyService, "economyService cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.registered = false;
    }

    /**
     * Registers this bridge with Vault via Bukkit's service manager.
     *
     * @return true if registration was successful
     * @since 1.0.0
     */
    public boolean register() {
        if (registered) {
            LOGGER.warning("BukkitVaultBridge is already registered");
            return false;
        }

        try {
            Bukkit.getServicesManager().register(
                    Economy.class,
                    this,
                    plugin,
                    ServicePriority.Highest
            );
            registered = true;
            LOGGER.info("Registered UnifiedEconomy with Vault");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to register with Vault: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregisters this bridge from Vault.
     *
     * @return true if unregistration was successful
     * @since 1.0.0
     */
    public boolean unregister() {
        if (!registered) {
            return false;
        }

        try {
            Bukkit.getServicesManager().unregister(Economy.class, this);
            registered = false;
            LOGGER.info("Unregistered UnifiedEconomy from Vault");
            return true;
        } catch (Exception e) {
            LOGGER.severe("Failed to unregister from Vault: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns whether this bridge is registered.
     *
     * @return true if registered
     * @since 1.0.0
     */
    public boolean isRegistered() {
        return registered;
    }

    // ========================================================================
    // Economy Interface Implementation
    // ========================================================================

    @Override
    public boolean isEnabled() {
        return economyService.isAvailable();
    }

    @Override
    @NotNull
    public String getName() {
        return "UnifiedEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return economyService.getDefaultCurrency().getDecimals();
    }

    @Override
    @NotNull
    public String format(double amount) {
        return economyService.format(BigDecimal.valueOf(amount));
    }

    @Override
    @NotNull
    public String currencyNamePlural() {
        return economyService.getDefaultCurrency().getNamePlural();
    }

    @Override
    @NotNull
    public String currencyNameSingular() {
        return economyService.getDefaultCurrency().getNameSingular();
    }

    // ========================================================================
    // Account Operations (UUID-based - preferred)
    // ========================================================================

    @Override
    public boolean hasAccount(@NotNull OfflinePlayer player) {
        return economyService.hasAccount(player.getUniqueId());
    }

    @Override
    public boolean hasAccount(@NotNull OfflinePlayer player, @Nullable String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player) {
        return economyService.getBalance(player.getUniqueId()).doubleValue();
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player, @Nullable String worldName) {
        return getBalance(player);
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, double amount) {
        return economyService.has(player.getUniqueId(), BigDecimal.valueOf(amount));
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, @Nullable String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    @NotNull
    public EconomyResponse withdrawPlayer(@NotNull OfflinePlayer player, double amount) {
        TransactionResult result = economyService.withdraw(
                player.getUniqueId(),
                BigDecimal.valueOf(amount),
                "Vault withdrawal"
        );
        return toEconomyResponse(result, amount);
    }

    @Override
    @NotNull
    public EconomyResponse withdrawPlayer(@NotNull OfflinePlayer player, @Nullable String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @NotNull
    public EconomyResponse depositPlayer(@NotNull OfflinePlayer player, double amount) {
        TransactionResult result = economyService.deposit(
                player.getUniqueId(),
                BigDecimal.valueOf(amount),
                "Vault deposit"
        );
        return toEconomyResponse(result, amount);
    }

    @Override
    @NotNull
    public EconomyResponse depositPlayer(@NotNull OfflinePlayer player, @Nullable String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(@NotNull OfflinePlayer player) {
        if (economyService.hasAccount(player.getUniqueId())) {
            return false;
        }
        String name = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        economyService.createAccount(player.getUniqueId(), name);
        return true;
    }

    @Override
    public boolean createPlayerAccount(@NotNull OfflinePlayer player, @Nullable String worldName) {
        return createPlayerAccount(player);
    }

    // ========================================================================
    // Account Operations (String-based - deprecated but required)
    // ========================================================================

    @Override
    @Deprecated
    public boolean hasAccount(@NotNull String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }

    @Override
    @Deprecated
    public boolean hasAccount(@NotNull String playerName, @Nullable String worldName) {
        return hasAccount(playerName);
    }

    @Override
    @Deprecated
    public double getBalance(@NotNull String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalance(player);
    }

    @Override
    @Deprecated
    public double getBalance(@NotNull String playerName, @Nullable String worldName) {
        return getBalance(playerName);
    }

    @Override
    @Deprecated
    public boolean has(@NotNull String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return has(player, amount);
    }

    @Override
    @Deprecated
    public boolean has(@NotNull String playerName, @Nullable String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse withdrawPlayer(@NotNull String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse withdrawPlayer(@NotNull String playerName, @Nullable String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse depositPlayer(@NotNull String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse depositPlayer(@NotNull String playerName, @Nullable String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(@NotNull String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(@NotNull String playerName, @Nullable String worldName) {
        return createPlayerAccount(playerName);
    }

    // ========================================================================
    // Bank Operations (Not Supported)
    // ========================================================================

    @Override
    @NotNull
    public EconomyResponse createBank(@NotNull String name, @NotNull OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse createBank(@NotNull String name, @NotNull String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse deleteBank(@NotNull String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse bankBalance(@NotNull String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse bankHas(@NotNull String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse bankWithdraw(@NotNull String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse bankDeposit(@NotNull String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse isBankOwner(@NotNull String name, @NotNull OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse isBankOwner(@NotNull String name, @NotNull String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public EconomyResponse isBankMember(@NotNull String name, @NotNull OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @Deprecated
    @NotNull
    public EconomyResponse isBankMember(@NotNull String name, @NotNull String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    @NotNull
    public List<String> getBanks() {
        return List.of();
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Converts a TransactionResult to a Vault EconomyResponse.
     *
     * @param result the transaction result
     * @param amount the original amount
     * @return the economy response
     */
    @NotNull
    private EconomyResponse toEconomyResponse(@NotNull TransactionResult result, double amount) {
        if (result.isSuccess()) {
            return new EconomyResponse(
                    amount,
                    result.newBalance().doubleValue(),
                    EconomyResponse.ResponseType.SUCCESS,
                    null
            );
        } else {
            EconomyResponse.ResponseType responseType = switch (result.status()) {
                case INSUFFICIENT_FUNDS -> EconomyResponse.ResponseType.FAILURE;
                case ACCOUNT_NOT_FOUND -> EconomyResponse.ResponseType.FAILURE;
                default -> EconomyResponse.ResponseType.FAILURE;
            };

            return new EconomyResponse(
                    amount,
                    result.previousBalance().doubleValue(),
                    responseType,
                    result.errorMessage().orElse("Transaction failed")
            );
        }
    }
}
