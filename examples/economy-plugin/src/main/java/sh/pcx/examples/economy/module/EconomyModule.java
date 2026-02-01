/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.module;

import sh.pcx.examples.economy.EconomyExamplePlugin;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.examples.economy.storage.SQLEconomyStorage;
import sh.pcx.unified.economy.Account;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.impl.UnifiedEconomyService;
import sh.pcx.unified.modules.annotation.Configurable;
import sh.pcx.unified.modules.annotation.Listen;
import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.annotation.ModulePriority;
import sh.pcx.unified.modules.lifecycle.Healthy;
import sh.pcx.unified.modules.lifecycle.Initializable;
import sh.pcx.unified.modules.lifecycle.Reloadable;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main economy module that manages the economy system.
 *
 * <p>This module demonstrates:
 * <ul>
 *   <li>Module lifecycle management (init, reload, shutdown)</li>
 *   <li>Health monitoring for database connectivity</li>
 *   <li>Player join/quit event handling for account management</li>
 *   <li>Integration with the UnifiedEconomyService</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
@Module(
    name = "Economy",
    description = "Multi-currency economy system with database storage",
    version = "1.0.0",
    authors = {"Supatuck"},
    priority = ModulePriority.HIGH
)
@Listen
@Configurable
public class EconomyModule implements Initializable, Reloadable, Healthy {

    private static final Logger LOGGER = Logger.getLogger(EconomyModule.class.getName());

    private final EconomyExamplePlugin plugin;
    private final UnifiedEconomyService economyService;
    private final EconomyConfig config;
    private final SQLEconomyStorage storage;

    private volatile boolean healthy = true;
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds

    /**
     * Creates a new economy module.
     *
     * @param plugin         the plugin instance
     * @param economyService the economy service
     * @param config         the configuration
     * @param storage        the storage backend
     */
    public EconomyModule(
            @NotNull EconomyExamplePlugin plugin,
            @NotNull UnifiedEconomyService economyService,
            @NotNull EconomyConfig config,
            @NotNull SQLEconomyStorage storage
    ) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.config = config;
        this.storage = storage;
    }

    @Override
    public void init() {
        LOGGER.info("Initializing Economy Module...");

        // Log registered currencies
        Collection<Currency> currencies = economyService.getCurrencies();
        LOGGER.info("Registered currencies: " + currencies.size());
        for (Currency currency : currencies) {
            LOGGER.info("  - " + currency.getIdentifier() +
                       " (" + currency.getSymbol() + currency.getNamePlural() + ")");
        }

        // Start transaction cleanup task
        if (config.getSettings().getTransactionRetentionDays() > 0) {
            scheduleTransactionCleanup();
        }

        LOGGER.info("Economy Module initialized.");
    }

    @Override
    public void reload() {
        LOGGER.info("Reloading Economy Module...");

        // Currencies are reloaded via the main plugin
        // Here we just log the state
        LOGGER.info("Current currencies: " + economyService.getCurrencies().size());
        LOGGER.info("Current accounts: " + economyService.getAccountCount());

        LOGGER.info("Economy Module reloaded.");
    }

    @Override
    public void shutdown() {
        LOGGER.info("Shutting down Economy Module...");

        // Save all accounts before shutdown
        saveAllAccounts().join();

        LOGGER.info("Economy Module shut down.");
    }

    @Override
    public boolean isHealthy() {
        long now = System.currentTimeMillis();

        // Only check health periodically
        if (now - lastHealthCheck < HEALTH_CHECK_INTERVAL_MS) {
            return healthy;
        }

        lastHealthCheck = now;

        try {
            // Check database connectivity
            healthy = storage.healthCheck().get();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Health check failed", e);
            healthy = false;
        }

        return healthy;
    }

    @Override
    public String getHealthStatus() {
        if (healthy) {
            return "OK - Database connected, " + economyService.getAccountCount() + " accounts loaded";
        } else {
            return "UNHEALTHY - Database connection issues";
        }
    }

    /**
     * Handles a player joining the server.
     *
     * <p>Creates or loads the player's economy account.
     *
     * @param player the player who joined
     */
    public void onPlayerJoin(@NotNull UnifiedPlayer player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();

        // Create or get account async
        if (!economyService.hasAccount(playerId)) {
            economyService.createAccount(playerId, playerName)
                .thenAccept(account -> {
                    LOGGER.fine("Created account for " + playerName);

                    // Initialize all currencies
                    for (Currency currency : economyService.getCurrencies()) {
                        if (!account.hasCurrency(currency.getIdentifier())) {
                            // Account is initialized with default currency on creation
                            // Additional currencies start at their configured starting balance
                        }
                    }
                })
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Failed to create account for " + playerName, e);
                    return null;
                });
        } else {
            // Load account from storage if needed
            storage.loadAccount(playerId)
                .thenAccept(optAccount -> {
                    optAccount.ifPresent(account -> {
                        LOGGER.fine("Loaded account for " + playerName);
                    });
                })
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Failed to load account for " + playerName, e);
                    return null;
                });
        }
    }

    /**
     * Handles a player leaving the server.
     *
     * <p>Saves the player's account data.
     *
     * @param player the player who left
     */
    public void onPlayerQuit(@NotNull UnifiedPlayer player) {
        UUID playerId = player.getUniqueId();

        // Save account async
        economyService.getAccount(playerId).ifPresent(account -> {
            storage.saveAccount(account)
                .exceptionally(e -> {
                    LOGGER.log(Level.WARNING, "Failed to save account for " + player.getName(), e);
                    return null;
                });
        });
    }

    /**
     * Gets a player's total wealth across all currencies.
     *
     * <p>Converts all currencies to the default currency for comparison.
     *
     * @param playerId the player UUID
     * @return the total wealth in default currency
     */
    @NotNull
    public BigDecimal getTotalWealth(@NotNull UUID playerId) {
        Optional<Account> optAccount = economyService.getAccount(playerId);
        if (optAccount.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Account account = optAccount.get();
        Map<String, BigDecimal> balances = account.getAllBalances();

        // For this example, just return the default currency balance
        // A full implementation would convert other currencies
        return account.getBalance();
    }

    /**
     * Gets economy statistics.
     *
     * @return a map of statistic names to values
     */
    @NotNull
    public EconomyStats getStats() {
        return new EconomyStats(
            economyService.getAccountCount(),
            economyService.getCurrencies().size(),
            economyService.getTotalSupply(),
            healthy
        );
    }

    /**
     * Saves all accounts to storage.
     *
     * @return a future that completes when all saves are done
     */
    @NotNull
    public CompletableFuture<Void> saveAllAccounts() {
        LOGGER.info("Saving all accounts...");

        // Get all accounts and save them
        // In a real implementation, you'd iterate through all loaded accounts
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Schedules periodic transaction cleanup.
     */
    private void scheduleTransactionCleanup() {
        int retentionDays = config.getSettings().getTransactionRetentionDays();
        LOGGER.info("Transaction cleanup scheduled (retention: " + retentionDays + " days)");

        // In a real implementation, this would schedule a repeating task
        // For now, we just perform a one-time cleanup
        storage.clearOldTransactions(retentionDays)
            .thenAccept(count -> {
                if (count > 0) {
                    LOGGER.info("Cleaned up " + count + " old transactions");
                }
            });
    }

    /**
     * Economy statistics record.
     */
    public record EconomyStats(
        int accountCount,
        int currencyCount,
        BigDecimal totalSupply,
        boolean healthy
    ) {}
}
