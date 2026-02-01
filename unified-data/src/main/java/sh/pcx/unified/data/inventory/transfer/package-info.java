/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Cross-server inventory transfer system.
 *
 * <p>This package provides functionality for transferring player inventories
 * between servers in a network. It handles serialization, queuing, and
 * delivery confirmation.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.data.inventory.transfer.InventoryTransfer} - Main transfer service</li>
 *   <li>{@link sh.pcx.unified.data.inventory.transfer.TransferPacket} - Serialized inventory packet</li>
 *   <li>{@link sh.pcx.unified.data.inventory.transfer.TransferQueue} - Queue for pending transfers</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.data.inventory.transfer;
