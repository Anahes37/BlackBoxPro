plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

base {
    archivesName.set("BlackBoxPro-neoforge-${property("minecraft_version_121")}")
}

repositories {
    mavenCentral()
    maven { setUrl("https://thedarkcolour.github.io/KotlinForForge/") }
}

neoForge {
    version = providers.gradleProperty("neoforge_version_121").get()

    mods {
        create("blackboxpro") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
        }
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${providers.gradleProperty("kotlin_for_forge_version_121").get()}")
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
