/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.config;

import sh.pcx.unified.economy.Currency;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the economy plugin.
 *
 * <p>Uses Sponge Configurate for type-safe YAML configuration.
 *
 * @author Supatuck
 * @since 1.0.0
 */
@ConfigSerializable
public class EconomyConfig {

    @Comment("Database configuration for storing economy data")
    @Setting("database")
    private DatabaseConfig database = new DatabaseConfig();

    @Comment("Currency definitions")
    @Setting("currencies")
    private List<CurrencyConfig> currencies = createDefaultCurrencies();

    @Comment("General economy settings")
    @Setting("settings")
    private EconomySettings settings = new EconomySettings();

    @Comment("Message templates")
    @Setting("messages")
    private Messages messages = new Messages();

    /**
     * Creates the default currency configurations.
     */
    private static List<CurrencyConfig> createDefaultCurrencies() {
        List<CurrencyConfig> defaults = new ArrayList<>();

        // Default dollar currency
        CurrencyConfig dollars = new CurrencyConfig();
        dollars.id = "dollars";
        dollars.nameSingular = "Dollar";
        dollars.namePlural = "Dollars";
        dollars.symbol = "$";
        dollars.decimals = 2;
        dollars.format = "#,##0.00";
        dollars.symbolPosition = Currency.SymbolPosition.PREFIX;
        dollars.startingBalance = 100.0;
        dollars.defaultCurrency = true;
        defaults.add(dollars);

        // Coins currency (no decimals)
        CurrencyConfig coins = new CurrencyConfig();
        coins.id = "coins";
        coins.nameSingular = "Coin";
        coins.namePlural = "Coins";
        coins.symbol = "C";
        coins.decimals = 0;
        coins.format = "#,##0";
        coins.symbolPosition = Currency.SymbolPosition.SUFFIX_SPACE;
        coins.startingBalance = 0.0;
        coins.defaultCurrency = false;
        defaults.add(coins);

        // Premium gems currency
        CurrencyConfig gems = new CurrencyConfig();
        gems.id = "gems";
        gems.nameSingular = "Gem";
        gems.namePlural = "Gems";
        gems.symbol = "\u2666"; // Diamond symbol
        gems.decimals = 0;
        gems.format = "#,##0";
        gems.symbolPosition = Currency.SymbolPosition.PREFIX_SPACE;
        gems.startingBalance = 0.0;
        gems.maxBalance = 100000;
        gems.defaultCurrency = false;
        defaults.add(gems);

        return defaults;
    }

    @NotNull
    public DatabaseConfig getDatabase() {
        return database;
    }

    @NotNull
    public List<CurrencyConfig> getCurrencies() {
        return currencies;
    }

    @NotNull
    public EconomySettings getSettings() {
        return settings;
    }

    @NotNull
    public Messages getMessages() {
        return messages;
    }

    /**
     * Database configuration.
     */
    @ConfigSerializable
    public static class DatabaseConfig {

        @Comment("Database type: SQLITE, MYSQL, MARIADB, POSTGRESQL")
        @Setting("type")
        private String type = "SQLITE";

        @Comment("Database host (for MySQL/MariaDB/PostgreSQL)")
        @Setting("host")
        private String host = "localhost";

        @Comment("Database port")
        @Setting("port")
        private int port = 3306;

        @Comment("Database name")
        @Setting("database")
        private String database = "economy";

        @Comment("Database username")
        @Setting("username")
        private String username = "root";

        @Comment("Database password")
        @Setting("password")
        private String password = "";

        @Comment("Table prefix")
        @Setting("table-prefix")
        private String tablePrefix = "eco_";

        @Comment("Connection pool settings")
        @Setting("pool")
        private PoolConfig pool = new PoolConfig();

        @NotNull
        public String getType() {
            return type;
        }

        @NotNull
        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @NotNull
        public String getDatabase() {
            return database;
        }

        @NotNull
        public String getUsername() {
            return username;
        }

        @NotNull
        public String getPassword() {
            return password;
        }

        @NotNull
        public String getTablePrefix() {
            return tablePrefix;
        }

        @NotNull
        public PoolConfig getPool() {
            return pool;
        }
    }

    /**
     * Connection pool configuration.
     */
    @ConfigSerializable
    public static class PoolConfig {

        @Comment("Maximum number of connections in the pool")
        @Setting("maximum-pool-size")
        private int maximumPoolSize = 10;

        @Comment("Minimum number of idle connections")
        @Setting("minimum-idle")
        private int minimumIdle = 2;

        @Comment("Connection timeout in milliseconds")
        @Setting("connection-timeout")
        private long connectionTimeout = 30000;

        @Comment("Maximum connection lifetime in milliseconds")
        @Setting("max-lifetime")
        private long maxLifetime = 1800000;

        @Comment("Idle timeout in milliseconds")
        @Setting("idle-timeout")
        private long idleTimeout = 600000;

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }
    }

    /**
     * Currency configuration.
     */
    @ConfigSerializable
    public static class CurrencyConfig {

        @Comment("Unique identifier for the currency")
        @Setting("id")
        private String id;

        @Comment("Singular name (e.g., 'Dollar')")
        @Setting("name-singular")
        private String nameSingular;

        @Comment("Plural name (e.g., 'Dollars')")
        @Setting("name-plural")
        private String namePlural;

        @Comment("Currency symbol (e.g., '$')")
        @Setting("symbol")
        private String symbol = "";

        @Comment("Number of decimal places (0-8)")
        @Setting("decimals")
        private int decimals = 2;

        @Comment("Display format pattern")
        @Setting("format")
        private String format = "#,##0.00";

        @Comment("Symbol position: PREFIX, SUFFIX, PREFIX_SPACE, SUFFIX_SPACE, NONE")
        @Setting("symbol-position")
        private Currency.SymbolPosition symbolPosition = Currency.SymbolPosition.PREFIX;

        @Comment("Starting balance for new accounts")
        @Setting("starting-balance")
        private double startingBalance = 0.0;

        @Comment("Minimum allowed balance (negative to allow debt)")
        @Setting("min-balance")
        private double minBalance = 0.0;

        @Comment("Maximum allowed balance")
        @Setting("max-balance")
        private double maxBalance = 999999999999999.0;

        @Comment("Whether this is the default currency")
        @Setting("default")
        private boolean defaultCurrency = false;

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public String getNameSingular() {
            return nameSingular;
        }

        @NotNull
        public String getNamePlural() {
            return namePlural;
        }

        @NotNull
        public String getSymbol() {
            return symbol;
        }

        public int getDecimals() {
            return decimals;
        }

        @NotNull
        public String getFormat() {
            return format;
        }

        @NotNull
        public Currency.SymbolPosition getSymbolPosition() {
            return symbolPosition;
        }

        public double getStartingBalance() {
            return startingBalance;
        }

        public double getMinBalance() {
            return minBalance;
        }

        public double getMaxBalance() {
            return maxBalance;
        }

        public boolean isDefaultCurrency() {
            return defaultCurrency;
        }
    }

    /**
     * General economy settings.
     */
    @ConfigSerializable
    public static class EconomySettings {

        @Comment("Enable transaction logging")
        @Setting("log-transactions")
        private boolean logTransactions = true;

        @Comment("Days to retain transaction history (0 = forever)")
        @Setting("transaction-retention-days")
        private int transactionRetentionDays = 30;

        @Comment("Minimum transfer amount")
        @Setting("min-transfer-amount")
        private double minTransferAmount = 0.01;

        @Comment("Maximum transfer amount (0 = unlimited)")
        @Setting("max-transfer-amount")
        private double maxTransferAmount = 0.0;

        @Comment("Transfer fee percentage (0-100)")
        @Setting("transfer-fee-percent")
        private double transferFeePercent = 0.0;

        @Comment("Number of entries to show in baltop")
        @Setting("baltop-entries")
        private int baltopEntries = 10;

        @Comment("Cache duration for baltop in seconds")
        @Setting("baltop-cache-duration")
        private int baltopCacheDuration = 60;

        @Comment("Enable economy GUI")
        @Setting("enable-gui")
        private boolean enableGui = true;

        public boolean isLogTransactions() {
            return logTransactions;
        }

        public int getTransactionRetentionDays() {
            return transactionRetentionDays;
        }

        public double getMinTransferAmount() {
            return minTransferAmount;
        }

        public double getMaxTransferAmount() {
            return maxTransferAmount;
        }

        public double getTransferFeePercent() {
            return transferFeePercent;
        }

        public int getBaltopEntries() {
            return baltopEntries;
        }

        public int getBaltopCacheDuration() {
            return baltopCacheDuration;
        }

        public boolean isEnableGui() {
            return enableGui;
        }
    }

    /**
     * Message templates for the economy plugin.
     */
    @ConfigSerializable
    public static class Messages {

        @Comment("Prefix for all messages")
        @Setting("prefix")
        private String prefix = "<gradient:#FFD700:#FFA500>[Economy]</gradient> ";

        @Comment("Balance display message")
        @Setting("balance")
        private String balance = "<gray>Your balance: <gold>{amount}</gold>";

        @Comment("Balance display for specific currency")
        @Setting("balance-currency")
        private String balanceCurrency = "<gray>Your <yellow>{currency}</yellow> balance: <gold>{amount}</gold>";

        @Comment("Other player's balance")
        @Setting("balance-other")
        private String balanceOther = "<gray>{player}'s balance: <gold>{amount}</gold>";

        @Comment("Payment sent message")
        @Setting("pay-sent")
        private String paySent = "<green>You sent <gold>{amount}</gold> to <yellow>{player}</yellow>";

        @Comment("Payment received message")
        @Setting("pay-received")
        private String payReceived = "<green>You received <gold>{amount}</gold> from <yellow>{player}</yellow>";

        @Comment("Insufficient funds message")
        @Setting("insufficient-funds")
        private String insufficientFunds = "<red>You don't have enough funds! Balance: <gold>{balance}</gold>";

        @Comment("Invalid amount message")
        @Setting("invalid-amount")
        private String invalidAmount = "<red>Invalid amount specified!";

        @Comment("Cannot pay self message")
        @Setting("cannot-pay-self")
        private String cannotPaySelf = "<red>You cannot pay yourself!";

        @Comment("Player not found message")
        @Setting("player-not-found")
        private String playerNotFound = "<red>Player not found: <yellow>{player}</yellow>";

        @Comment("Balance top header")
        @Setting("baltop-header")
        private String baltopHeader = "<gold><bold>Top Balances</bold></gold>";

        @Comment("Balance top entry format")
        @Setting("baltop-entry")
        private String baltopEntry = "<gray>{rank}. <yellow>{player}</yellow> - <gold>{amount}</gold>";

        @Comment("Balance top footer")
        @Setting("baltop-footer")
        private String baltopFooter = "<gray>Your rank: <yellow>#{rank}</yellow>";

        @Comment("Economy set message")
        @Setting("economy-set")
        private String economySet = "<green>Set <yellow>{player}</yellow>'s balance to <gold>{amount}</gold>";

        @Comment("Economy give message")
        @Setting("economy-give")
        private String economyGive = "<green>Gave <gold>{amount}</gold> to <yellow>{player}</yellow>";

        @Comment("Economy take message")
        @Setting("economy-take")
        private String economyTake = "<green>Took <gold>{amount}</gold> from <yellow>{player}</yellow>";

        @NotNull
        public String getPrefix() {
            return prefix;
        }

        @NotNull
        public String getBalance() {
            return balance;
        }

        @NotNull
        public String getBalanceCurrency() {
            return balanceCurrency;
        }

        @NotNull
        public String getBalanceOther() {
            return balanceOther;
        }

        @NotNull
        public String getPaySent() {
            return paySent;
        }

        @NotNull
        public String getPayReceived() {
            return payReceived;
        }

        @NotNull
        public String getInsufficientFunds() {
            return insufficientFunds;
        }

        @NotNull
        public String getInvalidAmount() {
            return invalidAmount;
        }

        @NotNull
        public String getCannotPaySelf() {
            return cannotPaySelf;
        }

        @NotNull
        public String getPlayerNotFound() {
            return playerNotFound;
        }

        @NotNull
        public String getBaltopHeader() {
            return baltopHeader;
        }

        @NotNull
        public String getBaltopEntry() {
            return baltopEntry;
        }

        @NotNull
        public String getBaltopFooter() {
            return baltopFooter;
        }

        @NotNull
        public String getEconomySet() {
            return economySet;
        }

        @NotNull
        public String getEconomyGive() {
            return economyGive;
        }

        @NotNull
        public String getEconomyTake() {
            return economyTake;
        }
    }
}
