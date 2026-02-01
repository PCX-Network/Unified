plugins {
    `java-library`
}

description = "UnifiedPlugin API - SQL, Redis, MongoDB, and Cache implementations"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))

    // Database drivers and connection pooling
    api(libs.hikaricp)
    api(libs.jedis)
    api(libs.lettuce)
    api(libs.mongodb.driver.sync)
    api(libs.mongodb.driver.reactivestreams)

    // Caching
    api(libs.caffeine)

    // Utilities
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
}
