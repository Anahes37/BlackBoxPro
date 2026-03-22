val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows

data class StandaloneProject(
    val prefix: String,
    val dir: File,
    val actions: List<String>,
    val gradlew: File = if (isWindows) dir.resolve("gradlew.bat") else dir.resolve("gradlew")
)

val standaloneProjects = mapOf(
    "mod" to StandaloneProject(
        prefix = "mod",
        dir = file("mod"),
        actions = listOf("build", "clean", "buildAll", "collectJars")
    ),
    "plugin" to StandaloneProject(
        prefix = "plugin",
        dir = file("plugin"),
        actions = listOf("build", "clean", "jar")
    ),
    "1.12.2" to StandaloneProject(
        prefix = "forge1122",
        dir = file("mod/1.12.2"),
        actions = listOf("build", "clean", "jar")
    )
)

for ((name, project) in standaloneProjects) {
    for (action in project.actions) {
        tasks.register<Exec>("${project.prefix}_$action") {
            group = "standalone"
            description = "$action 独立项目 $name"
            workingDir = project.dir
            commandLine(project.gradlew.absolutePath, "--no-daemon", action)
            isIgnoreExitValue = (action == "clean")
        }
    }
}

val collectJars = tasks.register<Sync>("collectJars") {
    group = "build"
    description = "收集客户端 mod 与服务端插件的 jar 到根 build/libs"
    into(layout.buildDirectory.dir("libs"))
    from(fileTree("mod/build/libs") { include("*.jar") })
    from(fileTree("plugin/build/libs") { include("*.jar") })
}

tasks.register("buildAll") {
    group = "build"
    description = "构建客户端 mod 与服务端插件并收集 jar 到根 build/libs"
    dependsOn("mod_buildAll", "plugin_build")
    finalizedBy(collectJars)
}

tasks.register("cleanAll") {
    group = "build"
    description = "清理所有模块（含根 build 目录）"
    dependsOn("mod_clean", "plugin_clean", "forge1122_clean")
    doLast { delete(layout.buildDirectory) }
}
