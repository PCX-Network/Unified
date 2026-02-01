/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.lifecycle;

/**
 * Represents the current state of a module in the UnifiedPlugin module system.
 *
 * <p>Modules transition through these states during their lifecycle:
 *
 * <pre>
 *                    ┌──────────────┐
 *                    │   UNLOADED   │
 *                    └──────┬───────┘
 *                           │ register()
 *                           ▼
 *                    ┌──────────────┐
 *          ┌─────────│   LOADING    │─────────┐
 *          │         └──────────────┘         │
 *          │ success                    fail  │
 *          ▼                                  ▼
 *   ┌──────────────┐                  ┌──────────────┐
 *   │   ENABLED    │                  │    FAILED    │
 *   └──────┬───────┘                  └──────────────┘
 *          │ disable()                        ▲
 *          ▼                                  │
 *   ┌──────────────┐                          │
 *   │   DISABLED   │──────────────────────────┘
 *   └──────┬───────┘        enable() fails
 *          │ unload()
 *          ▼
 *   ┌──────────────┐
 *   │   UNLOADED   │
 *   └──────────────┘
 * </pre>
 *
 * <h2>State Descriptions</h2>
 * <ul>
 *   <li><b>UNLOADED</b>: Module is not loaded into the system</li>
 *   <li><b>LOADING</b>: Module is being initialized (dependencies, config, DI)</li>
 *   <li><b>ENABLED</b>: Module is active and functioning normally</li>
 *   <li><b>DISABLED</b>: Module is loaded but not active (can be re-enabled)</li>
 *   <li><b>FAILED</b>: Module failed to load or enable due to an error</li>
 * </ul>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public enum ModuleState {

    /**
     * Module is not loaded into the system.
     *
     * <p>This is the initial state before a module is registered,
     * and the final state after a module is completely unloaded.
     */
    UNLOADED("Unloaded", false, false),

    /**
     * Module is currently being loaded.
     *
     * <p>During this state:
     * <ul>
     *   <li>Dependencies are being resolved</li>
     *   <li>Configuration is being loaded</li>
     *   <li>Dependency injection is being performed</li>
     *   <li>{@link Initializable#init} is being called</li>
     * </ul>
     */
    LOADING("Loading", true, false),

    /**
     * Module is active and functioning normally.
     *
     * <p>In this state:
     * <ul>
     *   <li>All listeners are registered and receiving events</li>
     *   <li>Commands are available</li>
     *   <li>Scheduled tasks are running</li>
     *   <li>Health monitoring is active (if implemented)</li>
     * </ul>
     */
    ENABLED("Enabled", true, true),

    /**
     * Module is loaded but not active.
     *
     * <p>In this state:
     * <ul>
     *   <li>Module instance exists in memory</li>
     *   <li>Configuration is loaded</li>
     *   <li>Listeners are unregistered</li>
     *   <li>Commands are unavailable</li>
     *   <li>Scheduled tasks are cancelled</li>
     * </ul>
     *
     * <p>Disabled modules can be quickly re-enabled without full reload.
     */
    DISABLED("Disabled", true, false),

    /**
     * Module failed to load or enable due to an error.
     *
     * <p>This state indicates:
     * <ul>
     *   <li>Missing required dependencies</li>
     *   <li>Configuration errors</li>
     *   <li>Initialization exceptions</li>
     *   <li>Circular dependency detection</li>
     * </ul>
     *
     * <p>Check the module's error message for details. Failed modules
     * may be retried after fixing the underlying issue.
     */
    FAILED("Failed", false, false);

    private final String displayName;
    private final boolean loaded;
    private final boolean active;

    /**
     * Constructs a module state.
     *
     * @param displayName the human-readable name for this state
     * @param loaded      whether the module is loaded in memory
     * @param active      whether the module is actively running
     */
    ModuleState(String displayName, boolean loaded, boolean active) {
        this.displayName = displayName;
        this.loaded = loaded;
        this.active = active;
    }

    /**
     * Returns the human-readable display name for this state.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether the module is loaded into memory in this state.
     *
     * @return {@code true} if the module instance exists
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Returns whether the module is actively running in this state.
     *
     * @return {@code true} if the module is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns whether the module can be enabled from this state.
     *
     * @return {@code true} if enable operation is valid
     */
    public boolean canEnable() {
        return this == DISABLED || this == FAILED;
    }

    /**
     * Returns whether the module can be disabled from this state.
     *
     * @return {@code true} if disable operation is valid
     */
    public boolean canDisable() {
        return this == ENABLED;
    }

    /**
     * Returns whether the module can be reloaded from this state.
     *
     * @return {@code true} if reload operation is valid
     */
    public boolean canReload() {
        return this == ENABLED || this == DISABLED;
    }

    /**
     * Returns whether this is an error state.
     *
     * @return {@code true} if this state indicates an error
     */
    public boolean isError() {
        return this == FAILED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
