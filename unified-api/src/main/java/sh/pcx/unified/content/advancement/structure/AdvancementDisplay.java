/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement.structure;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the visual display properties of an advancement.
 *
 * <p>AdvancementDisplay encapsulates all visual aspects of an advancement,
 * including the icon, title, description, frame type, and visibility settings.
 * This class is immutable; use the {@link Builder} to create instances.
 *
 * <h2>Display Properties</h2>
 * <ul>
 *   <li><b>Icon</b> - The item displayed as the advancement icon</li>
 *   <li><b>Title</b> - The advancement name shown in the GUI and toasts</li>
 *   <li><b>Description</b> - The detailed description shown on hover</li>
 *   <li><b>Frame</b> - The visual frame type (TASK, GOAL, CHALLENGE)</li>
 *   <li><b>Background</b> - Optional background texture for root advancements</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Simple display
 * AdvancementDisplay display = AdvancementDisplay.builder()
 *     .icon("minecraft:diamond")
 *     .title(Component.text("Diamond Collector"))
 *     .description(Component.text("Collect your first diamond"))
 *     .frame(AdvancementFrame.TASK)
 *     .build();
 *
 * // Hidden challenge with custom background
 * AdvancementDisplay hidden = AdvancementDisplay.builder()
 *     .icon("minecraft:nether_star")
 *     .title(Component.text("Secret Master"))
 *     .description(Component.text("Complete all hidden challenges"))
 *     .frame(AdvancementFrame.CHALLENGE)
 *     .hidden(true)
 *     .background("minecraft:textures/gui/advancements/backgrounds/nether.png")
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AdvancementFrame
 * @see sh.pcx.unified.content.advancement.CustomAdvancement
 */
public final class AdvancementDisplay {

    private final String icon;
    private final Component title;
    private final Component description;
    private final AdvancementFrame frame;
    private final String background;
    private final boolean showToast;
    private final boolean announceToChat;
    private final boolean hidden;
    private final Float x;
    private final Float y;

    /**
     * Private constructor - use {@link #builder()} to create instances.
     */
    private AdvancementDisplay(Builder builder) {
        this.icon = Objects.requireNonNull(builder.icon, "Icon cannot be null");
        this.title = Objects.requireNonNull(builder.title, "Title cannot be null");
        this.description = builder.description != null ? builder.description : Component.empty();
        this.frame = builder.frame != null ? builder.frame : AdvancementFrame.TASK;
        this.background = builder.background;
        this.showToast = builder.showToast;
        this.announceToChat = builder.announceToChat;
        this.hidden = builder.hidden;
        this.x = builder.x;
        this.y = builder.y;
    }

    /**
     * Creates a new builder for constructing AdvancementDisplay instances.
     *
     * @return a new Builder instance
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple display with the given icon and title.
     *
     * @param icon  the item type for the icon
     * @param title the advancement title
     * @return a new AdvancementDisplay
     * @since 1.0.0
     */
    @NotNull
    public static AdvancementDisplay simple(@NotNull String icon, @NotNull Component title) {
        return builder()
                .icon(icon)
                .title(title)
                .build();
    }

    /**
     * Returns the icon item type.
     *
     * @return the icon item ID (e.g., "minecraft:diamond")
     * @since 1.0.0
     */
    @NotNull
    public String getIcon() {
        return icon;
    }

    /**
     * Returns the advancement title.
     *
     * @return the title component
     * @since 1.0.0
     */
    @NotNull
    public Component getTitle() {
        return title;
    }

    /**
     * Returns the advancement description.
     *
     * @return the description component
     * @since 1.0.0
     */
    @NotNull
    public Component getDescription() {
        return description;
    }

    /**
     * Returns the frame type.
     *
     * @return the advancement frame type
     * @since 1.0.0
     */
    @NotNull
    public AdvancementFrame getFrame() {
        return frame;
    }

    /**
     * Returns the background texture path for root advancements.
     *
     * @return an Optional containing the background path if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<String> getBackground() {
        return Optional.ofNullable(background);
    }

    /**
     * Returns whether a toast notification should be shown on completion.
     *
     * @return true if toast should be shown
     * @since 1.0.0
     */
    public boolean shouldShowToast() {
        return showToast;
    }

    /**
     * Returns whether completion should be announced in chat.
     *
     * @return true if completion should be announced
     * @since 1.0.0
     */
    public boolean shouldAnnounceToChat() {
        return announceToChat;
    }

    /**
     * Returns whether this advancement is hidden until completed.
     *
     * <p>Hidden advancements are not visible in the advancement GUI
     * until the player has completed them. This is useful for secret
     * achievements or surprises.
     *
     * @return true if the advancement is hidden
     * @since 1.0.0
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Returns the X coordinate for display in the advancement tree.
     *
     * @return an Optional containing the X coordinate if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Float> getX() {
        return Optional.ofNullable(x);
    }

    /**
     * Returns the Y coordinate for display in the advancement tree.
     *
     * @return an Optional containing the Y coordinate if set
     * @since 1.0.0
     */
    @NotNull
    public Optional<Float> getY() {
        return Optional.ofNullable(y);
    }

    /**
     * Creates a new builder pre-populated with this display's values.
     *
     * @return a new Builder with current values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder()
                .icon(icon)
                .title(title)
                .description(description)
                .frame(frame)
                .background(background)
                .showToast(showToast)
                .announceToChat(announceToChat)
                .hidden(hidden)
                .position(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdvancementDisplay that = (AdvancementDisplay) o;
        return showToast == that.showToast &&
               announceToChat == that.announceToChat &&
               hidden == that.hidden &&
               icon.equals(that.icon) &&
               title.equals(that.title) &&
               description.equals(that.description) &&
               frame == that.frame &&
               Objects.equals(background, that.background) &&
               Objects.equals(x, that.x) &&
               Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, title, description, frame, background, showToast, announceToChat, hidden, x, y);
    }

    @Override
    public String toString() {
        return "AdvancementDisplay{" +
               "icon='" + icon + '\'' +
               ", title=" + title +
               ", frame=" + frame +
               ", hidden=" + hidden +
               '}';
    }

    /**
     * Fluent builder for creating {@link AdvancementDisplay} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private String icon;
        private Component title;
        private Component description;
        private AdvancementFrame frame = AdvancementFrame.TASK;
        private String background;
        private boolean showToast = true;
        private boolean announceToChat = false;
        private boolean hidden = false;
        private Float x;
        private Float y;

        /**
         * Sets the icon item type.
         *
         * @param icon the item type ID (e.g., "minecraft:diamond")
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder icon(@NotNull String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Sets the advancement title.
         *
         * @param title the title component
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder title(@NotNull Component title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the advancement title from a string.
         *
         * @param title the title text
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder title(@NotNull String title) {
            this.title = Component.text(title);
            return this;
        }

        /**
         * Sets the advancement description.
         *
         * @param description the description component
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@NotNull Component description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the advancement description from a string.
         *
         * @param description the description text
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder description(@NotNull String description) {
            this.description = Component.text(description);
            return this;
        }

        /**
         * Sets the frame type.
         *
         * @param frame the advancement frame type
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder frame(@NotNull AdvancementFrame frame) {
            this.frame = frame;
            return this;
        }

        /**
         * Sets the background texture path for root advancements.
         *
         * @param background the texture path (e.g., "minecraft:textures/gui/advancements/backgrounds/stone.png")
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder background(@Nullable String background) {
            this.background = background;
            return this;
        }

        /**
         * Sets whether a toast notification should be shown on completion.
         *
         * @param showToast true to show toast (default: true)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder showToast(boolean showToast) {
            this.showToast = showToast;
            return this;
        }

        /**
         * Sets whether completion should be announced in chat.
         *
         * @param announceToChat true to announce (default: false, except for GOAL and CHALLENGE)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder announceToChat(boolean announceToChat) {
            this.announceToChat = announceToChat;
            return this;
        }

        /**
         * Sets whether this advancement is hidden until completed.
         *
         * @param hidden true to hide the advancement (default: false)
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        /**
         * Sets the position in the advancement tree.
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder position(@Nullable Float x, @Nullable Float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
         * Sets the position in the advancement tree.
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder position(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
         * Builds the AdvancementDisplay instance.
         *
         * @return the constructed AdvancementDisplay
         * @throws NullPointerException if required fields are not set
         * @since 1.0.0
         */
        @NotNull
        public AdvancementDisplay build() {
            // Apply default announceToChat based on frame if not explicitly set
            if (frame != null && frame.shouldAnnounceToChat() && !announceToChat) {
                announceToChat = true;
            }
            return new AdvancementDisplay(this);
        }
    }
}
