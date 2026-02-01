/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking classes as data importers.
 *
 * <p>This annotation provides metadata about the importer that can be used
 * for automatic discovery and registration. Classes annotated with @Importer
 * must implement {@link DataImporter}.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Importer(
 *     name = "PerWorldInventory",
 *     description = "Import player inventories from PerWorldInventory",
 *     detectionFile = "plugins/PerWorldInventory/data",
 *     dataTypes = {"inventory", "armor", "enderchest"},
 *     priority = ImporterPriority.NORMAL
 * )
 * public class PerWorldInventoryImporter implements DataImporter {
 *     // Implementation
 * }
 * }</pre>
 *
 * <h2>Automatic Discovery</h2>
 * <p>Importers annotated with @Importer can be automatically discovered
 * through classpath scanning when the migration service initializes. The
 * {@link #detectionFile()} is used to determine if the source plugin's
 * data is available.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see DataImporter
 * @see MigrationService
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Importer {

    /**
     * The display name of the source plugin.
     *
     * <p>This is shown to users in UI and logs.
     *
     * @return the plugin name
     * @since 1.0.0
     */
    String name();

    /**
     * A brief description of what this importer does.
     *
     * @return the description
     * @since 1.0.0
     */
    String description() default "";

    /**
     * The file or folder used to detect if the source data is available.
     *
     * <p>Relative paths are resolved from the server directory.
     * Examples: "plugins/PerWorldInventory/data", "plugins/EssentialsX/userdata"
     *
     * @return the detection path
     * @since 1.0.0
     */
    String detectionFile();

    /**
     * The data types this importer supports.
     *
     * <p>Common types include: "inventory", "economy", "homes", "warps",
     * "permissions", "player-data"
     *
     * @return the array of supported data types
     * @since 1.0.0
     */
    String[] dataTypes() default {};

    /**
     * The minimum version of the source plugin supported.
     *
     * <p>Empty string means no minimum version.
     *
     * @return the minimum version
     * @since 1.0.0
     */
    String minVersion() default "";

    /**
     * The maximum version of the source plugin supported.
     *
     * <p>Empty string means no maximum version.
     *
     * @return the maximum version
     * @since 1.0.0
     */
    String maxVersion() default "";

    /**
     * The priority of this importer.
     *
     * <p>Higher priority importers are shown first in UI and are preferred
     * when multiple importers can handle the same data type.
     *
     * @return the importer priority
     * @since 1.0.0
     */
    ImporterPriority priority() default ImporterPriority.NORMAL;

    /**
     * Whether this importer is experimental.
     *
     * <p>Experimental importers show a warning before use.
     *
     * @return true if experimental
     * @since 1.0.0
     */
    boolean experimental() default false;

    /**
     * Priority levels for importers.
     *
     * @since 1.0.0
     */
    enum ImporterPriority {
        /** Lowest priority - fallback option */
        LOWEST,
        /** Low priority */
        LOW,
        /** Normal priority - default */
        NORMAL,
        /** High priority - preferred option */
        HIGH,
        /** Highest priority - official/verified importer */
        HIGHEST
    }
}
