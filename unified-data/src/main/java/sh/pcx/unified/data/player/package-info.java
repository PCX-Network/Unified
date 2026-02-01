/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Player data management system with cross-server synchronization.
 *
 * <p>This package provides a comprehensive player data management system including:
 * <ul>
 *   <li><b>Player Profiles:</b> Unified player data containers ({@link sh.pcx.unified.data.player.PlayerProfile})</li>
 *   <li><b>Type-Safe Data Keys:</b> Compile-time type safety ({@link sh.pcx.unified.data.player.DataKey})</li>
 *   <li><b>Session Management:</b> Login/logout tracking ({@link sh.pcx.unified.data.player.SessionManager})</li>
 *   <li><b>Cross-Server Sync:</b> Redis-based synchronization ({@link sh.pcx.unified.data.player.CrossServerSync})</li>
 *   <li><b>Distributed Locking:</b> Prevent concurrent modifications ({@link sh.pcx.unified.data.player.LockManager})</li>
 *   <li><b>GDPR Compliance:</b> Data export and erasure ({@link sh.pcx.unified.data.player.GDPRService})</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Inject the player data service
 * @Inject
 * private PlayerDataService playerData;
 *
 * // Define typed data keys
 * public static final DataKey<Integer> KILLS = DataKey.of("kills", Integer.class, 0);
 * public static final DataKey<Double> BALANCE = DataKey.of("balance", Double.class, 0.0);
 *
 * // Use player profiles
 * public void onKill(Player player) {
 *     PlayerProfile profile = playerData.getProfile(player.getUniqueId());
 *     int kills = profile.getData(KILLS);
 *     profile.setData(KILLS, kills + 1);
 *     // Data is automatically saved
 * }
 * }</pre>
 *
 * <h2>Key Classes</h2>
 * <table>
 *   <tr>
 *     <th>Class</th>
 *     <th>Purpose</th>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.PlayerDataService}</td>
 *     <td>Main service for loading and managing profiles</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.PlayerProfile}</td>
 *     <td>Complete player data container</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.DataKey}</td>
 *     <td>Type-safe key for storing data</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.PersistentDataKey}</td>
 *     <td>Key that persists to database</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.TransientDataKey}</td>
 *     <td>Key that lives only in memory</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.SessionManager}</td>
 *     <td>Tracks player sessions</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.CrossServerSync}</td>
 *     <td>Synchronizes data across servers</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.LockManager}</td>
 *     <td>Distributed locking for data consistency</td>
 *   </tr>
 *   <tr>
 *     <td>{@link sh.pcx.unified.data.player.GDPRService}</td>
 *     <td>GDPR compliance features</td>
 *   </tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.player;
