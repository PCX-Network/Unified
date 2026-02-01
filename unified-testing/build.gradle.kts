plugins {
    `java-library`
}

description = "UnifiedPlugin API - Mock server and test utilities (Phase 12: Testing Framework)"

dependencies {
    // Internal dependencies - needs access to all APIs for mocking
    api(project(":unified-api"))
    api(project(":unified-platform"))
    api(project(":unified-scheduler"))

    // Adventure API for text components
    api(libs.adventure.api)

    // Testing frameworks
    api(libs.bundles.testing)
    api(libs.mockk)

    // JUnit 5 for test annotations and extensions
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.params)

    // AssertJ for fluent assertions
    api(libs.assertj.core)

    // Jakarta/Javax Inject for @Inject support in tests
    compileOnly(libs.jakarta.inject)
    compileOnly(libs.javax.inject)

    // Platform APIs for mock implementations
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
}
