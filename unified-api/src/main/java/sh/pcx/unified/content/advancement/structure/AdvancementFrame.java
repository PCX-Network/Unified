/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.advancement.structure;

/**
 * Enumeration representing the frame type for advancement display.
 *
 * <p>The frame type determines the visual appearance of the advancement
 * in the advancement GUI, including the border style and toast notification
 * appearance when the advancement is completed.
 *
 * <h2>Frame Types</h2>
 * <ul>
 *   <li>{@link #TASK} - Default frame, represents simple tasks</li>
 *   <li>{@link #GOAL} - Rounded frame, represents milestones</li>
 *   <li>{@link #CHALLENGE} - Spiked frame, represents difficult challenges</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * AdvancementDisplay display = AdvancementDisplay.builder()
 *     .icon("minecraft:diamond_sword")
 *     .title(Component.text("Master Swordsman"))
 *     .frame(AdvancementFrame.CHALLENGE)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see AdvancementDisplay
 */
public enum AdvancementFrame {

    /**
     * Task frame - the default rectangular frame.
     *
     * <p>Used for simple, straightforward advancements that represent
     * basic tasks or actions. This is the most common frame type.
     */
    TASK("task", false),

    /**
     * Goal frame - a rounded, elliptical frame.
     *
     * <p>Used for milestone advancements that represent significant
     * progress points or objectives. Goals typically represent
     * intermediate achievements.
     */
    GOAL("goal", true),

    /**
     * Challenge frame - a spiked, star-like frame.
     *
     * <p>Used for difficult advancements that require significant
     * effort or skill to complete. Challenges trigger a special
     * announcement when completed and may grant unique rewards.
     */
    CHALLENGE("challenge", true);

    private final String id;
    private final boolean announceToChat;

    /**
     * Constructs an advancement frame type.
     *
     * @param id             the Minecraft identifier for this frame type
     * @param announceToChat whether completion should be announced in chat
     */
    AdvancementFrame(String id, boolean announceToChat) {
        this.id = id;
        this.announceToChat = announceToChat;
    }

    /**
     * Returns the Minecraft identifier for this frame type.
     *
     * @return the frame type identifier (e.g., "task", "goal", "challenge")
     * @since 1.0.0
     */
    public String getId() {
        return id;
    }

    /**
     * Returns whether advancement completion should be announced in chat.
     *
     * <p>Goals and challenges are announced by default, while tasks are not.
     *
     * @return true if completion should be announced to chat
     * @since 1.0.0
     */
    public boolean shouldAnnounceToChat() {
        return announceToChat;
    }

    /**
     * Parses a frame type from its string identifier.
     *
     * @param id the frame type identifier
     * @return the corresponding AdvancementFrame
     * @throws IllegalArgumentException if the identifier is not recognized
     * @since 1.0.0
     */
    public static AdvancementFrame fromId(String id) {
        for (AdvancementFrame frame : values()) {
            if (frame.id.equalsIgnoreCase(id)) {
                return frame;
            }
        }
        throw new IllegalArgumentException("Unknown advancement frame type: " + id);
    }

    /**
     * Attempts to parse a frame type from its string identifier.
     *
     * @param id           the frame type identifier
     * @param defaultValue the default value if parsing fails
     * @return the corresponding AdvancementFrame, or the default value
     * @since 1.0.0
     */
    public static AdvancementFrame fromIdOrDefault(String id, AdvancementFrame defaultValue) {
        try {
            return fromId(id);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
