import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.util.Properties

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.shadow) apply false
}

val projectVersion: String by project
val projectGroup: String by project

// Load local.properties if it exists (for local development)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun getReposiliteUsername(): String {
    return System.getenv("REPOSILITE_USERNAME")
        ?: localProperties.getProperty("reposilite.username", "")
}

fun getReposiliteToken(): String {
    return System.getenv("REPOSILITE_TOKEN")
        ?: localProperties.getProperty("reposilite.token", "")
}

allprojects {
    group = projectGroup
    version = projectVersion

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://libraries.minecraft.net/")
        maven("https://repo.dmulloy2.net/repository/public/") // ProtocolLib
        maven("https://jitpack.io") // VaultAPI and other GitHub packages
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf(
            "-parameters",
            "--enable-preview",
            "-Xlint:all",
            "-Xlint:-processing",
            "-Xlint:-serial",
            "-Xlint:-preview"
        ))
    }

    tasks.withType<Javadoc> {
        options {
            encoding = "UTF-8"
            (this as StandardJavadocDocletOptions).apply {
                addBooleanOption("html5", true)
                addStringOption("Xdoclint:none", "-quiet")
                links(
                    "https://docs.oracle.com/en/java/javase/21/docs/api/",
                    "https://jd.papermc.io/paper/1.21.4/",
                    "https://jd.advntr.dev/api/4.26.1/"
                )
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        maxHeapSize = "2g"
    }

    tasks.withType<Jar> {
        archiveBaseName.set(project.name)
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Supatuck",
                "Built-By" to System.getProperty("user.name"),
                "Built-JDK" to System.getProperty("java.version"),
                "Built-Gradle" to gradle.gradleVersion
            )
        }
    }

    // Common dependencies for all subprojects
    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations)
        compileOnly(rootProject.libs.checker.qual)

        testImplementation(rootProject.libs.bundles.testing)
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("UnifiedPlugin API - ${project.name}")
                    url.set("https://github.com/PCX-Network/Unified")
                    inceptionYear.set("2025")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("supatuck")
                            name.set("Supatuck")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/PCX-Network/Unified.git")
                        developerConnection.set("scm:git:ssh://github.com/PCX-Network/Unified.git")
                        url.set("https://github.com/PCX-Network/Unified")
                    }

                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/PCX-Network/Unified/issues")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/PCX-Network/Unified")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                    password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
                }
            }

            maven {
                name = "OSSRH"
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrh.username") as String? ?: ""
                    password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrh.password") as String? ?: ""
                }
            }

            maven {
                name = "Reposilite"
                val releasesRepoUrl = uri("https://repo.pcx.sh/releases")
                val snapshotsRepoUrl = uri("https://repo.pcx.sh/snapshots")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = getReposiliteUsername()
                    password = getReposiliteToken()
                }
            }
        }
    }

    signing {
        val signingKey = System.getenv("GPG_SIGNING_KEY") ?: project.findProperty("signing.key") as String?
        val signingPassword = System.getenv("GPG_SIGNING_PASSWORD") ?: project.findProperty("signing.password") as String?

        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["maven"])
        }
    }
}

// Root project tasks
tasks.register("cleanAll") {
    description = "Cleans all projects"
    group = "build"
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

tasks.register("buildAll") {
    description = "Builds all projects"
    group = "build"
    dependsOn(subprojects.map { it.tasks.named("build") })
}

tasks.register("publishAll") {
    description = "Publishes all projects"
    group = "publishing"
    dependsOn(subprojects.map { it.tasks.named("publish") })
}

tasks.register("testAll") {
    description = "Runs tests for all projects"
    group = "verification"
    dependsOn(subprojects.map { it.tasks.named("test") })
}

// Aggregate Javadoc from all subprojects
tasks.register<Javadoc>("aggregateJavadoc") {
    description = "Generates aggregated Javadoc for all modules"
    group = "documentation"

    val javadocTasks = subprojects.mapNotNull { subproject ->
        subproject.tasks.findByName("javadoc") as? Javadoc
    }

    dependsOn(javadocTasks)

    source = files(javadocTasks.map { it.source }).asFileTree
    classpath = files(javadocTasks.map { it.classpath })

    destinationDir = file("${buildDir}/docs/javadoc")

    options {
        encoding = "UTF-8"
        (this as StandardJavadocDocletOptions).apply {
            addBooleanOption("html5", true)
            addStringOption("Xdoclint:none", "-quiet")
            windowTitle = "Unified API Documentation"
            docTitle = "Unified API Documentation"
            header = "<b>Unified API</b>"
            bottom = "Copyright &copy; 2025 Supatuck. All rights reserved."
            links(
                "https://docs.oracle.com/en/java/javase/21/docs/api/",
                "https://jd.papermc.io/paper/1.21.4/",
                "https://jd.advntr.dev/api/4.26.1/"
            )
        }
    }
}
