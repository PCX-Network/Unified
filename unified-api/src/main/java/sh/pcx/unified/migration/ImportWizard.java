/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interactive wizard for guiding users through the import process.
 *
 * <p>ImportWizard provides a step-by-step interface for configuring and
 * executing imports, typically used for GUI-based migration tools.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ImportWizard wizard = migration.createWizard();
 *
 * // Step 1: Select source
 * List<DataImporter> available = wizard.getAvailableImporters();
 * wizard.selectImporter(available.get(0));
 *
 * // Step 2: Configure mapping
 * List<MappableField> fields = wizard.getMappableFields();
 * wizard.setMapping(FieldMapping.builder()
 *     .map("old_field", "new_field")
 *     .build());
 *
 * // Step 3: Preview
 * ImportResult preview = wizard.preview();
 * displayPreview(preview);
 *
 * // Step 4: Execute
 * wizard.onProgress(progress -> updateUI(progress));
 * ImportResult result = wizard.execute();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see MigrationService
 * @see DataImporter
 */
public interface ImportWizard {

    // ========================================================================
    // Step 1: Source Selection
    // ========================================================================

    /**
     * Returns available importers.
     *
     * @return list of importers that can be used
     * @since 1.0.0
     */
    @NotNull
    List<DataImporter> getAvailableImporters();

    /**
     * Selects the importer to use.
     *
     * @param importer the importer to select
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard selectImporter(@NotNull DataImporter importer);

    /**
     * Selects an importer by identifier.
     *
     * @param identifier the importer identifier
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard selectImporter(@NotNull String identifier);

    /**
     * Returns the selected importer.
     *
     * @return the selected importer, or null if none selected
     * @since 1.0.0
     */
    @Nullable
    DataImporter getSelectedImporter();

    /**
     * Sets the source folder.
     *
     * @param folder the source folder
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setSourceFolder(@NotNull Path folder);

    /**
     * Returns source information for the selected importer.
     *
     * @return source info, or null if no importer selected
     * @since 1.0.0
     */
    @Nullable
    DataImporter.SourceInfo getSourceInfo();

    // ========================================================================
    // Step 2: Mapping Configuration
    // ========================================================================

    /**
     * Returns fields that can be mapped.
     *
     * @return list of mappable fields
     * @since 1.0.0
     */
    @NotNull
    List<MappableField> getMappableFields();

    /**
     * Returns the default mapping for the selected importer.
     *
     * @return the default mapping
     * @since 1.0.0
     */
    @NotNull
    FieldMapping getDefaultMapping();

    /**
     * Sets the field mapping.
     *
     * @param mapping the field mapping
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setMapping(@NotNull FieldMapping mapping);

    /**
     * Returns the current field mapping.
     *
     * @return the current mapping
     * @since 1.0.0
     */
    @NotNull
    FieldMapping getMapping();

    // ========================================================================
    // Step 3: Options
    // ========================================================================

    /**
     * Sets whether to enable dry run mode.
     *
     * @param dryRun true for dry run
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setDryRun(boolean dryRun);

    /**
     * Sets the batch size.
     *
     * @param size the batch size
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setBatchSize(int size);

    /**
     * Sets the merge strategy.
     *
     * @param strategy the merge strategy
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setMergeStrategy(@NotNull ImportContext.MergeStrategy strategy);

    /**
     * Sets whether to create a backup.
     *
     * @param backup true to create backup
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setCreateBackup(boolean backup);

    /**
     * Sets an additional option.
     *
     * @param key   the option key
     * @param value the option value
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard setOption(@NotNull String key, @NotNull String value);

    // ========================================================================
    // Step 4: Preview and Validation
    // ========================================================================

    /**
     * Validates the current wizard configuration.
     *
     * @return the validation result
     * @since 1.0.0
     */
    @NotNull
    ValidationResult validate();

    /**
     * Returns an estimate of the import.
     *
     * @return the import estimate
     * @since 1.0.0
     */
    @NotNull
    ImportEstimate estimate();

    /**
     * Previews the import without making changes.
     *
     * @return the preview result
     * @since 1.0.0
     */
    @NotNull
    ImportResult preview();

    /**
     * Previews the import asynchronously.
     *
     * @return future with preview result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ImportResult> previewAsync();

    // ========================================================================
    // Step 5: Execution
    // ========================================================================

    /**
     * Sets the progress callback.
     *
     * @param callback the progress callback
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard onProgress(@NotNull Consumer<MigrationProgress> callback);

    /**
     * Sets the completion callback.
     *
     * @param callback the completion callback
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard onComplete(@NotNull Consumer<ImportResult> callback);

    /**
     * Executes the import.
     *
     * @return the import result
     * @since 1.0.0
     */
    @NotNull
    ImportResult execute();

    /**
     * Executes the import asynchronously.
     *
     * @return future with import result
     * @since 1.0.0
     */
    @NotNull
    CompletableFuture<ImportResult> executeAsync();

    /**
     * Cancels the current operation.
     *
     * @since 1.0.0
     */
    void cancel();

    // ========================================================================
    // State
    // ========================================================================

    /**
     * Returns the current wizard step.
     *
     * @return the current step
     * @since 1.0.0
     */
    @NotNull
    WizardStep getCurrentStep();

    /**
     * Checks if the wizard can proceed to the next step.
     *
     * @return true if next step is available
     * @since 1.0.0
     */
    boolean canProceed();

    /**
     * Checks if the wizard can go back to the previous step.
     *
     * @return true if previous step is available
     * @since 1.0.0
     */
    boolean canGoBack();

    /**
     * Proceeds to the next step.
     *
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard nextStep();

    /**
     * Goes back to the previous step.
     *
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard previousStep();

    /**
     * Resets the wizard to the initial state.
     *
     * @return this wizard
     * @since 1.0.0
     */
    @NotNull
    ImportWizard reset();

    /**
     * Wizard steps.
     *
     * @since 1.0.0
     */
    enum WizardStep {
        /** Select import source */
        SELECT_SOURCE,
        /** Configure field mapping */
        CONFIGURE_MAPPING,
        /** Set import options */
        SET_OPTIONS,
        /** Preview import */
        PREVIEW,
        /** Execute import */
        EXECUTE,
        /** Import complete */
        COMPLETE
    }
}
