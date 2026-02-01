/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.i18n.player;

import sh.pcx.unified.i18n.core.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player's locale preference and settings.
 *
 * <p>PlayerLocale tracks both the player's explicitly set preference
 * and their client-detected locale, providing the ability to determine
 * the effective locale to use for translations.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a player locale preference
 * PlayerLocale playerLocale = PlayerLocale.builder()
 *     .playerId(player.getUniqueId())
 *     .preferredLocale(Locale.GERMAN)
 *     .clientLocale(Locale.parse(player.getClientLocale()))
 *     .build();
 *
 * // Get effective locale
 * Locale effective = playerLocale.getEffectiveLocale(serverDefault);
 *
 * // Check if player has explicitly set their preference
 * if (playerLocale.hasPreference()) {
 *     // Use their preference
 * }
 * }</pre>
 *
 * <h2>Locale Priority</h2>
 * <ol>
 *   <li>Player's explicit preference (if set)</li>
 *   <li>Client-detected locale (if available and supported)</li>
 *   <li>Server default locale</li>
 * </ol>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Locale
 */
public final class PlayerLocale {

    private final UUID playerId;
    private final Locale preferredLocale;
    private final Locale clientLocale;
    private final Instant lastUpdated;
    private final boolean useClientLocale;

    private PlayerLocale(Builder builder) {
        this.playerId = Objects.requireNonNull(builder.playerId, "playerId cannot be null");
        this.preferredLocale = builder.preferredLocale;
        this.clientLocale = builder.clientLocale;
        this.lastUpdated = builder.lastUpdated != null ? builder.lastUpdated : Instant.now();
        this.useClientLocale = builder.useClientLocale;
    }

    /**
     * Creates a new builder for PlayerLocale.
     *
     * @return a new builder
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a PlayerLocale with only a player ID (no preferences set).
     *
     * @param playerId the player's UUID
     * @return a new PlayerLocale with no preferences
     * @since 1.0.0
     */
    @NotNull
    public static PlayerLocale empty(@NotNull UUID playerId) {
        return builder().playerId(playerId).build();
    }

    /**
     * Creates a PlayerLocale with an explicit preference.
     *
     * @param playerId the player's UUID
     * @param locale   the preferred locale
     * @return a new PlayerLocale with the preference
     * @since 1.0.0
     */
    @NotNull
    public static PlayerLocale of(@NotNull UUID playerId, @NotNull Locale locale) {
        return builder()
                .playerId(playerId)
                .preferredLocale(locale)
                .build();
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player ID
     * @since 1.0.0
     */
    @NotNull
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Returns the player's explicitly set locale preference.
     *
     * @return the preferred locale, or empty if not set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Locale> getPreferredLocale() {
        return Optional.ofNullable(preferredLocale);
    }

    /**
     * Returns the client-detected locale.
     *
     * @return the client locale, or empty if not detected
     * @since 1.0.0
     */
    @NotNull
    public Optional<Locale> getClientLocale() {
        return Optional.ofNullable(clientLocale);
    }

    /**
     * Checks if the player has explicitly set a locale preference.
     *
     * @return true if a preference is set
     * @since 1.0.0
     */
    public boolean hasPreference() {
        return preferredLocale != null;
    }

    /**
     * Checks if client locale auto-detection is enabled.
     *
     * @return true if client locale should be used when no preference is set
     * @since 1.0.0
     */
    public boolean isUseClientLocale() {
        return useClientLocale;
    }

    /**
     * Returns when this preference was last updated.
     *
     * @return the last update timestamp
     * @since 1.0.0
     */
    @NotNull
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Determines the effective locale for this player.
     *
     * <p>Priority order:
     * <ol>
     *   <li>Player's explicit preference</li>
     *   <li>Client locale (if useClientLocale is true)</li>
     *   <li>The provided default locale</li>
     * </ol>
     *
     * @param defaultLocale the server default locale
     * @return the effective locale to use
     * @since 1.0.0
     */
    @NotNull
    public Locale getEffectiveLocale(@NotNull Locale defaultLocale) {
        if (preferredLocale != null) {
            return preferredLocale;
        }
        if (useClientLocale && clientLocale != null) {
            return clientLocale;
        }
        return defaultLocale;
    }

    /**
     * Returns a new PlayerLocale with an updated preference.
     *
     * @param locale the new preferred locale, or null to clear
     * @return a new PlayerLocale with the updated preference
     * @since 1.0.0
     */
    @NotNull
    public PlayerLocale withPreferredLocale(@Nullable Locale locale) {
        return builder()
                .playerId(playerId)
                .preferredLocale(locale)
                .clientLocale(clientLocale)
                .useClientLocale(useClientLocale)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * Returns a new PlayerLocale with an updated client locale.
     *
     * @param locale the client locale
     * @return a new PlayerLocale with the updated client locale
     * @since 1.0.0
     */
    @NotNull
    public PlayerLocale withClientLocale(@Nullable Locale locale) {
        return builder()
                .playerId(playerId)
                .preferredLocale(preferredLocale)
                .clientLocale(locale)
                .useClientLocale(useClientLocale)
                .lastUpdated(lastUpdated)
                .build();
    }

    /**
     * Returns a new PlayerLocale with the preference cleared.
     *
     * @return a new PlayerLocale without a preference
     * @since 1.0.0
     */
    @NotNull
    public PlayerLocale clearPreference() {
        return withPreferredLocale(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlayerLocale other)) {
            return false;
        }
        return playerId.equals(other.playerId)
                && Objects.equals(preferredLocale, other.preferredLocale)
                && Objects.equals(clientLocale, other.clientLocale)
                && useClientLocale == other.useClientLocale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, preferredLocale, clientLocale, useClientLocale);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PlayerLocale{");
        sb.append("playerId=").append(playerId);
        if (preferredLocale != null) {
            sb.append(", preferred=").append(preferredLocale);
        }
        if (clientLocale != null) {
            sb.append(", client=").append(clientLocale);
        }
        sb.append(", useClient=").append(useClientLocale);
        return sb.append("}").toString();
    }

    /**
     * Builder for creating PlayerLocale instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private UUID playerId;
        private Locale preferredLocale;
        private Locale clientLocale;
        private Instant lastUpdated;
        private boolean useClientLocale = true;

        private Builder() {}

        /**
         * Sets the player's UUID.
         *
         * @param playerId the player ID
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder playerId(@NotNull UUID playerId) {
            this.playerId = playerId;
            return this;
        }

        /**
         * Sets the player's preferred locale.
         *
         * @param locale the preferred locale, or null to clear
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder preferredLocale(@Nullable Locale locale) {
            this.preferredLocale = locale;
            return this;
        }

        /**
         * Sets the client-detected locale.
         *
         * @param locale the client locale
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder clientLocale(@Nullable Locale locale) {
            this.clientLocale = locale;
            return this;
        }

        /**
         * Sets the last updated timestamp.
         *
         * @param lastUpdated the timestamp
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder lastUpdated(@Nullable Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        /**
         * Sets whether to use client locale when no preference is set.
         *
         * @param useClientLocale true to use client locale
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder useClientLocale(boolean useClientLocale) {
            this.useClientLocale = useClientLocale;
            return this;
        }

        /**
         * Builds the PlayerLocale.
         *
         * @return the constructed PlayerLocale
         * @throws NullPointerException if playerId is not set
         * @since 1.0.0
         */
        @NotNull
        public PlayerLocale build() {
            return new PlayerLocale(this);
        }
    }
}
