/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Database schema migration support.
 *
 * <p>This package provides version-controlled database schema migrations
 * for evolving the database structure over time.
 *
 * <h2>Core Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.sql.migration.Migration} - Interface for migrations</li>
 *   <li>{@link sh.pcx.unified.data.sql.migration.MigrationRunner} - Executes migrations</li>
 *   <li>{@link sh.pcx.unified.data.sql.migration.MigrationHistory} - Tracks applied migrations</li>
 * </ul>
 *
 * <h2>Creating Migrations</h2>
 * <pre>{@code
 * public class V1_CreatePlayersTable implements Migration {
 *
 *     @Override
 *     public int getVersion() {
 *         return 1;
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "Create players table";
 *     }
 *
 *     @Override
 *     public void up(DatabaseConnection connection) throws SQLException {
 *         connection.executeUpdate("""
 *             CREATE TABLE IF NOT EXISTS players (
 *                 uuid VARCHAR(36) PRIMARY KEY,
 *                 name VARCHAR(16) NOT NULL,
 *                 balance DOUBLE DEFAULT 0
 *             )
 *         """);
 *     }
 *
 *     @Override
 *     public void down(DatabaseConnection connection) throws SQLException {
 *         connection.executeUpdate("DROP TABLE IF EXISTS players");
 *     }
 * }
 * }</pre>
 *
 * <h2>Running Migrations</h2>
 * <pre>{@code
 * MigrationRunner runner = new MigrationRunner(connectionProvider, databaseType);
 *
 * // Register migrations
 * runner.register(new V1_CreatePlayersTable());
 * runner.register(new V2_AddPlayerLevel());
 *
 * // Apply all pending migrations
 * runner.migrate().join();
 *
 * // Check status
 * int currentVersion = runner.getCurrentVersion();
 * List<Migration> pending = runner.getPendingMigrations();
 *
 * // Rollback if needed
 * runner.rollback(1).join();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.sql.migration;
