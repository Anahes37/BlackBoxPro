pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 独立运行 1.12.2 构建时自行接入 common；作为 mod 的 included build 时由上层先构建 common jar。
if (gradle.parent == null) {
    includeBuild("../../common")
}

rootProject.name = "BlackBoxPro-1122"

include("runtime", "forge")
