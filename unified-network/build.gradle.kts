plugins {
    `java-library`
}

description = "UnifiedPlugin API - Cross-server messaging, packets, and entity AI"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))
    implementation(project(":unified-version"))
    implementation(project(":unified-data"))

    // Platform APIs (for channel adapters)
    compileOnly(libs.paper.api)
    compileOnly(libs.sponge.api)
    compileOnly(libs.velocity.api)

    // ProtocolLib (optional - for packet handling)
    compileOnly(libs.protocollib)

    // Messaging via Redis
    implementation(libs.jedis)
    implementation(libs.lettuce)

    // Serialization
    implementation(libs.gson)

    // Logging
    implementation(libs.slf4j.api)

    // Adventure components (for tab list, fake entities)
    api(libs.adventure.api)
}
