/*
 * Minigame Plugin Example - UnifiedPlugin API
 * Copyright (c) 2025 Supatuck
 * Licensed under the MIT License
 *
 * This example demonstrates a complete minigame plugin using the UnifiedPlugin API framework,
 * showcasing regions, scheduler, scoreboards, boss bars, holograms, and player data.
 */

plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
    // The unified-plugin gradle plugin provides automatic setup
    // In production, you would use: id("sh.pcx.unified-plugin") version "1.0.0"
}

group = "sh.pcx.example"
version = "1.0.0"
description = "A minigame plugin example demonstrating the UnifiedPlugin API framework"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven/")
    // In production, you would add the Supatuck repository:
    // maven("https://maven.pcxnetwork.net/releases/")
}

dependencies {
    // UnifiedPlugin API - compileOnly since it's provided at runtime
    // In production: compileOnly("sh.pcx:unified-api:1.0.0")
    compileOnly(project(":unified-api"))
    compileOnly(project(":unified-modules"))
    compileOnly(project(":unified-commands"))
    compileOnly(project(":unified-config"))
    compileOnly(project(":unified-gui"))
    compileOnly(project(":unified-scheduler"))
    compileOnly(project(":unified-visual"))
    compileOnly(project(":unified-data"))
    compileOnly(project(":unified-world"))

    // Paper API - for platform-specific features if needed
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // Adventure API - for text components
    compileOnly("net.kyori:adventure-api:4.18.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.18.0")

    // Configurate - for configuration (provided by UnifiedPluginAPI at runtime)
    compileOnly("org.spongepowered:configurate-yaml:4.1.2")

    // Guice for dependency injection
    compileOnly("com.google.inject:guice:7.0.0")

    // Annotations
    compileOnly("org.jetbrains:annotations:26.0.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf(
        "-parameters",  // Required for command framework parameter names
        "-Xlint:all",
        "-Xlint:-processing",
        "-Xlint:-serial"
    ))
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("MinigamePlugin-${project.version}.jar")

    // Minimize to reduce jar size
    minimize()

    // Relocate any shaded dependencies if needed
    // relocate("some.package", "sh.pcx.example.lib.some.package")
}

tasks.test {
    useJUnitPlatform()
}

// Process resources to replace tokens in plugin.yml
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "name" to "MinigamePlugin",
            "version" to project.version,
            "description" to project.description,
            "main" to "sh.pcx.example.minigame.MinigamePlugin"
        )
    }
}

// Build task depends on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}
