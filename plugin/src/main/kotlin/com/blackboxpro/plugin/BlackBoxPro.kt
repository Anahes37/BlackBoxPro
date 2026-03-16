package com.blackboxpro.plugin

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

/**
 * BlackBoxPro 服务端插件主入口。
 *
 * 通过 TabooLib 自动加载，无需手动注册。
 * ChannelHandler 通过 @Awake 自动注册通道。
 */
object BlackBoxPro : Plugin() {

    const val VERSION = "1.0.0"

    override fun onEnable() {
        info("[BlackBoxPro] Server plugin v$VERSION enabled.")
    }

    override fun onDisable() {
        info("[BlackBoxPro] Server plugin disabled.")
    }
}
