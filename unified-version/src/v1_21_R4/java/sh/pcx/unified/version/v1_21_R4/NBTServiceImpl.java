/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.v1_21_R4;

import sh.pcx.unified.version.api.NBTService;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * NBT service implementation for Minecraft 1.21.11+ (v1_21_R4).
 *
 * <p>This implementation handles NBT operations using Mojang mappings.
 *
 * <h2>CRITICAL Version Notes</h2>
 * <ul>
 *   <li><strong>Mojang Mappings:</strong> Class names are different!
 *       <ul>
 *         <li>CompoundTag (was NBTTagCompound)</li>
 *         <li>ListTag (was NBTTagList)</li>
 *         <li>NbtIo (was NBTCompressedStreamTools)</li>
 *         <li>TagParser (was MojangsonParser)</li>
 *       </ul>
 *   </li>
 *   <li>Data Components continue as in 1.20.5+</li>
 * </ul>
 *
 * <h2>Class Mappings</h2>
 * <table>
 *   <tr><th>Spigot Mapping</th><th>Mojang Mapping</th></tr>
 *   <tr><td>NBTTagCompound</td><td>CompoundTag</td></tr>
 *   <tr><td>NBTTagList</td><td>ListTag</td></tr>
 *   <tr><td>NBTTagString</td><td>StringTag</td></tr>
 *   <tr><td>NBTCompressedStreamTools</td><td>NbtIo</td></tr>
 *   <tr><td>MojangsonParser</td><td>TagParser</td></tr>
 * </table>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class NBTServiceImpl implements NBTService {

    // Mojang-mapped NBT class references
    // TODO: Initialize in constructor
    // private Class<?> compoundTagClass; // net.minecraft.nbt.CompoundTag
    // private Class<?> listTagClass;     // net.minecraft.nbt.ListTag
    // private Class<?> nbtIoClass;       // net.minecraft.nbt.NbtIo
    // private Class<?> tagParserClass;   // net.minecraft.nbt.TagParser

    /**
     * Creates a new NBT service for v1_21_R4 (Mojang mappings).
     */
    public NBTServiceImpl() {
        // TODO: Initialize Mojang-mapped NBT class references
        // Example:
        // compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag");
        // listTagClass = Class.forName("net.minecraft.nbt.ListTag");
        // nbtIoClass = Class.forName("net.minecraft.nbt.NbtIo");
    }

    @Override
    @NotNull
    public NBTCompound fromItemStack(@NotNull ItemStack item) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // Still uses Data Components (CustomData component)
        // CustomData.of(compoundTag) / customData.copyTag()
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public ItemStack toItemStack(@NotNull ItemStack item, @NotNull NBTCompound compound) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public boolean hasNBT(@NotNull ItemStack item) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        return false;
    }

    @Override
    @NotNull
    public ItemStack removeNBT(@NotNull ItemStack item) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound fromEntity(@NotNull Entity entity) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // Entity.saveWithoutId(CompoundTag) -> void
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public void applyToEntity(@NotNull Entity entity, @NotNull NBTCompound compound) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // Entity.load(CompoundTag) method
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound fromBlockEntity(@NotNull BlockState blockState) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // BlockEntity.saveWithFullMetadata(HolderLookup.Provider)
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public void applyToBlockEntity(@NotNull BlockState blockState, @NotNull NBTCompound compound) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound readFromFile(@NotNull File file) throws IOException {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // NbtIo.readCompressed(Path, NbtAccounter) or NbtIo.read(Path)
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public void writeToFile(@NotNull NBTCompound compound, @NotNull File file) throws IOException {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // NbtIo.writeCompressed(CompoundTag, Path)
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound readFromStream(@NotNull InputStream input) throws IOException {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public void writeToStream(@NotNull NBTCompound compound, @NotNull OutputStream output)
            throws IOException {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound createCompound() {
        // TODO: Return Mojang-mapped implementation
        // new CompoundTag()
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTList createList() {
        // TODO: Return Mojang-mapped implementation
        // new ListTag()
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public NBTCompound parseSnbt(@NotNull String snbt) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // TagParser.parseTag(String) returns CompoundTag
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    @NotNull
    public String toSnbt(@NotNull NBTCompound compound) {
        // TODO: Implement for v1_21_R4 with Mojang mappings
        // CompoundTag.toString() or SnbtSerializer
        throw new UnsupportedOperationException("Not yet implemented for v1_21_R4");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
