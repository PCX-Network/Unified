/*
 * UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.commands.parsing;

import sh.pcx.unified.commands.completion.CompletionContext;
import sh.pcx.unified.commands.core.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Parser for Minecraft material/item arguments.
 *
 * <p>Parses material names into Material enum values. Supports various
 * input formats for user convenience.</p>
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *   <li>Enum name: {@code DIAMOND_SWORD}, {@code STONE}</li>
 *   <li>Lowercase: {@code diamond_sword}, {@code stone}</li>
 *   <li>Namespaced: {@code minecraft:diamond_sword}</li>
 *   <li>Legacy IDs (optional): {@code 276} (deprecated)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Commands</h3>
 * <pre>{@code
 * @Subcommand("give")
 * public void give(
 *     @Sender Player sender,
 *     @Arg("player") Player target,
 *     @Arg("material") Material material,
 *     @Arg("amount") @Default("1") int amount
 * ) {
 *     ItemStack item = new ItemStack(material, amount);
 *     target.getInventory().addItem(item);
 * }
 *
 * // Invoked as:
 * // /give Steve diamond_sword
 * // /give Steve DIAMOND_SWORD 1
 * // /give Steve minecraft:diamond_sword
 * }</pre>
 *
 * <h3>Block-Only Materials</h3>
 * <pre>{@code
 * @Subcommand("setblock")
 * public void setBlock(
 *     @Sender Player player,
 *     @Arg("material") @Completions("@blocks") Material material
 * ) {
 *     // Only block materials are suggested
 * }
 * }</pre>
 *
 * <h2>Platform Implementation</h2>
 * <p>This parser requires platform-specific implementation to access
 * the Material registry. Paper/Spigot and Sponge implementations
 * are provided in their respective platform modules.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ArgumentParser
 */
public class MaterialParser implements ArgumentParser<Object> {

    /**
     * The Minecraft namespace prefix.
     */
    public static final String MINECRAFT_NAMESPACE = "minecraft:";

    @Override
    @NotNull
    public Object parse(@NotNull CommandContext context, @NotNull String input) throws ParseException {
        String materialName = input.trim();

        // Remove namespace prefix if present
        if (materialName.toLowerCase().startsWith(MINECRAFT_NAMESPACE)) {
            materialName = materialName.substring(MINECRAFT_NAMESPACE.length());
        }

        // Normalize to uppercase for enum lookup
        materialName = materialName.toUpperCase().replace(' ', '_').replace('-', '_');

        // Platform-specific implementation would look up the material
        throw new UnsupportedOperationException(
                "MaterialParser requires platform-specific implementation. " +
                        "Use PaperMaterialParser or SpongeMaterialParser."
        );
    }

    @Override
    @NotNull
    public List<String> suggest(@NotNull CompletionContext context) {
        // Platform-specific implementation would return material names
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public String getErrorMessage() {
        return "Unknown material: {input}";
    }

    /**
     * Normalizes a material name for lookup.
     *
     * <p>Converts to uppercase and replaces spaces/hyphens with underscores.</p>
     *
     * @param input the input material name
     * @return the normalized name
     */
    @NotNull
    public static String normalizeName(@NotNull String input) {
        String result = input.trim();

        // Remove namespace
        if (result.toLowerCase().startsWith(MINECRAFT_NAMESPACE)) {
            result = result.substring(MINECRAFT_NAMESPACE.length());
        }

        // Normalize characters
        return result.toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }
}
