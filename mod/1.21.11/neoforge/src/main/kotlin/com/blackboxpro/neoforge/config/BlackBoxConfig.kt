package com.blackboxpro.neoforge.config

import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfigSnapshot

object BlackBoxConfig {
    val current get() = RuntimeBlackBoxConfig.current
    fun load() { /* 硬编码默认值，无需加载 */ }
}
