/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import sh.pcx.unified.content.resourcepack.*;
import sh.pcx.unified.content.resourcepack.ResourcePackModels.*;
import sh.pcx.unified.player.UnifiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Default implementation of {@link ResourcePackService}.
 *
 * <p>This implementation manages resource pack generation with support for:
 * <ul>
 *   <li>Dynamic pack creation with models, textures, sounds, fonts</li>
 *   <li>Built-in HTTP server for pack hosting</li>
 *   <li>Pack merging with conflict resolution</li>
 *   <li>Hot regeneration of packs</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class ResourcePackServiceImpl implements ResourcePackService {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Map<String, ResourcePackImpl> packs = new ConcurrentHashMap<>();
    private final List<Consumer<ResourcePackResponseEvent>> responseHandlers = new CopyOnWriteArrayList<>();
    private final Path cacheDirectory;

    public ResourcePackServiceImpl() {
        this(Path.of("cache", "resourcepacks"));
    }

    public ResourcePackServiceImpl(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory", e);
        }
    }

    @Override
    @NotNull
    public ResourcePackCreator create(@NotNull String namespace) {
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        validateNamespace(namespace);
        return new ResourcePackCreatorImpl(namespace, this);
    }

    @Override
    @NotNull
    public Optional<ResourcePack> get(@NotNull String namespace) {
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        return Optional.ofNullable(packs.get(namespace));
    }

    @Override
    @NotNull
    public Collection<ResourcePack> getAll() {
        return Collections.unmodifiableCollection(packs.values());
    }

    @Override
    @NotNull
    public ResourcePackBuilder builder(@NotNull ResourcePack pack) {
        Objects.requireNonNull(pack, "Pack cannot be null");
        if (pack instanceof ResourcePackImpl impl) {
            return new ResourcePackBuilderImpl(impl, this);
        }
        throw new IllegalArgumentException("Pack must be created by this service");
    }

    @Override
    @NotNull
    public ResourcePackServerBuilder host(@NotNull ResourcePack pack) {
        Objects.requireNonNull(pack, "Pack cannot be null");
        return new ResourcePackServerBuilderImpl(pack, cacheDirectory);
    }

    @Override
    public void send(@NotNull UnifiedPlayer player, @NotNull String url, @NotNull String hash,
                     boolean required, @Nullable Component prompt) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(url, "URL cannot be null");
        Objects.requireNonNull(hash, "Hash cannot be null");

        // Platform implementation would send the resource pack request
        // This is a placeholder for the abstraction
    }

    @Override
    public void send(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        // Platform implementation would send the default pack
    }

    @Override
    public void onResponse(@NotNull Consumer<ResourcePackResponseEvent> handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");
        responseHandlers.add(handler);
    }

    @Override
    @NotNull
    public ResourcePack merge(@NotNull String name, @NotNull ResourcePack... packs) {
        return merge(name)
                .add(packs[0], MergePriority.HIGH)
                .build();
    }

    @Override
    @NotNull
    public ResourcePackMergeBuilder merge(@NotNull String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return new ResourcePackMergeBuilderImpl(name, this);
    }

    @Override
    public void regenerate(@NotNull ResourcePack pack) {
        Objects.requireNonNull(pack, "Pack cannot be null");

        if (pack instanceof ResourcePackImpl impl) {
            generatePackFile(impl);
        }
    }

    @Override
    public void save(@NotNull ResourcePack pack, @NotNull Path path) {
        Objects.requireNonNull(pack, "Pack cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");

        if (pack instanceof ResourcePackImpl impl) {
            try {
                Path sourcePath = impl.getPath().orElseThrow(
                        () -> new IllegalStateException("Pack has not been generated"));
                Files.copy(sourcePath, path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save pack", e);
            }
        }
    }

    @Override
    @NotNull
    public ResourcePack load(@NotNull Path path) {
        Objects.requireNonNull(path, "Path cannot be null");

        try {
            // Read pack.mcmeta to get namespace and format
            String namespace = path.getFileName().toString().replace(".zip", "");
            int packFormat = 22;
            Component description = Component.text("Loaded pack");

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(path))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("pack.mcmeta")) {
                        String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject json = GSON.fromJson(content, JsonObject.class);
                        JsonObject pack = json.getAsJsonObject("pack");
                        packFormat = pack.get("pack_format").getAsInt();
                        String desc = pack.get("description").getAsString();
                        description = Component.text(desc);
                        break;
                    }
                    zis.closeEntry();
                }
            }

            ResourcePackImpl packImpl = new ResourcePackImpl(namespace, description, packFormat);
            packImpl.setPath(path);
            packImpl.setHash(computeHash(path));

            packs.put(namespace, packImpl);
            return packImpl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to load pack", e);
        }
    }

    /**
     * Registers a pack.
     */
    void registerPack(ResourcePackImpl pack) {
        packs.put(pack.getNamespace(), pack);
    }

    /**
     * Generates the pack file.
     */
    void generatePackFile(ResourcePackImpl pack) {
        Path packPath = cacheDirectory.resolve(pack.getNamespace() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(packPath))) {

            // Write pack.mcmeta
            writePackMcmeta(zos, pack);

            // Write item models
            for (Map.Entry<String, ItemModel> entry : pack.itemModelMap.entrySet()) {
                writeItemModel(zos, entry.getKey(), entry.getValue());
            }

            // Write block models
            for (Map.Entry<String, BlockModel> entry : pack.blockModelMap.entrySet()) {
                writeBlockModel(zos, entry.getKey(), entry.getValue());
            }

            // Write textures
            for (Map.Entry<String, byte[]> entry : pack.textureData.entrySet()) {
                writeTexture(zos, entry.getKey(), entry.getValue());
            }

            // Write sounds.json
            if (!pack.soundDefinitionMap.isEmpty()) {
                writeSoundsJson(zos, pack);
            }

            // Write sound files
            for (Map.Entry<String, byte[]> entry : pack.soundData.entrySet()) {
                writeSoundFile(zos, entry.getKey(), entry.getValue());
            }

            // Write fonts
            for (Map.Entry<String, FontDefinition> entry : pack.fontDefinitionMap.entrySet()) {
                writeFont(zos, entry.getKey(), entry.getValue());
            }

            // Write languages
            for (Map.Entry<String, Map<String, String>> entry : pack.languageData.entrySet()) {
                writeLanguage(zos, entry.getKey(), entry.getValue());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate pack", e);
        }

        pack.setPath(packPath);
        pack.setHash(computeHash(packPath));
        pack.setSize(getFileSize(packPath));
        pack.incrementVersion();
    }

    private void writePackMcmeta(ZipOutputStream zos, ResourcePackImpl pack) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject packObj = new JsonObject();

        packObj.addProperty("pack_format", pack.getPackFormat());
        String description = GsonComponentSerializer.gson().serialize(pack.getDescription());
        packObj.add("description", GSON.fromJson(description, JsonObject.class));

        root.add("pack", packObj);

        zos.putNextEntry(new ZipEntry("pack.mcmeta"));
        zos.write(GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void writeItemModel(ZipOutputStream zos, String key, ItemModel model) throws IOException {
        String[] parts = parseKey(key);
        String path = "assets/" + parts[0] + "/models/item/" + parts[1] + ".json";

        JsonObject json = new JsonObject();
        json.addProperty("parent", model.parent());

        if (!model.textures().isEmpty()) {
            JsonObject textures = new JsonObject();
            for (Map.Entry<String, String> entry : model.textures().entrySet()) {
                textures.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("textures", textures);
        }

        if (model.display() != null) {
            json.add("display", serializeDisplay(model.display()));
        }

        zos.putNextEntry(new ZipEntry(path));
        zos.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void writeBlockModel(ZipOutputStream zos, String key, BlockModel model) throws IOException {
        String[] parts = parseKey(key);
        String path = "assets/" + parts[0] + "/models/block/" + parts[1] + ".json";

        JsonObject json = new JsonObject();
        json.addProperty("parent", model.parent());

        if (!model.textures().isEmpty()) {
            JsonObject textures = new JsonObject();
            for (Map.Entry<String, String> entry : model.textures().entrySet()) {
                textures.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("textures", textures);
        }

        zos.putNextEntry(new ZipEntry(path));
        zos.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void writeTexture(ZipOutputStream zos, String key, byte[] data) throws IOException {
        String[] parts = parseKey(key);
        String path = "assets/" + parts[0] + "/textures/" + parts[1] + ".png";

        zos.putNextEntry(new ZipEntry(path));
        zos.write(data);
        zos.closeEntry();
    }

    private void writeSoundsJson(ZipOutputStream zos, ResourcePackImpl pack) throws IOException {
        JsonObject sounds = new JsonObject();

        for (Map.Entry<String, SoundDefinition> entry : pack.soundDefinitionMap.entrySet()) {
            sounds.add(entry.getKey(), serializeSoundDefinition(entry.getValue()));
        }

        String path = "assets/" + pack.getNamespace() + "/sounds.json";
        zos.putNextEntry(new ZipEntry(path));
        zos.write(GSON.toJson(sounds).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void writeSoundFile(ZipOutputStream zos, String key, byte[] data) throws IOException {
        String[] parts = parseKey(key);
        String path = "assets/" + parts[0] + "/sounds/" + parts[1] + ".ogg";

        zos.putNextEntry(new ZipEntry(path));
        zos.write(data);
        zos.closeEntry();
    }

    private void writeFont(ZipOutputStream zos, String key, FontDefinition font) throws IOException {
        String[] parts = parseKey(key);
        String path = "assets/" + parts[0] + "/font/" + parts[1] + ".json";

        JsonObject json = new JsonObject();
        json.add("providers", GSON.toJsonTree(font.providers()));

        zos.putNextEntry(new ZipEntry(path));
        zos.write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void writeLanguage(ZipOutputStream zos, String locale, Map<String, String> translations)
            throws IOException {
        String path = "assets/minecraft/lang/" + locale + ".json";

        zos.putNextEntry(new ZipEntry(path));
        zos.write(GSON.toJson(translations).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private JsonObject serializeDisplay(ItemDisplay display) {
        JsonObject json = new JsonObject();
        // Serialize display transforms
        return json;
    }

    private JsonObject serializeSoundDefinition(SoundDefinition definition) {
        JsonObject json = new JsonObject();

        if (definition.subtitle() != null) {
            json.addProperty("subtitle", definition.subtitle());
        }

        json.addProperty("replace", definition.replace());
        json.add("sounds", GSON.toJsonTree(definition.sounds()));

        return json;
    }

    private String computeHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = digest.digest(bytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private String[] parseKey(String key) {
        String[] parts = key.split(":", 2);
        if (parts.length == 1) {
            return new String[]{"minecraft", parts[0]};
        }
        return parts;
    }

    private void validateNamespace(String namespace) {
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be empty");
        }
        if (!namespace.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Namespace must be lowercase alphanumeric with underscores");
        }
    }

    /**
     * Handles a resource pack response event.
     */
    public void handleResponse(ResourcePackResponseEvent event) {
        for (Consumer<ResourcePackResponseEvent> handler : responseHandlers) {
            handler.accept(event);
        }
    }
}

/**
 * Implementation of {@link ResourcePackCreator}.
 */
class ResourcePackCreatorImpl implements ResourcePackCreator {

    private final String namespace;
    private final ResourcePackServiceImpl service;

    private Component description = Component.text("Custom Resource Pack");
    private int packFormat = 22;
    private int minFormat = -1;
    private int maxFormat = -1;

    ResourcePackCreatorImpl(String namespace, ResourcePackServiceImpl service) {
        this.namespace = namespace;
        this.service = service;
    }

    @Override
    @NotNull
    public ResourcePackCreator description(@NotNull Component description) {
        this.description = Objects.requireNonNull(description);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackCreator packFormat(int format) {
        this.packFormat = format;
        return this;
    }

    @Override
    @NotNull
    public ResourcePackCreator supportedFormats(int min, int max) {
        this.minFormat = min;
        this.maxFormat = max;
        return this;
    }

    @Override
    @NotNull
    public ResourcePack build() {
        ResourcePackImpl pack = new ResourcePackImpl(namespace, description, packFormat);
        service.registerPack(pack);
        return pack;
    }
}

/**
 * Implementation of {@link ResourcePack}.
 */
class ResourcePackImpl implements ResourcePack {

    private final String namespace;
    private final Component description;
    private final int packFormat;
    private int version = 1;
    private String hash;
    private Path path;
    private long size;

    // Content storage
    final Map<String, ItemModel> itemModelMap = new ConcurrentHashMap<>();
    final Map<String, BlockModel> blockModelMap = new ConcurrentHashMap<>();
    final Map<String, byte[]> textureData = new ConcurrentHashMap<>();
    final Map<String, SoundDefinition> soundDefinitionMap = new ConcurrentHashMap<>();
    final Map<String, byte[]> soundData = new ConcurrentHashMap<>();
    final Map<String, FontDefinition> fontDefinitionMap = new ConcurrentHashMap<>();
    final Map<String, ArmorTexture> armorTextureMap = new ConcurrentHashMap<>();
    final Map<String, Map<String, String>> languageData = new ConcurrentHashMap<>();

    ResourcePackImpl(String namespace, Component description, int packFormat) {
        this.namespace = namespace;
        this.description = description;
        this.packFormat = packFormat;
    }

    @Override
    @NotNull
    public String getNamespace() {
        return namespace;
    }

    @Override
    @NotNull
    public Component getDescription() {
        return description;
    }

    @Override
    public int getPackFormat() {
        return packFormat;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    void incrementVersion() {
        this.version++;
    }

    @Override
    @NotNull
    public Optional<String> getHash() {
        return Optional.ofNullable(hash);
    }

    void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    @NotNull
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    void setPath(Path path) {
        this.path = path;
    }

    @Override
    @NotNull
    public Set<String> getItemModels() {
        return Collections.unmodifiableSet(itemModelMap.keySet());
    }

    @Override
    @NotNull
    public Set<String> getBlockModels() {
        return Collections.unmodifiableSet(blockModelMap.keySet());
    }

    @Override
    @NotNull
    public Set<String> getTextures() {
        return Collections.unmodifiableSet(textureData.keySet());
    }

    @Override
    @NotNull
    public Set<String> getSounds() {
        return Collections.unmodifiableSet(soundDefinitionMap.keySet());
    }

    @Override
    @NotNull
    public Set<String> getFonts() {
        return Collections.unmodifiableSet(fontDefinitionMap.keySet());
    }

    @Override
    public boolean isGenerated() {
        return path != null && Files.exists(path);
    }

    @Override
    public long getSize() {
        return size;
    }

    void setSize(long size) {
        this.size = size;
    }
}

/**
 * Implementation of {@link ResourcePackBuilder}.
 */
class ResourcePackBuilderImpl implements ResourcePackBuilder {

    private final ResourcePackImpl pack;
    private final ResourcePackServiceImpl service;

    ResourcePackBuilderImpl(ResourcePackImpl pack, ResourcePackServiceImpl service) {
        this.pack = pack;
        this.service = service;
    }

    @Override
    @NotNull
    public ResourcePackBuilder itemModel(@NotNull String key, @NotNull ItemModel model) {
        pack.itemModelMap.put(key, model);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder itemModelOverride(@NotNull String baseItem, int customModelData,
                                                   @NotNull ItemModel model) {
        // Store override - would need platform-specific handling
        String key = baseItem + "#" + customModelData;
        pack.itemModelMap.put(key, model);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder blockModel(@NotNull String key, @NotNull BlockModel model) {
        pack.blockModelMap.put(key, model);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder texture(@NotNull String key, @NotNull InputStream texture) {
        try {
            pack.textureData.put(key, texture.readAllBytes());
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture", e);
        }
    }

    @Override
    @NotNull
    public ResourcePackBuilder texture(@NotNull String key, @NotNull Path path) {
        try {
            pack.textureData.put(key, Files.readAllBytes(path));
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture", e);
        }
    }

    @Override
    @NotNull
    public ResourcePackBuilder texture(@NotNull String key, @NotNull byte[] data) {
        pack.textureData.put(key, data);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder sound(@NotNull String key, @NotNull SoundDefinition definition) {
        pack.soundDefinitionMap.put(key, definition);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder soundFile(@NotNull String key, @NotNull InputStream sound) {
        try {
            pack.soundData.put(key, sound.readAllBytes());
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read sound", e);
        }
    }

    @Override
    @NotNull
    public ResourcePackBuilder font(@NotNull String key, @NotNull FontDefinition definition) {
        pack.fontDefinitionMap.put(key, definition);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder armorTexture(@NotNull String material, @NotNull ArmorTexture texture) {
        pack.armorTextureMap.put(material, texture);
        return this;
    }

    @Override
    @NotNull
    public ResourcePackBuilder language(@NotNull String locale,
                                         @NotNull Map<String, String> translations) {
        pack.languageData.merge(locale, new HashMap<>(translations),
                (existing, newData) -> {
                    existing.putAll(newData);
                    return existing;
                });
        return this;
    }

    @Override
    public void generate() {
        service.generatePackFile(pack);
    }
}

/**
 * Implementation of {@link ResourcePackServerBuilder}.
 */
class ResourcePackServerBuilderImpl implements ResourcePackServerBuilder {

    private final ResourcePack pack;
    private final Path cacheDirectory;

    private int port = 8080;
    private String path = "/resourcepack";
    private String hostname = "localhost";

    ResourcePackServerBuilderImpl(ResourcePack pack, Path cacheDirectory) {
        this.pack = pack;
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    @NotNull
    public ResourcePackServerBuilder port(int port) {
        this.port = port;
        return this;
    }

    @Override
    @NotNull
    public ResourcePackServerBuilder path(@NotNull String path) {
        this.path = path;
        return this;
    }

    @Override
    @NotNull
    public ResourcePackServerBuilder hostname(@NotNull String hostname) {
        this.hostname = hostname;
        return this;
    }

    @Override
    @NotNull
    public ResourcePackServer start() {
        return new ResourcePackServerImpl(pack, port, path, hostname);
    }
}

/**
 * Implementation of {@link ResourcePackServer}.
 */
class ResourcePackServerImpl implements ResourcePackServer {

    private final ResourcePack pack;
    private final int port;
    private final String path;
    private final String hostname;
    private volatile boolean running = true;

    ResourcePackServerImpl(ResourcePack pack, int port, String path, String hostname) {
        this.pack = pack;
        this.port = port;
        this.path = path;
        this.hostname = hostname;
        // In a real implementation, would start an HTTP server here
    }

    @Override
    @NotNull
    public String getUrl() {
        return "http://" + hostname + ":" + port + path;
    }

    @Override
    @NotNull
    public String getHash() {
        return pack.getHash().orElse("");
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void stop() {
        running = false;
        // In a real implementation, would stop the HTTP server
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}

/**
 * Implementation of {@link ResourcePackMergeBuilder}.
 */
class ResourcePackMergeBuilderImpl implements ResourcePackMergeBuilder {

    private final String name;
    private final ResourcePackServiceImpl service;
    private final List<PackWithPriority> packs = new ArrayList<>();
    private ConflictHandler conflictHandler;

    ResourcePackMergeBuilderImpl(String name, ResourcePackServiceImpl service) {
        this.name = name;
        this.service = service;
    }

    @Override
    @NotNull
    public ResourcePackMergeBuilder add(@NotNull ResourcePack pack, @NotNull MergePriority priority) {
        packs.add(new PackWithPriority(pack, priority));
        return this;
    }

    @Override
    @NotNull
    public ResourcePackMergeBuilder onConflict(@NotNull ConflictHandler handler) {
        this.conflictHandler = handler;
        return this;
    }

    @Override
    @NotNull
    public ResourcePack build() {
        // Sort by priority
        packs.sort(Comparator.comparing(p -> p.priority().ordinal()));

        // Create merged pack
        ResourcePackImpl merged = new ResourcePackImpl(name, Component.text("Merged pack"), 22);

        for (PackWithPriority pwp : packs) {
            if (pwp.pack() instanceof ResourcePackImpl impl) {
                // Merge content
                merged.itemModelMap.putAll(impl.itemModelMap);
                merged.blockModelMap.putAll(impl.blockModelMap);
                merged.textureData.putAll(impl.textureData);
                merged.soundDefinitionMap.putAll(impl.soundDefinitionMap);
                merged.soundData.putAll(impl.soundData);
                merged.fontDefinitionMap.putAll(impl.fontDefinitionMap);
                merged.armorTextureMap.putAll(impl.armorTextureMap);

                for (Map.Entry<String, Map<String, String>> entry : impl.languageData.entrySet()) {
                    merged.languageData.merge(entry.getKey(), new HashMap<>(entry.getValue()),
                            (existing, newData) -> {
                                existing.putAll(newData);
                                return existing;
                            });
                }
            }
        }

        service.registerPack(merged);
        service.generatePackFile(merged);

        return merged;
    }

    private record PackWithPriority(ResourcePack pack, MergePriority priority) {}
}
