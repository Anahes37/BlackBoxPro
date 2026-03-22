import java.util.Properties

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "BlackBoxPro-mod"

val sharedProps = Properties().apply {
    file("gradle.properties").reader().use(::load)
}

// 版本特定属性：每个子版本可有自己的 gradle.properties 覆盖共享属性
val versionProps = mutableMapOf<String, Properties>()
listOf("1.21.1").forEach { ver ->
    val propsFile = file("/gradle.properties")
    if (propsFile.exists()) {
        val p = Properties().apply { propsFile.reader().use(::load) }
        listOf(":", "::runtime", "::fabric", "::neoforge").forEach { path ->
            versionProps[path] = p
        }
    }
}

gradle.beforeProject {
    sharedProps.forEach { key, value ->
        extensions.extraProperties[key.toString()] = value
    }
    versionProps[path]?.forEach { key, value ->
        extensions.extraProperties[key.toString()] = value
    }
}

include("common")
include("1.21.11:runtime")
include("1.21.11:fabric")
include("1.21.11:neoforge")
include("1.21.1:runtime")
include("1.21.1:fabric")

project(":common").projectDir = file("../common")
project(":1.21.11").projectDir = file("1.21.11")
project(":1.21.11:runtime").projectDir = file("1.21.11/runtime")
project(":1.21.11:fabric").projectDir = file("1.21.11/fabric")
project(":1.21.11:neoforge").projectDir = file("1.21.11/neoforge")
project(":1.21.1").projectDir = file("1.21.1")
project(":1.21.1:runtime").projectDir = file("1.21.1/runtime")
project(":1.21.1:fabric").projectDir = file("1.21.1/fabric")
