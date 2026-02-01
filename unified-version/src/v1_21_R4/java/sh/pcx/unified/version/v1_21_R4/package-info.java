/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * NMS implementations for Minecraft 1.21.11+ (v1_21_R4).
 *
 * <p>This package contains version-specific implementations for Minecraft 1.21.11
 * and later versions, which introduced significant changes to how NMS code is accessed.
 *
 * <h2>CRITICAL Changes in 1.21.11+</h2>
 *
 * <h3>Mojang Mappings</h3>
 * <p>Paper 1.21.11+ uses Mojang mappings instead of Spigot mappings. This means:
 * <ul>
 *   <li>NMS class names are completely different</li>
 *   <li>Method names follow Mojang's naming conventions</li>
 *   <li>No more version-suffixed CraftBukkit packages</li>
 *   <li>Code using reflection must be updated</li>
 * </ul>
 *
 * <h3>Registry-Based GameRules</h3>
 * <p>Gamerules now use snake_case naming in the registry:
 * <ul>
 *   <li>Old: {@code doFireTick}, {@code keepInventory}, {@code mobGriefing}</li>
 *   <li>New: {@code do_fire_tick}, {@code keep_inventory}, {@code mob_griefing}</li>
 * </ul>
 *
 * <h3>Class Name Mappings</h3>
 * <table>
 *   <caption>Key class name changes from Spigot to Mojang mappings</caption>
 *   <tr><th>Spigot Mapping</th><th>Mojang Mapping</th></tr>
 *   <tr><td>EntityPlayer</td><td>ServerPlayer</td></tr>
 *   <tr><td>WorldServer</td><td>ServerLevel</td></tr>
 *   <tr><td>IChatBaseComponent</td><td>Component</td></tr>
 *   <tr><td>NBTTagCompound</td><td>CompoundTag</td></tr>
 *   <tr><td>PacketPlayOutChat</td><td>ClientboundSystemChatPacket</td></tr>
 *   <tr><td>PlayerConnection</td><td>ServerGamePacketListenerImpl</td></tr>
 * </table>
 *
 * <h2>Implementation Classes</h2>
 * <ul>
 *   <li>{@link sh.pcx.unified.version.v1_21_R4.NMSBridgeImpl}</li>
 *   <li>{@link sh.pcx.unified.version.v1_21_R4.NBTServiceImpl}</li>
 *   <li>{@link sh.pcx.unified.version.v1_21_R4.ComponentBridgeImpl}</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
@NullMarked
package sh.pcx.unified.version.v1_21_R4;

import org.jspecify.annotations.NullMarked;
