/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Comprehensive backup and restore system for player data and inventories.
 *
 * <p>This package provides a complete backup solution including:
 * <ul>
 *   <li><b>Backup Types</b>: Full, incremental, and differential backups</li>
 *   <li><b>Scheduling</b>: Automatic scheduled backups with configurable intervals</li>
 *   <li><b>Versioning</b>: Version chain tracking with rollback support</li>
 *   <li><b>Storage</b>: Local and remote storage backends (S3, FTP)</li>
 *   <li><b>Compression</b>: Multiple compression algorithms (GZIP, LZ4, ZSTD)</li>
 *   <li><b>Retention</b>: Configurable retention policies</li>
 *   <li><b>Death Recovery</b>: Player death item recovery system</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create backup service
 * BackupStorage storage = new LocalBackupStorage(backupDir);
 * BackupService backupService = BackupService.create(storage);
 *
 * // Create a full backup
 * Backup backup = backupService.createBackup(playerId, BackupType.FULL).join();
 *
 * // Schedule automatic backups
 * BackupScheduler scheduler = backupService.getScheduler();
 * scheduler.scheduleDaily(BackupType.INCREMENTAL, LocalTime.of(3, 0));
 *
 * // Restore from backup
 * RestoreResult result = backupService.restore(player, backup.id()).join();
 *
 * // Death recovery
 * DeathRecovery recovery = backupService.getDeathRecovery();
 * recovery.recoverLastDeath(player).join();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.backup;
