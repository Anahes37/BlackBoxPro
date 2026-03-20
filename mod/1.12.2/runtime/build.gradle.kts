plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
}

group = "com.blackboxpro"
version = rootProject.version

base {
    archivesName.set("blackboxpro-1122-runtime")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":common"))
    implementation("com.google.code.gson:gson:2.8.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}
