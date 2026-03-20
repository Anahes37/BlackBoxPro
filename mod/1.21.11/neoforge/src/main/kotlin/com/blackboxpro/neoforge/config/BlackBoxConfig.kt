package com.blackboxpro.neoforge.config

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfigSnapshot
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.neoforged.fml.loading.FMLPaths
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object BlackBoxConfig {
    private val logger = LoggerFactory.getLogger("BlackBoxPro-Config")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath: Path = FMLPaths.CONFIGDIR.get().resolve("blackboxpro-neoforge.json")

    @Volatile
    var current: RuntimeBlackBoxConfigSnapshot = RuntimeBlackBoxConfigSnapshot()
        private set

    fun load() {
        if (Files.exists(configPath)) {
            try {
                val json = Files.readString(configPath)
                apply(gson.fromJson(json, RuntimeBlackBoxConfigSnapshot::class.java) ?: RuntimeBlackBoxConfigSnapshot())
                logger.info("Config loaded from {}", configPath)
            } catch (e: Exception) {
                logger.error("Failed to load config, using defaults", e)
                apply(RuntimeBlackBoxConfigSnapshot())
                save()
            }
        } else {
            apply(RuntimeBlackBoxConfigSnapshot())
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

    private fun apply(config: RuntimeBlackBoxConfigSnapshot) {
        current = config
        RuntimeBlackBoxConfig.update(config)
    }
}
