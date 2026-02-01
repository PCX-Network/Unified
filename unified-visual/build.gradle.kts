plugins {
    `java-library`
}

description = "UnifiedPlugin API - Holograms, Scoreboards, Bossbars, Titles, Particles, and Sounds"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-scheduler"))

    // Guice for dependency injection
    implementation(libs.guice)

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // jspecify for null annotations
    compileOnly("org.jspecify:jspecify:1.0.0")
}
