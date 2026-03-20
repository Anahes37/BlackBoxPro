import java.util.Properties

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "BlackBoxPro-mod"

val sharedProps = Properties().apply {
    file("gradle.properties").reader().use(::load)
}

gradle.beforeProject {
    sharedProps.forEach { key, value ->
        extensions.extraProperties[key.toString()] = value
    }
}

include("common")
include("1.21.11:fabric")
include("1.21.11:neoforge")

project(":1.21.11").projectDir = file("1.21.11")
project(":1.21.11:fabric").projectDir = file("1.21.11/fabric")
project(":1.21.11:neoforge").projectDir = file("1.21.11/neoforge")
