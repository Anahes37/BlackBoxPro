import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.1"
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

val commonJar = rootProject.extra["commonJar"] as File
val commonJarFiles = files(commonJar)

sourceSets {
    main {
        java.srcDir(rootProject.file("runtime/src/main/kotlin"))
    }
}

configurations.create("embeddedCommon")

base.archivesName.set("BlackBoxPro-forge-1.12.2")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion.set("1.12.2")
    mcpMappingChannel.set("stable")
    mcpMappingVersion.set("39")

    extraRunJvmArguments.addAll("-ea:com.blackboxpro")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(commonJarFiles)
    add("embeddedCommon", commonJarFiles)
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.25")
    implementation("com.google.code.gson:gson:2.8.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)
    inputs.property("mcversion", "1.12.2")

    filesMatching("mcmod.info") {
        expand(mapOf("version" to project.version, "mcversion" to "1.12.2"))
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.getByName("embeddedCommon").files.map { zipTree(it) }
    })
    from({
        configurations.getByName("runtimeClasspath").files
            .filter { it.name.startsWith("kotlin-") }
            .map { zipTree(it) }
    })
}
