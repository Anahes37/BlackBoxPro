plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("net.neoforged.moddev") version "2.0.140"
}

base {
    archivesName.set("BlackBoxPro-runtime-1.21.1")
}

repositories {
    mavenCentral()
}

neoForge {
    neoFormVersion = "1.21.11-20251209.172050"
}

dependencies {
    api(project(":common"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}
