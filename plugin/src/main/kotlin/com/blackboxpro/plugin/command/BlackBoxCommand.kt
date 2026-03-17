package com.blackboxpro.plugin.command

import com.blackboxpro.plugin.BlackBoxPro
import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.plugin.channel.ChannelHandler
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestUncheck

@CommandHeader("blackbox", permission = "blackbox.admin")
object BlackBoxCommand {

    private val gson = Gson()

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §fServer Plugin v${BlackBoxPro.VERSION}")
            sender.sendMessage("§7/blackbox send <player> <action> [params_json] §f- 发送指令 (JSON)")
            sender.sendMessage("§7/blackbox exec <player> <action> [key:value ...] §f- 发送指令 (扁平化)")
            sender.sendMessage("§7/blackbox test <player> §f- 执行集成测试")
            sender.sendMessage("§7/blackbox status §f- 查看状态")
            sender.sendMessage("§7/blackbox reload §f- 重载配置")
        }
    }

    @CommandBody
    val send = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("action") {
                execute<CommandSender> { sender, context, _ ->
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
                    execute<CommandSender> { sender, context, _ ->
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
    val exec = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("action") {
                suggestUncheck { ActionParamRegistry.getActionIds() }
                execute<CommandSender> { sender, context, _ ->
                    executeFlat(sender, context["player"], context["action"], emptyList())
                }
                dynamic("params") {
                    suggestUncheck {
                        val action = ctx["action"]
                        val allParams = ActionParamRegistry.getParams(action) ?: return@suggestUncheck emptyList()
                        val currentInput = ctx.self()
                        val enteredKeys = currentInput.split(" ")
                            .filter { it.contains(':') }
                            .map { it.substringBefore(':') }
                            .toSet()
                        allParams.filterNot { it in enteredKeys }.map { "$it:" }
                    }
                    execute<CommandSender> { sender, context, _ ->
                        val rawParams = context["params"].split(" ").filter { it.isNotBlank() }
                        executeFlat(sender, context["player"], context["action"], rawParams)
                    }
                }
            }
        }
    }

    private fun executeFlat(sender: CommandSender, playerName: String, action: String, rawParams: List<String>) {
        val player = Bukkit.getPlayerExact(playerName)
        if (player == null) {
            sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
            return
        }
        val params = FlatParamParser.parse(rawParams)
        if (params.size() == 0) {
            BlackBoxApi.send(player, action)
            sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}")
        } else {
            // 使用 callback 模式避免 sendAsync 中的 CompletableFuture.delayedExecutor（Java 9+）
            BlackBoxApi.send(player, action, params) { response ->
                sender.sendMessage("§6[BlackBoxPro] 响应: §f${response.status} §7${response.message ?: ""}")
            }
            sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，参数: §7$params")
        }
    }

    @CommandBody
    val test = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val playerName = context["player"]
                val player = Bukkit.getPlayerExact(playerName)
                if (player == null) {
                    sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                    return@execute
                }
                BlackBoxTestRunner.runAll(player, sender)
            }
        }
    }

    @CommandBody
    val status = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §f状态信息:")
            sender.sendMessage("§7  版本: §f${BlackBoxPro.VERSION}")
            sender.sendMessage("§7  调试模式: §f${BlackBoxSettings.debug}")
            sender.sendMessage("§7  响应超时: §f${BlackBoxSettings.responseTimeoutMs}ms")
            sender.sendMessage("§7  等待响应数: §f${ChannelHandler.pendingCount()}")
            sender.sendMessage("§7  在线玩家数: §f${Bukkit.getOnlinePlayers().size}")
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            BlackBoxSettings.conf.reload()
            sender.sendMessage("§a[BlackBoxPro] 配置已重载。")
        }
    }
}
