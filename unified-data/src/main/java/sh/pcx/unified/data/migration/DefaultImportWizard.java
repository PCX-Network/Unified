/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.migration;

import sh.pcx.unified.migration.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Default implementation of {@link ImportWizard}.
 *
 * <p>Provides a step-by-step wizard interface for configuring and
 * executing imports.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class DefaultImportWizard implements ImportWizard {

    private final DefaultMigrationService service;
    private final ExecutorService executor;
    private final Logger logger;

    private WizardStep currentStep = WizardStep.SELECT_SOURCE;
    private DataImporter selectedImporter;
    private Path sourceFolder;
    private FieldMapping mapping;

    private boolean dryRun = false;
    private int batchSize = 100;
    private ImportContext.MergeStrategy mergeStrategy = ImportContext.MergeStrategy.SKIP;
    private boolean createBackup = true;
    private final Map<String, String> options = new HashMap<>();

    private Consumer<MigrationProgress> progressCallback;
    private Consumer<ImportResult> completeCallback;

    private volatile boolean cancelled = false;

    /**
     * Creates a new import wizard.
     */
    DefaultImportWizard(@NotNull DefaultMigrationService service,
                        @NotNull ExecutorService executor,
                        @NotNull Logger logger) {
        this.service = service;
        this.executor = executor;
        this.logger = logger;
    }

    // ========================================================================
    // Step 1: Source Selection
    // ========================================================================

    @Override
    @NotNull
    public List<DataImporter> getAvailableImporters() {
        return service.getAvailableImporters();
    }

    @Override
    @NotNull
    public ImportWizard selectImporter(@NotNull DataImporter importer) {
        this.selectedImporter = Objects.requireNonNull(importer);
        this.sourceFolder = importer.getDefaultSourceFolder();
        this.mapping = importer.getDefaultMapping();
        return this;
    }

    @Override
    @NotNull
    public ImportWizard selectImporter(@NotNull String identifier) {
        DataImporter importer = service.findImporter(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Unknown importer: " + identifier));
        return selectImporter(importer);
    }

    @Override
    @Nullable
    public DataImporter getSelectedImporter() {
        return selectedImporter;
    }

    @Override
    @NotNull
    public ImportWizard setSourceFolder(@NotNull Path folder) {
        this.sourceFolder = Objects.requireNonNull(folder);
        return this;
    }

    @Override
    @Nullable
    public DataImporter.SourceInfo getSourceInfo() {
        if (selectedImporter == null) {
            return null;
        }
        return selectedImporter.getSourceInfo();
    }

    // ========================================================================
    // Step 2: Mapping Configuration
    // ========================================================================

    @Override
    @NotNull
    public List<MappableField> getMappableFields() {
        if (selectedImporter == null) {
            return List.of();
        }
        return selectedImporter.getMappableFields();
    }

    @Override
    @NotNull
    public FieldMapping getDefaultMapping() {
        if (selectedImporter == null) {
            return FieldMapping.identity();
        }
        return selectedImporter.getDefaultMapping();
    }

    @Override
    @NotNull
    public ImportWizard setMapping(@NotNull FieldMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping);
        return this;
    }

    @Override
    @NotNull
    public FieldMapping getMapping() {
        return mapping != null ? mapping : FieldMapping.identity();
    }

    // ========================================================================
    // Step 3: Options
    // ========================================================================

    @Override
    @NotNull
    public ImportWizard setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    @Override
    @NotNull
    public ImportWizard setBatchSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        this.batchSize = size;
        return this;
    }

    @Override
    @NotNull
    public ImportWizard setMergeStrategy(@NotNull ImportContext.MergeStrategy strategy) {
        this.mergeStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    @Override
    @NotNull
    public ImportWizard setCreateBackup(boolean backup) {
        this.createBackup = backup;
        return this;
    }

    @Override
    @NotNull
    public ImportWizard setOption(@NotNull String key, @NotNull String value) {
        this.options.put(
                Objects.requireNonNull(key),
                Objects.requireNonNull(value)
        );
        return this;
    }

    // ========================================================================
    // Step 4: Preview and Validation
    // ========================================================================

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();

        if (selectedImporter == null) {
            result.error("importer", "No importer selected");
            return result.build();
        }

        if (sourceFolder == null) {
            result.error("sourceFolder", "Source folder not set");
        } else if (!java.nio.file.Files.exists(sourceFolder)) {
            result.error("sourceFolder", "Source folder does not exist");
        }

        if (!selectedImporter.canImport()) {
            result.error("importer", "Importer cannot import from source");
        }

        return result.build();
    }

    @Override
    @NotNull
    public ImportEstimate estimate() {
        if (selectedImporter == null || sourceFolder == null) {
            return ImportEstimate.unknown();
        }

        DefaultImportContext context = new DefaultImportContext.Builder()
                .sourceFolder(sourceFolder)
                .fieldMapping(getMapping())
                .dryRun(true)
                .batchSize(batchSize)
                .logger(logger)
                .build();

        return selectedImporter.estimateImport(context);
    }

    @Override
    @NotNull
    public ImportResult preview() {
        boolean originalDryRun = dryRun;
        dryRun = true;
        try {
            return execute();
        } finally {
            dryRun = originalDryRun;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<ImportResult> previewAsync() {
        boolean originalDryRun = dryRun;
        dryRun = true;
        return executeAsync().whenComplete((r, t) -> dryRun = originalDryRun);
    }

    // ========================================================================
    // Step 5: Execution
    // ========================================================================

    @Override
    @NotNull
    public ImportWizard onProgress(@NotNull Consumer<MigrationProgress> callback) {
        this.progressCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public ImportWizard onComplete(@NotNull Consumer<ImportResult> callback) {
        this.completeCallback = Objects.requireNonNull(callback);
        return this;
    }

    @Override
    @NotNull
    public ImportResult execute() {
        ValidationResult validation = validate();
        if (validation.hasErrors()) {
            return ImportResult.failed("Validation failed: " +
                    validation.getErrors().get(0).message());
        }

        currentStep = WizardStep.EXECUTE;
        cancelled = false;

        ImportBuilder builder = service.importFrom(selectedImporter)
                .sourceFolder(sourceFolder)
                .mapping(getMapping())
                .dryRun(dryRun)
                .batchSize(batchSize)
                .mergeStrategy(mergeStrategy)
                .createBackup(createBackup);

        // Add options
        for (var entry : options.entrySet()) {
            builder.option(entry.getKey(), entry.getValue());
        }

        // Add callbacks
        if (progressCallback != null) {
            builder.onProgress(progressCallback);
        }

        ImportResult result = builder.execute();

        currentStep = WizardStep.COMPLETE;

        if (completeCallback != null) {
            completeCallback.accept(result);
        }

        return result;
    }

    @Override
    @NotNull
    public CompletableFuture<ImportResult> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute, executor);
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    // ========================================================================
    // State
    // ========================================================================

    @Override
    @NotNull
    public WizardStep getCurrentStep() {
        return currentStep;
    }

    @Override
    public boolean canProceed() {
        return switch (currentStep) {
            case SELECT_SOURCE -> selectedImporter != null && sourceFolder != null;
            case CONFIGURE_MAPPING -> mapping != null;
            case SET_OPTIONS -> true;
            case PREVIEW -> validate().isValid();
            case EXECUTE -> false;
            case COMPLETE -> false;
        };
    }

    @Override
    public boolean canGoBack() {
        return switch (currentStep) {
            case SELECT_SOURCE -> false;
            case CONFIGURE_MAPPING, SET_OPTIONS, PREVIEW -> true;
            case EXECUTE, COMPLETE -> false;
        };
    }

    @Override
    @NotNull
    public ImportWizard nextStep() {
        if (!canProceed()) {
            throw new IllegalStateException("Cannot proceed from current step");
        }

        currentStep = switch (currentStep) {
            case SELECT_SOURCE -> WizardStep.CONFIGURE_MAPPING;
            case CONFIGURE_MAPPING -> WizardStep.SET_OPTIONS;
            case SET_OPTIONS -> WizardStep.PREVIEW;
            case PREVIEW -> WizardStep.EXECUTE;
            default -> throw new IllegalStateException("Cannot proceed from " + currentStep);
        };

        return this;
    }

    @Override
    @NotNull
    public ImportWizard previousStep() {
        if (!canGoBack()) {
            throw new IllegalStateException("Cannot go back from current step");
        }

        currentStep = switch (currentStep) {
            case CONFIGURE_MAPPING -> WizardStep.SELECT_SOURCE;
            case SET_OPTIONS -> WizardStep.CONFIGURE_MAPPING;
            case PREVIEW -> WizardStep.SET_OPTIONS;
            default -> throw new IllegalStateException("Cannot go back from " + currentStep);
        };

        return this;
    }

    @Override
    @NotNull
    public ImportWizard reset() {
        currentStep = WizardStep.SELECT_SOURCE;
        selectedImporter = null;
        sourceFolder = null;
        mapping = null;
        dryRun = false;
        batchSize = 100;
        mergeStrategy = ImportContext.MergeStrategy.SKIP;
        createBackup = true;
        options.clear();
        progressCallback = null;
        completeCallback = null;
        cancelled = false;
        return this;
    }
}
