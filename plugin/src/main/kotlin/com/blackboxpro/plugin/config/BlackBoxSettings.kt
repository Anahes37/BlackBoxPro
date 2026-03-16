package com.blackboxpro.plugin.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * BlackBoxPro 服务端插件配置。
 */
object BlackBoxSettings {

    @Config("config.yml")
    lateinit var conf: Configuration
        private set

    val debug: Boolean get() = conf.getBoolean("debug", false)

    val responseTimeoutMs: Long get() = conf.getLong("response-timeout-ms", 5000L)

    val maxPayloadSize: Int get() = conf.getInt("max-payload-size", 32767)
}
