/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Vault economy integration.
 *
 * <p>This package provides compatibility with the Vault economy API,
 * allowing plugins that use Vault to work seamlessly with UnifiedEconomy.
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.economy.vault.VaultBridge} - Vault compatibility bridge</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create and register the bridge
 * VaultBridge bridge = new VaultBridge(economyService, "MyPlugin", nameResolver);
 * bridge.register();
 *
 * // Don't forget to unregister on disable
 * bridge.unregister();
 * }</pre>
 *
 * <h2>Compatibility Notes</h2>
 * <ul>
 *   <li>Bank accounts are not supported</li>
 *   <li>Only the default currency is exposed through Vault</li>
 *   <li>World-specific economies are not supported</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.economy.vault;
