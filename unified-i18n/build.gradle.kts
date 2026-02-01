plugins {
    `java-library`
}

description = "UnifiedPlugin API - Localization, Placeholders, and Permissions system"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-config"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Utilities
    implementation(libs.commons.lang3)
}
