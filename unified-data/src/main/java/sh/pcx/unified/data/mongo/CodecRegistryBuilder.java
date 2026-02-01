/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.mongo;

import com.mongodb.MongoClientSettings;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Builder for creating custom MongoDB codec registries.
 *
 * <p>This builder simplifies the creation of codec registries by providing
 * a fluent API for adding individual codecs, codec providers, and combining
 * multiple registries.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a codec registry with custom codecs
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addCodec(new UUIDCodec())
 *     .addCodec(new LocationCodec())
 *     .addCodec(new ItemStackCodec())
 *     .addCodec(new ComponentCodec())
 *     .build();
 *
 * // Create a registry with POJO support
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .withPojoSupport()
 *     .addCodec(new UUIDCodec())
 *     .build();
 *
 * // Create a registry for specific POJO classes
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .withPojoSupport(PlayerData.class, GuildData.class)
 *     .build();
 *
 * // Combine with existing registries
 * CodecRegistry registry = CodecRegistryBuilder.create()
 *     .addRegistry(existingRegistry)
 *     .addCodec(new CustomCodec())
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Create a new builder instance for
 * each thread or synchronize access externally.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see UUIDCodec
 * @see LocationCodec
 * @see ItemStackCodec
 * @see ComponentCodec
 */
public final class CodecRegistryBuilder {

    private final List<Codec<?>> codecs = new ArrayList<>();
    private final List<CodecProvider> providers = new ArrayList<>();
    private final List<CodecRegistry> registries = new ArrayList<>();
    private boolean includeDefault = true;
    private boolean pojoSupport = false;
    private List<Class<?>> pojoClasses = null;

    private CodecRegistryBuilder() {
        // Use static factory method
    }

    /**
     * Creates a new codec registry builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static CodecRegistryBuilder create() {
        return new CodecRegistryBuilder();
    }

    /**
     * Adds a codec to the registry.
     *
     * @param codec the codec to add
     * @param <T>   the codec type
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public <T> CodecRegistryBuilder addCodec(@NotNull Codec<T> codec) {
        Objects.requireNonNull(codec, "Codec cannot be null");
        codecs.add(codec);
        return this;
    }

    /**
     * Adds multiple codecs to the registry.
     *
     * @param codecs the codecs to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addCodecs(@NotNull Codec<?>... codecs) {
        Objects.requireNonNull(codecs, "Codecs cannot be null");
        this.codecs.addAll(Arrays.asList(codecs));
        return this;
    }

    /**
     * Adds multiple codecs to the registry.
     *
     * @param codecs the codecs to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addCodecs(@NotNull Iterable<? extends Codec<?>> codecs) {
        Objects.requireNonNull(codecs, "Codecs cannot be null");
        codecs.forEach(this.codecs::add);
        return this;
    }

    /**
     * Adds a codec provider to the registry.
     *
     * @param provider the codec provider to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addProvider(@NotNull CodecProvider provider) {
        Objects.requireNonNull(provider, "Provider cannot be null");
        providers.add(provider);
        return this;
    }

    /**
     * Adds multiple codec providers to the registry.
     *
     * @param providers the codec providers to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addProviders(@NotNull CodecProvider... providers) {
        Objects.requireNonNull(providers, "Providers cannot be null");
        this.providers.addAll(Arrays.asList(providers));
        return this;
    }

    /**
     * Adds an existing codec registry to be combined.
     *
     * @param registry the codec registry to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addRegistry(@NotNull CodecRegistry registry) {
        Objects.requireNonNull(registry, "Registry cannot be null");
        registries.add(registry);
        return this;
    }

    /**
     * Adds multiple codec registries to be combined.
     *
     * @param registries the codec registries to add
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder addRegistries(@NotNull CodecRegistry... registries) {
        Objects.requireNonNull(registries, "Registries cannot be null");
        this.registries.addAll(Arrays.asList(registries));
        return this;
    }

    /**
     * Enables automatic POJO codec support.
     *
     * <p>This allows automatic serialization/deserialization of POJOs
     * without requiring custom codec implementations.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder withPojoSupport() {
        this.pojoSupport = true;
        this.pojoClasses = null;
        return this;
    }

    /**
     * Enables POJO codec support for specific classes.
     *
     * @param classes the POJO classes to register
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder withPojoSupport(@NotNull Class<?>... classes) {
        Objects.requireNonNull(classes, "Classes cannot be null");
        this.pojoSupport = true;
        this.pojoClasses = Arrays.asList(classes);
        return this;
    }

    /**
     * Disables POJO codec support.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder withoutPojoSupport() {
        this.pojoSupport = false;
        this.pojoClasses = null;
        return this;
    }

    /**
     * Includes the default MongoDB codec registry.
     *
     * <p>This is enabled by default and includes codecs for standard
     * BSON types like ObjectId, Date, Binary, etc.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder withDefaultCodecs() {
        this.includeDefault = true;
        return this;
    }

    /**
     * Excludes the default MongoDB codec registry.
     *
     * <p>Use this if you want complete control over which codecs
     * are included. Note: This may cause issues with basic BSON types.
     *
     * @return this builder
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistryBuilder withoutDefaultCodecs() {
        this.includeDefault = false;
        return this;
    }

    /**
     * Builds the codec registry.
     *
     * @return the built codec registry
     * @since 1.0.0
     */
    @NotNull
    public CodecRegistry build() {
        List<CodecRegistry> allRegistries = new ArrayList<>();

        // Add default codecs first if enabled
        if (includeDefault) {
            allRegistries.add(MongoClientSettings.getDefaultCodecRegistry());
        }

        // Add custom registries
        allRegistries.addAll(registries);

        // Add individual codecs
        if (!codecs.isEmpty()) {
            allRegistries.add(CodecRegistries.fromCodecs(codecs));
        }

        // Add codec providers
        if (!providers.isEmpty()) {
            allRegistries.add(CodecRegistries.fromProviders(providers));
        }

        // Add POJO support if enabled
        if (pojoSupport) {
            PojoCodecProvider.Builder pojoBuilder = PojoCodecProvider.builder();

            if (pojoClasses != null && !pojoClasses.isEmpty()) {
                for (Class<?> clazz : pojoClasses) {
                    pojoBuilder.register(clazz);
                }
            } else {
                pojoBuilder.automatic(true);
            }

            allRegistries.add(CodecRegistries.fromProviders(pojoBuilder.build()));
        }

        // Combine all registries
        if (allRegistries.isEmpty()) {
            return MongoClientSettings.getDefaultCodecRegistry();
        } else if (allRegistries.size() == 1) {
            return allRegistries.get(0);
        } else {
            return CodecRegistries.fromRegistries(allRegistries);
        }
    }

    /**
     * Creates a codec registry with just the Unified API's custom codecs.
     *
     * <p>This includes codecs for UUID, UnifiedLocation, UnifiedItemStack,
     * and Adventure Component.
     *
     * @return a codec registry with Unified API codecs
     * @since 1.0.0
     */
    @NotNull
    public static CodecRegistry createUnifiedCodecs() {
        return CodecRegistryBuilder.create()
                .addCodec(new UUIDCodec())
                .addCodec(new LocationCodec())
                .addCodec(new ItemStackCodec())
                .addCodec(new ComponentCodec())
                .build();
    }

    /**
     * Creates a codec registry suitable for Minecraft plugins.
     *
     * <p>This includes the default codecs, Unified API codecs, and POJO support.
     *
     * @return a full-featured codec registry for plugins
     * @since 1.0.0
     */
    @NotNull
    public static CodecRegistry createPluginCodecs() {
        return CodecRegistryBuilder.create()
                .addCodec(new UUIDCodec())
                .addCodec(new LocationCodec())
                .addCodec(new ItemStackCodec())
                .addCodec(new ComponentCodec())
                .withPojoSupport()
                .build();
    }
}
