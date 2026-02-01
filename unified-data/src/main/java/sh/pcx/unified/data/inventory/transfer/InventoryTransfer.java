/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.transfer;

import sh.pcx.unified.data.inventory.core.ApplyMode;
import sh.pcx.unified.data.inventory.core.CaptureOptions;
import sh.pcx.unified.data.inventory.core.InventoryService;
import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Manages cross-server inventory transfers.
 *
 * <p>InventoryTransfer provides a complete solution for transferring player inventories
 * between servers in a network. It handles capturing, serializing, queuing, sending,
 * receiving, and applying inventory data.
 *
 * <h2>Transfer Flow</h2>
 * <ol>
 *   <li>Source server captures player inventory</li>
 *   <li>Inventory is serialized into a TransferPacket</li>
 *   <li>Packet is sent via messaging system (Redis, plugin messaging, etc.)</li>
 *   <li>Target server receives and queues the packet</li>
 *   <li>When player joins, inventory is restored</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventoryTransfer transfer;
 *
 * // On source server - initiate transfer
 * transfer.initiateTransfer(player, "lobby")
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             player.sendMessage("Inventory will be transferred!");
 *         }
 *     });
 *
 * // On target server - register receiver
 * transfer.registerReceiver((packet) -> {
 *     // Packet received from another server
 *     transfer.queueForPlayer(packet.getPlayerId(), packet);
 * });
 *
 * // On player join - apply pending transfer
 * transfer.applyPendingTransfer(player)
 *     .thenAccept(applied -> {
 *         if (applied) {
 *             player.sendMessage("Inventory restored!");
 *         }
 *     });
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TransferPacket
 * @see TransferQueue
 */
public class InventoryTransfer {

    private final InventoryService inventoryService;
    private final TransferQueue transferQueue;
    private final Map<UUID, TransferPacket> pendingTransfers;

    private String serverName;
    private Duration defaultTtl;
    private TransferPacket.TransferPriority defaultPriority;
    private BiConsumer<TransferPacket, TransferCallback> sender;
    private CaptureOptions captureOptions;
    private ApplyMode applyMode;
    private boolean clearOnTransfer;

    /**
     * Creates a new InventoryTransfer instance.
     *
     * @param inventoryService the inventory service
     * @since 1.0.0
     */
    public InventoryTransfer(@NotNull InventoryService inventoryService) {
        this.inventoryService = Objects.requireNonNull(inventoryService);
        this.transferQueue = new TransferQueue();
        this.pendingTransfers = new ConcurrentHashMap<>();

        this.defaultTtl = Duration.ofMinutes(5);
        this.defaultPriority = TransferPacket.TransferPriority.NORMAL;
        this.captureOptions = CaptureOptions.full();
        this.applyMode = ApplyMode.REPLACE;
        this.clearOnTransfer = true;
    }

    // ========== Configuration ==========

    /**
     * Sets the current server name.
     *
     * @param serverName the server name
     * @since 1.0.0
     */
    public void setServerName(@NotNull String serverName) {
        this.serverName = serverName;
    }

    /**
     * Sets the default TTL for transfer packets.
     *
     * @param ttl the time-to-live
     * @since 1.0.0
     */
    public void setDefaultTtl(@NotNull Duration ttl) {
        this.defaultTtl = ttl;
    }

    /**
     * Sets the default transfer priority.
     *
     * @param priority the priority
     * @since 1.0.0
     */
    public void setDefaultPriority(@NotNull TransferPacket.TransferPriority priority) {
        this.defaultPriority = priority;
    }

    /**
     * Sets the capture options for transfers.
     *
     * @param options the capture options
     * @since 1.0.0
     */
    public void setCaptureOptions(@NotNull CaptureOptions options) {
        this.captureOptions = options;
    }

    /**
     * Sets the apply mode for received transfers.
     *
     * @param mode the apply mode
     * @since 1.0.0
     */
    public void setApplyMode(@NotNull ApplyMode mode) {
        this.applyMode = mode;
    }

    /**
     * Sets whether to clear inventory on transfer initiation.
     *
     * @param clear true to clear
     * @since 1.0.0
     */
    public void setClearOnTransfer(boolean clear) {
        this.clearOnTransfer = clear;
    }

    /**
     * Registers the packet sender.
     *
     * <p>The sender is responsible for transmitting packets to other servers.
     *
     * @param sender the sender function
     * @since 1.0.0
     */
    public void registerSender(@NotNull BiConsumer<TransferPacket, TransferCallback> sender) {
        this.sender = sender;
    }

    // ========== Transfer Initiation ==========

    /**
     * Initiates an inventory transfer for a player.
     *
     * @param player       the player
     * @param targetServer the target server
     * @return a future containing the transfer result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<TransferResult> initiateTransfer(
        @NotNull UnifiedPlayer player,
        @NotNull String targetServer
    ) {
        return initiateTransfer(player, targetServer, TransferPacket.TransferReason.SERVER_TRANSFER);
    }

    /**
     * Initiates an inventory transfer with a specific reason.
     *
     * @param player       the player
     * @param targetServer the target server
     * @param reason       the transfer reason
     * @return a future containing the transfer result
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<TransferResult> initiateTransfer(
        @NotNull UnifiedPlayer player,
        @NotNull String targetServer,
        @NotNull TransferPacket.TransferReason reason
    ) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(targetServer, "Target server cannot be null");
        Objects.requireNonNull(reason, "Reason cannot be null");

        if (sender == null) {
            return CompletableFuture.completedFuture(
                TransferResult.failure("No sender registered"));
        }

        // Capture inventory
        InventorySnapshot snapshot = inventoryService.capture(player, captureOptions);
        byte[] data = snapshot.toBytes();

        // Create packet
        TransferPacket packet = TransferPacket.builder(player.getUniqueId(), data)
            .sourceServer(serverName)
            .targetServer(targetServer)
            .priority(defaultPriority)
            .ttlMillis(defaultTtl.toMillis())
            .reason(reason)
            .build();

        // Send packet
        CompletableFuture<TransferResult> future = new CompletableFuture<>();

        sender.accept(packet, new TransferCallback() {
            @Override
            public void onSuccess() {
                // Clear inventory after successful send
                if (clearOnTransfer) {
                    inventoryService.clear(player);
                }
                future.complete(TransferResult.success(packet.getPacketId()));
            }

            @Override
            public void onFailure(@NotNull String error) {
                future.complete(TransferResult.failure(error));
            }
        });

        return future;
    }

    /**
     * Creates a transfer packet without sending it.
     *
     * @param player       the player
     * @param targetServer the target server
     * @return the created packet
     * @since 1.0.0
     */
    @NotNull
    public TransferPacket createPacket(
        @NotNull UnifiedPlayer player,
        @NotNull String targetServer
    ) {
        InventorySnapshot snapshot = inventoryService.capture(player, captureOptions);
        byte[] data = snapshot.toBytes();

        return TransferPacket.builder(player.getUniqueId(), data)
            .sourceServer(serverName)
            .targetServer(targetServer)
            .priority(defaultPriority)
            .ttlMillis(defaultTtl.toMillis())
            .reason(TransferPacket.TransferReason.SERVER_TRANSFER)
            .build();
    }

    // ========== Transfer Reception ==========

    /**
     * Receives and queues a transfer packet.
     *
     * @param packet the received packet
     * @since 1.0.0
     */
    public void receivePacket(@NotNull TransferPacket packet) {
        Objects.requireNonNull(packet, "Packet cannot be null");

        if (packet.isExpired()) {
            return; // Discard expired packets
        }

        pendingTransfers.put(packet.getPlayerId(), packet);
    }

    /**
     * Queues a packet for a specific player.
     *
     * @param playerId the player's UUID
     * @param packet   the packet
     * @since 1.0.0
     */
    public void queueForPlayer(@NotNull UUID playerId, @NotNull TransferPacket packet) {
        pendingTransfers.put(playerId, packet);
    }

    /**
     * Checks if there's a pending transfer for a player.
     *
     * @param playerId the player's UUID
     * @return true if a transfer is pending
     * @since 1.0.0
     */
    public boolean hasPendingTransfer(@NotNull UUID playerId) {
        TransferPacket packet = pendingTransfers.get(playerId);
        return packet != null && !packet.isExpired();
    }

    /**
     * Gets a pending transfer for a player.
     *
     * @param playerId the player's UUID
     * @return the pending packet, or empty if none
     * @since 1.0.0
     */
    @NotNull
    public Optional<TransferPacket> getPendingTransfer(@NotNull UUID playerId) {
        TransferPacket packet = pendingTransfers.get(playerId);
        if (packet == null || packet.isExpired()) {
            pendingTransfers.remove(playerId);
            return Optional.empty();
        }
        return Optional.of(packet);
    }

    // ========== Transfer Application ==========

    /**
     * Applies a pending transfer to a player.
     *
     * @param player the player
     * @return a future that completes with true if a transfer was applied
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> applyPendingTransfer(@NotNull UnifiedPlayer player) {
        return applyPendingTransfer(player, applyMode);
    }

    /**
     * Applies a pending transfer with a specific apply mode.
     *
     * @param player the player
     * @param mode   the apply mode
     * @return a future that completes with true if a transfer was applied
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Boolean> applyPendingTransfer(
        @NotNull UnifiedPlayer player,
        @NotNull ApplyMode mode
    ) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(mode, "Apply mode cannot be null");

        TransferPacket packet = pendingTransfers.remove(player.getUniqueId());
        if (packet == null || packet.isExpired()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                InventorySnapshot snapshot = inventoryService.fromBytes(packet.getInventoryData());
                snapshot.applyTo(player, mode);
                return true;
            } catch (Exception e) {
                // Failed to apply
                return false;
            }
        });
    }

    /**
     * Applies a specific packet to a player.
     *
     * @param player the player
     * @param packet the packet to apply
     * @return true if applied successfully
     * @since 1.0.0
     */
    public boolean applyPacket(@NotNull UnifiedPlayer player, @NotNull TransferPacket packet) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(packet, "Packet cannot be null");

        if (packet.isExpired()) {
            return false;
        }

        try {
            InventorySnapshot snapshot = inventoryService.fromBytes(packet.getInventoryData());
            snapshot.applyTo(player, applyMode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Queue Management ==========

    /**
     * Gets the transfer queue.
     *
     * @return the transfer queue
     * @since 1.0.0
     */
    @NotNull
    public TransferQueue getQueue() {
        return transferQueue;
    }

    /**
     * Clears pending transfer for a player.
     *
     * @param playerId the player's UUID
     * @return true if a transfer was cleared
     * @since 1.0.0
     */
    public boolean clearPendingTransfer(@NotNull UUID playerId) {
        return pendingTransfers.remove(playerId) != null;
    }

    /**
     * Clears all pending transfers.
     *
     * @since 1.0.0
     */
    public void clearAllPending() {
        pendingTransfers.clear();
    }

    /**
     * Gets the count of pending transfers.
     *
     * @return the pending count
     * @since 1.0.0
     */
    public int getPendingCount() {
        return pendingTransfers.size();
    }

    /**
     * Cleans up expired pending transfers.
     *
     * @return the count of removed transfers
     * @since 1.0.0
     */
    public int cleanupExpired() {
        int removed = 0;
        var iterator = pendingTransfers.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    // ========== Shutdown ==========

    /**
     * Shuts down the transfer system.
     *
     * @return a future that completes when shutdown is complete
     * @since 1.0.0
     */
    @NotNull
    public CompletableFuture<Void> shutdown() {
        transferQueue.stopProcessing();
        return CompletableFuture.completedFuture(null);
    }

    // ========== Nested Types ==========

    /**
     * Result of a transfer operation.
     *
     * @since 1.0.0
     */
    public record TransferResult(
        boolean success,
        @Nullable String packetId,
        @Nullable String error
    ) {
        @NotNull
        public static TransferResult success(@NotNull String packetId) {
            return new TransferResult(true, packetId, null);
        }

        @NotNull
        public static TransferResult failure(@NotNull String error) {
            return new TransferResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }
    }

    /**
     * Callback for transfer operations.
     *
     * @since 1.0.0
     */
    public interface TransferCallback {
        /**
         * Called when the transfer succeeds.
         */
        void onSuccess();

        /**
         * Called when the transfer fails.
         *
         * @param error the error message
         */
        void onFailure(@NotNull String error);
    }
}
