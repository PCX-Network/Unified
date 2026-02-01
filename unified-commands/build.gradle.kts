plugins {
    `java-library`
}

description = "UnifiedPlugin API - Command framework with annotation-based registration"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-i18n"))

    // Platform APIs for command handling
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Utilities
    implementation(libs.commons.lang3)
}
