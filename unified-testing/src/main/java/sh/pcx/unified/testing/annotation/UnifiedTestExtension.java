/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.annotation;

import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.testing.player.MockPlayer;
import sh.pcx.unified.testing.server.MockServer;
import sh.pcx.unified.testing.server.MockServerConfiguration;
import sh.pcx.unified.testing.world.MockWorld;
import sh.pcx.unified.world.UnifiedWorld;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit 5 extension for UnifiedPlugin testing.
 *
 * <p>This extension provides automatic MockServer lifecycle management,
 * dependency injection, and test resource creation based on annotations.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Automatic MockServer start/stop</li>
 *   <li>Plugin loading via {@link UnifiedTest#plugins()}</li>
 *   <li>Server reset between tests via {@link UnifiedTest#resetBetweenTests()}</li>
 *   <li>Field injection for MockServer and plugins</li>
 *   <li>Parameter resolution for test methods</li>
 *   <li>Automatic player creation via {@link WithPlayer}</li>
 *   <li>Automatic world creation via {@link WithWorld}</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class UnifiedTestExtension implements
    BeforeAllCallback,
    AfterAllCallback,
    BeforeEachCallback,
    AfterEachCallback,
    ParameterResolver,
    TestInstancePostProcessor {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(UnifiedTestExtension.class);

    private static final String SERVER_KEY = "mockServer";
    private static final String PLAYERS_KEY = "testPlayers";
    private static final String WORLDS_KEY = "testWorlds";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Create and start MockServer
        MockServerConfiguration config = buildConfiguration(context);
        MockServer server = MockServer.start(config);

        // Store in context
        getStore(context).put(SERVER_KEY, server);

        // Load plugins if specified
        UnifiedTest unifiedTest = context.getRequiredTestClass().getAnnotation(UnifiedTest.class);
        if (unifiedTest != null) {
            for (Class<?> pluginClass : unifiedTest.plugins()) {
                if (UnifiedPlugin.class.isAssignableFrom(pluginClass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends UnifiedPlugin> plugin = (Class<? extends UnifiedPlugin>) pluginClass;
                    server.loadPlugin(plugin);
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        MockServer server = getServer(context);
        if (server != null) {
            server.stop();
            getStore(context).remove(SERVER_KEY);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        MockServer server = getServer(context);
        if (server == null) {
            return;
        }

        // Reset server if configured
        UnifiedTest unifiedTest = context.getRequiredTestClass().getAnnotation(UnifiedTest.class);
        if (unifiedTest != null && unifiedTest.resetBetweenTests()) {
            server.reset();
        }

        // Create players from annotations
        List<MockPlayer> players = createPlayersFromAnnotations(context, server);
        getStore(context).put(PLAYERS_KEY, players);

        // Create worlds from annotations
        List<MockWorld> worlds = createWorldsFromAnnotations(context, server);
        getStore(context).put(WORLDS_KEY, worlds);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Cleanup test resources if needed
        getStore(context).remove(PLAYERS_KEY);
        getStore(context).remove(WORLDS_KEY);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        MockServer server = getServer(context);
        if (server == null) {
            return;
        }

        // Inject fields
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.inject.Inject.class) ||
                field.isAnnotationPresent(javax.inject.Inject.class)) {

                field.setAccessible(true);

                if (MockServer.class.isAssignableFrom(field.getType())) {
                    field.set(testInstance, server);
                } else if (UnifiedPlugin.class.isAssignableFrom(field.getType())) {
                    @SuppressWarnings("unchecked")
                    Class<? extends UnifiedPlugin> pluginClass =
                        (Class<? extends UnifiedPlugin>) field.getType();
                    server.getPlugin(pluginClass).ifPresent(plugin -> {
                        try {
                            field.set(testInstance, plugin);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Failed to inject plugin", e);
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean supportsParameter(
        ParameterContext parameterContext,
        ExtensionContext extensionContext
    ) {
        Class<?> type = parameterContext.getParameter().getType();
        return MockServer.class.isAssignableFrom(type) ||
               MockPlayer.class.isAssignableFrom(type) ||
               MockWorld.class.isAssignableFrom(type) ||
               UnifiedPlayer.class.isAssignableFrom(type) ||
               UnifiedWorld.class.isAssignableFrom(type) ||
               UnifiedPlugin.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(
        ParameterContext parameterContext,
        ExtensionContext extensionContext
    ) {
        MockServer server = getServer(extensionContext);
        if (server == null) {
            throw new ParameterResolutionException("MockServer not available");
        }

        Parameter parameter = parameterContext.getParameter();
        Class<?> type = parameter.getType();

        // MockServer
        if (MockServer.class.isAssignableFrom(type)) {
            return server;
        }

        // MockPlayer / UnifiedPlayer
        if (MockPlayer.class.isAssignableFrom(type) || UnifiedPlayer.class.isAssignableFrom(type)) {
            return resolvePlayer(parameter, extensionContext, server);
        }

        // MockWorld / UnifiedWorld
        if (MockWorld.class.isAssignableFrom(type) || UnifiedWorld.class.isAssignableFrom(type)) {
            return resolveWorld(parameter, extensionContext, server);
        }

        // UnifiedPlugin
        if (UnifiedPlugin.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends UnifiedPlugin> pluginClass = (Class<? extends UnifiedPlugin>) type;
            return server.getPlugin(pluginClass)
                .orElseThrow(() -> new ParameterResolutionException(
                    "Plugin not loaded: " + type.getSimpleName()));
        }

        throw new ParameterResolutionException("Cannot resolve parameter: " + type);
    }

    private MockPlayer resolvePlayer(Parameter parameter, ExtensionContext context, MockServer server) {
        // Check for @WithPlayer on parameter
        WithPlayer withPlayer = parameter.getAnnotation(WithPlayer.class);
        if (withPlayer != null) {
            return createPlayer(withPlayer, parameter, server);
        }

        // Check for @WithPlayer on method
        Method method = context.getRequiredTestMethod();
        WithPlayer[] annotations = method.getAnnotationsByType(WithPlayer.class);
        if (annotations.length > 0) {
            // Use parameter name or first annotation
            String paramName = parameter.getName();
            for (WithPlayer wp : annotations) {
                String name = wp.value().isEmpty() ? wp.name() : wp.value();
                if (name.equalsIgnoreCase(paramName)) {
                    return createPlayer(wp, parameter, server);
                }
            }
            // Default to first
            return createPlayer(annotations[0], parameter, server);
        }

        // Check stored players
        @SuppressWarnings("unchecked")
        List<MockPlayer> players = (List<MockPlayer>) getStore(context).get(PLAYERS_KEY);
        if (players != null && !players.isEmpty()) {
            return players.getFirst();
        }

        throw new ParameterResolutionException(
            "No @WithPlayer annotation found for parameter: " + parameter.getName());
    }

    private MockWorld resolveWorld(Parameter parameter, ExtensionContext context, MockServer server) {
        // Check for @WithWorld on method
        Method method = context.getRequiredTestMethod();
        WithWorld withWorld = method.getAnnotation(WithWorld.class);
        if (withWorld != null) {
            return createWorld(withWorld, server);
        }

        // Return default world
        return (MockWorld) server.getDefaultWorld();
    }

    private MockPlayer createPlayer(WithPlayer annotation, Parameter parameter, MockServer server) {
        String name = annotation.value().isEmpty() ? annotation.name() : annotation.value();
        if (name.isEmpty()) {
            name = parameter.getName();
            if (name.startsWith("arg")) {
                name = "Player" + System.currentTimeMillis();
            }
        }

        MockPlayer player = server.addPlayer(name);
        player.setOp(annotation.op());
        player.setGameMode(annotation.gameMode());
        player.setHealth(annotation.health());
        player.setFoodLevel(annotation.foodLevel());

        // Apply permissions from method annotations
        try {
            Method method = (Method) parameter.getDeclaringExecutable();
            WithPermission[] permissions = method.getAnnotationsByType(WithPermission.class);
            for (WithPermission perm : permissions) {
                player.addPermission(perm.value());
            }
        } catch (Exception ignored) {}

        return player;
    }

    private MockWorld createWorld(WithWorld annotation, MockServer server) {
        UnifiedWorld.Environment environment = switch (annotation.type()) {
            case NORMAL, FLAT, VOID -> UnifiedWorld.Environment.NORMAL;
            case NETHER -> UnifiedWorld.Environment.NETHER;
            case THE_END -> UnifiedWorld.Environment.THE_END;
        };

        MockWorld world;
        try {
            world = server.createWorld(annotation.name(), environment);
        } catch (IllegalArgumentException e) {
            // World already exists
            world = server.getMockWorld(annotation.name());
        }

        if (annotation.seed() != 0) {
            world.setSeed(annotation.seed());
        }
        world.setSpawnLocation(annotation.spawnX(), annotation.spawnY(), annotation.spawnZ());

        return world;
    }

    private List<MockPlayer> createPlayersFromAnnotations(ExtensionContext context, MockServer server) {
        List<MockPlayer> players = new ArrayList<>();

        Method method = context.getRequiredTestMethod();
        WithPlayer[] annotations = method.getAnnotationsByType(WithPlayer.class);

        for (WithPlayer annotation : annotations) {
            String name = annotation.value().isEmpty() ? annotation.name() : annotation.value();
            if (!name.isEmpty()) {
                MockPlayer player = server.addPlayer(name);
                player.setOp(annotation.op());
                player.setGameMode(annotation.gameMode());
                player.setHealth(annotation.health());
                player.setFoodLevel(annotation.foodLevel());

                WithPermission[] permissions = method.getAnnotationsByType(WithPermission.class);
                for (WithPermission perm : permissions) {
                    player.addPermission(perm.value());
                }

                players.add(player);
            }
        }

        return players;
    }

    private List<MockWorld> createWorldsFromAnnotations(ExtensionContext context, MockServer server) {
        List<MockWorld> worlds = new ArrayList<>();

        Method method = context.getRequiredTestMethod();
        WithWorld annotation = method.getAnnotation(WithWorld.class);

        if (annotation != null) {
            worlds.add(createWorld(annotation, server));
        }

        return worlds;
    }

    private MockServerConfiguration buildConfiguration(ExtensionContext context) {
        MockServerConfiguration.Builder builder = MockServerConfiguration.builder();

        // Check for @ServerTest
        ServerTest serverTest = context.getRequiredTestClass().getAnnotation(ServerTest.class);
        if (serverTest != null) {
            String[] versionParts = serverTest.minecraftVersion().split("\\.");
            if (versionParts.length >= 2) {
                int major = Integer.parseInt(versionParts[0]);
                int minor = Integer.parseInt(versionParts[1]);
                int patch = versionParts.length >= 3 ? Integer.parseInt(versionParts[2]) : 0;
                builder.minecraftVersion(major, minor, patch);
            }
            builder.maxPlayers(serverTest.maxPlayers());
            builder.onlineMode(serverTest.onlineMode());
        }

        return builder.build();
    }

    private MockServer getServer(ExtensionContext context) {
        return getStore(context).get(SERVER_KEY, MockServer.class);
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
