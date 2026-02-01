plugins {
    `java-library`
}

description = "UnifiedPlugin API - Public interfaces for plugin development"

dependencies {
    // Core frameworks - API only, provided at runtime
    api(libs.guice)
    api(libs.bundles.adventure)
    api(libs.bundles.configurate)
    api(libs.caffeine)
    api(libs.slf4j.api)
    api(libs.gson)

    // Annotations
    compileOnlyApi(libs.jetbrains.annotations)
    compileOnlyApi(libs.checker.qual)
    compileOnlyApi("org.jspecify:jspecify:1.0.0")

    // Platform APIs - compile only for interface definitions
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
}
