pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "unified-plugin-api"

include("unified-api")
include("unified-core")
include("unified-platform")
include("unified-version")
include("unified-data")
include("unified-gui")
include("unified-commands")
include("unified-config")
include("unified-scheduler")
include("unified-modules")
include("unified-economy")
include("unified-i18n")
include("unified-visual")
include("unified-network")
include("unified-world")
include("unified-content")
include("unified-testing")
include("unified-tools")

// Example plugins
include("examples:basic-plugin")
