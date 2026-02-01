/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.version.api;

import sh.pcx.unified.service.Service;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for version-safe NBT (Named Binary Tag) manipulation.
 *
 * <p>This service provides a unified API for reading and writing NBT data across
 * different Minecraft versions. It handles the differences between legacy NBT
 * systems and the newer Data Components system introduced in 1.20.5.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Inject
 * private NBTService nbt;
 *
 * public void modifyItem(ItemStack item) {
 *     // Read NBT from item
 *     NBTCompound compound = nbt.fromItemStack(item);
 *
 *     // Read values
 *     String customData = compound.getString("custom_key");
 *     int level = compound.getInt("level");
 *
 *     // Modify values
 *     compound.setString("custom_key", "new_value");
 *     compound.setInt("level", level + 1);
 *     compound.setList("tags", List.of("rare", "soulbound"));
 *
 *     // Apply back to item
 *     ItemStack modified = nbt.toItemStack(item, compound);
 * }
 *
 * public void modifyEntity(Entity entity) {
 *     // Read entity NBT
 *     NBTCompound entityNbt = nbt.fromEntity(entity);
 *
 *     // Modify AI
 *     entityNbt.setBoolean("NoAI", true);
 *     entityNbt.setBoolean("Silent", true);
 *
 *     // Apply changes
 *     nbt.applyToEntity(entity, entityNbt);
 * }
 * }</pre>
 *
 * <h2>Data Components (1.20.5+)</h2>
 * <p>Starting with Minecraft 1.20.5, items use Data Components instead of NBT tags.
 * This service transparently handles both systems:
 * <ul>
 *   <li>On 1.20.5+: Uses Data Components API with NBT-like wrapper</li>
 *   <li>Pre-1.20.5: Direct NBT manipulation (if supported)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>NBTCompound instances are NOT thread-safe. Create separate instances for
 * each thread or use external synchronization.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see NBTCompound
 * @see NMSBridge
 */
public interface NBTService extends Service {

    // ===== Item Stack Operations =====

    /**
     * Reads NBT data from an item stack.
     *
     * <p>Returns a copy of the item's NBT data. Modifications to the returned
     * compound do not affect the original item.
     *
     * @param item the item stack
     * @return the NBT compound (empty if item has no NBT)
     * @since 1.0.0
     */
    @NotNull
    NBTCompound fromItemStack(@NotNull ItemStack item);

    /**
     * Creates a new item stack with the specified NBT data.
     *
     * @param item     the original item stack (used as template)
     * @param compound the NBT data to apply
     * @return a new item stack with the applied NBT
     * @since 1.0.0
     */
    @NotNull
    ItemStack toItemStack(@NotNull ItemStack item, @NotNull NBTCompound compound);

    /**
     * Checks if an item stack has custom NBT data.
     *
     * @param item the item stack
     * @return true if the item has custom NBT
     * @since 1.0.0
     */
    boolean hasNBT(@NotNull ItemStack item);

    /**
     * Removes all custom NBT data from an item stack.
     *
     * @param item the item stack
     * @return a new item stack without custom NBT
     * @since 1.0.0
     */
    @NotNull
    ItemStack removeNBT(@NotNull ItemStack item);

    // ===== Entity Operations =====

    /**
     * Reads NBT data from an entity.
     *
     * @param entity the entity
     * @return the entity's NBT compound
     * @since 1.0.0
     */
    @NotNull
    NBTCompound fromEntity(@NotNull Entity entity);

    /**
     * Applies NBT data to an entity.
     *
     * <p>This modifies the entity's server-side state.
     *
     * @param entity   the entity
     * @param compound the NBT data to apply
     * @since 1.0.0
     */
    void applyToEntity(@NotNull Entity entity, @NotNull NBTCompound compound);

    // ===== Block Entity Operations =====

    /**
     * Reads NBT data from a tile entity (block with state).
     *
     * @param blockState the block state
     * @return the block entity's NBT compound
     * @since 1.0.0
     */
    @NotNull
    NBTCompound fromBlockEntity(@NotNull BlockState blockState);

    /**
     * Applies NBT data to a tile entity.
     *
     * @param blockState the block state
     * @param compound   the NBT data to apply
     * @since 1.0.0
     */
    void applyToBlockEntity(@NotNull BlockState blockState, @NotNull NBTCompound compound);

    // ===== File Operations =====

    /**
     * Reads an NBT compound from a file.
     *
     * @param file the file to read from
     * @return the NBT compound
     * @throws IOException if reading fails
     * @since 1.0.0
     */
    @NotNull
    NBTCompound readFromFile(@NotNull File file) throws IOException;

    /**
     * Writes an NBT compound to a file.
     *
     * @param compound the NBT compound
     * @param file     the file to write to
     * @throws IOException if writing fails
     * @since 1.0.0
     */
    void writeToFile(@NotNull NBTCompound compound, @NotNull File file) throws IOException;

    /**
     * Reads an NBT compound from an input stream.
     *
     * @param input the input stream
     * @return the NBT compound
     * @throws IOException if reading fails
     * @since 1.0.0
     */
    @NotNull
    NBTCompound readFromStream(@NotNull InputStream input) throws IOException;

    /**
     * Writes an NBT compound to an output stream.
     *
     * @param compound the NBT compound
     * @param output   the output stream
     * @throws IOException if writing fails
     * @since 1.0.0
     */
    void writeToStream(@NotNull NBTCompound compound, @NotNull OutputStream output) throws IOException;

    // ===== Factory Methods =====

    /**
     * Creates a new empty NBT compound.
     *
     * @return a new empty compound
     * @since 1.0.0
     */
    @NotNull
    NBTCompound createCompound();

    /**
     * Creates a new NBT list.
     *
     * @return a new empty list
     * @since 1.0.0
     */
    @NotNull
    NBTList createList();

    /**
     * Parses NBT from SNBT (Stringified NBT) format.
     *
     * @param snbt the SNBT string
     * @return the parsed NBT compound
     * @throws IllegalArgumentException if parsing fails
     * @since 1.0.0
     */
    @NotNull
    NBTCompound parseSnbt(@NotNull String snbt);

    /**
     * Converts an NBT compound to SNBT format.
     *
     * @param compound the NBT compound
     * @return the SNBT string
     * @since 1.0.0
     */
    @NotNull
    String toSnbt(@NotNull NBTCompound compound);

    @Override
    default String getServiceName() {
        return "NBTService";
    }

    /**
     * Represents an NBT compound tag (map of key-value pairs).
     *
     * <p>This interface provides a type-safe wrapper around NBT operations.
     *
     * @since 1.0.0
     */
    interface NBTCompound {

        // ===== Primitive Getters =====

        /**
         * Gets a byte value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        byte getByte(@NotNull String key);

        /**
         * Gets a short value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        short getShort(@NotNull String key);

        /**
         * Gets an integer value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        int getInt(@NotNull String key);

        /**
         * Gets a long value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        long getLong(@NotNull String key);

        /**
         * Gets a float value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        float getFloat(@NotNull String key);

        /**
         * Gets a double value.
         *
         * @param key the key
         * @return the value, or 0 if not present
         */
        double getDouble(@NotNull String key);

        /**
         * Gets a boolean value.
         *
         * @param key the key
         * @return the value, or false if not present
         */
        boolean getBoolean(@NotNull String key);

        /**
         * Gets a string value.
         *
         * @param key the key
         * @return the value, or empty string if not present
         */
        @NotNull
        String getString(@NotNull String key);

        /**
         * Gets a byte array value.
         *
         * @param key the key
         * @return the value, or empty array if not present
         */
        byte[] getByteArray(@NotNull String key);

        /**
         * Gets an integer array value.
         *
         * @param key the key
         * @return the value, or empty array if not present
         */
        int[] getIntArray(@NotNull String key);

        /**
         * Gets a long array value.
         *
         * @param key the key
         * @return the value, or empty array if not present
         */
        long[] getLongArray(@NotNull String key);

        /**
         * Gets a UUID value.
         *
         * @param key the key
         * @return the value, or null if not present
         */
        @Nullable
        UUID getUUID(@NotNull String key);

        // ===== Compound Getters =====

        /**
         * Gets a nested compound.
         *
         * @param key the key
         * @return the compound, or a new empty compound if not present
         */
        @NotNull
        NBTCompound getCompound(@NotNull String key);

        /**
         * Gets a list.
         *
         * @param key the key
         * @return the list, or a new empty list if not present
         */
        @NotNull
        NBTList getList(@NotNull String key);

        // ===== Primitive Setters =====

        /**
         * Sets a byte value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setByte(@NotNull String key, byte value);

        /**
         * Sets a short value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setShort(@NotNull String key, short value);

        /**
         * Sets an integer value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setInt(@NotNull String key, int value);

        /**
         * Sets a long value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setLong(@NotNull String key, long value);

        /**
         * Sets a float value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setFloat(@NotNull String key, float value);

        /**
         * Sets a double value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setDouble(@NotNull String key, double value);

        /**
         * Sets a boolean value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setBoolean(@NotNull String key, boolean value);

        /**
         * Sets a string value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setString(@NotNull String key, @NotNull String value);

        /**
         * Sets a byte array value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setByteArray(@NotNull String key, byte[] value);

        /**
         * Sets an integer array value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setIntArray(@NotNull String key, int[] value);

        /**
         * Sets a long array value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setLongArray(@NotNull String key, long[] value);

        /**
         * Sets a UUID value.
         *
         * @param key   the key
         * @param value the value
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setUUID(@NotNull String key, @NotNull UUID value);

        // ===== Compound Setters =====

        /**
         * Sets a nested compound.
         *
         * @param key      the key
         * @param compound the compound
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setCompound(@NotNull String key, @NotNull NBTCompound compound);

        /**
         * Sets a list from a collection of strings.
         *
         * @param key    the key
         * @param values the string values
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setList(@NotNull String key, @NotNull List<String> values);

        /**
         * Sets a list.
         *
         * @param key  the key
         * @param list the list
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound setList(@NotNull String key, @NotNull NBTList list);

        // ===== Utility Methods =====

        /**
         * Checks if a key exists.
         *
         * @param key the key
         * @return true if the key exists
         */
        boolean contains(@NotNull String key);

        /**
         * Checks if a key exists with a specific type.
         *
         * @param key     the key
         * @param typeId the NBT type ID
         * @return true if the key exists with the specified type
         */
        boolean contains(@NotNull String key, int typeId);

        /**
         * Removes a key.
         *
         * @param key the key
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound remove(@NotNull String key);

        /**
         * Gets all keys in this compound.
         *
         * @return set of keys
         */
        @NotNull
        Set<String> getKeys();

        /**
         * Gets the number of entries.
         *
         * @return the size
         */
        int size();

        /**
         * Checks if this compound is empty.
         *
         * @return true if empty
         */
        boolean isEmpty();

        /**
         * Clears all entries.
         *
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound clear();

        /**
         * Creates a deep copy of this compound.
         *
         * @return a new compound with copied data
         */
        @NotNull
        NBTCompound copy();

        /**
         * Merges another compound into this one.
         *
         * <p>Existing keys are overwritten by the source compound.
         *
         * @param source the compound to merge from
         * @return this compound for chaining
         */
        @NotNull
        NBTCompound merge(@NotNull NBTCompound source);

        /**
         * Converts this compound to a map.
         *
         * @return map representation
         */
        @NotNull
        Map<String, Object> toMap();

        /**
         * Gets the underlying NMS compound tag.
         *
         * @return the NMS object
         */
        @NotNull
        Object getHandle();
    }

    /**
     * Represents an NBT list tag.
     *
     * @since 1.0.0
     */
    interface NBTList {

        /**
         * Gets the size of this list.
         *
         * @return the size
         */
        int size();

        /**
         * Checks if this list is empty.
         *
         * @return true if empty
         */
        boolean isEmpty();

        /**
         * Gets a string at the specified index.
         *
         * @param index the index
         * @return the string value
         */
        @NotNull
        String getString(int index);

        /**
         * Gets a compound at the specified index.
         *
         * @param index the index
         * @return the compound value
         */
        @NotNull
        NBTCompound getCompound(int index);

        /**
         * Gets an integer at the specified index.
         *
         * @param index the index
         * @return the integer value
         */
        int getInt(int index);

        /**
         * Gets a double at the specified index.
         *
         * @param index the index
         * @return the double value
         */
        double getDouble(int index);

        /**
         * Adds a string to this list.
         *
         * @param value the value
         * @return this list for chaining
         */
        @NotNull
        NBTList addString(@NotNull String value);

        /**
         * Adds a compound to this list.
         *
         * @param compound the compound
         * @return this list for chaining
         */
        @NotNull
        NBTList addCompound(@NotNull NBTCompound compound);

        /**
         * Adds an integer to this list.
         *
         * @param value the value
         * @return this list for chaining
         */
        @NotNull
        NBTList addInt(int value);

        /**
         * Adds a double to this list.
         *
         * @param value the value
         * @return this list for chaining
         */
        @NotNull
        NBTList addDouble(double value);

        /**
         * Removes an element at the specified index.
         *
         * @param index the index
         * @return this list for chaining
         */
        @NotNull
        NBTList remove(int index);

        /**
         * Clears all elements.
         *
         * @return this list for chaining
         */
        @NotNull
        NBTList clear();

        /**
         * Creates a deep copy of this list.
         *
         * @return a new list with copied data
         */
        @NotNull
        NBTList copy();

        /**
         * Gets the underlying NMS list tag.
         *
         * @return the NMS object
         */
        @NotNull
        Object getHandle();
    }
}
