/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy;

import sh.pcx.examples.economy.commands.BalanceCommand;
import sh.pcx.examples.economy.commands.BalanceTopCommand;
import sh.pcx.examples.economy.commands.EconomyAdminCommand;
import sh.pcx.examples.economy.commands.PayCommand;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.examples.economy.module.EconomyModule;
import sh.pcx.examples.economy.storage.SQLEconomyStorage;
import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.commands.core.CommandService;
import sh.pcx.unified.config.ConfigService;
import sh.pcx.unified.economy.Currency;
import sh.pcx.unified.economy.EconomyService;
import sh.pcx.unified.economy.impl.UnifiedEconomyService;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * Example economy plugin demonstrating the UnifiedPlugin-API.
 *
 * <p>This plugin showcases:
 * <ul>
 *   <li>Multi-currency economy system</li>
 *   <li>Database storage with HikariCP</li>
 *   <li>Annotation-based commands</li>
 *   <li>Interactive GUI for transfers</li>
 *   <li>Transaction logging</li>
 *   <li>Configuration management</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class EconomyExamplePlugin extends UnifiedPlugin {

    private EconomyConfig config;
    private EconomyModule economyModule;
    private SQLEconomyStorage storage;
    private UnifiedEconomyService economyService;

    @Override
    public void onLoad() {
        getLogger().info("Loading Economy Example Plugin...");

        // Save default configuration
        saveDefaultResource("config.yml");
    }

    @Override
    public void onEnable() {
        getLogger().info("Enabling Economy Example Plugin...");

        // Load configuration
        loadConfiguration();

        // Initialize database storage
        initializeStorage();

        // Initialize economy service
        initializeEconomy();

        // Register commands
        registerCommands();

        // Register the economy module
        registerModule();

        getLogger().info("Economy Example Plugin enabled successfully!");
        getLogger().info("Currencies registered: " + economyService.getCurrencies().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Economy Example Plugin...");

        // Save all data
        if (storage != null) {
            storage.shutdown().join();
        }

        getLogger().info("Economy Example Plugin disabled.");
    }

    @Override
    public void onReload() {
        getLogger().info("Reloading Economy Example Plugin...");

        // Reload configuration
        loadConfiguration();

        // Re-register currencies
        registerCurrencies();

        getLogger().info("Economy Example Plugin reloaded.");
    }

    /**
     * Loads the plugin configuration.
     */
    private void loadConfiguration() {
        ConfigService configService = getServices().get(ConfigService.class);
        config = configService.load(EconomyConfig.class, getDataFolder().resolve("config.yml"));
    }

    /**
     * Initializes the database storage.
     */
    private void initializeStorage() {
        storage = new SQLEconomyStorage(config.getDatabase(), getDataFolder());
        storage.initialize().join();
        getLogger().info("Database storage initialized: " + storage.getName());
    }

    /**
     * Initializes the economy service.
     */
    private void initializeEconomy() {
        // Create economy service with storage and event publisher
        economyService = new UnifiedEconomyService(storage, this::publishEvent);

        // Register configured currencies
        registerCurrencies();

        // Register as a service for other plugins
        getServices().register(EconomyService.class, economyService);
    }

    /**
     * Registers currencies from configuration.
     */
    private void registerCurrencies() {
        for (EconomyConfig.CurrencyConfig currencyConfig : config.getCurrencies()) {
            Currency currency = Currency.builder(currencyConfig.getId())
                    .names(currencyConfig.getNameSingular(), currencyConfig.getNamePlural())
                    .symbol(currencyConfig.getSymbol())
                    .decimals(currencyConfig.getDecimals())
                    .formatPattern(currencyConfig.getFormat())
                    .symbolPosition(currencyConfig.getSymbolPosition())
                    .startingBalance(BigDecimal.valueOf(currencyConfig.getStartingBalance()))
                    .minBalance(BigDecimal.valueOf(currencyConfig.getMinBalance()))
                    .maxBalance(BigDecimal.valueOf(currencyConfig.getMaxBalance()))
                    .defaultCurrency(currencyConfig.isDefaultCurrency())
                    .build();

            try {
                economyService.registerCurrency(currency);
                getLogger().info("Registered currency: " + currency.getIdentifier());
            } catch (IllegalArgumentException e) {
                // Currency already registered, skip
                getLogger().fine("Currency already registered: " + currency.getIdentifier());
            }
        }
    }

    /**
     * Registers commands with the command service.
     */
    private void registerCommands() {
        CommandService commands = getServices().get(CommandService.class);

        commands.register(new BalanceCommand(economyService, config));
        commands.register(new PayCommand(economyService, config));
        commands.register(new BalanceTopCommand(economyService, config));
        commands.register(new EconomyAdminCommand(economyService, config));

        getLogger().info("Commands registered.");
    }

    /**
     * Registers the economy module.
     */
    private void registerModule() {
        economyModule = new EconomyModule(this, economyService, config, storage);
        // Module registration would be handled by the module system
    }

    /**
     * Publishes an event to the event system.
     *
     * @param event the event to publish
     */
    private void publishEvent(Object event) {
        // In a real implementation, this would publish to the platform's event system
        getLogger().fine("Event published: " + event.getClass().getSimpleName());
    }

    /**
     * Returns the economy configuration.
     *
     * @return the economy config
     */
    @NotNull
    public EconomyConfig getEconomyConfig() {
        return config;
    }

    /**
     * Returns the economy service.
     *
     * @return the economy service
     */
    @NotNull
    public UnifiedEconomyService getEconomyService() {
        return economyService;
    }

    /**
     * Returns the economy storage.
     *
     * @return the storage backend
     */
    @NotNull
    public SQLEconomyStorage getStorage() {
        return storage;
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        // Platform-specific implementation would go here
        // For this example, we rely on the framework's default behavior
    }
}
