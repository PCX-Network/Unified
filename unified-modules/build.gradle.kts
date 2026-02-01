plugins {
    `java-library`
}

description = "UnifiedPlugin API - Module system for hot-swappable plugin components"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-config"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Utilities
    implementation(libs.commons.io)
}
