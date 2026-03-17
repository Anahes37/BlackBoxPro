pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "BlackBoxPro"

include("fabric-1.21.11")
include("neoforge-1.21.11")
