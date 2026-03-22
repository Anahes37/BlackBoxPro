import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import java.util.Properties

plugins {
    id("fabric-loom") version "1.14-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.140" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
}

val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
val gradlew1122 = if (isWindows) file("1.12.2/gradlew.bat") else file("1.12.2/gradlew")

val sharedProps = Properties()
file("gradle.properties").inputStream().use { sharedProps.load(it) }

allprojects {
    group = "com.blackboxpro"
    version = sharedProps.getProperty("version", "0.0.0")
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType(JavaCompile::class.java).configureEach {
        options.encoding = "UTF-8"
    }
}

val build1122 = tasks.register("build1122", Exec::class.java) {
    group = "build"
    description = "使用 1.12.2 独立 Gradle 构建 runtime 与 forge"
    dependsOn(":common:jar")
    workingDir = file("1.12.2")
    commandLine(gradlew1122.absolutePath, "--no-daemon", "build")
}

val clean1122 = tasks.register("clean1122", Exec::class.java) {
    group = "build"
    description = "清理 1.12.2 独立 Gradle 构建产物"
    workingDir = file("1.12.2")
    commandLine(gradlew1122.absolutePath, "--no-daemon", "clean")
    isIgnoreExitValue = true
}

val collectJars = tasks.register("collectJars", Copy::class.java) {
    group = "build"
    description = "收集 common/runtime/fabric/neoforge/1.12.2 的 jar 到 mod/build/libs"
    dependsOn(build1122)
    into(layout.buildDirectory.dir("libs"))
    from(project(":common").layout.buildDirectory.dir("libs"))
    from(project(":1.21.11:runtime").layout.buildDirectory.dir("libs"))
    from(project(":1.21.11:fabric").layout.buildDirectory.dir("libs"))
    from(project(":1.21.11:neoforge").layout.buildDirectory.dir("libs"))
    from(file("1.12.2/build/libs"))
}

tasks.register("buildAll") {
    group = "build"
    description = "构建 common、runtime、fabric、neoforge、1.12.2 并收集 jar"
    dependsOn(":common:build", ":1.21.11:runtime:build", ":1.21.11:fabric:build", ":1.21.11:neoforge:build", build1122)
    finalizedBy(collectJars)
}
