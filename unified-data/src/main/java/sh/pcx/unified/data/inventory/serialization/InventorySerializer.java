/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.serialization;

import sh.pcx.unified.data.inventory.core.InventorySlot;
import sh.pcx.unified.data.inventory.core.InventorySnapshot;
import sh.pcx.unified.data.inventory.core.InventorySnapshotImpl;
import sh.pcx.unified.item.UnifiedItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Serializes and deserializes complete inventory snapshots.
 *
 * <p>InventorySerializer handles the serialization of entire inventory states,
 * including main inventory, armor, offhand, ender chest, and metadata. It
 * supports multiple serialization formats and ensures complete fidelity
 * during round-trip serialization.
 *
 * <h2>Serialization Contents</h2>
 * <ul>
 *   <li>Snapshot metadata (ID, player ID, timestamp, version)</li>
 *   <li>Main inventory (36 slots)</li>
 *   <li>Armor (4 slots)</li>
 *   <li>Offhand (1 slot)</li>
 *   <li>Ender chest (27 slots, if captured)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private InventorySerializer serializer;
 *
 * // Serialize a snapshot
 * byte[] bytes = serializer.serializeSnapshot(snapshot);
 * String base64 = serializer.serializeSnapshotToBase64(snapshot);
 * String json = serializer.serializeSnapshotToJson(snapshot);
 *
 * // Deserialize
 * InventorySnapshot fromBytes = serializer.deserializeSnapshot(bytes);
 * InventorySnapshot fromBase64 = serializer.deserializeSnapshotFromBase64(base64);
 * InventorySnapshot fromJson = serializer.deserializeSnapshotFromJson(json);
 *
 * // With specific format
 * byte[] data = serializer.serialize(snapshot, SerializationFormat.COMPRESSED);
 * InventorySnapshot restored = serializer.deserialize(data, SerializationFormat.COMPRESSED);
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see InventorySnapshot
 * @see ItemSerializer
 * @see SerializationFormat
 */
public class InventorySerializer {

    private static final int MAGIC_NUMBER = 0x494E5653; // "INVS"
    private static final int VERSION = 1;

    private static InventorySerializer instance;

    private final ItemSerializer itemSerializer;

    /**
     * Creates an InventorySerializer with default item serializer.
     *
     * @since 1.0.0
     */
    public InventorySerializer() {
        this(ItemSerializer.getInstance());
    }

    /**
     * Creates an InventorySerializer with a specific item serializer.
     *
     * @param itemSerializer the item serializer to use
     * @since 1.0.0
     */
    public InventorySerializer(@NotNull ItemSerializer itemSerializer) {
        this.itemSerializer = Objects.requireNonNull(itemSerializer);
    }

    /**
     * Returns the singleton instance.
     *
     * @return the inventory serializer instance
     * @since 1.0.0
     */
    @NotNull
    public static InventorySerializer getInstance() {
        if (instance == null) {
            instance = new InventorySerializer();
        }
        return instance;
    }

    /**
     * Sets the singleton instance.
     *
     * @param serializer the serializer instance
     * @since 1.0.0
     */
    public static void setInstance(@NotNull InventorySerializer serializer) {
        instance = serializer;
    }

    // ========== Binary Serialization ==========

    /**
     * Serializes a snapshot to bytes.
     *
     * @param snapshot the snapshot to serialize
     * @return the serialized bytes
     * @since 1.0.0
     */
    public byte @NotNull [] serializeSnapshot(@NotNull InventorySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Header
            dos.writeInt(MAGIC_NUMBER);
            dos.writeInt(VERSION);

            // Metadata
            dos.writeUTF(snapshot.getSnapshotId());
            dos.writeLong(snapshot.getPlayerId().getMostSignificantBits());
            dos.writeLong(snapshot.getPlayerId().getLeastSignificantBits());
            dos.writeLong(snapshot.getCapturedAt().toEpochMilli());
            dos.writeInt(snapshot.getVersion());

            // Main inventory
            UnifiedItemStack[] contents = snapshot.getContents();
            dos.writeInt(contents.length);
            for (UnifiedItemStack item : contents) {
                writeItem(dos, item);
            }

            // Armor
            UnifiedItemStack[] armor = snapshot.getArmorContents();
            dos.writeInt(armor.length);
            for (UnifiedItemStack item : armor) {
                writeItem(dos, item);
            }

            // Offhand
            writeItem(dos, snapshot.getOffhand());

            // Ender chest
            boolean hasEnderChest = snapshot.hasEnderChest();
            dos.writeBoolean(hasEnderChest);
            if (hasEnderChest) {
                UnifiedItemStack[] enderChest = snapshot.getEnderChest();
                dos.writeInt(enderChest.length);
                for (UnifiedItemStack item : enderChest) {
                    writeItem(dos, item);
                }
            }

            return baos.toByteArray();

        } catch (IOException e) {
            throw new ItemSerializer.SerializationException("Failed to serialize snapshot", e);
        }
    }

    /**
     * Deserializes a snapshot from bytes.
     *
     * @param bytes the serialized bytes
     * @return the deserialized snapshot
     * @since 1.0.0
     */
    @NotNull
    public InventorySnapshot deserializeSnapshot(byte @NotNull [] bytes) {
        Objects.requireNonNull(bytes, "Bytes cannot be null");

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {

            // Header
            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new ItemSerializer.SerializationException("Invalid snapshot magic number");
            }

            int version = dis.readInt();
            if (version != VERSION) {
                throw new ItemSerializer.SerializationException("Unsupported snapshot version: " + version);
            }

            // Metadata
            String snapshotId = dis.readUTF();
            UUID playerId = new UUID(dis.readLong(), dis.readLong());
            Instant capturedAt = Instant.ofEpochMilli(dis.readLong());
            int snapshotVersion = dis.readInt();

            // Main inventory
            int contentsLength = dis.readInt();
            UnifiedItemStack[] contents = new UnifiedItemStack[contentsLength];
            for (int i = 0; i < contentsLength; i++) {
                contents[i] = readItem(dis);
            }

            // Armor
            int armorLength = dis.readInt();
            UnifiedItemStack[] armor = new UnifiedItemStack[armorLength];
            for (int i = 0; i < armorLength; i++) {
                armor[i] = readItem(dis);
            }

            // Offhand
            UnifiedItemStack offhand = readItem(dis);

            // Ender chest
            UnifiedItemStack[] enderChest = null;
            if (dis.readBoolean()) {
                int enderLength = dis.readInt();
                enderChest = new UnifiedItemStack[enderLength];
                for (int i = 0; i < enderLength; i++) {
                    enderChest[i] = readItem(dis);
                }
            }

            // Build snapshot
            InventorySnapshot.Builder builder = InventorySnapshot.builder(playerId)
                .snapshotId(snapshotId)
                .capturedAt(capturedAt)
                .version(snapshotVersion)
                .contents(contents)
                .armor(armor)
                .offhand(offhand);

            if (enderChest != null) {
                builder.enderChest(enderChest);
            }

            return builder.build();

        } catch (IOException e) {
            throw new ItemSerializer.SerializationException("Failed to deserialize snapshot", e);
        }
    }

    // ========== Base64 Serialization ==========

    /**
     * Serializes a snapshot to Base64.
     *
     * @param snapshot the snapshot to serialize
     * @return the Base64-encoded string
     * @since 1.0.0
     */
    @NotNull
    public String serializeSnapshotToBase64(@NotNull InventorySnapshot snapshot) {
        return Base64.getEncoder().encodeToString(serializeSnapshot(snapshot));
    }

    /**
     * Deserializes a snapshot from Base64.
     *
     * @param base64 the Base64-encoded string
     * @return the deserialized snapshot
     * @since 1.0.0
     */
    @NotNull
    public InventorySnapshot deserializeSnapshotFromBase64(@NotNull String base64) {
        return deserializeSnapshot(Base64.getDecoder().decode(base64));
    }

    // ========== JSON Serialization ==========

    /**
     * Serializes a snapshot to JSON.
     *
     * @param snapshot the snapshot to serialize
     * @return the JSON string
     * @since 1.0.0
     */
    @NotNull
    public String serializeSnapshotToJson(@NotNull InventorySnapshot snapshot) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Metadata
        json.append("\"snapshotId\":\"").append(snapshot.getSnapshotId()).append("\"");
        json.append(",\"playerId\":\"").append(snapshot.getPlayerId()).append("\"");
        json.append(",\"capturedAt\":").append(snapshot.getCapturedAt().toEpochMilli());
        json.append(",\"version\":").append(snapshot.getVersion());

        // Main inventory
        json.append(",\"contents\":[");
        UnifiedItemStack[] contents = snapshot.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (i > 0) json.append(",");
            json.append(itemSerializer.toJson(contents[i]));
        }
        json.append("]");

        // Armor
        json.append(",\"armor\":[");
        UnifiedItemStack[] armor = snapshot.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (i > 0) json.append(",");
            json.append(itemSerializer.toJson(armor[i]));
        }
        json.append("]");

        // Offhand
        json.append(",\"offhand\":").append(itemSerializer.toJson(snapshot.getOffhand()));

        // Ender chest
        if (snapshot.hasEnderChest()) {
            json.append(",\"enderChest\":[");
            UnifiedItemStack[] enderChest = snapshot.getEnderChest();
            for (int i = 0; i < enderChest.length; i++) {
                if (i > 0) json.append(",");
                json.append(itemSerializer.toJson(enderChest[i]));
            }
            json.append("]");
        }

        // Include binary data for full fidelity restoration
        json.append(",\"_data\":\"").append(serializeSnapshotToBase64(snapshot)).append("\"");

        json.append("}");
        return json.toString();
    }

    /**
     * Deserializes a snapshot from JSON.
     *
     * @param json the JSON string
     * @return the deserialized snapshot
     * @since 1.0.0
     */
    @NotNull
    public InventorySnapshot deserializeSnapshotFromJson(@NotNull String json) {
        // Look for _data field for full fidelity restoration
        int dataIndex = json.indexOf("\"_data\":\"");
        if (dataIndex != -1) {
            int start = dataIndex + 9;
            int end = json.indexOf("\"", start);
            if (end != -1) {
                String base64 = json.substring(start, end);
                return deserializeSnapshotFromBase64(base64);
            }
        }

        throw new ItemSerializer.SerializationException(
            "JSON parsing without _data field not implemented");
    }

    // ========== Compressed Serialization ==========

    /**
     * Serializes a snapshot with compression.
     *
     * @param snapshot the snapshot to serialize
     * @return the compressed bytes
     * @since 1.0.0
     */
    public byte @NotNull [] serializeSnapshotCompressed(@NotNull InventorySnapshot snapshot) {
        byte[] uncompressed = serializeSnapshot(snapshot);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {

            gzos.write(uncompressed);
            gzos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new ItemSerializer.SerializationException("Failed to compress snapshot", e);
        }
    }

    /**
     * Deserializes a compressed snapshot.
     *
     * @param compressed the compressed bytes
     * @return the deserialized snapshot
     * @since 1.0.0
     */
    @NotNull
    public InventorySnapshot deserializeSnapshotCompressed(byte @NotNull [] compressed) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            return deserializeSnapshot(baos.toByteArray());

        } catch (IOException e) {
            throw new ItemSerializer.SerializationException("Failed to decompress snapshot", e);
        }
    }

    // ========== Format-Specific Serialization ==========

    /**
     * Serializes a snapshot with a specific format.
     *
     * @param snapshot the snapshot to serialize
     * @param format   the serialization format
     * @return the serialized data as bytes
     * @since 1.0.0
     */
    public byte @NotNull [] serialize(@NotNull InventorySnapshot snapshot, @NotNull SerializationFormat format) {
        return switch (format) {
            case BINARY -> serializeSnapshot(snapshot);
            case BASE64 -> serializeSnapshotToBase64(snapshot).getBytes();
            case JSON -> serializeSnapshotToJson(snapshot).getBytes();
            case COMPRESSED -> serializeSnapshotCompressed(snapshot);
            case NBT -> throw new UnsupportedOperationException("NBT format not implemented");
        };
    }

    /**
     * Deserializes a snapshot with a specific format.
     *
     * @param data   the serialized data
     * @param format the serialization format
     * @return the deserialized snapshot
     * @since 1.0.0
     */
    @NotNull
    public InventorySnapshot deserialize(byte @NotNull [] data, @NotNull SerializationFormat format) {
        return switch (format) {
            case BINARY -> deserializeSnapshot(data);
            case BASE64 -> deserializeSnapshotFromBase64(new String(data));
            case JSON -> deserializeSnapshotFromJson(new String(data));
            case COMPRESSED -> deserializeSnapshotCompressed(data);
            case NBT -> throw new UnsupportedOperationException("NBT format not implemented");
        };
    }

    // ========== Helper Methods ==========

    private void writeItem(DataOutputStream dos, UnifiedItemStack item) throws IOException {
        byte[] itemData = item != null ? itemSerializer.toBytes(item) : new byte[0];
        dos.writeInt(itemData.length);
        if (itemData.length > 0) {
            dos.write(itemData);
        }
    }

    private UnifiedItemStack readItem(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length == 0) {
            return null;
        }
        byte[] itemData = new byte[length];
        dis.readFully(itemData);
        return itemSerializer.fromBytes(itemData);
    }

    /**
     * Returns the item serializer.
     *
     * @return the item serializer
     * @since 1.0.0
     */
    @NotNull
    public ItemSerializer getItemSerializer() {
        return itemSerializer;
    }

    /**
     * Estimates the serialized size of a snapshot.
     *
     * @param snapshot the snapshot
     * @return estimated size in bytes
     * @since 1.0.0
     */
    public int estimateSize(@NotNull InventorySnapshot snapshot) {
        // Rough estimate: header (20) + metadata (100) + items
        int estimate = 120;

        // Main inventory: ~100 bytes per non-empty slot
        estimate += snapshot.getNonEmptySlots().size() * 100;

        // Ender chest
        if (snapshot.hasEnderChest()) {
            estimate += 50; // header
            UnifiedItemStack[] enderChest = snapshot.getEnderChest();
            for (UnifiedItemStack item : enderChest) {
                if (item != null && !item.isEmpty()) {
                    estimate += 100;
                }
            }
        }

        return estimate;
    }
}
