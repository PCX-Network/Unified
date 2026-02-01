plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.task)
}

description = "UnifiedPlugin API - Core runtime implementation and main deployable plugin JAR"

dependencies {
    // All internal modules - unified-core bundles everything
    api(project(":unified-api"))
    api(project(":unified-platform"))
    api(project(":unified-version"))
    api(project(":unified-data"))
    api(project(":unified-gui"))
    api(project(":unified-commands"))
    api(project(":unified-config"))
    api(project(":unified-scheduler"))
    api(project(":unified-modules"))
    api(project(":unified-economy"))
    api(project(":unified-i18n"))
    api(project(":unified-visual"))
    api(project(":unified-network"))
    api(project(":unified-world"))
    api(project(":unified-content"))

    // Core frameworks - bundled in fat JAR
    implementation(libs.bundles.guice)
    implementation(libs.bundles.adventure)
    implementation(libs.bundles.configurate)
    implementation(libs.bundles.database)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.slf4j.api)
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)

    // Platform APIs - provided at runtime
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("UnifiedPluginAPI")

    // Relocate dependencies to avoid conflicts
    relocate("com.google.inject", "sh.pcx.libs.guice")
    relocate("org.spongepowered.configurate", "sh.pcx.libs.configurate")
    relocate("com.zaxxer.hikari", "sh.pcx.libs.hikari")
    relocate("com.github.benmanes.caffeine", "sh.pcx.libs.caffeine")
    relocate("redis.clients.jedis", "sh.pcx.libs.jedis")
    relocate("io.lettuce", "sh.pcx.libs.lettuce")
    relocate("com.mongodb", "sh.pcx.libs.mongodb")
    relocate("org.bson", "sh.pcx.libs.bson")
    relocate("com.google.gson", "sh.pcx.libs.gson")
    relocate("org.apache.commons", "sh.pcx.libs.commons")

    // Don't relocate Adventure - Paper provides it
    // Don't relocate SLF4J - Paper provides it

    // Exclude unnecessary files
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/MANIFEST.MF")

    // Merge service files
    mergeServiceFiles()

    manifest {
        attributes(
            "Implementation-Title" to "UnifiedPlugin API",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Supatuck",
            "Main-Class" to "sh.pcx.unified.core.UnifiedPluginAPI",
            "Multi-Release" to "true"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("slim")
}

// Configure run-paper for development testing
tasks.runServer {
    minecraftVersion("1.21.4")
    jvmArgs("-Xms2G", "-Xmx2G")
}

// Process resources for plugin descriptors
tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "name" to "UnifiedPluginAPI",
        "main" to "sh.pcx.unified.core.UnifiedPluginAPI",
        "apiVersion" to "1.21"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml", "META-INF/sponge_plugins.json")) {
        expand(props)
    }
}
