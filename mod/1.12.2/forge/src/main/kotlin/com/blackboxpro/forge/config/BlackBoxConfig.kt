package com.blackboxpro.forge.config

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfigSnapshot
import com.blackboxpro.common.runtime.config.RuntimeExecutionConfig
import com.blackboxpro.common.runtime.config.RuntimeLoggingConfig
import com.blackboxpro.common.runtime.config.RuntimeNavigationConfig
import com.blackboxpro.common.runtime.config.RuntimeNetworkConfig
import com.blackboxpro.common.runtime.config.RuntimePathfindingConfig
import com.blackboxpro.common.runtime.config.RuntimeSafetyConfig
import com.blackboxpro.common.runtime.config.RuntimeScreenshotConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraftforge.fml.common.Loader
import org.apache.logging.log4j.LogManager
import java.io.File

data class LoggingConfig(
    val level: String = "INFO",
    val logCommands: Boolean = true,
    val logResponses: Boolean = true,
    val logPackets: Boolean = false
)

data class NetworkConfig(
    val httpPort: Int = 38081,
    val responseTimeoutMs: Long = 10000L
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

data class BlackBoxConfig(
    val logging: LoggingConfig = LoggingConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val execution: ExecutionConfig = ExecutionConfig(),
    val pathfinding: PathfindingConfig = PathfindingConfig(),
    val safety: SafetyConfig = SafetyConfig(),
    val screenshot: ScreenshotConfig = ScreenshotConfig(),
    val navigation: NavigationConfig = NavigationConfig()
) {
    companion object {
        private val logger = LogManager.getLogger("BlackBoxPro-Config")
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        @Volatile
        var current: BlackBoxConfig = BlackBoxConfig()
            private set

        fun load() {
            val configDir = Loader.instance().configDir
            val configFile = File(configDir, "blackboxpro-forge.json")

            if (configFile.exists()) {
                try {
                    val json = configFile.readText()
                    current = gson.fromJson(json, BlackBoxConfig::class.java) ?: BlackBoxConfig()
                    apply()
                    logger.info("Config loaded from {}", configFile.absolutePath)
                } catch (e: Exception) {
                    logger.error("Failed to load config, using defaults", e)
                    current = BlackBoxConfig()
                    save()
                }
            } else {
                current = BlackBoxConfig()
                save()
                apply()
                logger.info("Default config created at {}", configFile.absolutePath)
            }
        }

        fun save() {
            try {
                val configDir = Loader.instance().configDir
                val configFile = File(configDir, "blackboxpro-forge.json")
                configDir.mkdirs()
                configFile.writeText(gson.toJson(current))
            } catch (e: Exception) {
                logger.error("Failed to save config", e)
            }
        }

        private fun apply() {
            RuntimeBlackBoxConfig.update(
                RuntimeBlackBoxConfigSnapshot(
                    logging = RuntimeLoggingConfig(
                        level = current.logging.level,
                        logCommands = current.logging.logCommands,
                        logResponses = current.logging.logResponses,
                        logPackets = current.logging.logPackets
                    ),
                    network = RuntimeNetworkConfig(
                        httpPort = current.network.httpPort,
                        responseTimeoutMs = current.network.responseTimeoutMs
                    ),
                    execution = RuntimeExecutionConfig(
                        maxDelayTicks = current.execution.maxDelayTicks,
                        maxBatchSize = current.execution.maxBatchSize,
                        defaultBreakTicks = current.execution.defaultBreakTicks
                    ),
                    pathfinding = RuntimePathfindingConfig(
                        maxDistance = current.pathfinding.maxDistance,
                        stepSize = current.pathfinding.stepSize,
                        arrivalThreshold = current.pathfinding.arrivalThreshold
                    ),
                    safety = RuntimeSafetyConfig(
                        enabled = current.safety.enabled,
                        allowedActions = current.safety.allowedActions,
                        blockedActions = current.safety.blockedActions,
                        requireServerHandshake = current.safety.requireServerHandshake
                    ),
                    screenshot = RuntimeScreenshotConfig(
                        rootDirectory = current.screenshot.rootDirectory,
                        maxPerTest = current.screenshot.maxPerTest
                    ),
                    navigation = RuntimeNavigationConfig(
                        maxDistance = current.navigation.maxDistance,
                        arrivalThreshold = current.navigation.arrivalThreshold,
                        stepSize = current.navigation.stepSize,
                        maxIterations = current.navigation.maxIterations,
                        maxPathLength = current.navigation.maxPathLength,
                        defaultTimeout = current.navigation.defaultTimeout,
                        jumpCost = current.navigation.jumpCost,
                        fallCost = current.navigation.fallCost,
                        nodeArrivalThreshold = current.navigation.nodeArrivalThreshold
                    )
                )
            )
        }
    }
}
