plugins {
    `java-library`
}

description = "UnifiedPlugin API - Platform adapters for Paper, Folia, and Sponge"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Utilities
    implementation(libs.commons.lang3)
}
