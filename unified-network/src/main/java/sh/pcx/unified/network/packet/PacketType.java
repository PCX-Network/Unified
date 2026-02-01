/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.network.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Enumeration of Minecraft packet types with metadata.
 *
 * <p>This enum provides a type-safe way to reference Minecraft protocol packets
 * across different versions. Each packet type includes its direction (client/server),
 * category, and version-specific mappings.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Check packet direction
 * PacketType type = PacketType.PLAY_OUT_SPAWN_ENTITY;
 * if (type.getDirection() == PacketDirection.OUTBOUND) {
 *     // Handle outbound packet
 * }
 *
 * // Listen for specific packets
 * packetService.addListener(PacketType.PLAY_IN_USE_ENTITY, event -> {
 *     // Handle entity interaction
 * });
 *
 * // Check packet category
 * if (type.getCategory() == PacketCategory.PLAY) {
 *     // Play-state packet
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see Packet
 * @see PacketService
 */
public enum PacketType {

    // ========================
    // HANDSHAKE PACKETS
    // ========================

    /** Client handshake packet. */
    HANDSHAKE_IN_SET_PROTOCOL(PacketDirection.INBOUND, PacketCategory.HANDSHAKE, "SetProtocol"),

    // ========================
    // STATUS PACKETS
    // ========================

    /** Server status request. */
    STATUS_IN_START(PacketDirection.INBOUND, PacketCategory.STATUS, "Start"),

    /** Server ping request. */
    STATUS_IN_PING(PacketDirection.INBOUND, PacketCategory.STATUS, "Ping"),

    /** Server status response. */
    STATUS_OUT_SERVER_INFO(PacketDirection.OUTBOUND, PacketCategory.STATUS, "ServerInfo"),

    /** Server ping response. */
    STATUS_OUT_PONG(PacketDirection.OUTBOUND, PacketCategory.STATUS, "Pong"),

    // ========================
    // LOGIN PACKETS
    // ========================

    /** Login start from client. */
    LOGIN_IN_START(PacketDirection.INBOUND, PacketCategory.LOGIN, "Start"),

    /** Encryption response from client. */
    LOGIN_IN_ENCRYPTION_BEGIN(PacketDirection.INBOUND, PacketCategory.LOGIN, "EncryptionBegin"),

    /** Custom payload during login. */
    LOGIN_IN_CUSTOM_PAYLOAD(PacketDirection.INBOUND, PacketCategory.LOGIN, "CustomPayload"),

    /** Login acknowledged by client. */
    LOGIN_IN_LOGIN_ACKNOWLEDGED(PacketDirection.INBOUND, PacketCategory.LOGIN, "LoginAcknowledged"),

    /** Disconnect during login. */
    LOGIN_OUT_DISCONNECT(PacketDirection.OUTBOUND, PacketCategory.LOGIN, "Disconnect"),

    /** Encryption request to client. */
    LOGIN_OUT_ENCRYPTION_BEGIN(PacketDirection.OUTBOUND, PacketCategory.LOGIN, "EncryptionBegin"),

    /** Login success to client. */
    LOGIN_OUT_SUCCESS(PacketDirection.OUTBOUND, PacketCategory.LOGIN, "Success"),

    /** Set compression to client. */
    LOGIN_OUT_SET_COMPRESSION(PacketDirection.OUTBOUND, PacketCategory.LOGIN, "SetCompression"),

    /** Custom payload to client. */
    LOGIN_OUT_CUSTOM_PAYLOAD(PacketDirection.OUTBOUND, PacketCategory.LOGIN, "CustomPayload"),

    // ========================
    // CONFIGURATION PACKETS (1.20.2+)
    // ========================

    /** Client settings during configuration. */
    CONFIG_IN_SETTINGS(PacketDirection.INBOUND, PacketCategory.CONFIGURATION, "Settings"),

    /** Custom payload during configuration. */
    CONFIG_IN_CUSTOM_PAYLOAD(PacketDirection.INBOUND, PacketCategory.CONFIGURATION, "CustomPayload"),

    /** Configuration acknowledged. */
    CONFIG_IN_FINISH(PacketDirection.INBOUND, PacketCategory.CONFIGURATION, "Finish"),

    /** Keep alive response. */
    CONFIG_IN_KEEP_ALIVE(PacketDirection.INBOUND, PacketCategory.CONFIGURATION, "KeepAlive"),

    /** Resource pack response. */
    CONFIG_IN_RESOURCE_PACK(PacketDirection.INBOUND, PacketCategory.CONFIGURATION, "ResourcePack"),

    /** Custom payload to client. */
    CONFIG_OUT_CUSTOM_PAYLOAD(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "CustomPayload"),

    /** Disconnect during configuration. */
    CONFIG_OUT_DISCONNECT(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "Disconnect"),

    /** Finish configuration. */
    CONFIG_OUT_FINISH(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "Finish"),

    /** Keep alive request. */
    CONFIG_OUT_KEEP_ALIVE(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "KeepAlive"),

    /** Registry data. */
    CONFIG_OUT_REGISTRY_DATA(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "RegistryData"),

    /** Resource pack to client. */
    CONFIG_OUT_RESOURCE_PACK(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "ResourcePack"),

    /** Feature flags. */
    CONFIG_OUT_UPDATE_ENABLED_FEATURES(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "UpdateEnabledFeatures"),

    /** Update tags. */
    CONFIG_OUT_UPDATE_TAGS(PacketDirection.OUTBOUND, PacketCategory.CONFIGURATION, "UpdateTags"),

    // ========================
    // PLAY INBOUND PACKETS
    // ========================

    /** Teleport confirmation from client. */
    PLAY_IN_TELEPORT_ACCEPT(PacketDirection.INBOUND, PacketCategory.PLAY, "TeleportAccept"),

    /** Block query from client. */
    PLAY_IN_TILE_NBT_QUERY(PacketDirection.INBOUND, PacketCategory.PLAY, "TileNBTQuery"),

    /** Chat message from client. */
    PLAY_IN_CHAT(PacketDirection.INBOUND, PacketCategory.PLAY, "Chat"),

    /** Client command (respawn, stats). */
    PLAY_IN_CLIENT_COMMAND(PacketDirection.INBOUND, PacketCategory.PLAY, "ClientCommand"),

    /** Client settings. */
    PLAY_IN_SETTINGS(PacketDirection.INBOUND, PacketCategory.PLAY, "Settings"),

    /** Tab complete request. */
    PLAY_IN_TAB_COMPLETE(PacketDirection.INBOUND, PacketCategory.PLAY, "TabComplete"),

    /** Click window button. */
    PLAY_IN_ENCHANT_ITEM(PacketDirection.INBOUND, PacketCategory.PLAY, "EnchantItem"),

    /** Window click. */
    PLAY_IN_WINDOW_CLICK(PacketDirection.INBOUND, PacketCategory.PLAY, "WindowClick"),

    /** Close window. */
    PLAY_IN_CLOSE_WINDOW(PacketDirection.INBOUND, PacketCategory.PLAY, "CloseWindow"),

    /** Plugin message from client. */
    PLAY_IN_CUSTOM_PAYLOAD(PacketDirection.INBOUND, PacketCategory.PLAY, "CustomPayload"),

    /** Edit book. */
    PLAY_IN_B_EDIT(PacketDirection.INBOUND, PacketCategory.PLAY, "BEdit"),

    /** Entity NBT query. */
    PLAY_IN_ENTITY_NBT_QUERY(PacketDirection.INBOUND, PacketCategory.PLAY, "EntityNBTQuery"),

    /** Interact with entity. */
    PLAY_IN_USE_ENTITY(PacketDirection.INBOUND, PacketCategory.PLAY, "UseEntity"),

    /** Jigsaw generate. */
    PLAY_IN_JIGSAW_GENERATE(PacketDirection.INBOUND, PacketCategory.PLAY, "JigsawGenerate"),

    /** Keep alive response. */
    PLAY_IN_KEEP_ALIVE(PacketDirection.INBOUND, PacketCategory.PLAY, "KeepAlive"),

    /** Lock difficulty. */
    PLAY_IN_DIFFICULTY_LOCK(PacketDirection.INBOUND, PacketCategory.PLAY, "DifficultyLock"),

    /** Player position. */
    PLAY_IN_POSITION(PacketDirection.INBOUND, PacketCategory.PLAY, "Position"),

    /** Player position and look. */
    PLAY_IN_POSITION_LOOK(PacketDirection.INBOUND, PacketCategory.PLAY, "PositionLook"),

    /** Player look. */
    PLAY_IN_LOOK(PacketDirection.INBOUND, PacketCategory.PLAY, "Look"),

    /** Player flying state. */
    PLAY_IN_FLYING(PacketDirection.INBOUND, PacketCategory.PLAY, "Flying"),

    /** Vehicle move. */
    PLAY_IN_VEHICLE_MOVE(PacketDirection.INBOUND, PacketCategory.PLAY, "VehicleMove"),

    /** Steer boat. */
    PLAY_IN_BOAT_MOVE(PacketDirection.INBOUND, PacketCategory.PLAY, "BoatMove"),

    /** Pick item. */
    PLAY_IN_PICK_ITEM(PacketDirection.INBOUND, PacketCategory.PLAY, "PickItem"),

    /** Craft recipe request. */
    PLAY_IN_AUTO_RECIPE(PacketDirection.INBOUND, PacketCategory.PLAY, "AutoRecipe"),

    /** Player abilities. */
    PLAY_IN_ABILITIES(PacketDirection.INBOUND, PacketCategory.PLAY, "Abilities"),

    /** Player digging. */
    PLAY_IN_BLOCK_DIG(PacketDirection.INBOUND, PacketCategory.PLAY, "BlockDig"),

    /** Entity action (sneak, sprint, etc). */
    PLAY_IN_ENTITY_ACTION(PacketDirection.INBOUND, PacketCategory.PLAY, "EntityAction"),

    /** Steer vehicle. */
    PLAY_IN_STEER_VEHICLE(PacketDirection.INBOUND, PacketCategory.PLAY, "SteerVehicle"),

    /** Pong response. */
    PLAY_IN_PONG(PacketDirection.INBOUND, PacketCategory.PLAY, "Pong"),

    /** Recipe book state. */
    PLAY_IN_RECIPE_SETTINGS(PacketDirection.INBOUND, PacketCategory.PLAY, "RecipeSettings"),

    /** Set displayed recipe. */
    PLAY_IN_RECIPE_DISPLAYED(PacketDirection.INBOUND, PacketCategory.PLAY, "RecipeDisplayed"),

    /** Rename item. */
    PLAY_IN_ITEM_NAME(PacketDirection.INBOUND, PacketCategory.PLAY, "ItemName"),

    /** Resource pack status. */
    PLAY_IN_RESOURCE_PACK_STATUS(PacketDirection.INBOUND, PacketCategory.PLAY, "ResourcePackStatus"),

    /** Advancement tab. */
    PLAY_IN_ADVANCEMENTS(PacketDirection.INBOUND, PacketCategory.PLAY, "Advancements"),

    /** Select trade. */
    PLAY_IN_TR_SEL(PacketDirection.INBOUND, PacketCategory.PLAY, "TrSel"),

    /** Set beacon effect. */
    PLAY_IN_BEACON(PacketDirection.INBOUND, PacketCategory.PLAY, "Beacon"),

    /** Held item change. */
    PLAY_IN_HELD_ITEM_SLOT(PacketDirection.INBOUND, PacketCategory.PLAY, "HeldItemSlot"),

    /** Update command block. */
    PLAY_IN_SET_COMMAND_BLOCK(PacketDirection.INBOUND, PacketCategory.PLAY, "SetCommandBlock"),

    /** Update command block minecart. */
    PLAY_IN_SET_COMMAND_MINECART(PacketDirection.INBOUND, PacketCategory.PLAY, "SetCommandMinecart"),

    /** Creative inventory action. */
    PLAY_IN_SET_CREATIVE_SLOT(PacketDirection.INBOUND, PacketCategory.PLAY, "SetCreativeSlot"),

    /** Update jigsaw block. */
    PLAY_IN_SET_JIGSAW(PacketDirection.INBOUND, PacketCategory.PLAY, "SetJigsaw"),

    /** Update structure block. */
    PLAY_IN_STRUCT(PacketDirection.INBOUND, PacketCategory.PLAY, "Struct"),

    /** Update sign. */
    PLAY_IN_UPDATE_SIGN(PacketDirection.INBOUND, PacketCategory.PLAY, "UpdateSign"),

    /** Animation (arm swing). */
    PLAY_IN_ARM_ANIMATION(PacketDirection.INBOUND, PacketCategory.PLAY, "ArmAnimation"),

    /** Spectate. */
    PLAY_IN_SPECTATE(PacketDirection.INBOUND, PacketCategory.PLAY, "Spectate"),

    /** Use item on block. */
    PLAY_IN_USE_ITEM(PacketDirection.INBOUND, PacketCategory.PLAY, "UseItem"),

    /** Use item. */
    PLAY_IN_BLOCK_PLACE(PacketDirection.INBOUND, PacketCategory.PLAY, "BlockPlace"),

    // ========================
    // PLAY OUTBOUND PACKETS
    // ========================

    /** Bundle delimiter. */
    PLAY_OUT_BUNDLE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Bundle"),

    /** Spawn entity. */
    PLAY_OUT_SPAWN_ENTITY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SpawnEntity"),

    /** Spawn experience orb. */
    PLAY_OUT_SPAWN_ENTITY_EXPERIENCE_ORB(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SpawnEntityExperienceOrb"),

    /** Entity animation. */
    PLAY_OUT_ANIMATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Animation"),

    /** Award statistics. */
    PLAY_OUT_STATISTIC(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Statistic"),

    /** Acknowledge block change. */
    PLAY_OUT_BLOCK_CHANGED_ACK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "BlockChangedAck"),

    /** Block break animation. */
    PLAY_OUT_BLOCK_BREAK_ANIMATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "BlockBreakAnimation"),

    /** Block entity data. */
    PLAY_OUT_TILE_ENTITY_DATA(PacketDirection.OUTBOUND, PacketCategory.PLAY, "TileEntityData"),

    /** Block action. */
    PLAY_OUT_BLOCK_ACTION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "BlockAction"),

    /** Block change. */
    PLAY_OUT_BLOCK_CHANGE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "BlockChange"),

    /** Boss bar. */
    PLAY_OUT_BOSS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Boss"),

    /** Server difficulty. */
    PLAY_OUT_SERVER_DIFFICULTY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ServerDifficulty"),

    /** Chunk batch finished. */
    PLAY_OUT_CHUNK_BATCH_FINISHED(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ChunkBatchFinished"),

    /** Chunk batch start. */
    PLAY_OUT_CHUNK_BATCH_START(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ChunkBatchStart"),

    /** Chunk biomes. */
    PLAY_OUT_CHUNK_BIOMES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ChunkBiomes"),

    /** Clear titles. */
    PLAY_OUT_CLEAR_TITLES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ClearTitles"),

    /** Tab complete. */
    PLAY_OUT_TAB_COMPLETE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "TabComplete"),

    /** Commands. */
    PLAY_OUT_COMMANDS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Commands"),

    /** Close window. */
    PLAY_OUT_CLOSE_WINDOW(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CloseWindow"),

    /** Window items. */
    PLAY_OUT_WINDOW_ITEMS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "WindowItems"),

    /** Window property. */
    PLAY_OUT_WINDOW_DATA(PacketDirection.OUTBOUND, PacketCategory.PLAY, "WindowData"),

    /** Set slot. */
    PLAY_OUT_SET_SLOT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetSlot"),

    /** Cooldown. */
    PLAY_OUT_SET_COOLDOWN(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetCooldown"),

    /** Custom chat completions. */
    PLAY_OUT_CUSTOM_CHAT_COMPLETIONS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CustomChatCompletions"),

    /** Plugin message. */
    PLAY_OUT_CUSTOM_PAYLOAD(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CustomPayload"),

    /** Damage event. */
    PLAY_OUT_DAMAGE_EVENT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "DamageEvent"),

    /** Delete chat message. */
    PLAY_OUT_DELETE_CHAT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "DeleteChat"),

    /** Disconnect. */
    PLAY_OUT_KICK_DISCONNECT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "KickDisconnect"),

    /** Disguised chat message. */
    PLAY_OUT_DISGUISED_CHAT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "DisguisedChat"),

    /** Entity status. */
    PLAY_OUT_ENTITY_STATUS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityStatus"),

    /** Explosion. */
    PLAY_OUT_EXPLOSION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Explosion"),

    /** Unload chunk. */
    PLAY_OUT_UNLOAD_CHUNK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "UnloadChunk"),

    /** Game state change. */
    PLAY_OUT_GAME_STATE_CHANGE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "GameStateChange"),

    /** Open horse window. */
    PLAY_OUT_OPEN_WINDOW_HORSE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "OpenWindowHorse"),

    /** Hurt animation. */
    PLAY_OUT_HURT_ANIMATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "HurtAnimation"),

    /** Initialize world border. */
    PLAY_OUT_INITIALIZE_BORDER(PacketDirection.OUTBOUND, PacketCategory.PLAY, "InitializeBorder"),

    /** Keep alive. */
    PLAY_OUT_KEEP_ALIVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "KeepAlive"),

    /** Chunk data. */
    PLAY_OUT_MAP_CHUNK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "MapChunk"),

    /** World event. */
    PLAY_OUT_WORLD_EVENT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "WorldEvent"),

    /** Particle. */
    PLAY_OUT_WORLD_PARTICLES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "WorldParticles"),

    /** Light update. */
    PLAY_OUT_LIGHT_UPDATE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "LightUpdate"),

    /** Login (join game). */
    PLAY_OUT_LOGIN(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Login"),

    /** Map data. */
    PLAY_OUT_MAP(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Map"),

    /** Merchant offers. */
    PLAY_OUT_OPEN_WINDOW_MERCHANT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "OpenWindowMerchant"),

    /** Entity position. */
    PLAY_OUT_REL_ENTITY_MOVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "RelEntityMove"),

    /** Entity position and rotation. */
    PLAY_OUT_REL_ENTITY_MOVE_LOOK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "RelEntityMoveLook"),

    /** Entity rotation. */
    PLAY_OUT_ENTITY_LOOK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityLook"),

    /** Vehicle move. */
    PLAY_OUT_VEHICLE_MOVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "VehicleMove"),

    /** Open book. */
    PLAY_OUT_OPEN_BOOK(PacketDirection.OUTBOUND, PacketCategory.PLAY, "OpenBook"),

    /** Open window. */
    PLAY_OUT_OPEN_WINDOW(PacketDirection.OUTBOUND, PacketCategory.PLAY, "OpenWindow"),

    /** Open sign editor. */
    PLAY_OUT_OPEN_SIGN_EDITOR(PacketDirection.OUTBOUND, PacketCategory.PLAY, "OpenSignEditor"),

    /** Ping. */
    PLAY_OUT_PING(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Ping"),

    /** Pong response. */
    PLAY_OUT_PONG_RESPONSE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "PongResponse"),

    /** Craft recipe response. */
    PLAY_OUT_AUTO_RECIPE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "AutoRecipe"),

    /** Player abilities. */
    PLAY_OUT_ABILITIES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Abilities"),

    /** Player chat message. */
    PLAY_OUT_PLAYER_CHAT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "PlayerChat"),

    /** End combat. */
    PLAY_OUT_COMBAT_END(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CombatEnd"),

    /** Enter combat. */
    PLAY_OUT_COMBAT_ENTER(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CombatEnter"),

    /** Combat kill. */
    PLAY_OUT_COMBAT_KILL(PacketDirection.OUTBOUND, PacketCategory.PLAY, "CombatKill"),

    /** Player info remove. */
    PLAY_OUT_PLAYER_INFO_REMOVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "PlayerInfoRemove"),

    /** Player info update. */
    PLAY_OUT_PLAYER_INFO_UPDATE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "PlayerInfoUpdate"),

    /** Face player. */
    PLAY_OUT_LOOK_AT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "LookAt"),

    /** Player position and look. */
    PLAY_OUT_POSITION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Position"),

    /** Unlock recipes. */
    PLAY_OUT_RECIPES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Recipes"),

    /** Destroy entities. */
    PLAY_OUT_ENTITY_DESTROY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityDestroy"),

    /** Remove entity effect. */
    PLAY_OUT_REMOVE_ENTITY_EFFECT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "RemoveEntityEffect"),

    /** Reset score. */
    PLAY_OUT_RESET_SCORE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ResetScore"),

    /** Remove resource pack. */
    PLAY_OUT_RESOURCE_PACK_POP(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ResourcePackPop"),

    /** Add resource pack. */
    PLAY_OUT_RESOURCE_PACK_PUSH(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ResourcePackPush"),

    /** Respawn. */
    PLAY_OUT_RESPAWN(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Respawn"),

    /** Entity head rotation. */
    PLAY_OUT_ENTITY_HEAD_ROTATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityHeadRotation"),

    /** Multi block change. */
    PLAY_OUT_MULTI_BLOCK_CHANGE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "MultiBlockChange"),

    /** Select advancement tab. */
    PLAY_OUT_SELECT_ADVANCEMENT_TAB(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SelectAdvancementTab"),

    /** Server data. */
    PLAY_OUT_SERVER_DATA(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ServerData"),

    /** Action bar. */
    PLAY_OUT_SET_ACTION_BAR_TEXT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetActionBarText"),

    /** Border center. */
    PLAY_OUT_SET_BORDER_CENTER(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetBorderCenter"),

    /** Border lerp size. */
    PLAY_OUT_SET_BORDER_LERP_SIZE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetBorderLerpSize"),

    /** Border size. */
    PLAY_OUT_SET_BORDER_SIZE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetBorderSize"),

    /** Border warning delay. */
    PLAY_OUT_SET_BORDER_WARNING_DELAY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetBorderWarningDelay"),

    /** Border warning distance. */
    PLAY_OUT_SET_BORDER_WARNING_DISTANCE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetBorderWarningDistance"),

    /** Camera. */
    PLAY_OUT_CAMERA(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Camera"),

    /** Held item change. */
    PLAY_OUT_HELD_ITEM_SLOT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "HeldItemSlot"),

    /** Update view center. */
    PLAY_OUT_VIEW_CENTRE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ViewCentre"),

    /** Update view distance. */
    PLAY_OUT_VIEW_DISTANCE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ViewDistance"),

    /** Spawn position. */
    PLAY_OUT_SPAWN_POSITION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SpawnPosition"),

    /** Display scoreboard. */
    PLAY_OUT_SCOREBOARD_DISPLAY_OBJECTIVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ScoreboardDisplayObjective"),

    /** Entity metadata. */
    PLAY_OUT_ENTITY_METADATA(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityMetadata"),

    /** Attach entity. */
    PLAY_OUT_ATTACH_ENTITY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "AttachEntity"),

    /** Entity velocity. */
    PLAY_OUT_ENTITY_VELOCITY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityVelocity"),

    /** Entity equipment. */
    PLAY_OUT_ENTITY_EQUIPMENT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityEquipment"),

    /** Set experience. */
    PLAY_OUT_EXPERIENCE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Experience"),

    /** Update health. */
    PLAY_OUT_UPDATE_HEALTH(PacketDirection.OUTBOUND, PacketCategory.PLAY, "UpdateHealth"),

    /** Scoreboard objective. */
    PLAY_OUT_SCOREBOARD_OBJECTIVE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ScoreboardObjective"),

    /** Set passengers. */
    PLAY_OUT_MOUNT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Mount"),

    /** Teams. */
    PLAY_OUT_SCOREBOARD_TEAM(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ScoreboardTeam"),

    /** Update score. */
    PLAY_OUT_SCOREBOARD_SCORE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "ScoreboardScore"),

    /** Set simulation distance. */
    PLAY_OUT_SIMULATION_DISTANCE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SimulationDistance"),

    /** Set title subtitle. */
    PLAY_OUT_SET_SUBTITLE_TEXT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetSubtitleText"),

    /** Time update. */
    PLAY_OUT_UPDATE_TIME(PacketDirection.OUTBOUND, PacketCategory.PLAY, "UpdateTime"),

    /** Set title text. */
    PLAY_OUT_SET_TITLE_TEXT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetTitleText"),

    /** Set title animation times. */
    PLAY_OUT_SET_TITLES_ANIMATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SetTitlesAnimation"),

    /** Entity sound effect. */
    PLAY_OUT_ENTITY_SOUND(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntitySound"),

    /** Sound effect. */
    PLAY_OUT_NAMED_SOUND_EFFECT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "NamedSoundEffect"),

    /** Start configuration. */
    PLAY_OUT_START_CONFIGURATION(PacketDirection.OUTBOUND, PacketCategory.PLAY, "StartConfiguration"),

    /** Stop sound. */
    PLAY_OUT_STOP_SOUND(PacketDirection.OUTBOUND, PacketCategory.PLAY, "StopSound"),

    /** System chat message. */
    PLAY_OUT_SYSTEM_CHAT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "SystemChat"),

    /** Tab list header footer. */
    PLAY_OUT_PLAYER_LIST_HEADER_FOOTER(PacketDirection.OUTBOUND, PacketCategory.PLAY, "PlayerListHeaderFooter"),

    /** NBT query response. */
    PLAY_OUT_NBT_QUERY(PacketDirection.OUTBOUND, PacketCategory.PLAY, "NBTQuery"),

    /** Collect item. */
    PLAY_OUT_COLLECT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Collect"),

    /** Entity teleport. */
    PLAY_OUT_ENTITY_TELEPORT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityTeleport"),

    /** Tick step. */
    PLAY_OUT_TICKING_STATE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "TickingState"),

    /** Tick step response. */
    PLAY_OUT_TICKING_STEP(PacketDirection.OUTBOUND, PacketCategory.PLAY, "TickingStep"),

    /** Advancements. */
    PLAY_OUT_ADVANCEMENTS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Advancements"),

    /** Entity attributes. */
    PLAY_OUT_UPDATE_ATTRIBUTES(PacketDirection.OUTBOUND, PacketCategory.PLAY, "UpdateAttributes"),

    /** Entity effect. */
    PLAY_OUT_ENTITY_EFFECT(PacketDirection.OUTBOUND, PacketCategory.PLAY, "EntityEffect"),

    /** Update recipes. */
    PLAY_OUT_RECIPE_UPDATE(PacketDirection.OUTBOUND, PacketCategory.PLAY, "RecipeUpdate"),

    /** Update tags. */
    PLAY_OUT_TAGS(PacketDirection.OUTBOUND, PacketCategory.PLAY, "Tags"),

    // ========================
    // LEGACY/SPECIAL
    // ========================

    /** Unknown packet type. */
    UNKNOWN(PacketDirection.UNKNOWN, PacketCategory.UNKNOWN, "Unknown");

    private static final Map<String, PacketType> BY_NAME = new HashMap<>();

    static {
        for (PacketType type : values()) {
            BY_NAME.put(type.name().toLowerCase(), type);
            BY_NAME.put(type.nmsName.toLowerCase(), type);
        }
    }

    private final PacketDirection direction;
    private final PacketCategory category;
    private final String nmsName;

    /**
     * Constructs a packet type.
     *
     * @param direction the packet direction
     * @param category  the packet category
     * @param nmsName   the NMS packet name
     */
    PacketType(@NotNull PacketDirection direction, @NotNull PacketCategory category, @NotNull String nmsName) {
        this.direction = direction;
        this.category = category;
        this.nmsName = nmsName;
    }

    /**
     * Returns the direction of this packet (inbound/outbound).
     *
     * @return the packet direction
     * @since 1.0.0
     */
    @NotNull
    public PacketDirection getDirection() {
        return direction;
    }

    /**
     * Returns the category of this packet.
     *
     * @return the packet category
     * @since 1.0.0
     */
    @NotNull
    public PacketCategory getCategory() {
        return category;
    }

    /**
     * Returns the NMS packet name.
     *
     * @return the NMS name
     * @since 1.0.0
     */
    @NotNull
    public String getNmsName() {
        return nmsName;
    }

    /**
     * Checks if this packet is inbound (from client to server).
     *
     * @return true if inbound
     * @since 1.0.0
     */
    public boolean isInbound() {
        return direction == PacketDirection.INBOUND;
    }

    /**
     * Checks if this packet is outbound (from server to client).
     *
     * @return true if outbound
     * @since 1.0.0
     */
    public boolean isOutbound() {
        return direction == PacketDirection.OUTBOUND;
    }

    /**
     * Checks if this is a play-state packet.
     *
     * @return true if play packet
     * @since 1.0.0
     */
    public boolean isPlayPacket() {
        return category == PacketCategory.PLAY;
    }

    /**
     * Looks up a packet type by name.
     *
     * @param name the packet name
     * @return the packet type, or UNKNOWN if not found
     * @since 1.0.0
     */
    @NotNull
    public static PacketType fromName(@Nullable String name) {
        if (name == null) {
            return UNKNOWN;
        }
        return BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
    }

    /**
     * Looks up a packet type by NMS class name.
     *
     * @param nmsClassName the NMS class name
     * @return the packet type, or UNKNOWN if not found
     * @since 1.0.0
     */
    @NotNull
    public static PacketType fromNmsClass(@Nullable String nmsClassName) {
        if (nmsClassName == null) {
            return UNKNOWN;
        }

        for (PacketType type : values()) {
            if (nmsClassName.contains(type.nmsName)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * Packet direction enumeration.
     *
     * @since 1.0.0
     */
    public enum PacketDirection {
        /** Packet from client to server. */
        INBOUND,
        /** Packet from server to client. */
        OUTBOUND,
        /** Unknown direction. */
        UNKNOWN
    }

    /**
     * Packet category enumeration.
     *
     * @since 1.0.0
     */
    public enum PacketCategory {
        /** Handshake packets. */
        HANDSHAKE,
        /** Server status packets. */
        STATUS,
        /** Login packets. */
        LOGIN,
        /** Configuration packets (1.20.2+). */
        CONFIGURATION,
        /** Play-state packets. */
        PLAY,
        /** Unknown category. */
        UNKNOWN
    }
}
