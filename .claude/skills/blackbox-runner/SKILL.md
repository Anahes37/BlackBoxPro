---
name: blackbox-runner
description: BlackBoxPro 测试环境管理。启动/停止服务端和客户端，构建部署，执行集成测试。支持 1.21.11 和 1.12.2 两个版本。
---

BlackBoxPro 测试环境统一管理技能。根据用户指定版本，控制服务端/客户端启停、构建、部署与测试。

## 当前仓库结构

- 根目录：`$CWD`
- 1.21.11 Mod：`mod/1.21.11/fabric`、`mod/1.21.11/neoforge`
- 共享协议层：`mod/common`
- Plugin：`plugin`
- Forge 1.12.2：`forge-1.12.2`

## 版本环境速查

### 1.21.11

| 项目 | 值 |
|------|-----|
| Java | `E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe` |
| 服务端目录 | `E:\paper-1.21.11` |
| 服务端 JAR | `paper-1.21.11-97.jar` |
| 服务端 JVM | `-Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication` |
| 客户端目录 | `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4` |
| 客户端启动 | `powershell -NoProfile -Command "& 'I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\launch.bat'"` |
| 玩家名 | `Player` |
| Mod 产物 | `$CWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar` |
| Plugin 产物 | `$CWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |

### 1.12.2

| 项目 | 值 |
|------|-----|
| Java | `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe` |
| 服务端目录 | `E:\paper-1.12.2` |
| 服务端 JAR | `paper.jar` |
| 服务端 JVM | `-Xms2G -Xmx4G -XX:+UseG1GC` |
| RCON | 端口 `25575`，密码 `123456` |
| 客户端目录 | `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860` |
| 客户端启动 | `powershell -NoProfile -Command "& 'I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\start-client.bat'"` |
| 玩家名 | `BlackBoxTester` |
| Mod 产物 | `$CWD\forge-1.12.2\build\libs\BlackBoxPro-forge-1.12.2-*.jar` |
| Plugin 产物 | `$CWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar` |

## 构建

在仓库根目录执行：

```powershell
# 1.21.11 mod（common + fabric + neoforge）
.\gradlew mod_buildAll

# plugin
.\gradlew plugin_build

# forge 1.12.2
.\gradlew forge1122_build

# 全量
.\gradlew buildAll
```

## 部署

### 1.21.11

```powershell
Copy-Item "$PWD\mod\1.21.11\fabric\build\libs\BlackBoxPro-fabric-1.21.11-*.jar" "I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\mods\" -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" "E:\paper-1.21.11\plugins\" -Force
```

### 1.12.2

```powershell
Copy-Item "$PWD\forge-1.12.2\build\libs\BlackBoxPro-forge-1.12.2-*.jar" "I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\mods\" -Force
Copy-Item "$PWD\plugin\build\libs\BlackBoxPro-Plugin-*.jar" "E:\paper-1.12.2\plugins\" -Force
```

## 启动

### 服务端

1.21.11：
```powershell
Set-Location 'E:\paper-1.21.11'
& 'E:\AdoptOpenJDK\zulu21.36.17\bin\java.exe' -Xms4G -Xmx4G -XX:+UseG1GC -XX:+OptimizeStringConcat -XX:MaxGCPauseMillis=10 -XX:+UseStringDeduplication -jar 'paper-1.21.11-97.jar' nogui
```

1.12.2：
```powershell
Set-Location 'E:\paper-1.12.2'
& 'E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe' -Xms2G -Xmx4G -XX:+UseG1GC -jar 'paper.jar' nogui
```

等待日志出现 `Done`。

### 客户端

1.21.11：
```powershell
& 'I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\launch.bat'
```

1.12.2：
```powershell
& 'I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\start-client.bat'
```

等待服务端出现 `joined the game`。

## 停止

- 1.21.11 服务端：Terminal 写入 `stop\n`
- 1.21.11 客户端：Terminal 写入 `\x03`
- 1.12.2 客户端：Terminal 写入 `\x03`，出现 `终止批处理操作吗(Y/N)?` 后写入 `Y\n`
- 1.12.2 服务端：优先用下方 PowerShell RCON

## 1.12.2 RCON（PowerShell）

```powershell
function Invoke-BlackBoxRcon {
    param(
        [string]$Command,
        [string]$Host = '127.0.0.1',
        [int]$Port = 25575,
        [string]$Password = '123456'
    )

    $client = [System.Net.Sockets.TcpClient]::new($Host, $Port)
    $stream = $client.GetStream()
    $writer = New-Object System.IO.BinaryWriter($stream)
    $reader = New-Object System.IO.BinaryReader($stream)

    function Send-RconPacket([int]$RequestId, [int]$Type, [string]$Body) {
        $payload = [System.Text.Encoding]::UTF8.GetBytes($Body)
        $packetLength = 4 + 4 + $payload.Length + 2
        $writer.Write([BitConverter]::GetBytes($packetLength))
        $writer.Write([BitConverter]::GetBytes($RequestId))
        $writer.Write([BitConverter]::GetBytes($Type))
        $writer.Write($payload)
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Flush()
    }

    function Read-RconPacket {
        $length = $reader.ReadInt32()
        $requestId = $reader.ReadInt32()
        $type = $reader.ReadInt32()
        $bodyBytes = $reader.ReadBytes($length - 8)
        [pscustomobject]@{
            RequestId = $requestId
            Type = $type
            Body = [System.Text.Encoding]::UTF8.GetString($bodyBytes).TrimEnd([char]0)
        }
    }

    Send-RconPacket 0 3 $Password
    [void](Read-RconPacket)
    Send-RconPacket 1 2 $Command
    $response = Read-RconPacket
    $client.Close()
    return $response.Body
}
```

示例：
```powershell
Invoke-BlackBoxRcon 'blackbox test BlackBoxTester'
Invoke-BlackBoxRcon 'stop'
```

## 测试

- 1.21.11：服务端控制台执行 `blackbox test Player`
- 1.12.2：执行 `Invoke-BlackBoxRcon 'blackbox test BlackBoxTester'`

截图路径：
- `I:\PCL\.minecraft\versions\1.21.11-Fabric 0.18.4\screenshots\blackboxpro\Player\integration_<ts>\`
- `I:\PCL\.minecraft\versions\1.12.2-Forge_14.23.5.2860\screenshots\blackboxpro\BlackBoxTester\integration_<ts>\`

## 注意事项

- 修改 Plugin 后必须重启服务端。
- 修改 Mod 后必须重启客户端。
- 1.12.2 只用 PowerShell RCON，不用 Python。
- 两个版本不要同时启动，都会占用 `25565`。
