package com.blackboxpro.plugin.command

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.plugin.channel.ChannelHandler
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.info
import taboolib.platform.util.sendLang

@CommandHeader("blackbox", permission = "blackbox.admin")
object BlackBoxCommand {

    private val gson = Gson()

    @CommandBody
    val main = mainCommand {
        execute<org.bukkit.command.CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §fServer Plugin v${com.blackboxpro.plugin.BlackBoxPro.VERSION}")
            sender.sendMessage("§7/blackbox send <player> <action> [params_json] §f- 发送指令")
            sender.sendMessage("§7/blackbox status §f- 查看状态")
        }
    }

    @CommandBody
    val send = subCommand {
        dynamic("player") {
            dynamic("action") {
                execute<org.bukkit.command.CommandSender> { sender, context, _ ->
                    val playerName = context["player"]
                    val action = context["action"]
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player == null) {
                        sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                        return@execute
                    }
                    BlackBoxApi.send(player, action)
                    sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}")
                }
                dynamic("params") {
                    execute<org.bukkit.command.CommandSender> { sender, context, _ ->
                        val playerName = context["player"]
                        val action = context["action"]
                        val paramsRaw = context["params"]
                        val player = Bukkit.getPlayerExact(playerName)
                        if (player == null) {
                            sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                            return@execute
                        }
                        val params = try {
                            gson.fromJson(paramsRaw, JsonObject::class.java) ?: JsonObject()
                        } catch (e: JsonSyntaxException) {
                            sender.sendMessage("§c[BlackBoxPro] 无效的 JSON 参数: ${e.message}")
                            return@execute
                        }
                        BlackBoxApi.sendAsync(player, action, params).thenAccept { response ->
                            sender.sendMessage("§6[BlackBoxPro] 响应: §f${response.status} §7${response.message ?: ""}")
                        }
                        sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，等待响应...")
                    }
                }
            }
        }
    }

    @CommandBody
    val status = subCommand {
        execute<org.bukkit.command.CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §f状态信息:")
            sender.sendMessage("§7  等待响应数: §f${ChannelHandler.pendingCount()}")
            sender.sendMessage("§7  在线玩家数: §f${Bukkit.getOnlinePlayers().size}")
        }
    }
}
