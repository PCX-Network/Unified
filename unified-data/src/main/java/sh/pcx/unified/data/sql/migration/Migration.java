/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.sql.migration;

import sh.pcx.unified.data.sql.DatabaseConnection;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Interface for database schema migrations.
 *
 * <p>Migrations are used to evolve the database schema over time in a
 * controlled, versioned manner. Each migration has a unique version number
 * and contains both upgrade (up) and rollback (down) logic.
 *
 * <h2>Example Usage</h2>
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
 *                 balance DOUBLE DEFAULT 0,
 *                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 *             )
 *         """);
 *
 *         // Create index
 *         connection.executeUpdate(
 *             "CREATE INDEX idx_players_name ON players(name)"
 *         );
 *     }
 *
 *     @Override
 *     public void down(DatabaseConnection connection) throws SQLException {
 *         connection.executeUpdate("DROP TABLE IF EXISTS players");
 *     }
 * }
 *
 * public class V2_AddPlayerLevel implements Migration {
 *
 *     @Override
 *     public int getVersion() {
 *         return 2;
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "Add level column to players table";
 *     }
 *
 *     @Override
 *     public void up(DatabaseConnection connection) throws SQLException {
 *         connection.executeUpdate(
 *             "ALTER TABLE players ADD COLUMN level INT DEFAULT 1"
 *         );
 *     }
 *
 *     @Override
 *     public void down(DatabaseConnection connection) throws SQLException {
 *         connection.executeUpdate(
 *             "ALTER TABLE players DROP COLUMN level"
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Migration Naming Convention</h2>
 * <p>It's recommended to use a naming pattern like:
 * <ul>
 *   <li>{@code V1_CreatePlayersTable}</li>
 *   <li>{@code V2_AddPlayerLevel}</li>
 *   <li>{@code V3_CreateTransactionsTable}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Migrations should be stateless and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationRunner
 * @see MigrationHistory
 */
public interface Migration {

    /**
     * Returns the unique version number of this migration.
     *
     * <p>Migrations are applied in version order. Version numbers must be
     * positive integers and should never be reused or changed after the
     * migration has been applied.
     *
     * @return the migration version
     * @since 1.0.0
     */
    int getVersion();

    /**
     * Returns a human-readable description of this migration.
     *
     * <p>This is stored in the migration history table for reference.
     *
     * @return the migration description
     * @since 1.0.0
     */
    @NotNull
    String getDescription();

    /**
     * Applies the migration (upgrade).
     *
     * <p>This method should contain the SQL statements to modify the schema.
     * It runs within a transaction that will be rolled back if an exception
     * is thrown.
     *
     * <h2>Example</h2>
     * <pre>{@code
     * @Override
     * public void up(DatabaseConnection connection) throws SQLException {
     *     connection.executeUpdate("""
     *         CREATE TABLE players (
     *             uuid VARCHAR(36) PRIMARY KEY,
     *             name VARCHAR(16) NOT NULL
     *         )
     *     """);
     * }
     * }</pre>
     *
     * @param connection the database connection
     * @throws SQLException if a database error occurs
     * @since 1.0.0
     */
    void up(@NotNull DatabaseConnection connection) throws SQLException;

    /**
     * Reverts the migration (downgrade/rollback).
     *
     * <p>This method should undo the changes made by {@link #up(DatabaseConnection)}.
     * Not all migrations can be safely rolled back (e.g., dropping data).
     *
     * <h2>Example</h2>
     * <pre>{@code
     * @Override
     * public void down(DatabaseConnection connection) throws SQLException {
     *     connection.executeUpdate("DROP TABLE IF EXISTS players");
     * }
     * }</pre>
     *
     * @param connection the database connection
     * @throws SQLException if a database error occurs
     * @throws UnsupportedOperationException if rollback is not supported
     * @since 1.0.0
     */
    void down(@NotNull DatabaseConnection connection) throws SQLException;

    /**
     * Returns whether this migration supports rollback.
     *
     * <p>Some migrations cannot be safely rolled back (e.g., data migrations
     * that delete data). Override this method to return false if rollback
     * is not supported.
     *
     * @return true if {@link #down(DatabaseConnection)} is implemented
     * @since 1.0.0
     */
    default boolean supportsRollback() {
        return true;
    }

    /**
     * Returns the checksum of this migration.
     *
     * <p>The checksum is used to detect if a migration has been modified
     * after it was applied. Override to provide a custom checksum.
     *
     * @return the migration checksum
     * @since 1.0.0
     */
    default int getChecksum() {
        return (getClass().getName() + getVersion() + getDescription()).hashCode();
    }

    /**
     * Returns the author of this migration.
     *
     * @return the author name, or "unknown" if not specified
     * @since 1.0.0
     */
    @NotNull
    default String getAuthor() {
        return "unknown";
    }
}
