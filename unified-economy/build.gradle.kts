plugins {
    `java-library`
}

description = "UnifiedPlugin API - Economy API with multi-currency support"

repositories {
    maven("https://jitpack.io")
}

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-data"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Vault API for compatibility bridge
    compileOnly(libs.vault.api)
}
