import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("fabric-loom")
    id("org.jetbrains.kotlin.jvm")
}

evaluationDependsOn(":1.21.11:runtime")

val commonSourceSet = project(":common").the<SourceSetContainer>()["main"]
val runtimeSharedDir = project(":1.21.11:runtime").file("src/main/kotlin/com/blackboxpro/runtime")

the<SourceSetContainer>()["main"].java.srcDir(runtimeSharedDir)

base {
    archivesName.set("BlackBoxPro-fabric-${property("minecraft_version")}")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
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
    from(commonSourceSet.output)
}

tasks.named<Jar>("sourcesJar") {
    from(commonSourceSet.allSource)
}
