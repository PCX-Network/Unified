/*
 * Economy Plugin Example
 * Demonstrates the UnifiedPlugin-API economy, database, commands, and GUI features
 */

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.task)
}

group = "sh.pcx.examples"
version = "1.0.0"
description = "Example economy plugin demonstrating UnifiedPlugin-API features"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    // UnifiedPlugin API modules
    compileOnly(project(":unified-api"))
    compileOnly(project(":unified-economy"))
    compileOnly(project(":unified-commands"))
    compileOnly(project(":unified-gui"))
    compileOnly(project(":unified-data"))
    compileOnly(project(":unified-config"))
    compileOnly(project(":unified-modules"))

    // Platform API (Paper)
    compileOnly(libs.paper.api)

    // Adventure for text components
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.text.minimessage)

    // Annotations
    compileOnly(libs.jetbrains.annotations)

    // Database
    implementation(libs.hikaricp)

    // Testing
    testImplementation(libs.bundles.testing)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "sh.pcx.examples.economy.libs.hikari")

        dependencies {
            include(dependency("com.zaxxer:HikariCP"))
        }

        minimize()
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "name" to "EconomyExample",
            "description" to project.description
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.4")
    }
}
