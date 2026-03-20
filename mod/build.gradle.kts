import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
}

val sharedProps = Properties().apply {
    file("gradle.properties").reader().use(::load)
}

allprojects {
    group = "com.blackboxpro"
    version = sharedProps.getProperty("version", "0.0.0")
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

val collectJars = tasks.register<Copy>("collectJars") {
    group = "build"
    description = "收集 common/fabric/neoforge 的 jar 到 mod/build/libs"
    into(layout.buildDirectory.dir("libs"))
    from(project(":common").layout.buildDirectory.dir("libs"))
    from(project(":1.21.11:fabric").layout.buildDirectory.dir("libs"))
    from(project(":1.21.11:neoforge").layout.buildDirectory.dir("libs"))
}

tasks.register("buildAll") {
    group = "build"
    description = "构建 common、fabric、neoforge 并收集 jar"
    dependsOn(":common:build", ":1.21.11:fabric:build", ":1.21.11:neoforge:build")
    finalizedBy(collectJars)
}
