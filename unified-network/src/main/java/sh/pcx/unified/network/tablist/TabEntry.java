/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.tablist;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an entry in the player tab list.
 *
 * <p>Tab entries can be real players or fake entries used for NPCs,
 * information display, or formatting.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a fake player entry
 * TabEntry npc = TabEntry.builder()
 *     .name("GuideNPC")
 *     .displayName(Component.text("[NPC] Guide", NamedTextColor.YELLOW))
 *     .gameMode(0)  // Survival
 *     .latency(50)
 *     .skin(skinTexture, skinSignature)
 *     .build();
 *
 * // Create an info entry
 * TabEntry info = TabEntry.builder()
 *     .name("info-1")
 *     .displayName(Component.text("Players: 42/100", NamedTextColor.GRAY))
 *     .listed(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see TabListService
 */
public interface TabEntry {

    /**
     * Returns the unique ID of this entry.
     *
     * @return the entry UUID
     * @since 1.0.0
     */
    @NotNull
    UUID getUuid();

    /**
     * Returns the profile name of this entry.
     *
     * <p>This is the internal name used for identification.
     *
     * @return the profile name
     * @since 1.0.0
     */
    @NotNull
    String getName();

    /**
     * Returns the display name shown in the tab list.
     *
     * @return the display name
     * @since 1.0.0
     */
    @NotNull
    Component getDisplayName();

    /**
     * Sets the display name.
     *
     * @param displayName the new display name
     * @since 1.0.0
     */
    void setDisplayName(@NotNull Component displayName);

    /**
     * Returns the game mode of this entry.
     *
     * @return the game mode (0=survival, 1=creative, 2=adventure, 3=spectator)
     * @since 1.0.0
     */
    int getGameMode();

    /**
     * Sets the game mode.
     *
     * @param gameMode the game mode
     * @since 1.0.0
     */
    void setGameMode(int gameMode);

    /**
     * Returns the latency for this entry.
     *
     * @return the latency in milliseconds
     * @since 1.0.0
     */
    int getLatency();

    /**
     * Sets the latency.
     *
     * @param latency the latency in milliseconds
     * @since 1.0.0
     */
    void setLatency(int latency);

    /**
     * Returns the skin texture value.
     *
     * @return the texture value, or null if default
     * @since 1.0.0
     */
    @Nullable
    String getSkinTexture();

    /**
     * Returns the skin signature.
     *
     * @return the texture signature, or null if default
     * @since 1.0.0
     */
    @Nullable
    String getSkinSignature();

    /**
     * Sets the skin.
     *
     * @param texture   the texture value
     * @param signature the texture signature
     * @since 1.0.0
     */
    void setSkin(@Nullable String texture, @Nullable String signature);

    /**
     * Checks if this entry is listed in the tab list.
     *
     * @return true if listed
     * @since 1.0.0
     */
    boolean isListed();

    /**
     * Sets whether this entry is listed.
     *
     * @param listed true to list
     * @since 1.0.0
     */
    void setListed(boolean listed);

    /**
     * Returns the sort priority.
     *
     * @return the sort priority
     * @since 1.0.0
     */
    int getSortPriority();

    /**
     * Sets the sort priority.
     *
     * @param priority the sort priority (lower = higher in list)
     * @since 1.0.0
     */
    void setSortPriority(int priority);

    /**
     * Creates a new tab entry builder.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    static Builder builder() {
        return new Builder() {
            private UUID uuid = UUID.randomUUID();
            private String name = "";
            private Component displayName = Component.empty();
            private int gameMode = 0;
            private int latency = 0;
            private String skinTexture;
            private String skinSignature;
            private boolean listed = true;
            private int sortPriority = 0;

            @Override
            public Builder uuid(@NotNull UUID uuid) {
                this.uuid = uuid;
                return this;
            }

            @Override
            public Builder name(@NotNull String name) {
                this.name = name;
                if (displayName.equals(Component.empty())) {
                    this.displayName = Component.text(name);
                }
                return this;
            }

            @Override
            public Builder displayName(@NotNull Component displayName) {
                this.displayName = displayName;
                return this;
            }

            @Override
            public Builder gameMode(int gameMode) {
                this.gameMode = gameMode;
                return this;
            }

            @Override
            public Builder latency(int latency) {
                this.latency = latency;
                return this;
            }

            @Override
            public Builder skin(@Nullable String texture, @Nullable String signature) {
                this.skinTexture = texture;
                this.skinSignature = signature;
                return this;
            }

            @Override
            public Builder listed(boolean listed) {
                this.listed = listed;
                return this;
            }

            @Override
            public Builder sortPriority(int priority) {
                this.sortPriority = priority;
                return this;
            }

            @Override
            public TabEntry build() {
                return new SimpleTabEntry(
                        uuid, name, displayName, gameMode, latency,
                        skinTexture, skinSignature, listed, sortPriority
                );
            }
        };
    }

    /**
     * Builder for creating tab entries.
     *
     * @since 1.0.0
     */
    interface Builder {
        /**
         * Sets the entry UUID.
         *
         * @param uuid the UUID
         * @return this builder
         */
        @NotNull
        Builder uuid(@NotNull UUID uuid);

        /**
         * Sets the profile name.
         *
         * @param name the name
         * @return this builder
         */
        @NotNull
        Builder name(@NotNull String name);

        /**
         * Sets the display name.
         *
         * @param displayName the display name
         * @return this builder
         */
        @NotNull
        Builder displayName(@NotNull Component displayName);

        /**
         * Sets the game mode.
         *
         * @param gameMode the game mode (0-3)
         * @return this builder
         */
        @NotNull
        Builder gameMode(int gameMode);

        /**
         * Sets the latency.
         *
         * @param latency latency in milliseconds
         * @return this builder
         */
        @NotNull
        Builder latency(int latency);

        /**
         * Sets the skin.
         *
         * @param texture   texture value
         * @param signature texture signature
         * @return this builder
         */
        @NotNull
        Builder skin(@Nullable String texture, @Nullable String signature);

        /**
         * Sets whether this entry is listed.
         *
         * @param listed true to list
         * @return this builder
         */
        @NotNull
        Builder listed(boolean listed);

        /**
         * Sets the sort priority.
         *
         * @param priority sort priority
         * @return this builder
         */
        @NotNull
        Builder sortPriority(int priority);

        /**
         * Builds the tab entry.
         *
         * @return the built entry
         */
        @NotNull
        TabEntry build();
    }

    /**
     * Simple implementation of TabEntry.
     */
    record SimpleTabEntry(
            UUID uuid,
            String name,
            Component displayName,
            int gameMode,
            int latency,
            String skinTexture,
            String skinSignature,
            boolean listed,
            int sortPriority
    ) implements TabEntry {

        private static Component mutableDisplayName;
        private static int mutableGameMode;
        private static int mutableLatency;
        private static String mutableSkinTexture;
        private static String mutableSkinSignature;
        private static boolean mutableListed;
        private static int mutableSortPriority;

        public SimpleTabEntry {
            mutableDisplayName = displayName;
            mutableGameMode = gameMode;
            mutableLatency = latency;
            mutableSkinTexture = skinTexture;
            mutableSkinSignature = skinSignature;
            mutableListed = listed;
            mutableSortPriority = sortPriority;
        }

        @Override
        public @NotNull UUID getUuid() {
            return uuid;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return mutableDisplayName;
        }

        @Override
        public void setDisplayName(@NotNull Component displayName) {
            mutableDisplayName = displayName;
        }

        @Override
        public int getGameMode() {
            return mutableGameMode;
        }

        @Override
        public void setGameMode(int gameMode) {
            mutableGameMode = gameMode;
        }

        @Override
        public int getLatency() {
            return mutableLatency;
        }

        @Override
        public void setLatency(int latency) {
            mutableLatency = latency;
        }

        @Override
        public @Nullable String getSkinTexture() {
            return mutableSkinTexture;
        }

        @Override
        public @Nullable String getSkinSignature() {
            return mutableSkinSignature;
        }

        @Override
        public void setSkin(@Nullable String texture, @Nullable String signature) {
            mutableSkinTexture = texture;
            mutableSkinSignature = signature;
        }

        @Override
        public boolean isListed() {
            return mutableListed;
        }

        @Override
        public void setListed(boolean listed) {
            mutableListed = listed;
        }

        @Override
        public int getSortPriority() {
            return mutableSortPriority;
        }

        @Override
        public void setSortPriority(int priority) {
            mutableSortPriority = priority;
        }
    }
}
