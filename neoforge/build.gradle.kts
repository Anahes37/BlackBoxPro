plugins {
    id("net.neoforged.gradle.userdev") version "${property("neogradle_version")}"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

base {
    archivesName.set("blackboxpro-neoforge")
}

repositories {
    mavenCentral()
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

dependencies {
    implementation("net.neoforged:neoforge:${property("neoforge_version")}")
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}
