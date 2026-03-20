import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

base {
    archivesName.set("BlackBoxPro-neoforge-${property("minecraft_version")}")
}

repositories {
    mavenCentral()
    maven { setUrl("https://thedarkcolour.github.io/KotlinForForge/") }
}

neoForge {
    version = property("neoforge_version").toString()
}

dependencies {
    implementation(project(":common"))
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

tasks.named<Jar>("jar") {
    from(project(":common").the<SourceSetContainer>()["main"].output)
}

tasks.named<Jar>("sourcesJar") {
    from(project(":common").the<SourceSetContainer>()["main"].allSource)
}
