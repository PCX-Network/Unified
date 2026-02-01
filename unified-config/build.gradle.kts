plugins {
    `java-library`
}

description = "UnifiedPlugin API - Configuration system with hot-reload support"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))

    // Configuration libraries
    api(libs.bundles.configurate)

    // Serialization
    implementation(libs.gson)

    // Utilities
    implementation(libs.commons.io)
}
