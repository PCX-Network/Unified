plugins {
    `java-library`
}

description = "UnifiedPlugin API - GUI framework for inventory-based interfaces"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-scheduler"))

    // Platform APIs for inventory handling
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
}
