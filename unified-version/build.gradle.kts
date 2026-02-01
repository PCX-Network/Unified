plugins {
    `java-library`
}

description = "UnifiedPlugin API - NMS version compatibility layer (1.20.5 - 1.21.11)"

dependencies {
    // Internal dependencies
    api(project(":unified-api"))

    // Platform APIs for NMS access
    compileOnly(libs.paper.api)

    // Utilities
    implementation(libs.commons.lang3)
}

// Version-specific source sets for NMS implementations
sourceSets {
    create("v1_20_R4") {
        java.srcDir("src/v1_20_R4/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
    create("v1_21_R1") {
        java.srcDir("src/v1_21_R1/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
    create("v1_21_R2") {
        java.srcDir("src/v1_21_R2/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
    create("v1_21_R3") {
        java.srcDir("src/v1_21_R3/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
    create("v1_21_R4") {
        java.srcDir("src/v1_21_R4/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
}

// Ensure version-specific compilations run after main
tasks.named("compileV1_20_R4Java") { dependsOn("compileJava") }
tasks.named("compileV1_21_R1Java") { dependsOn("compileJava") }
tasks.named("compileV1_21_R2Java") { dependsOn("compileJava") }
tasks.named("compileV1_21_R3Java") { dependsOn("compileJava") }
tasks.named("compileV1_21_R4Java") { dependsOn("compileJava") }

tasks.jar {
    from(sourceSets["v1_20_R4"].output)
    from(sourceSets["v1_21_R1"].output)
    from(sourceSets["v1_21_R2"].output)
    from(sourceSets["v1_21_R3"].output)
    from(sourceSets["v1_21_R4"].output)
}
