# BlackBoxFabric 项目创建计划

## 目标
创建 Fabric 1.21.11 Kotlin Mod 项目骨架，暂不实现通讯和客户端行为。

## 版本信息（基于 fabricmc.net 官方公告）
- Minecraft: 1.21.11
- Fabric Loader: 0.18.0
- Fabric Loom: 1.14
- Fabric API: 0.139.5+1.21.11
- fabric-language-kotlin: 1.13.4+kotlin.2.2.0
- Yarn Mappings: 1.21.11+build.1
- Kotlin: 2.2.0
- Java: 21

## 文件结构
```
BlackBoxFabric/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/blackbox/fabric/
│       │       └── BlackBoxFabric.kt          # ClientModInitializer 入口
│       └── resources/
│           └── fabric.mod.json
```

## 实现步骤
1. gradle.properties - 版本属性
2. settings.gradle.kts - 项目名
3. build.gradle.kts - 构建配置
4. gradle-wrapper.properties - Gradle 8.x
5. fabric.mod.json - Mod 元数据
6. BlackBoxFabric.kt - 空的客户端入口
