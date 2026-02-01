plugins {
    `java-library`
    `java-gradle-plugin`
    application
}

description = "UnifiedPlugin API - Gradle plugin and CLI tools"

application {
    mainClass.set("sh.pcx.unified.tools.cli.UnifiedCli")
}

gradlePlugin {
    plugins {
        create("unifiedPlugin") {
            id = "sh.pcx.unified"
            implementationClass = "sh.pcx.unified.tools.gradle.UnifiedGradlePlugin"
            displayName = "UnifiedPlugin API Gradle Plugin"
            description = "Gradle plugin for building plugins with the UnifiedPlugin API"
        }
    }
}

dependencies {
    // Internal dependencies
    implementation(project(":unified-api"))

    // Gradle API
    compileOnly(gradleApi())

    // CLI framework
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    // File utilities
    implementation(libs.commons.io)

    // Serialization
    implementation(libs.gson)

    // Template engine
    implementation("org.freemarker:freemarker:2.3.33")

    // SLF4J for logging
    implementation(libs.slf4j.api)

    // Logging implementation for CLI
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
}
