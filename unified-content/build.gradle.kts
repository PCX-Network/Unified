plugins {
    `java-library`
}

description = "UnifiedPlugin API - Enchantments, Loot, Advancements, Recipes, and Resource Packs"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-version"))
    implementation(project(":unified-config"))

    // Platform APIs
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)

    // Serialization
    implementation(libs.gson)

    // Utilities
    implementation(libs.commons.io)
}
