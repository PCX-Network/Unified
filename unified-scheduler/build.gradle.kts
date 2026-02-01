plugins {
    `java-library`
}

description = "UnifiedPlugin API - Scheduler system with Folia region-aware support"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))

    // Platform APIs for scheduler access
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
}
