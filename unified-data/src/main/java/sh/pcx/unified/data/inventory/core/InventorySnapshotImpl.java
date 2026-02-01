/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.inventory.core;

import sh.pcx.unified.data.inventory.serialization.InventorySerializer;
import sh.pcx.unified.item.UnifiedItemStack;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * Default implementation of {@link InventorySnapshot}.
 *
 * <p>This class provides an immutable snapshot of a player's inventory state.
 * All arrays are defensively copied to ensure immutability.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class InventorySnapshotImpl implements InventorySnapshot {

    private final String snapshotId;
    private final UUID playerId;
    private final Instant capturedAt;
    private final int version;

    private final UnifiedItemStack[] contents;
    private final UnifiedItemStack[] armor;
    private final UnifiedItemStack offhand;
    private final UnifiedItemStack[] enderChest;

    private InventorySnapshotImpl(BuilderImpl builder) {
        this.snapshotId = builder.snapshotId != null ? builder.snapshotId : UUID.randomUUID().toString();
        this.playerId = builder.playerId;
        this.capturedAt = builder.capturedAt != null ? builder.capturedAt : Instant.now();
        this.version = builder.version;

        // Defensive copies
        this.contents = builder.contents != null
            ? Arrays.copyOf(builder.contents, InventorySlot.MAIN_INVENTORY_SIZE)
            : new UnifiedItemStack[InventorySlot.MAIN_INVENTORY_SIZE];
        this.armor = builder.armor != null
            ? Arrays.copyOf(builder.armor, InventorySlot.ARMOR_SIZE)
            : new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
        this.offhand = builder.offhand;
        this.enderChest = builder.enderChest != null
            ? Arrays.copyOf(builder.enderChest, builder.enderChest.length)
            : null;
    }

    @Override
    @NotNull
    public String getSnapshotId() {
        return snapshotId;
    }

    @Override
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    @NotNull
    public Instant getCapturedAt() {
        return capturedAt;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    @NotNull
    public UnifiedItemStack[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    @Override
    @NotNull
    public UnifiedItemStack[] getArmorContents() {
        return Arrays.copyOf(armor, armor.length);
    }

    @Override
    @NotNull
    public UnifiedItemStack getOffhand() {
        return offhand != null ? offhand : UnifiedItemStack.empty();
    }

    @Override
    @NotNull
    public UnifiedItemStack[] getEnderChest() {
        if (enderChest == null) {
            return new UnifiedItemStack[0];
        }
        return Arrays.copyOf(enderChest, enderChest.length);
    }

    @Override
    public boolean hasEnderChest() {
        return enderChest != null && enderChest.length > 0;
    }

    @Override
    @NotNull
    public UnifiedItemStack getItem(int slot) {
        if (slot < 0 || slot >= contents.length) {
            throw new IndexOutOfBoundsException("Slot index out of range: " + slot);
        }
        UnifiedItemStack item = contents[slot];
        return item != null ? item : UnifiedItemStack.empty();
    }

    @Override
    @NotNull
    public List<InventorySlot> getAllSlots() {
        List<InventorySlot> slots = new ArrayList<>();

        // Main inventory
        for (int i = 0; i < contents.length; i++) {
            slots.add(InventorySlot.main(i, contents[i]));
        }

        // Armor
        slots.add(InventorySlot.armor(InventorySlot.BOOTS_SLOT, armor[0]));
        slots.add(InventorySlot.armor(InventorySlot.LEGGINGS_SLOT, armor[1]));
        slots.add(InventorySlot.armor(InventorySlot.CHESTPLATE_SLOT, armor[2]));
        slots.add(InventorySlot.armor(InventorySlot.HELMET_SLOT, armor[3]));

        // Offhand
        slots.add(InventorySlot.offhand(offhand));

        // Ender chest
        if (enderChest != null) {
            for (int i = 0; i < enderChest.length; i++) {
                slots.add(InventorySlot.enderChest(i, enderChest[i]));
            }
        }

        return Collections.unmodifiableList(slots);
    }

    @Override
    @NotNull
    public List<InventorySlot> getNonEmptySlots() {
        return getAllSlots().stream()
            .filter(slot -> !slot.isEmpty())
            .toList();
    }

    @Override
    @NotNull
    public UnifiedItemStack getHelmet() {
        return armor[3] != null ? armor[3] : UnifiedItemStack.empty();
    }

    @Override
    @NotNull
    public UnifiedItemStack getChestplate() {
        return armor[2] != null ? armor[2] : UnifiedItemStack.empty();
    }

    @Override
    @NotNull
    public UnifiedItemStack getLeggings() {
        return armor[1] != null ? armor[1] : UnifiedItemStack.empty();
    }

    @Override
    @NotNull
    public UnifiedItemStack getBoots() {
        return armor[0] != null ? armor[0] : UnifiedItemStack.empty();
    }

    @Override
    public int getTotalItemCount() {
        int count = 0;

        for (UnifiedItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                count += item.getAmount();
            }
        }

        for (UnifiedItemStack item : armor) {
            if (item != null && !item.isEmpty()) {
                count += item.getAmount();
            }
        }

        if (offhand != null && !offhand.isEmpty()) {
            count += offhand.getAmount();
        }

        if (enderChest != null) {
            for (UnifiedItemStack item : enderChest) {
                if (item != null && !item.isEmpty()) {
                    count += item.getAmount();
                }
            }
        }

        return count;
    }

    @Override
    public int getEmptySlotCount() {
        int count = 0;
        for (UnifiedItemStack item : contents) {
            if (item == null || item.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        for (UnifiedItemStack item : contents) {
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }

        for (UnifiedItemStack item : armor) {
            if (item != null && !item.isEmpty()) {
                return false;
            }
        }

        if (offhand != null && !offhand.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public byte @NotNull [] toBytes() {
        return InventorySerializer.getInstance().serializeSnapshot(this);
    }

    @Override
    @NotNull
    public String toBase64() {
        return Base64.getEncoder().encodeToString(toBytes());
    }

    @Override
    @NotNull
    public String toJson() {
        return InventorySerializer.getInstance().serializeSnapshotToJson(this);
    }

    @Override
    public void applyTo(@NotNull UnifiedPlayer player) {
        applyTo(player, ApplyMode.REPLACE);
    }

    @Override
    public void applyTo(@NotNull UnifiedPlayer player, @NotNull ApplyMode mode) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(mode, "ApplyMode cannot be null");

        // Delegate to the service for actual application
        InventoryService.getInstance().apply(player, this, mode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventorySnapshotImpl that)) return false;
        return snapshotId.equals(that.snapshotId);
    }

    @Override
    public int hashCode() {
        return snapshotId.hashCode();
    }

    @Override
    public String toString() {
        return "InventorySnapshot{" +
            "id='" + snapshotId + '\'' +
            ", playerId=" + playerId +
            ", capturedAt=" + capturedAt +
            ", version=" + version +
            ", itemCount=" + getTotalItemCount() +
            ", hasEnderChest=" + hasEnderChest() +
            '}';
    }

    /**
     * Builder implementation for InventorySnapshot.
     */
    public static final class BuilderImpl implements InventorySnapshot.Builder {

        private final UUID playerId;
        private String snapshotId;
        private Instant capturedAt;
        private int version = 1;

        private UnifiedItemStack[] contents;
        private UnifiedItemStack[] armor;
        private UnifiedItemStack offhand;
        private UnifiedItemStack[] enderChest;

        public BuilderImpl(@NotNull UUID playerId) {
            this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
            this.contents = new UnifiedItemStack[InventorySlot.MAIN_INVENTORY_SIZE];
            this.armor = new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
        }

        @Override
        @NotNull
        public Builder contents(@NotNull UnifiedItemStack[] contents) {
            this.contents = Arrays.copyOf(contents, InventorySlot.MAIN_INVENTORY_SIZE);
            return this;
        }

        @Override
        @NotNull
        public Builder armor(@NotNull UnifiedItemStack[] armor) {
            this.armor = Arrays.copyOf(armor, InventorySlot.ARMOR_SIZE);
            return this;
        }

        @Override
        @NotNull
        public Builder offhand(@Nullable UnifiedItemStack offhand) {
            this.offhand = offhand;
            return this;
        }

        @Override
        @NotNull
        public Builder enderChest(@NotNull UnifiedItemStack[] enderChest) {
            this.enderChest = Arrays.copyOf(enderChest, enderChest.length);
            return this;
        }

        @Override
        @NotNull
        public Builder slot(int slot, @Nullable UnifiedItemStack item) {
            if (slot < 0 || slot >= InventorySlot.MAIN_INVENTORY_SIZE) {
                throw new IllegalArgumentException("Slot must be 0-35: " + slot);
            }
            if (contents == null) {
                contents = new UnifiedItemStack[InventorySlot.MAIN_INVENTORY_SIZE];
            }
            contents[slot] = item;
            return this;
        }

        @Override
        @NotNull
        public Builder helmet(@Nullable UnifiedItemStack helmet) {
            if (armor == null) {
                armor = new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
            }
            armor[3] = helmet;
            return this;
        }

        @Override
        @NotNull
        public Builder chestplate(@Nullable UnifiedItemStack chestplate) {
            if (armor == null) {
                armor = new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
            }
            armor[2] = chestplate;
            return this;
        }

        @Override
        @NotNull
        public Builder leggings(@Nullable UnifiedItemStack leggings) {
            if (armor == null) {
                armor = new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
            }
            armor[1] = leggings;
            return this;
        }

        @Override
        @NotNull
        public Builder boots(@Nullable UnifiedItemStack boots) {
            if (armor == null) {
                armor = new UnifiedItemStack[InventorySlot.ARMOR_SIZE];
            }
            armor[0] = boots;
            return this;
        }

        @Override
        @NotNull
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        @Override
        @NotNull
        public Builder snapshotId(@NotNull String snapshotId) {
            this.snapshotId = Objects.requireNonNull(snapshotId);
            return this;
        }

        @Override
        @NotNull
        public Builder capturedAt(@NotNull Instant timestamp) {
            this.capturedAt = Objects.requireNonNull(timestamp);
            return this;
        }

        @Override
        @NotNull
        public InventorySnapshot build() {
            return new InventorySnapshotImpl(this);
        }
    }
}
