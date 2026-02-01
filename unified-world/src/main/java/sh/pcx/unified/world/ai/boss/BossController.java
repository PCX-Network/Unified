package sh.pcx.unified.world.ai.boss;

import sh.pcx.unified.world.ai.core.AIController;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * Controls boss entity behavior with phase management.
 *
 * <p>The BossController extends standard AI capabilities with boss-specific
 * features including health phases, special attacks, and participant tracking.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Health-based phase transitions</li>
 *   <li>Custom phase behaviors</li>
 *   <li>Boss bar management</li>
 *   <li>Participant tracking and damage attribution</li>
 *   <li>Special attack cooldowns</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * BossController boss = aiService.createBossController(entity, "Dragon Lord");
 *
 * // Add phases
 * boss.addPhase(BossPhase.builder("phase1")
 *     .healthRange(0.75, 1.0)
 *     .behavior(behaviorTree1)
 *     .build());
 *
 * boss.addPhase(BossPhase.builder("phase2")
 *     .healthRange(0.25, 0.75)
 *     .behavior(behaviorTree2)
 *     .onEnter(ctx -> ctx.broadcastMessage("The dragon is enraged!"))
 *     .build());
 *
 * // Start the boss fight
 * boss.start();
 * }</pre>
 *
 * @author Supatuck
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BossController {

    /**
     * Gets the underlying AI controller.
     *
     * @return the AI controller
     */
    @NotNull
    AIController getAIController();

    /**
     * Gets the boss entity.
     *
     * @return the boss entity
     */
    @NotNull
    LivingEntity getEntity();

    /**
     * Gets the boss display name.
     *
     * @return the display name
     */
    @NotNull
    String getName();

    /**
     * Sets the boss display name.
     *
     * @param name the new display name
     */
    void setName(@NotNull String name);

    /**
     * Adds a phase to this boss.
     *
     * @param phase the phase to add
     * @return this controller for chaining
     */
    @NotNull
    BossController addPhase(@NotNull BossPhase phase);

    /**
     * Gets the current phase.
     *
     * @return the current phase, or empty if not started
     */
    @NotNull
    Optional<BossPhase> getCurrentPhase();

    /**
     * Gets all phases.
     *
     * @return collection of all phases
     */
    @NotNull
    Collection<BossPhase> getPhases();

    /**
     * Gets the current health percentage.
     *
     * @return the health percentage (0.0 to 1.0)
     */
    double getHealthPercentage();

    /**
     * Starts the boss fight.
     */
    void start();

    /**
     * Stops the boss fight.
     */
    void stop();

    /**
     * Checks if the boss fight is active.
     *
     * @return true if active
     */
    boolean isActive();

    /**
     * Gets all participants in the boss fight.
     *
     * @return collection of participants
     */
    @NotNull
    Collection<Player> getParticipants();

    /**
     * Adds a participant to the fight.
     *
     * @param player the player to add
     */
    void addParticipant(@NotNull Player player);

    /**
     * Removes a participant from the fight.
     *
     * @param player the player to remove
     */
    void removeParticipant(@NotNull Player player);

    /**
     * Gets the damage dealt by a participant.
     *
     * @param player the player
     * @return the damage dealt
     */
    double getDamageDealt(@NotNull Player player);

    /**
     * Records damage dealt by a participant.
     *
     * @param player the player
     * @param damage the damage amount
     */
    void recordDamage(@NotNull Player player, double damage);

    /**
     * Shows the boss bar to a player.
     *
     * @param player the player
     */
    void showBossBar(@NotNull Player player);

    /**
     * Hides the boss bar from a player.
     *
     * @param player the player
     */
    void hideBossBar(@NotNull Player player);

    /**
     * Ticks the boss controller.
     */
    void tick();

    /**
     * Represents a boss phase.
     */
    interface BossPhase {

        /**
         * Gets the phase name.
         *
         * @return the name
         */
        @NotNull
        String getName();

        /**
         * Gets the minimum health percentage for this phase.
         *
         * @return the minimum health (0.0 to 1.0)
         */
        double getMinHealth();

        /**
         * Gets the maximum health percentage for this phase.
         *
         * @return the maximum health (0.0 to 1.0)
         */
        double getMaxHealth();

        /**
         * Called when entering this phase.
         *
         * @param controller the boss controller
         */
        void onEnter(@NotNull BossController controller);

        /**
         * Called when exiting this phase.
         *
         * @param controller the boss controller
         */
        void onExit(@NotNull BossController controller);

        /**
         * Ticks this phase.
         *
         * @param controller the boss controller
         */
        void tick(@NotNull BossController controller);

        /**
         * Creates a new phase builder.
         *
         * @param name the phase name
         * @return the builder
         */
        @NotNull
        static Builder builder(@NotNull String name) {
            return new Builder(name);
        }

        /**
         * Builder for creating boss phases.
         */
        class Builder {
            private final String name;
            private double minHealth = 0.0;
            private double maxHealth = 1.0;

            /**
             * Creates a new builder.
             *
             * @param name the phase name
             */
            public Builder(@NotNull String name) {
                this.name = name;
            }

            /**
             * Sets the health range for this phase.
             *
             * @param min minimum health percentage
             * @param max maximum health percentage
             * @return this builder
             */
            @NotNull
            public Builder healthRange(double min, double max) {
                this.minHealth = min;
                this.maxHealth = max;
                return this;
            }

            /**
             * Builds the boss phase.
             *
             * @return the boss phase
             */
            @NotNull
            public BossPhase build() {
                return new DefaultBossPhase(name, minHealth, maxHealth);
            }
        }
    }
}

/**
 * Default implementation of BossPhase.
 */
class DefaultBossPhase implements BossController.BossPhase {

    private final String name;
    private final double minHealth;
    private final double maxHealth;

    DefaultBossPhase(@NotNull String name, double minHealth, double maxHealth) {
        this.name = name;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public double getMinHealth() {
        return minHealth;
    }

    @Override
    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void onEnter(@NotNull BossController controller) {
        // Default no-op
    }

    @Override
    public void onExit(@NotNull BossController controller) {
        // Default no-op
    }

    @Override
    public void tick(@NotNull BossController controller) {
        // Default no-op
    }
}
