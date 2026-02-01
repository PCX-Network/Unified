/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.server;

import sh.pcx.unified.server.MinecraftVersion;
import sh.pcx.unified.server.ServerType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Configuration record for MockServer settings.
 *
 * <p>This record holds all configurable settings for a mock server instance.
 * Use the builder pattern via {@link #builder()} for convenient construction.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockServerConfiguration config = MockServerConfiguration.builder()
 *     .minecraftVersion(MinecraftVersion.of(1, 21, 4))
 *     .serverType(ServerType.PAPER)
 *     .maxPlayers(100)
 *     .onlineMode(true)
 *     .build();
 *
 * MockServer server = MockServer.start(config);
 * }</pre>
 *
 * @param minecraftVersion the Minecraft version to simulate
 * @param serverType       the server type to simulate
 * @param maxPlayers       the maximum number of players
 * @param onlineMode       whether online mode is enabled
 * @param defaultWorldName the name of the default world
 * @param serverName       the server name
 * @param serverVersion    the server version string
 *
 * @since 1.0.0
 * @author Supatuck
 */
public record MockServerConfiguration(
    @NotNull MinecraftVersion minecraftVersion,
    @NotNull ServerType serverType,
    int maxPlayers,
    boolean onlineMode,
    @NotNull String defaultWorldName,
    @NotNull String serverName,
    @NotNull String serverVersion
) {

    /**
     * Creates a new configuration with validated parameters.
     */
    public MockServerConfiguration {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion cannot be null");
        Objects.requireNonNull(serverType, "serverType cannot be null");
        Objects.requireNonNull(defaultWorldName, "defaultWorldName cannot be null");
        Objects.requireNonNull(serverName, "serverName cannot be null");
        Objects.requireNonNull(serverVersion, "serverVersion cannot be null");

        if (maxPlayers <= 0) {
            throw new IllegalArgumentException("maxPlayers must be positive");
        }
    }

    /**
     * Creates a new builder for MockServerConfiguration.
     *
     * @return a new builder instance
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default configuration.
     *
     * @return the default configuration
     */
    @NotNull
    public static MockServerConfiguration defaults() {
        return builder().build();
    }

    /**
     * Builder for {@link MockServerConfiguration}.
     */
    public static final class Builder {
        private MinecraftVersion minecraftVersion = new MinecraftVersion(1, 21, 4);
        private ServerType serverType = ServerType.PAPER;
        private int maxPlayers = 20;
        private boolean onlineMode = true;
        private String defaultWorldName = "world";
        private String serverName = "MockServer";
        private String serverVersion = "1.0.0-TEST";

        private Builder() {}

        /**
         * Sets the Minecraft version to simulate.
         *
         * @param version the Minecraft version
         * @return this builder
         */
        @NotNull
        public Builder minecraftVersion(@NotNull MinecraftVersion version) {
            this.minecraftVersion = Objects.requireNonNull(version);
            return this;
        }

        /**
         * Sets the Minecraft version using major, minor, and patch numbers.
         *
         * @param major the major version
         * @param minor the minor version
         * @param patch the patch version
         * @return this builder
         */
        @NotNull
        public Builder minecraftVersion(int major, int minor, int patch) {
            this.minecraftVersion = new MinecraftVersion(major, minor, patch);
            return this;
        }

        /**
         * Sets the server type to simulate.
         *
         * @param serverType the server type
         * @return this builder
         */
        @NotNull
        public Builder serverType(@NotNull ServerType serverType) {
            this.serverType = Objects.requireNonNull(serverType);
            return this;
        }

        /**
         * Sets the maximum number of players.
         *
         * @param maxPlayers the max players
         * @return this builder
         */
        @NotNull
        public Builder maxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
            return this;
        }

        /**
         * Sets whether online mode is enabled.
         *
         * @param onlineMode the online mode setting
         * @return this builder
         */
        @NotNull
        public Builder onlineMode(boolean onlineMode) {
            this.onlineMode = onlineMode;
            return this;
        }

        /**
         * Sets the default world name.
         *
         * @param worldName the world name
         * @return this builder
         */
        @NotNull
        public Builder defaultWorldName(@NotNull String worldName) {
            this.defaultWorldName = Objects.requireNonNull(worldName);
            return this;
        }

        /**
         * Sets the server name.
         *
         * @param serverName the server name
         * @return this builder
         */
        @NotNull
        public Builder serverName(@NotNull String serverName) {
            this.serverName = Objects.requireNonNull(serverName);
            return this;
        }

        /**
         * Sets the server version string.
         *
         * @param serverVersion the server version
         * @return this builder
         */
        @NotNull
        public Builder serverVersion(@NotNull String serverVersion) {
            this.serverVersion = Objects.requireNonNull(serverVersion);
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the built configuration
         */
        @NotNull
        public MockServerConfiguration build() {
            return new MockServerConfiguration(
                minecraftVersion,
                serverType,
                maxPlayers,
                onlineMode,
                defaultWorldName,
                serverName,
                serverVersion
            );
        }
    }
}
