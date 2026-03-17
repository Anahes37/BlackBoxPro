plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
}

subprojects {
    group = "com.blackboxpro"
    version = rootProject.version
}

// ======================== 独立子项目委托构建 ========================
// plugin 和 forge-1.12.2 是独立 Gradle 项目（不同插件体系/JDK 版本），
// 无法通过 include 纳入根项目，这里通过 Exec 调用各自的 gradlew 避免锁冲突。

val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
val standaloneProjects = mapOf(
    "plugin"       to ("plugin"    to file("plugin")),
    "forge-1.12.2" to ("forge1122" to file("forge-1.12.2"))
)

for ((name, value) in standaloneProjects) {
    val (prefix, dir) = value
    val gradlew = if (isWindows) file("$dir/gradlew.bat") else file("$dir/gradlew")
    for (action in listOf("build", "clean", "jar")) {
        tasks.register<Exec>("${prefix}_$action") {
            group = "standalone"
            description = "$action 独立项目 $name"
            workingDir = dir
            commandLine(gradlew.absolutePath, action)
            isIgnoreExitValue = (action == "clean") // clean 容忍文件锁失败
        }
    }
}

val collectJars = tasks.register<Copy>("collectJars") {
    group = "build"
    description = "收集所有模块的 jar 到根 build/libs"
    into(layout.buildDirectory.dir("libs"))
    subprojects.forEach { from(it.layout.buildDirectory.dir("libs")) }
    standaloneProjects.values.forEach { (_, dir) -> from(dir.resolve("build/libs")) }
}

// buildAll: 构建所有模块（RFG 迁移后 forge-1.12.2 也可用 JDK 21 运行 Gradle）
tasks.register("buildAll") {
    group = "build"
    description = "构建所有模块并收集 jar 到根 build/libs"
    dependsOn(subprojects.map { ":${it.name}:build" })
    dependsOn("plugin_build", "forge1122_build")
    finalizedBy(collectJars)
}

tasks.register("cleanAll") {
    group = "build"
    description = "清理所有模块（含根 build 目录）"
    dependsOn(subprojects.map { ":${it.name}:clean" })
    dependsOn("plugin_clean", "forge1122_clean")
    doLast { delete(layout.buildDirectory) }
}
