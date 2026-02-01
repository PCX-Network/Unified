/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Central registry for all serializers.
 *
 * <p>Serializers provides static access to built-in serializers and allows
 * registration of custom serializers. Built-in serializers are lazily initialized
 * on first access.
 *
 * <h2>Built-in Serializers</h2>
 * <ul>
 *   <li>{@link #itemStack()} - ItemStack serialization with full NBT</li>
 *   <li>{@link #itemMeta()} - ItemMeta serialization</li>
 *   <li>{@link #enchantment()} - Enchantment serialization</li>
 *   <li>{@link #potionEffect()} - Potion effect serialization</li>
 *   <li>{@link #location()} - Location with world reference</li>
 *   <li>{@link #blockData()} - Block data serialization</li>
 *   <li>{@link #chunk()} - Chunk position serialization</li>
 *   <li>{@link #component()} - Adventure Component serialization</li>
 *   <li>{@link #miniMessage()} - MiniMessage format</li>
 *   <li>{@link #legacyText()} - Legacy color codes</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Use built-in serializers
 * String encoded = Serializers.itemStack().serialize(item);
 * ItemStack restored = Serializers.itemStack().deserialize(encoded);
 *
 * // Register custom serializer
 * Serializers.register(MyClass.class, new MyClassSerializer());
 *
 * // Retrieve custom serializer
 * Serializer<MyClass> serializer = Serializers.get(MyClass.class)
 *     .orElseThrow(() -> new IllegalStateException("Serializer not registered"));
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Serializer
 * @see SerializationContext
 */
public final class Serializers {

    private static final Map<Class<?>, Serializer<?>> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Supplier<? extends Serializer<?>>> LAZY_REGISTRY = new ConcurrentHashMap<>();

    // Lazy initialization holders for built-in serializers
    private static volatile ItemStackSerializer itemStackSerializer;
    private static volatile ItemMetaSerializer itemMetaSerializer;
    private static volatile EnchantmentSerializer enchantmentSerializer;
    private static volatile PotionEffectSerializer potionEffectSerializer;
    private static volatile LocationSerializer locationSerializer;
    private static volatile BlockDataSerializer blockDataSerializer;
    private static volatile ChunkSerializer chunkSerializer;
    private static volatile ComponentSerializer componentSerializer;
    private static volatile MiniMessageSerializer miniMessageSerializer;
    private static volatile LegacyTextSerializer legacyTextSerializer;

    private Serializers() {
        // Prevent instantiation
    }

    // ========================================
    // Built-in Serializers
    // ========================================

    /**
     * Returns the ItemStack serializer.
     *
     * <p>Supports full NBT preservation and cross-version compatibility.
     *
     * @return the ItemStack serializer
     * @since 1.0.0
     */
    @NotNull
    public static ItemStackSerializer itemStack() {
        ItemStackSerializer result = itemStackSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = itemStackSerializer;
                if (result == null) {
                    result = new ItemStackSerializer();
                    itemStackSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the ItemMeta serializer.
     *
     * @return the ItemMeta serializer
     * @since 1.0.0
     */
    @NotNull
    public static ItemMetaSerializer itemMeta() {
        ItemMetaSerializer result = itemMetaSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = itemMetaSerializer;
                if (result == null) {
                    result = new ItemMetaSerializer();
                    itemMetaSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the Enchantment serializer.
     *
     * @return the Enchantment serializer
     * @since 1.0.0
     */
    @NotNull
    public static EnchantmentSerializer enchantment() {
        EnchantmentSerializer result = enchantmentSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = enchantmentSerializer;
                if (result == null) {
                    result = new EnchantmentSerializer();
                    enchantmentSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the PotionEffect serializer.
     *
     * @return the PotionEffect serializer
     * @since 1.0.0
     */
    @NotNull
    public static PotionEffectSerializer potionEffect() {
        PotionEffectSerializer result = potionEffectSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = potionEffectSerializer;
                if (result == null) {
                    result = new PotionEffectSerializer();
                    potionEffectSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the Location serializer.
     *
     * @return the Location serializer
     * @since 1.0.0
     */
    @NotNull
    public static LocationSerializer location() {
        LocationSerializer result = locationSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = locationSerializer;
                if (result == null) {
                    result = new LocationSerializer();
                    locationSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the BlockData serializer.
     *
     * @return the BlockData serializer
     * @since 1.0.0
     */
    @NotNull
    public static BlockDataSerializer blockData() {
        BlockDataSerializer result = blockDataSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = blockDataSerializer;
                if (result == null) {
                    result = new BlockDataSerializer();
                    blockDataSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the Chunk serializer.
     *
     * @return the Chunk serializer
     * @since 1.0.0
     */
    @NotNull
    public static ChunkSerializer chunk() {
        ChunkSerializer result = chunkSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = chunkSerializer;
                if (result == null) {
                    result = new ChunkSerializer();
                    chunkSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the Adventure Component serializer.
     *
     * @return the Component serializer
     * @since 1.0.0
     */
    @NotNull
    public static ComponentSerializer component() {
        ComponentSerializer result = componentSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = componentSerializer;
                if (result == null) {
                    result = new ComponentSerializer();
                    componentSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the MiniMessage serializer.
     *
     * @return the MiniMessage serializer
     * @since 1.0.0
     */
    @NotNull
    public static MiniMessageSerializer miniMessage() {
        MiniMessageSerializer result = miniMessageSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = miniMessageSerializer;
                if (result == null) {
                    result = new MiniMessageSerializer();
                    miniMessageSerializer = result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the legacy text serializer.
     *
     * @return the legacy text serializer
     * @since 1.0.0
     */
    @NotNull
    public static LegacyTextSerializer legacyText() {
        LegacyTextSerializer result = legacyTextSerializer;
        if (result == null) {
            synchronized (Serializers.class) {
                result = legacyTextSerializer;
                if (result == null) {
                    result = new LegacyTextSerializer();
                    legacyTextSerializer = result;
                }
            }
        }
        return result;
    }

    // ========================================
    // Registry Methods
    // ========================================

    /**
     * Registers a serializer for a type.
     *
     * <p>Replaces any existing serializer for the type.
     *
     * @param type       the type class
     * @param serializer the serializer
     * @param <T>        the type
     * @since 1.0.0
     */
    public static <T> void register(@NotNull Class<T> type, @NotNull Serializer<T> serializer) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(serializer, "serializer cannot be null");
        REGISTRY.put(type, serializer);
    }

    /**
     * Registers a serializer lazily (created on first access).
     *
     * @param type     the type class
     * @param supplier the serializer supplier
     * @param <T>      the type
     * @since 1.0.0
     */
    public static <T> void registerLazy(@NotNull Class<T> type,
                                         @NotNull Supplier<Serializer<T>> supplier) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        LAZY_REGISTRY.put(type, supplier);
    }

    /**
     * Unregisters a serializer for a type.
     *
     * @param type the type class
     * @param <T>  the type
     * @return the removed serializer, or null if none was registered
     * @since 1.0.0
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> unregister(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");
        LAZY_REGISTRY.remove(type);
        return (Serializer<T>) REGISTRY.remove(type);
    }

    /**
     * Gets a registered serializer for a type.
     *
     * @param type the type class
     * @param <T>  the type
     * @return an Optional containing the serializer if registered
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> Optional<Serializer<T>> get(@NotNull Class<T> type) {
        Objects.requireNonNull(type, "type cannot be null");

        // Check direct registry first
        Serializer<?> serializer = REGISTRY.get(type);
        if (serializer != null) {
            return Optional.of((Serializer<T>) serializer);
        }

        // Check lazy registry
        Supplier<? extends Serializer<?>> supplier = LAZY_REGISTRY.get(type);
        if (supplier != null) {
            serializer = supplier.get();
            REGISTRY.put(type, serializer);
            LAZY_REGISTRY.remove(type);
            return Optional.of((Serializer<T>) serializer);
        }

        return Optional.empty();
    }

    /**
     * Gets a registered serializer, throwing if not found.
     *
     * @param type the type class
     * @param <T>  the type
     * @return the serializer
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    public static <T> Serializer<T> require(@NotNull Class<T> type) {
        return get(type).orElseThrow(() ->
                new IllegalArgumentException("No serializer registered for type: " + type.getName()));
    }

    /**
     * Checks if a serializer is registered for a type.
     *
     * @param type the type class
     * @return true if a serializer is registered
     * @since 1.0.0
     */
    public static boolean isRegistered(@NotNull Class<?> type) {
        return REGISTRY.containsKey(type) || LAZY_REGISTRY.containsKey(type);
    }

    /**
     * Returns all registered serializers.
     *
     * <p>Note: This does not include lazy serializers until they are accessed.
     *
     * @return an unmodifiable map of type to serializer
     * @since 1.0.0
     */
    @NotNull
    public static Map<Class<?>, Serializer<?>> getAll() {
        return Map.copyOf(REGISTRY);
    }

    /**
     * Clears all registered serializers.
     *
     * <p>Warning: This does not clear built-in serializers.
     *
     * @since 1.0.0
     */
    public static void clear() {
        REGISTRY.clear();
        LAZY_REGISTRY.clear();
    }

    // ========================================
    // Convenience Methods
    // ========================================

    /**
     * Serializes an object using the registered serializer.
     *
     * @param value   the object to serialize
     * @param context the serialization context
     * @param <T>     the type
     * @return the serialized string
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> String serialize(@NotNull T value, @NotNull SerializationContext context) {
        Class<T> type = (Class<T>) value.getClass();
        Serializer<T> serializer = require(type);
        return serializer.serialize(value, context);
    }

    /**
     * Serializes an object to JSON using the registered serializer.
     *
     * @param value the object to serialize
     * @param <T>   the type
     * @return the JSON string
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> String toJson(@NotNull T value) {
        Class<T> type = (Class<T>) value.getClass();
        Serializer<T> serializer = require(type);
        return serializer.toJson(value);
    }

    /**
     * Serializes an object to Base64 using the registered serializer.
     *
     * @param value the object to serialize
     * @param <T>   the type
     * @return the Base64 string
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> String toBase64(@NotNull T value) {
        Class<T> type = (Class<T>) value.getClass();
        Serializer<T> serializer = require(type);
        return serializer.toBase64(value);
    }

    /**
     * Deserializes a string using the registered serializer.
     *
     * @param data    the serialized string
     * @param type    the target type class
     * @param context the serialization context
     * @param <T>     the type
     * @return the deserialized object
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    public static <T> T deserialize(@NotNull String data, @NotNull Class<T> type,
                                     @NotNull SerializationContext context) {
        Serializer<T> serializer = require(type);
        return serializer.deserialize(data, context);
    }

    /**
     * Deserializes a JSON string using the registered serializer.
     *
     * @param json the JSON string
     * @param type the target type class
     * @param <T>  the type
     * @return the deserialized object
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    public static <T> T fromJson(@NotNull String json, @NotNull Class<T> type) {
        Serializer<T> serializer = require(type);
        return serializer.fromJson(json);
    }

    /**
     * Deserializes a Base64 string using the registered serializer.
     *
     * @param base64 the Base64 string
     * @param type   the target type class
     * @param <T>    the type
     * @return the deserialized object
     * @throws IllegalArgumentException if no serializer is registered
     * @since 1.0.0
     */
    @NotNull
    public static <T> T fromBase64(@NotNull String base64, @NotNull Class<T> type) {
        Serializer<T> serializer = require(type);
        return serializer.fromBase64(base64);
    }
}
