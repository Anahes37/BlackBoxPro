---
description: "控制 Minecraft Paper 1.12.2 服务端。在 Terminal 1 中启动、停止、重启服务端，或执行服务端控制台命令。"
argument-hint: "<start|stop|restart|cmd|status|log> [args...]"
allowed-tools: [Terminal, Bash, Read]
---

# Minecraft 1.12.2 Server Controller (Terminal 1)

用户执行了: `/server-1.12.2 $ARGUMENTS`

## 环境配置

- Java 路径: `E:\AdoptOpenJDK\zulu8.0.422\bin\java.exe`
- 服务端目录: `E:\paper-1.12.2`
- JAR 文件: `paper.jar`
- JVM 参数: `-Xms2G -Xmx4G -XX:+UseG1GC`
- RCON: 端口 25575，密码 `123456`
- 终端: Terminal 1（通过 `Terminal list` 获取，选择列表中的第一个终端）

## 操作流程

### 通用步骤：获取终端 ID

每次操作前，先用 `Terminal list` 获取所有终端，选择名称包含 "Terminal 1" 或列表中的第一个终端作为服务端终端。

### start - 启动服务端

1. 用 `Terminal read` 读取最后 3 行，确认终端空闲（有 `$` 提示符）
2. 如果终端不空闲（服务端可能已在运行），提示用户先 stop
3. 发送启动命令:
   ```
   cd E:/paper-1.12.2 && "E:/AdoptOpenJDK/zulu8.0.422/bin/java.exe" -Xms2G -Xmx4G -XX:+UseG1GC -jar paper.jar nogui
   ```
4. 每隔几秒用 `Terminal read` 读取最后 10 行，检查是否出现 `Done` 关键字
5. 出现 `Done` 后报告启动成功
6. 如果 60 秒内未出现 `Done`，报告可能启动失败并展示最近日志

### stop - 停止服务端

1. 优先通过 RCON 发送 stop 命令（避免终端 readline 模式问题）:
   ```python
   python -c "
   import socket, struct
   def rcon(cmd):
       s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
       s.settimeout(10)
       s.connect(('127.0.0.1', 25575))
       pw = b'123456\x00\x00'
       p = struct.pack('<ii', 0, 3) + pw
       s.send(struct.pack('<i', len(p)) + p)
       s.recv(4096)
       p = struct.pack('<ii', 1, 2) + cmd.encode() + b'\x00\x00'
       s.send(struct.pack('<i', len(p)) + p)
       r = s.recv(4096)
       s.close()
   rcon('stop')
   "
   ```
2. 等待几秒后用 `Terminal read` 确认服务端已关闭（出现 `$` 提示符）

### restart - 重启服务端

1. 先执行 stop 流程
2. 确认停止后执行 start 流程

### cmd - 执行服务端命令

参数格式: `/server-1.12.2 cmd <command>`

通过 RCON 执行命令（避免终端 readline 模式问题）:
```python
python -c "
import socket, struct
def rcon(cmd):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(10)
    s.connect(('127.0.0.1', 25575))
    pw = b'123456\x00\x00'
    p = struct.pack('<ii', 0, 3) + pw
    s.send(struct.pack('<i', len(p)) + p)
    s.recv(4096)
    p = struct.pack('<ii', 1, 2) + cmd.encode() + b'\x00\x00'
    s.send(struct.pack('<i', len(p)) + p)
    r = s.recv(4096)
    l = struct.unpack('<i', r[:4])[0]
    body = r[12:4+l-2].decode('utf-8', errors='replace')
    s.close()
    return body
print(rcon('<command>'))
"
```

### status - 查看状态

1. 检查端口 25565 是否在监听: `netstat -ano | grep "25565.*LISTEN"`
2. 用 `Terminal read` 读取最后 10 行

### log - 查看日志

参数格式: `/server-1.12.2 log [n]`，n 默认 30

用 `Terminal read` 读取最后 n 行并展示。

## 注意事项

- Paper 1.12.2 的终端有 readline 模式，直接 `Terminal write` 发送命令可能不被执行，优先使用 RCON
- TabooLib 插件不支持 PlugMan 热重载，修改插件后必须重启服务端
- 服务端使用 JDK 8（zulu8.0.422）
