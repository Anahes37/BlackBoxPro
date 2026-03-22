import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev") version "2.0.140"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

evaluationDependsOn(":common")
evaluationDependsOn(":1.21.11:runtime")

val commonSourceSet = project(":common").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val runtimeSourceSet = project(":1.21.11:runtime").extensions.getByType(SourceSetContainer::class.java).getByName("main")
val localSourceSet = extensions.getByType(SourceSetContainer::class.java).getByName("main")

base {
    archivesName.set("BlackBoxPro-neoforge-1.21.1")
}

repositories {
    mavenCentral()
    maven { setUrl("https://thedarkcolour.github.io/KotlinForForge/") }
}

neoForge {
    version = property("neoforge_version").toString()

    runs {
        configureEach {
            val blackboxpro = mods.create("blackboxpro")
            blackboxpro.sourceSet(localSourceSet)
            blackboxpro.sourceSet(runtimeSourceSet)
            blackboxpro.sourceSet(commonSourceSet)
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":1.21.11:runtime"))
    implementation("thedarkcolour:kotlinforforge-neoforge:${property("kotlin_for_forge_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }

    from(runtimeSourceSet.resources)
    from(commonSourceSet.resources)
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
    from(runtimeSourceSet.output)
    from(commonSourceSet.output)
}

tasks.named<Jar>("sourcesJar") {
    from(runtimeSourceSet.allSource)
    from(commonSourceSet.allSource)
}
