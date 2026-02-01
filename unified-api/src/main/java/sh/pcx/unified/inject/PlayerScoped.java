/*
 * UnifiedPlugin API
 * Copyright (c) 2024 Supatuck
 * Licensed under the MIT License
 */
package sh.pcx.unified.inject;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation indicating that a single instance is created per player session.
 *
 * <p>Objects annotated with {@code @PlayerScoped} have their lifecycle tied to a player's
 * session. A new instance is created when a player joins the server, and that same instance
 * is used for all injections within that player's context. When the player leaves, the
 * instance is eligible for cleanup and any {@link PreDestroy} methods are invoked.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li><b>Creation:</b> First injection request for a player creates the instance</li>
 *   <li><b>Reuse:</b> Subsequent injections in the same player context return the same instance</li>
 *   <li><b>Destruction:</b> Instance is destroyed when the player disconnects</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Player Session Data</h3>
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerSession {
 *     private final UUID playerId;
 *     private long loginTime;
 *     private int killCount;
 *     private int deathCount;
 *
 *     @Inject
 *     public PlayerSession(UnifiedPlayer player) {
 *         this.playerId = player.getUniqueId();
 *         this.loginTime = System.currentTimeMillis();
 *     }
 *
 *     @PostConstruct
 *     public void onSessionStart() {
 *         // Called after injection is complete
 *     }
 *
 *     @PreDestroy
 *     public void onSessionEnd() {
 *         // Save session statistics before cleanup
 *     }
 *
 *     public Duration getSessionDuration() {
 *         return Duration.ofMillis(System.currentTimeMillis() - loginTime);
 *     }
 * }
 * }</pre>
 *
 * <h3>Player-Specific Service</h3>
 * <pre>{@code
 * @Service
 * @PlayerScoped
 * public class PlayerCooldownManager {
 *     private final Map<String, Long> cooldowns = new HashMap<>();
 *
 *     public boolean hasCooldown(String ability) {
 *         Long expiry = cooldowns.get(ability);
 *         return expiry != null && System.currentTimeMillis() < expiry;
 *     }
 *
 *     public void setCooldown(String ability, Duration duration) {
 *         cooldowns.put(ability, System.currentTimeMillis() + duration.toMillis());
 *     }
 * }
 * }</pre>
 *
 * <h3>Accessing Player-Scoped Services</h3>
 * <pre>{@code
 * @Service
 * @Singleton
 * public class CombatHandler {
 *     @Inject
 *     private Provider<PlayerCooldownManager> cooldownProvider;
 *
 *     public void handleAttack(Player attacker, Player target) {
 *         // Provider automatically resolves to the correct player scope
 *         PlayerCooldownManager cooldowns = cooldownProvider.get();
 *         if (!cooldowns.hasCooldown("attack")) {
 *             // Process attack
 *             cooldowns.setCooldown("attack", Duration.ofSeconds(1));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Player-scoped instances are created per player, so multiple players will have
 * separate instances. However, a single player's instance may be accessed from multiple
 * threads, so implementations should be thread-safe if concurrent access is possible.</p>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see WorldScoped
 * @see PluginScoped
 * @see sh.pcx.unified.inject.scope.PlayerScope
 * @see sh.pcx.unified.inject.scope.ScopeManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ScopeAnnotation
public @interface PlayerScoped {
}
