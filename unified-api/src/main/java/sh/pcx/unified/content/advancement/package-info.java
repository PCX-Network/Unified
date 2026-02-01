/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Custom advancement system for the UnifiedPlugin framework.
 *
 * <p>This package provides a complete API for creating custom advancements
 * with GUI integration, triggers, rewards, progress tracking, and
 * persistence across restarts.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.content.advancement.AdvancementService} - Main service interface</li>
 *   <li>{@link sh.pcx.unified.content.advancement.CustomAdvancement} - Advancement representation</li>
 *   <li>{@link sh.pcx.unified.content.advancement.AdvancementBuilder} - Fluent builder</li>
 *   <li>{@link sh.pcx.unified.content.advancement.Trigger} - Completion triggers</li>
 *   <li>{@link sh.pcx.unified.content.advancement.CustomTrigger} - Plugin-defined triggers</li>
 *   <li>{@link sh.pcx.unified.content.advancement.AdvancementReward} - Completion rewards</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * CustomAdvancement explorer = advancements.create("myplugin:explorer")
 *     .parent(root)
 *     .display(AdvancementDisplay.builder()
 *         .icon("minecraft:filled_map")
 *         .title(Component.text("World Explorer"))
 *         .frame(AdvancementFrame.GOAL)
 *         .build())
 *     .criteria("visit_nether", Trigger.changedDimension("minecraft:the_nether"))
 *     .criteria("visit_end", Trigger.changedDimension("minecraft:the_end"))
 *     .requireAll()
 *     .reward(AdvancementReward.experience(500))
 *     .register();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.content.advancement;
