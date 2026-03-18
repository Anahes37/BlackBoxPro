package com.blackboxpro.neoforge.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.neoforged.fml.loading.FMLPaths
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

data class LoggingConfig(
    val level: String = "INFO",
    val logCommands: Boolean = true,
    val logResponses: Boolean = true,
    val logPackets: Boolean = false
)

data class NetworkConfig(
    val commandChannel: String = "blackbox:command",
    val responseChannel: String = "blackbox:response",
    val maxPayloadSize: Int = 32767
)

data class ExecutionConfig(
    val maxDelayTicks: Int = 6000,
    val maxBatchSize: Int = 100,
    val defaultBreakTicks: Int = 20
)

data class PathfindingConfig(
    val maxDistance: Double = 100.0,
    val stepSize: Double = 0.2158,
    val arrivalThreshold: Double = 0.5
)

data class SafetyConfig(
    val enabled: Boolean = true,
    val allowedActions: Set<String> = emptySet(),
    val blockedActions: Set<String> = emptySet(),
    val requireServerHandshake: Boolean = false
)

data class ScreenshotConfig(
    val rootDirectory: String = "screenshots/blackboxpro",
    val maxPerTest: Int = 999
)

data class NavigationConfig(
    val maxDistance: Double = 100.0,
    val arrivalThreshold: Double = 0.5,
    val stepSize: Double = 0.2158,
    val maxIterations: Int = 5000,
    val maxPathLength: Int = 200,
    val defaultTimeout: Int = 600,
    val jumpCost: Double = 1.5,
    val fallCost: Double = 1.2,
    val nodeArrivalThreshold: Double = 0.3
)

data class HttpConfig(
    val enabled: Boolean = false,
    val port: Int = 25580,
    val bindAddress: String = "127.0.0.1"
)

data class BlackBoxConfig(
    val logging: LoggingConfig = LoggingConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val execution: ExecutionConfig = ExecutionConfig(),
    val pathfinding: PathfindingConfig = PathfindingConfig(),
    val safety: SafetyConfig = SafetyConfig(),
    val screenshot: ScreenshotConfig = ScreenshotConfig(),
    val navigation: NavigationConfig = NavigationConfig(),
    val http: HttpConfig = HttpConfig()
) {
    companion object {
        private val logger = LoggerFactory.getLogger("BlackBoxPro-Config")
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        private val configPath: Path = FMLPaths.CONFIGDIR.get().resolve("blackboxpro-neoforge.json")

        @Volatile
        var current: BlackBoxConfig = BlackBoxConfig()
            private set

        fun load() {
            if (Files.exists(configPath)) {
                try {
                    val json = Files.readString(configPath)
                    current = gson.fromJson(json, BlackBoxConfig::class.java) ?: BlackBoxConfig()
                    logger.info("Config loaded from {}", configPath)
                } catch (e: Exception) {
                    logger.error("Failed to load config, using defaults", e)
                    current = BlackBoxConfig()
                    save()
                }
            } else {
                current = BlackBoxConfig()
                save()
                logger.info("Default config created at {}", configPath)
            }
        }

        fun save() {
            try {
                Files.createDirectories(configPath.parent)
                Files.writeString(configPath, gson.toJson(current))
            } catch (e: Exception) {
                logger.error("Failed to save config", e)
            }
        }
    }
}
