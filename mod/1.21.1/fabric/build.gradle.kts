import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import java.util.Properties

plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

// 直接从本版本目录读取独立的版本属性，不依赖父项目注入
val localProps = Properties().apply {
    projectDir.parentFile.resolve("gradle.properties").reader().use(::load)
}
fun localProp(key: String) = localProps.getProperty(key)
    ?: error("Missing property '$key' in mod/1.21.1/gradle.properties")

evaluationDependsOn(":1.21.1:runtime")

val commonSourceSet = project(":common").the<SourceSetContainer>()["main"]
val runtimeSharedDir = project(":1.21.1:runtime").file("src/main/kotlin/com/blackboxpro/runtime")

the<SourceSetContainer>()["main"].java.srcDir(runtimeSharedDir)

base {
    archivesName.set("BlackBoxPro-fabric-${localProp("minecraft_version")}")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    minecraft("com.mojang:minecraft:${localProp("minecraft_version")}")
    mappings("net.fabricmc:yarn:${localProp("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${localProp("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${localProp("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${localProp("fabric_kotlin_version")}")
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
