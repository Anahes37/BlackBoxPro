---
name: blackbox-runner
description: BlackBoxPro 测试环境管理。启动/停止服务端和客户端，构建部署，执行集成测试。支持 1.21.11 和 1.12.2 两个版本。
---

BlackBoxPro 测试环境统一管理技能。根据用户指定版本，控制服务端/客户端启停、构建、部署与测试。

## 当前仓库结构

- 根目录：`$CWD`（`F:\minecraft\mod\BlackBoxPro`）
- 1.21.11 Mod：`mod/1.21.11/fabric`、`mod/1.21.11/neoforge`
- 1.12.2 Mod：`mod/1.12.2/forge`
- Plugin：`plugin`

## 版本环境速查

### 1.21.11

| 项目 | 值 |
|------|-----|
| Java（Gradle） | `C:\Program Files\Java\jdk-21` |
| 服务端目录 | `E:\paper-1.21.11` |
| 服务端 JAR | `paper-1.21.11-97.jar` |
| 服务端 JVM | `-Xms4G -Xmx4G -XX:+UseG1GC` |
| 服务端 Java | `C:\Program Files\Java\jdk-21\bin\java.exe` |
| 客户端启动 | `runClient`（`mod/1.21.11/fabric` 子项目，待确认） |
| 玩家名 | `Player` |
| Mod 产物 | `$CWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar` |
| Plugin 产物 | `$CWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |

### 1.12.2

| 项目 | 值 |
|------|-----|
| Java（Gradle） | `C:\Program Files\Java\jdk-17`（Gradle 用 17，runClient 内部自动用 8） |
| 服务端目录 | `F:\minecraft\server\paper-1.12.2` |
| 服务端 JAR | `Paper-1.12.2-build1620.jar` |
| 服务端 JVM | `-Xms2G -Xmx4G -XX:+UseG1GC` |
| 服务端 Java | `C:\Program Files\Java\jdk-1.8\bin\java.exe` |
| 客户端启动 | `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew :forge:runClient --no-daemon`（在 `mod/1.12.2` 目录） |
| 玩家名 | `Developer`（runClient 开发模式默认名） |
| Mod 产物 | `$CWD\mod\1.12.2\forge\build\libs\BlackBoxPro-forge-1.12.2-*.jar` |
| Plugin 产物 | `$CWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |

## 构建

在仓库根目录执行（需指定 Java 21）：

```bash
# 1.21.11 mod（common + fabric + neoforge）
JAVA_HOME="/c/Program Files/Java/jdk-21" ./gradlew mod_buildAll --no-daemon

# plugin
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew plugin_build --no-daemon

# forge 1.12.2
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew forge1122_build --no-daemon

# 全量
JAVA_HOME="/c/Program Files/Java/jdk-21" ./gradlew buildAll --no-daemon
```

## 部署

Plugin jar 被服务端占用时无法覆盖，**必须先停服再部署再启服**。

### 1.12.2

```powershell
# 停服（见下方停止章节）
# 部署 plugin
Remove-Item 'F:\minecraft\server\paper-1.12.2\plugins\BlackBoxPro-Plugin-*.jar' -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" 'F:\minecraft\server\paper-1.12.2\plugins\' -Force
# 部署 mod（无需停客户端，runClient 重启即可）
# mod jar 在 mod/1.12.2/forge/build/libs/ 下，runClient 启动时自动加载
```

### 1.21.11

```powershell
Copy-Item "$PWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar" "E:\paper-1.21.11\mods\" -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" "E:\paper-1.21.11\plugins\" -Force
```

## 启动

### 服务端（Terminal 1）

1.12.2（先 cd 进服务端目录）：
```bash
cd /f/minecraft/server/paper-1.12.2
"/c/Program Files/Java/jdk-1.8/bin/java.exe" -Xms2G -Xmx4G -XX:+UseG1GC -jar Paper-1.12.2-build1620.jar nogui
```

1.21.11：
```bash
cd /e/paper-1.21.11
"/c/Program Files/Java/jdk-21/bin/java.exe" -Xms4G -Xmx4G -XX:+UseG1GC -jar paper-1.21.11-97.jar nogui
```

等待日志出现 `Done`。

### 客户端（Terminal 2）

1.12.2（在仓库根目录）：
```bash
cd /f/minecraft/mod/BlackBoxPro/mod/1.12.2
JAVA_HOME="/c/Program Files/Java/jdk-17" ./gradlew :forge:runClient --no-daemon
```

1.21.11：
```bash
# 待确认 runClient 任务
cd /f/minecraft/mod/BlackBoxPro
JAVA_HOME="/c/Program Files/Java/jdk-21" ./gradlew :1.21.11:fabric:runClient --no-daemon
```

等待服务端出现 `joined the game`，然后发 HTTP connect_to_server（见测试章节）。

## 停止

**禁止使用 RCON，所有停服通过 HTTP 接口完成。**

### 停止服务端

```bash
# 两个版本通用，HTTP 端口均为 8080
curl -s -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"stop","action":"stop_server"}'
```

### 停止客户端

- Terminal 写入 `\x03`（Ctrl-C）
- 若出现 `终止批处理操作吗(Y/N)?`，再写入 `Y\n`

## 连接服务器

客户端启动后默认在主菜单，通过 HTTP 让客户端自动连接：

```bash
curl -s -X POST http://localhost:8081/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"c1","action":"connect_to_server","params":{"ip":"127.0.0.1","port":25565}}'
```

> 注：客户端 HTTP 端口为 8081，服务端为 8080。

## 测试

```bash
# 全量测试（1.12.2 玩家名 Developer，1.21.11 玩家名 Player）
curl -s -X POST http://localhost:8080/execute \
  -H "Content-Type: application/json" \
  -d '{"id":"run-1","action":"run_test","params":{"player":"Developer","scope":"full"}}'
```

响应包含完整结果 JSON（阻塞直到完成，约 50s）。

截图路径（用例主动截图时落盘）：
- 1.12.2：`$CWD\mod\1.12.2\forge\run\screenshots\blackboxpro\`
- 1.21.11：待确认

## 注意事项

- 修改 Plugin 后必须停服 → 部署 → 重启服务端。
- 修改 Mod 后需重启客户端（停止 runClient 进程后重新执行）。
- 两个版本不要同时启动，都会占用端口 `25565`。
- 服务端 HTTP 端口 `8080`，客户端 HTTP 端口 `8081`。
- 禁止使用 RCON（已废弃）。
