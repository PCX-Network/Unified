/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.core;

import sh.pcx.unified.UnifiedPlugin;
import sh.pcx.unified.modules.annotation.Command;
import sh.pcx.unified.modules.annotation.Listen;
import sh.pcx.unified.modules.annotation.Module;
import sh.pcx.unified.modules.lifecycle.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Loads modules from classes and packages.
 *
 * <p>The ModuleLoader handles module instantiation, package scanning for
 * auto-discovery, and initial module setup. It supports both explicit
 * registration and automatic discovery via package scanning.
 *
 * <h2>Loading Methods</h2>
 * <ul>
 *   <li><b>Explicit</b>: Register specific classes with {@link #load(Class)}</li>
 *   <li><b>Package Scan</b>: Discover modules with {@link #scanPackage(String)}</li>
 * </ul>
 *
 * <h2>Loading Process</h2>
 * <ol>
 *   <li>Validate class has @Module annotation</li>
 *   <li>Instantiate module using constructor injection</li>
 *   <li>Register module with the registry</li>
 *   <li>Return the loaded module for further initialization</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ModuleLoader loader = new ModuleLoader(plugin, registry);
 *
 * // Load specific module
 * Optional<Object> economy = loader.load(EconomyModule.class);
 *
 * // Scan package for modules
 * List<Class<?>> discovered = loader.scanPackage("com.example.plugin.modules");
 * for (Class<?> moduleClass : discovered) {
 *     loader.load(moduleClass);
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see ModuleRegistry
 * @see ModuleManager
 */
public final class ModuleLoader {

    private final UnifiedPlugin plugin;
    private final ModuleRegistry registry;
    private final Logger logger;

    /**
     * Creates a new module loader.
     *
     * @param plugin   the owning plugin
     * @param registry the module registry
     */
    public ModuleLoader(@NotNull UnifiedPlugin plugin, @NotNull ModuleRegistry registry) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.logger = plugin.getLogger();
    }

    /**
     * Loads a module from its class.
     *
     * @param moduleClass the class annotated with @Module
     * @return an Optional containing the loaded instance, empty if loading failed
     */
    @NotNull
    public Optional<Object> load(@NotNull Class<?> moduleClass) {
        Objects.requireNonNull(moduleClass, "Module class cannot be null");

        Module annotation = moduleClass.getAnnotation(Module.class);
        if (annotation == null) {
            logger.warning("Class " + moduleClass.getName() + " is not annotated with @Module");
            return Optional.empty();
        }

        String name = annotation.name();
        if (registry.contains(name)) {
            logger.warning("Module '" + name + "' is already registered");
            return Optional.empty();
        }

        try {
            long startTime = System.currentTimeMillis();

            // Create instance
            Object instance = instantiate(moduleClass);

            // Register with registry
            registry.register(name, instance, moduleClass);
            registry.setState(name, ModuleState.LOADING);

            long loadTime = System.currentTimeMillis() - startTime;
            registry.getEntry(name).ifPresent(entry -> entry.setLoadTime(loadTime));

            logger.info("Loaded module: " + name + " (" + loadTime + "ms)");
            return Optional.of(instance);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load module: " + name, e);
            registry.setState(name, ModuleState.FAILED);
            registry.setError(name, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Instantiates a module class using its constructor.
     *
     * @param moduleClass the class to instantiate
     * @return the new instance
     * @throws Exception if instantiation fails
     */
    @NotNull
    private Object instantiate(@NotNull Class<?> moduleClass) throws Exception {
        // Try default constructor first
        try {
            Constructor<?> defaultConstructor = moduleClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            return defaultConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Try constructor with UnifiedPlugin
            try {
                Constructor<?> pluginConstructor = moduleClass.getDeclaredConstructor(UnifiedPlugin.class);
                pluginConstructor.setAccessible(true);
                return pluginConstructor.newInstance(plugin);
            } catch (NoSuchMethodException e2) {
                throw new IllegalStateException(
                        "Module " + moduleClass.getName() + " must have a no-arg constructor or constructor accepting UnifiedPlugin"
                );
            }
        }
    }

    /**
     * Scans a package for classes annotated with @Module.
     *
     * @param packageName the package to scan
     * @return a list of discovered module classes
     */
    @NotNull
    public List<Class<?>> scanPackage(@NotNull String packageName) {
        Objects.requireNonNull(packageName, "Package name cannot be null");

        List<Class<?>> modules = new ArrayList<>();

        try {
            ClassLoader classLoader = plugin.getClass().getClassLoader();
            String path = packageName.replace('.', '/');

            // Get the JAR file or directory containing the classes
            URL resource = classLoader.getResource(path);
            if (resource == null) {
                logger.warning("Package not found: " + packageName);
                return modules;
            }

            String protocol = resource.getProtocol();
            if ("file".equals(protocol)) {
                // Scanning from filesystem (development)
                modules.addAll(scanDirectory(new File(resource.toURI()), packageName));
            } else if ("jar".equals(protocol)) {
                // Scanning from JAR file
                modules.addAll(scanJar(resource, path, classLoader));
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error scanning package: " + packageName, e);
        }

        logger.info("Discovered " + modules.size() + " modules in package " + packageName);
        return modules;
    }

    /**
     * Scans a directory for module classes.
     */
    private List<Class<?>> scanDirectory(File directory, String packageName) {
        List<Class<?>> modules = new ArrayList<>();

        if (!directory.exists()) {
            return modules;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return modules;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                modules.addAll(scanDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Module.class)) {
                        modules.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Skip classes that can't be loaded
                }
            }
        }

        return modules;
    }

    /**
     * Scans a JAR file for module classes.
     */
    private List<Class<?>> scanJar(URL jarUrl, String packagePath, ClassLoader classLoader) {
        List<Class<?>> modules = new ArrayList<>();

        String jarPath = jarUrl.getPath();
        // Extract JAR path from URL like "file:/path/to/file.jar!/package/path"
        int jarEnd = jarPath.indexOf("!");
        if (jarEnd > 0) {
            jarPath = jarPath.substring(5, jarEnd); // Remove "file:" prefix
        }

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (clazz.isAnnotationPresent(Module.class)) {
                            modules.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Skip classes that can't be loaded
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading JAR file: " + jarPath, e);
        }

        return modules;
    }

    /**
     * Checks if a module class has the @Listen annotation.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class has @Listen
     */
    public boolean hasListenAnnotation(@NotNull Class<?> moduleClass) {
        return moduleClass.isAnnotationPresent(Listen.class);
    }

    /**
     * Checks if a module class has the @Command annotation.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class has @Command
     */
    public boolean hasCommandAnnotation(@NotNull Class<?> moduleClass) {
        return moduleClass.isAnnotationPresent(Command.class);
    }

    /**
     * Checks if a module class implements Initializable.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class implements Initializable
     */
    public boolean isInitializable(@NotNull Class<?> moduleClass) {
        return Initializable.class.isAssignableFrom(moduleClass);
    }

    /**
     * Checks if a module class implements Reloadable.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class implements Reloadable
     */
    public boolean isReloadable(@NotNull Class<?> moduleClass) {
        return Reloadable.class.isAssignableFrom(moduleClass);
    }

    /**
     * Checks if a module class implements Healthy.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class implements Healthy
     */
    public boolean isHealthy(@NotNull Class<?> moduleClass) {
        return Healthy.class.isAssignableFrom(moduleClass);
    }

    /**
     * Checks if a module class implements Disableable.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class implements Disableable
     */
    public boolean isDisableable(@NotNull Class<?> moduleClass) {
        return Disableable.class.isAssignableFrom(moduleClass);
    }

    /**
     * Checks if a module class implements Schedulable.
     *
     * @param moduleClass the module class
     * @return {@code true} if the class implements Schedulable
     */
    public boolean isSchedulable(@NotNull Class<?> moduleClass) {
        return Schedulable.class.isAssignableFrom(moduleClass);
    }

    /**
     * Gets the @Module annotation from a class.
     *
     * @param moduleClass the class
     * @return an Optional containing the annotation if present
     */
    @NotNull
    public Optional<Module> getModuleAnnotation(@NotNull Class<?> moduleClass) {
        return Optional.ofNullable(moduleClass.getAnnotation(Module.class));
    }

    /**
     * Gets the @Listen annotation from a class.
     *
     * @param moduleClass the class
     * @return an Optional containing the annotation if present
     */
    @NotNull
    public Optional<Listen> getListenAnnotation(@NotNull Class<?> moduleClass) {
        return Optional.ofNullable(moduleClass.getAnnotation(Listen.class));
    }

    /**
     * Gets the @Command annotation from a class.
     *
     * @param moduleClass the class
     * @return an Optional containing the annotation if present
     */
    @NotNull
    public Optional<Command> getCommandAnnotation(@NotNull Class<?> moduleClass) {
        return Optional.ofNullable(moduleClass.getAnnotation(Command.class));
    }
}
