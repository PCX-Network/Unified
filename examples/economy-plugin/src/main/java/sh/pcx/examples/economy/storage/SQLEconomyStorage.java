/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.examples.economy.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sh.pcx.examples.economy.config.EconomyConfig;
import sh.pcx.unified.economy.Account;
import sh.pcx.unified.economy.BalanceEntry;
import sh.pcx.unified.economy.Transaction;
import sh.pcx.unified.economy.storage.EconomyStorage;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQL-based storage implementation for economy data.
 *
 * <p>Supports SQLite, MySQL, MariaDB, and PostgreSQL through HikariCP
 * connection pooling. Provides async operations for all database access.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Connection pooling with HikariCP</li>
 *   <li>Async operations via ExecutorService</li>
 *   <li>Transaction history with configurable retention</li>
 *   <li>Optimized queries for leaderboards</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public class SQLEconomyStorage implements EconomyStorage {

    private static final Logger LOGGER = Logger.getLogger(SQLEconomyStorage.class.getName());

    private final EconomyConfig.DatabaseConfig config;
    private final Path dataFolder;
    private final String tablePrefix;
    private final ExecutorService executor;

    private HikariDataSource dataSource;
    private String databaseType;
    private volatile boolean available = false;

    // SQL statements (will be initialized based on database type)
    private String createAccountsTable;
    private String createBalancesTable;
    private String createTransactionsTable;
    private String insertAccount;
    private String updateAccount;
    private String selectAccount;
    private String deleteAccount;
    private String selectBalance;
    private String upsertBalance;
    private String selectTopBalances;
    private String insertTransaction;
    private String selectTransactions;
    private String deleteOldTransactions;

    /**
     * Creates a new SQL economy storage.
     *
     * @param config     the database configuration
     * @param dataFolder the plugin data folder
     */
    public SQLEconomyStorage(@NotNull EconomyConfig.DatabaseConfig config, @NotNull Path dataFolder) {
        this.config = config;
        this.dataFolder = dataFolder;
        this.tablePrefix = config.getTablePrefix();
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Economy-Storage-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                initializeDataSource();
                initializeSqlStatements();
                createTables();
                available = true;
                LOGGER.info("SQL Economy Storage initialized successfully.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize SQL storage", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        }, executor);
    }

    /**
     * Initializes the HikariCP data source.
     */
    private void initializeDataSource() {
        HikariConfig hikariConfig = new HikariConfig();

        databaseType = config.getType().toUpperCase();

        switch (databaseType) {
            case "SQLITE" -> {
                Path dbFile = dataFolder.resolve("economy.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                // SQLite doesn't support multiple connections well
                hikariConfig.setMaximumPoolSize(1);
            }
            case "MYSQL", "MARIADB" -> {
                String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                        config.getHost(), config.getPort(), config.getDatabase());
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                applyPoolSettings(hikariConfig);
            }
            case "POSTGRESQL" -> {
                String url = String.format("jdbc:postgresql://%s:%d/%s",
                        config.getHost(), config.getPort(), config.getDatabase());
                hikariConfig.setJdbcUrl(url);
                hikariConfig.setUsername(config.getUsername());
                hikariConfig.setPassword(config.getPassword());
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                applyPoolSettings(hikariConfig);
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }

        hikariConfig.setPoolName("EconomyPool");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * Applies pool settings from configuration.
     */
    private void applyPoolSettings(HikariConfig hikariConfig) {
        EconomyConfig.PoolConfig pool = config.getPool();
        hikariConfig.setMaximumPoolSize(pool.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(pool.getMinimumIdle());
        hikariConfig.setConnectionTimeout(pool.getConnectionTimeout());
        hikariConfig.setMaxLifetime(pool.getMaxLifetime());
        hikariConfig.setIdleTimeout(pool.getIdleTimeout());
    }

    /**
     * Initializes SQL statements based on database type.
     */
    private void initializeSqlStatements() {
        String autoIncrement = databaseType.equals("POSTGRESQL") ? "SERIAL" : "INTEGER PRIMARY KEY AUTO_INCREMENT";
        String textType = databaseType.equals("POSTGRESQL") ? "TEXT" : "VARCHAR(255)";
        String timestampDefault = databaseType.equals("SQLITE") ? "DEFAULT CURRENT_TIMESTAMP" : "DEFAULT CURRENT_TIMESTAMP";

        // Table creation
        createAccountsTable = String.format("""
            CREATE TABLE IF NOT EXISTS %saccounts (
                uuid %s NOT NULL PRIMARY KEY,
                name %s NOT NULL,
                created_at TIMESTAMP %s,
                last_modified TIMESTAMP %s
            )
            """, tablePrefix, textType, textType, timestampDefault, timestampDefault);

        createBalancesTable = String.format("""
            CREATE TABLE IF NOT EXISTS %sbalances (
                uuid %s NOT NULL,
                currency_id %s NOT NULL,
                balance DECIMAL(20, 8) NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, currency_id)
            )
            """, tablePrefix, textType, textType);

        createTransactionsTable = String.format("""
            CREATE TABLE IF NOT EXISTS %stransactions (
                id %s NOT NULL PRIMARY KEY,
                transaction_id %s NOT NULL UNIQUE,
                account_id %s NOT NULL,
                target_account_id %s,
                currency_id %s NOT NULL,
                type %s NOT NULL,
                amount DECIMAL(20, 8) NOT NULL,
                balance_before DECIMAL(20, 8) NOT NULL,
                balance_after DECIMAL(20, 8) NOT NULL,
                reason %s,
                source %s,
                timestamp TIMESTAMP %s,
                INDEX idx_account_id (account_id),
                INDEX idx_timestamp (timestamp)
            )
            """.replace("INTEGER PRIMARY KEY AUTO_INCREMENT", autoIncrement),
                tablePrefix,
                databaseType.equals("SQLITE") ? "INTEGER PRIMARY KEY AUTOINCREMENT" : autoIncrement,
                textType, textType, textType, textType, textType, textType, textType, timestampDefault);

        // For SQLite, remove INDEX syntax and add separately
        if (databaseType.equals("SQLITE")) {
            createTransactionsTable = String.format("""
                CREATE TABLE IF NOT EXISTS %stransactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_id TEXT NOT NULL UNIQUE,
                    account_id TEXT NOT NULL,
                    target_account_id TEXT,
                    currency_id TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount DECIMAL(20, 8) NOT NULL,
                    balance_before DECIMAL(20, 8) NOT NULL,
                    balance_after DECIMAL(20, 8) NOT NULL,
                    reason TEXT,
                    source TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """, tablePrefix);
        }

        // CRUD operations
        insertAccount = String.format(
                "INSERT INTO %saccounts (uuid, name, created_at, last_modified) VALUES (?, ?, ?, ?)",
                tablePrefix);

        updateAccount = String.format(
                "UPDATE %saccounts SET name = ?, last_modified = ? WHERE uuid = ?",
                tablePrefix);

        selectAccount = String.format(
                "SELECT uuid, name, created_at, last_modified FROM %saccounts WHERE uuid = ?",
                tablePrefix);

        deleteAccount = String.format(
                "DELETE FROM %saccounts WHERE uuid = ?",
                tablePrefix);

        selectBalance = String.format(
                "SELECT balance FROM %sbalances WHERE uuid = ? AND currency_id = ?",
                tablePrefix);

        // Upsert for balance updates
        if (databaseType.equals("SQLITE")) {
            upsertBalance = String.format(
                    "INSERT OR REPLACE INTO %sbalances (uuid, currency_id, balance) VALUES (?, ?, ?)",
                    tablePrefix);
        } else if (databaseType.equals("POSTGRESQL")) {
            upsertBalance = String.format("""
                INSERT INTO %sbalances (uuid, currency_id, balance) VALUES (?, ?, ?)
                ON CONFLICT (uuid, currency_id) DO UPDATE SET balance = EXCLUDED.balance
                """, tablePrefix);
        } else {
            upsertBalance = String.format("""
                INSERT INTO %sbalances (uuid, currency_id, balance) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                """, tablePrefix);
        }

        selectTopBalances = String.format("""
            SELECT a.uuid, a.name, b.balance
            FROM %sbalances b
            JOIN %saccounts a ON a.uuid = b.uuid
            WHERE b.currency_id = ?
            ORDER BY b.balance DESC
            LIMIT ?
            """, tablePrefix, tablePrefix);

        insertTransaction = String.format("""
            INSERT INTO %stransactions
            (transaction_id, account_id, target_account_id, currency_id, type, amount, balance_before, balance_after, reason, source, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, tablePrefix);

        selectTransactions = String.format("""
            SELECT transaction_id, account_id, target_account_id, currency_id, type, amount, balance_before, balance_after, reason, source, timestamp
            FROM %stransactions
            WHERE account_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """, tablePrefix);

        deleteOldTransactions = String.format(
                "DELETE FROM %stransactions WHERE timestamp < ?",
                tablePrefix);
    }

    /**
     * Creates the database tables.
     */
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createAccountsTable);
            stmt.execute(createBalancesTable);
            stmt.execute(createTransactionsTable);

            // Create indexes for SQLite
            if (databaseType.equals("SQLITE")) {
                stmt.execute(String.format(
                        "CREATE INDEX IF NOT EXISTS idx_tx_account ON %stransactions (account_id)",
                        tablePrefix));
                stmt.execute(String.format(
                        "CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON %stransactions (timestamp)",
                        tablePrefix));
            }

            LOGGER.info("Database tables created/verified.");
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            available = false;
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            executor.shutdown();
            LOGGER.info("SQL Economy Storage shut down.");
        });
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Account>> loadAccount(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectAccount)) {

                stmt.setString(1, playerId.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Account exists, but we return empty for now
                        // Full implementation would reconstruct the account
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load account: " + playerId, e);
            }
            return Optional.empty();
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> saveAccount(@NotNull Account account) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Update or insert account
                    try (PreparedStatement checkStmt = conn.prepareStatement(selectAccount)) {
                        checkStmt.setString(1, account.getOwnerId().toString());
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                // Update
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateAccount)) {
                                    updateStmt.setString(1, account.getOwnerName());
                                    updateStmt.setTimestamp(2, Timestamp.from(Instant.now()));
                                    updateStmt.setString(3, account.getOwnerId().toString());
                                    updateStmt.executeUpdate();
                                }
                            } else {
                                // Insert
                                try (PreparedStatement insertStmt = conn.prepareStatement(insertAccount)) {
                                    insertStmt.setString(1, account.getOwnerId().toString());
                                    insertStmt.setString(2, account.getOwnerName());
                                    insertStmt.setTimestamp(3, Timestamp.from(account.getCreatedAt()));
                                    insertStmt.setTimestamp(4, Timestamp.from(Instant.now()));
                                    insertStmt.executeUpdate();
                                }
                            }
                        }
                    }

                    // Save all balances
                    for (var entry : account.getAllBalances().entrySet()) {
                        try (PreparedStatement balanceStmt = conn.prepareStatement(upsertBalance)) {
                            balanceStmt.setString(1, account.getOwnerId().toString());
                            balanceStmt.setString(2, entry.getKey());
                            balanceStmt.setBigDecimal(3, entry.getValue());
                            balanceStmt.executeUpdate();
                        }
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to save account: " + account.getOwnerId(), e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> deleteAccount(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteAccount)) {

                stmt.setString(1, playerId.toString());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to delete account: " + playerId, e);
                return false;
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> accountExists(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectAccount)) {

                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to check account exists: " + playerId, e);
                return false;
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<List<Account>> loadAllAccounts() {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    @NotNull
    public CompletableFuture<Void> saveAllAccounts(@NotNull List<Account> accounts) {
        return CompletableFuture.allOf(
                accounts.stream()
                        .map(this::saveAccount)
                        .toArray(CompletableFuture[]::new)
        );
    }

    @Override
    @NotNull
    public CompletableFuture<Void> updateBalance(@NotNull UUID playerId, @NotNull String currencyId, @NotNull BigDecimal newBalance) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsertBalance)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, currencyId);
                stmt.setBigDecimal(3, newBalance);
                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to update balance: " + playerId, e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<List<BalanceEntry>> getTopBalances(@NotNull String currencyId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<BalanceEntry> entries = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectTopBalances)) {

                stmt.setString(1, currencyId);
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    int rank = 1;
                    while (rs.next()) {
                        entries.add(new BalanceEntry(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("name"),
                                rs.getBigDecimal("balance"),
                                currencyId,
                                rank++
                        ));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to get top balances", e);
            }

            return entries;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getBalanceRank(@NotNull UUID playerId, @NotNull String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = String.format("""
                SELECT COUNT(*) + 1 as rank FROM %sbalances
                WHERE currency_id = ? AND balance > (
                    SELECT COALESCE(balance, 0) FROM %sbalances WHERE uuid = ? AND currency_id = ?
                )
                """, tablePrefix, tablePrefix);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, currencyId);
                stmt.setString(2, playerId.toString());
                stmt.setString(3, currencyId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("rank");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to get balance rank", e);
            }

            return -1;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<BigDecimal> getTotalSupply(@NotNull String currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = String.format(
                    "SELECT COALESCE(SUM(balance), 0) as total FROM %sbalances WHERE currency_id = ?",
                    tablePrefix);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, currencyId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBigDecimal("total");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to get total supply", e);
            }

            return BigDecimal.ZERO;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> getAccountCount() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = String.format("SELECT COUNT(*) as count FROM %saccounts", tablePrefix);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("count");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to get account count", e);
            }

            return 0;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> saveTransaction(@NotNull Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertTransaction)) {

                stmt.setString(1, transaction.getTransactionId().toString());
                stmt.setString(2, transaction.getAccountId().toString());
                stmt.setString(3, transaction.getTargetAccountId().map(UUID::toString).orElse(null));
                stmt.setString(4, transaction.getCurrencyId());
                stmt.setString(5, transaction.getType().name());
                stmt.setBigDecimal(6, transaction.getAmount());
                stmt.setBigDecimal(7, transaction.getBalanceBefore());
                stmt.setBigDecimal(8, transaction.getBalanceAfter());
                stmt.setString(9, transaction.getReason().orElse(null));
                stmt.setString(10, transaction.getSource().orElse(null));
                stmt.setTimestamp(11, Timestamp.from(transaction.getTimestamp()));

                stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to save transaction", e);
            }
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<List<Transaction>> loadTransactionHistory(@NotNull UUID playerId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = new ArrayList<>();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectTransactions)) {

                stmt.setString(1, playerId.toString());
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String targetId = rs.getString("target_account_id");
                        transactions.add(Transaction.builder()
                                .transactionId(UUID.fromString(rs.getString("transaction_id")))
                                .accountId(UUID.fromString(rs.getString("account_id")))
                                .targetAccountId(targetId != null ? UUID.fromString(targetId) : null)
                                .currencyId(rs.getString("currency_id"))
                                .type(Transaction.Type.valueOf(rs.getString("type")))
                                .amount(rs.getBigDecimal("amount"))
                                .balanceBefore(rs.getBigDecimal("balance_before"))
                                .balanceAfter(rs.getBigDecimal("balance_after"))
                                .reason(rs.getString("reason"))
                                .source(rs.getString("source"))
                                .timestamp(rs.getTimestamp("timestamp").toInstant())
                                .build());
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load transaction history", e);
            }

            return transactions;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<List<Transaction>> loadTransactionHistory(@NotNull UUID playerId, @NotNull String currencyId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = new ArrayList<>();
            String sql = selectTransactions.replace("WHERE account_id = ?", "WHERE account_id = ? AND currency_id = ?");

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, currencyId);
                stmt.setInt(3, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String targetId = rs.getString("target_account_id");
                        transactions.add(Transaction.builder()
                                .transactionId(UUID.fromString(rs.getString("transaction_id")))
                                .accountId(UUID.fromString(rs.getString("account_id")))
                                .targetAccountId(targetId != null ? UUID.fromString(targetId) : null)
                                .currencyId(rs.getString("currency_id"))
                                .type(Transaction.Type.valueOf(rs.getString("type")))
                                .amount(rs.getBigDecimal("amount"))
                                .balanceBefore(rs.getBigDecimal("balance_before"))
                                .balanceAfter(rs.getBigDecimal("balance_after"))
                                .reason(rs.getString("reason"))
                                .source(rs.getString("source"))
                                .timestamp(rs.getTimestamp("timestamp").toInstant())
                                .build());
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load transaction history", e);
            }

            return transactions;
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Transaction>> loadTransaction(@NotNull UUID transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = String.format("""
                SELECT transaction_id, account_id, target_account_id, currency_id, type, amount, balance_before, balance_after, reason, source, timestamp
                FROM %stransactions WHERE transaction_id = ?
                """, tablePrefix);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, transactionId.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String targetId = rs.getString("target_account_id");
                        return Optional.of(Transaction.builder()
                                .transactionId(UUID.fromString(rs.getString("transaction_id")))
                                .accountId(UUID.fromString(rs.getString("account_id")))
                                .targetAccountId(targetId != null ? UUID.fromString(targetId) : null)
                                .currencyId(rs.getString("currency_id"))
                                .type(Transaction.Type.valueOf(rs.getString("type")))
                                .amount(rs.getBigDecimal("amount"))
                                .balanceBefore(rs.getBigDecimal("balance_before"))
                                .balanceAfter(rs.getBigDecimal("balance_after"))
                                .reason(rs.getString("reason"))
                                .source(rs.getString("source"))
                                .timestamp(rs.getTimestamp("timestamp").toInstant())
                                .build());
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load transaction: " + transactionId, e);
            }

            return Optional.empty();
        }, executor);
    }

    @Override
    @NotNull
    public CompletableFuture<Integer> clearOldTransactions(int olderThanDays) {
        return CompletableFuture.supplyAsync(() -> {
            Instant cutoff = Instant.now().minusSeconds(olderThanDays * 86400L);

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(deleteOldTransactions)) {

                stmt.setTimestamp(1, Timestamp.from(cutoff));
                return stmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to clear old transactions", e);
                return 0;
            }
        }, executor);
    }

    @Override
    @NotNull
    public String getName() {
        return databaseType;
    }

    @Override
    public boolean isAvailable() {
        return available && dataSource != null && !dataSource.isClosed();
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                return false;
            }

            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(5);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Health check failed", e);
                return false;
            }
        }, executor);
    }
}
