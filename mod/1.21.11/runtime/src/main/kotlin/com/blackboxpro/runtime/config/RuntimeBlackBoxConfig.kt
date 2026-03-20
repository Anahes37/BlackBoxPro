package com.blackboxpro.runtime.config

data class RuntimeLoggingConfig(
    val level: String = "INFO",
    val logCommands: Boolean = true,
    val logResponses: Boolean = true,
    val logPackets: Boolean = false
)

data class RuntimeNetworkConfig(
    val commandChannel: String = "blackbox:command",
    val responseChannel: String = "blackbox:response",
    val maxPayloadSize: Int = 32767
)

data class RuntimeExecutionConfig(
    val maxDelayTicks: Int = 6000,
    val maxBatchSize: Int = 100,
    val defaultBreakTicks: Int = 20
)

data class RuntimePathfindingConfig(
    val maxDistance: Double = 100.0,
    val stepSize: Double = 0.2158,
    val arrivalThreshold: Double = 0.5
)

data class RuntimeSafetyConfig(
    val enabled: Boolean = true,
    val allowedActions: Set<String> = emptySet(),
    val blockedActions: Set<String> = emptySet(),
    val requireServerHandshake: Boolean = false
)

data class RuntimeScreenshotConfig(
    val rootDirectory: String = "screenshots/blackboxpro",
    val maxPerTest: Int = 999
)

data class RuntimeNavigationConfig(
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

data class RuntimeBlackBoxConfigSnapshot(
    val logging: RuntimeLoggingConfig = RuntimeLoggingConfig(),
    val network: RuntimeNetworkConfig = RuntimeNetworkConfig(),
    val execution: RuntimeExecutionConfig = RuntimeExecutionConfig(),
    val pathfinding: RuntimePathfindingConfig = RuntimePathfindingConfig(),
    val safety: RuntimeSafetyConfig = RuntimeSafetyConfig(),
    val screenshot: RuntimeScreenshotConfig = RuntimeScreenshotConfig(),
    val navigation: RuntimeNavigationConfig = RuntimeNavigationConfig()
)

object RuntimeBlackBoxConfig {
    @Volatile
    var current: RuntimeBlackBoxConfigSnapshot = RuntimeBlackBoxConfigSnapshot()
        private set

    fun update(snapshot: RuntimeBlackBoxConfigSnapshot) {
        current = snapshot
    }
}
