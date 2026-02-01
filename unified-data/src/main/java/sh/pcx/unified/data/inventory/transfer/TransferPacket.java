/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Serialized inventory packet for cross-server transfer.
 *
 * <p>TransferPacket encapsulates all the data needed to transfer a player's
 * inventory between servers. It includes the serialized inventory data,
 * metadata about the transfer, and optional additional data.
 *
 * <h2>Packet Contents</h2>
 * <ul>
 *   <li><b>Player ID</b>: UUID of the player</li>
 *   <li><b>Inventory Data</b>: Serialized inventory snapshot</li>
 *   <li><b>Timestamp</b>: When the packet was created</li>
 *   <li><b>Source/Target</b>: Server names for routing</li>
 *   <li><b>Priority</b>: Transfer priority level</li>
 *   <li><b>TTL</b>: Time-to-live for the packet</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a transfer packet
 * TransferPacket packet = TransferPacket.builder(playerId, inventoryData)
 *     .sourceServer("survival")
 *     .targetServer("lobby")
 *     .priority(TransferPriority.HIGH)
 *     .ttlSeconds(60)
 *     .build();
 *
 * // Serialize for network transfer
 * byte[] bytes = packet.toBytes();
 * String base64 = packet.toBase64();
 *
 * // Deserialize on target server
 * TransferPacket received = TransferPacket.fromBytes(bytes);
 * byte[] inventoryData = received.getInventoryData();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventoryTransfer
 * @see TransferQueue
 */
public final class TransferPacket implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MAGIC_NUMBER = 0x494E5654; // "INVT"
    private static final int VERSION = 1;

    private final String packetId;
    private final UUID playerId;
    private final byte[] inventoryData;
    private final Instant createdAt;
    private final String sourceServer;
    private final String targetServer;
    private final TransferPriority priority;
    private final long ttlMillis;
    private final Map<String, String> metadata;
    private final TransferReason reason;

    private TransferPacket(Builder builder) {
        this.packetId = builder.packetId != null ? builder.packetId : UUID.randomUUID().toString();
        this.playerId = Objects.requireNonNull(builder.playerId, "Player ID cannot be null");
        this.inventoryData = Objects.requireNonNull(builder.inventoryData, "Inventory data cannot be null");
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.sourceServer = builder.sourceServer;
        this.targetServer = builder.targetServer;
        this.priority = builder.priority != null ? builder.priority : TransferPriority.NORMAL;
        this.ttlMillis = builder.ttlMillis > 0 ? builder.ttlMillis : 60000; // Default 60 seconds
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.reason = builder.reason != null ? builder.reason : TransferReason.SERVER_TRANSFER;
    }

    /**
     * Creates a new packet builder.
     *
     * @param playerId      the player's UUID
     * @param inventoryData the serialized inventory data
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull UUID playerId, byte @NotNull [] inventoryData) {
        return new Builder(playerId, inventoryData);
    }

    /**
     * Deserializes a packet from bytes.
     *
     * @param bytes the serialized packet data
     * @return the deserialized packet
     * @throws IllegalArgumentException if data is invalid
     * @since 1.0.0
     */
    @NotNull
    public static TransferPacket fromBytes(byte @NotNull [] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IllegalArgumentException("Invalid packet magic number");
            }

            int version = dis.readInt();
            if (version != VERSION) {
                throw new IllegalArgumentException("Unsupported packet version: " + version);
            }

            String packetId = dis.readUTF();
            UUID playerId = new UUID(dis.readLong(), dis.readLong());
            long createdAt = dis.readLong();
            String sourceServer = dis.readBoolean() ? dis.readUTF() : null;
            String targetServer = dis.readBoolean() ? dis.readUTF() : null;
            TransferPriority priority = TransferPriority.values()[dis.readByte()];
            long ttlMillis = dis.readLong();
            TransferReason reason = TransferReason.values()[dis.readByte()];

            int dataLength = dis.readInt();
            byte[] inventoryData = new byte[dataLength];
            dis.readFully(inventoryData);

            int metadataCount = dis.readInt();
            var metadataBuilder = new java.util.HashMap<String, String>();
            for (int i = 0; i < metadataCount; i++) {
                metadataBuilder.put(dis.readUTF(), dis.readUTF());
            }

            return new Builder(playerId, inventoryData)
                .packetId(packetId)
                .createdAt(Instant.ofEpochMilli(createdAt))
                .sourceServer(sourceServer)
                .targetServer(targetServer)
                .priority(priority)
                .ttlMillis(ttlMillis)
                .reason(reason)
                .metadata(metadataBuilder)
                .build();

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize packet", e);
        }
    }

    /**
     * Deserializes a packet from Base64.
     *
     * @param base64 the Base64-encoded packet
     * @return the deserialized packet
     * @throws IllegalArgumentException if data is invalid
     * @since 1.0.0
     */
    @NotNull
    public static TransferPacket fromBase64(@NotNull String base64) {
        return fromBytes(Base64.getDecoder().decode(base64));
    }

    // ========== Getters ==========

    /**
     * Returns the unique packet ID.
     *
     * @return the packet ID
     * @since 1.0.0
     */
    @NotNull
    public String getPacketId() {
        return packetId;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player ID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the serialized inventory data.
     *
     * @return copy of the inventory data
     * @since 1.0.0
     */
    public byte @NotNull [] getInventoryData() {
        return inventoryData.clone();
    }

    /**
     * Returns when this packet was created.
     *
     * @return the creation timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the source server name.
     *
     * @return the source server, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getSourceServer() {
        return sourceServer;
    }

    /**
     * Returns the target server name.
     *
     * @return the target server, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getTargetServer() {
        return targetServer;
    }

    /**
     * Returns the transfer priority.
     *
     * @return the priority
     * @since 1.0.0
     */
    @NotNull
    public TransferPriority getPriority() {
        return priority;
    }

    /**
     * Returns the time-to-live in milliseconds.
     *
     * @return the TTL in milliseconds
     * @since 1.0.0
     */
    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * Returns the transfer reason.
     *
     * @return the reason
     * @since 1.0.0
     */
    @NotNull
    public TransferReason getReason() {
        return reason;
    }

    /**
     * Returns the metadata map.
     *
     * @return unmodifiable metadata map
     * @since 1.0.0
     */
    @NotNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Gets a metadata value.
     *
     * @param key the metadata key
     * @return the value, or null if not set
     * @since 1.0.0
     */
    @Nullable
    public String getMetadata(@NotNull String key) {
        return metadata.get(key);
    }

    // ========== Status ==========

    /**
     * Checks if this packet has expired.
     *
     * @return true if expired
     * @since 1.0.0
     */
    public boolean isExpired() {
        return Instant.now().isAfter(getExpiresAt());
    }

    /**
     * Returns when this packet expires.
     *
     * @return the expiration timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getExpiresAt() {
        return createdAt.plusMillis(ttlMillis);
    }

    /**
     * Returns the remaining TTL in milliseconds.
     *
     * @return remaining TTL, or 0 if expired
     * @since 1.0.0
     */
    public long getRemainingTtlMillis() {
        long remaining = getExpiresAt().toEpochMilli() - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Returns the size of the inventory data in bytes.
     *
     * @return the data size
     * @since 1.0.0
     */
    public int getDataSize() {
        return inventoryData.length;
    }

    // ========== Serialization ==========

    /**
     * Serializes this packet to bytes.
     *
     * @return the serialized packet data
     * @since 1.0.0
     */
    public byte @NotNull [] toBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(MAGIC_NUMBER);
            dos.writeInt(VERSION);
            dos.writeUTF(packetId);
            dos.writeLong(playerId.getMostSignificantBits());
            dos.writeLong(playerId.getLeastSignificantBits());
            dos.writeLong(createdAt.toEpochMilli());

            dos.writeBoolean(sourceServer != null);
            if (sourceServer != null) dos.writeUTF(sourceServer);

            dos.writeBoolean(targetServer != null);
            if (targetServer != null) dos.writeUTF(targetServer);

            dos.writeByte(priority.ordinal());
            dos.writeLong(ttlMillis);
            dos.writeByte(reason.ordinal());

            dos.writeInt(inventoryData.length);
            dos.write(inventoryData);

            dos.writeInt(metadata.size());
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize packet", e);
        }
    }

    /**
     * Serializes this packet to Base64.
     *
     * @return the Base64-encoded packet
     * @since 1.0.0
     */
    @NotNull
    public String toBase64() {
        return Base64.getEncoder().encodeToString(toBytes());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransferPacket that)) return false;
        return packetId.equals(that.packetId);
    }

    @Override
    public int hashCode() {
        return packetId.hashCode();
    }

    @Override
    public String toString() {
        return "TransferPacket{" +
            "id='" + packetId + '\'' +
            ", player=" + playerId +
            ", source='" + sourceServer + '\'' +
            ", target='" + targetServer + '\'' +
            ", priority=" + priority +
            ", reason=" + reason +
            ", dataSize=" + inventoryData.length +
            ", expired=" + isExpired() +
            '}';
    }

    // ========== Enums ==========

    /**
     * Transfer priority levels.
     *
     * @since 1.0.0
     */
    public enum TransferPriority {
        /**
         * Low priority - processed after normal and high.
         */
        LOW,

        /**
         * Normal priority - default level.
         */
        NORMAL,

        /**
         * High priority - processed before normal and low.
         */
        HIGH,

        /**
         * Immediate priority - processed as soon as possible.
         */
        IMMEDIATE
    }

    /**
     * Reasons for inventory transfer.
     *
     * @since 1.0.0
     */
    public enum TransferReason {
        /**
         * Player is transferring to another server.
         */
        SERVER_TRANSFER,

        /**
         * Player disconnected and inventory is being preserved.
         */
        DISCONNECT,

        /**
         * Manual backup request.
         */
        BACKUP,

        /**
         * Synchronization between servers.
         */
        SYNC,

        /**
         * Rollback/restore operation.
         */
        RESTORE,

        /**
         * Plugin-initiated transfer.
         */
        PLUGIN
    }

    // ========== Builder ==========

    /**
     * Builder for TransferPacket.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final UUID playerId;
        private final byte[] inventoryData;

        private String packetId;
        private Instant createdAt;
        private String sourceServer;
        private String targetServer;
        private TransferPriority priority;
        private long ttlMillis;
        private Map<String, String> metadata;
        private TransferReason reason;

        private Builder(@NotNull UUID playerId, byte @NotNull [] inventoryData) {
            this.playerId = playerId;
            this.inventoryData = inventoryData.clone();
        }

        @NotNull
        public Builder packetId(@NotNull String packetId) {
            this.packetId = packetId;
            return this;
        }

        @NotNull
        public Builder createdAt(@NotNull Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @NotNull
        public Builder sourceServer(@Nullable String sourceServer) {
            this.sourceServer = sourceServer;
            return this;
        }

        @NotNull
        public Builder targetServer(@Nullable String targetServer) {
            this.targetServer = targetServer;
            return this;
        }

        @NotNull
        public Builder priority(@NotNull TransferPriority priority) {
            this.priority = priority;
            return this;
        }

        @NotNull
        public Builder ttlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
            return this;
        }

        @NotNull
        public Builder ttlSeconds(long seconds) {
            this.ttlMillis = seconds * 1000;
            return this;
        }

        @NotNull
        public Builder reason(@NotNull TransferReason reason) {
            this.reason = reason;
            return this;
        }

        @NotNull
        public Builder metadata(@NotNull Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        @NotNull
        public TransferPacket build() {
            return new TransferPacket(this);
        }
    }
}
