plugins {
    `java-library`
}

description = "UnifiedPlugin API - World generation, Regions, and Conditions"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-data"))
    implementation(project(":unified-version"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Utilities
    implementation(libs.commons.lang3)
}
