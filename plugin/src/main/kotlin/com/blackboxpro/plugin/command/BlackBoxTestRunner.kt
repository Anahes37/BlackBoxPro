package com.blackboxpro.plugin.command

import com.blackboxpro.plugin.api.action.*
import com.blackboxpro.plugin.channel.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 集成测试运行器。
 *
 * 通过 /blackbox test <player> 触发，按顺序执行所有测试用例，
 * 验证 plugin↔mod 通讯和客户端行为控制。
 */
object BlackBoxTestRunner {

    private data class TestCase(
        val name: String,
        val run: (Player) -> CompletableFuture<ResponseMessage>
    )

    fun runAll(player: Player, sender: CommandSender) {
        val cases = buildCases(player)
        sender.sendMessage("§6[BlackBoxPro Test] §f开始执行 ${cases.size} 个测试用例...")
        sender.sendMessage("")

        val results = mutableListOf<Pair<String, Boolean>>()
        val startTime = System.currentTimeMillis()

        // 用 CompletableFuture 链串联，每个用例之间间隔 1 秒
        var chain = CompletableFuture.completedFuture<Unit>(Unit)

        cases.forEachIndexed { index, case ->
            chain = chain.thenCompose {
                delay(1000L)
            }.thenCompose {
                val t0 = System.currentTimeMillis()
                case.run(player).thenApply { response ->
                    val elapsed = System.currentTimeMillis() - t0
                    val passed = response.isSuccess
                    results.add(case.name to passed)
                    if (passed) {
                        sender.sendMessage("§a[BlackBoxPro Test] §a✓ §f#${index + 1} ${case.name} §7(${elapsed}ms)")
                    } else {
                        val reason = response.message ?: response.status
                        sender.sendMessage("§c[BlackBoxPro Test] §c✗ §f#${index + 1} ${case.name} §c- $reason §7(${elapsed}ms)")
                    }
                }
            }
        }

        chain.thenRun {
            val totalTime = System.currentTimeMillis() - startTime
            val passed = results.count { it.second }
            val failed = results.size - passed
            sender.sendMessage("")
            sender.sendMessage("§6[BlackBoxPro Test] §f完成: §a$passed/${results.size} 通过§f, §c$failed 失败 §7(总耗时 ${totalTime}ms)")
        }.exceptionally { ex ->
            sender.sendMessage("§c[BlackBoxPro Test] 测试异常中断: ${ex.message}")
            null
        }
    }

    private fun buildCases(player: Player): List<TestCase> {
        val loc = player.location
        return listOf(
            // #1 通讯基础 - Ping/Pong
            TestCase("通讯基础 - PlayerOnGround") { p ->
                MovementActions.playerOnGround(p, true)
            },
            // #2 移动控制
            TestCase("移动控制 - PlayerMove") { p ->
                MovementActions.playerMove(p, loc.x + 2, loc.y, loc.z)
            },
            // #3 视角控制
            TestCase("视角控制 - LookAt") { p ->
                CompositeActions.lookAt(p, 0.0, 100.0, 0.0)
            },
            // #4 玩家状态 - 潜行
            TestCase("玩家状态 - 潜行") { p ->
                PlayerActions.sneakStart(p).thenCompose {
                    CompositeActions.wait(p, 20)
                }.thenCompose {
                    PlayerActions.sneakStop(p)
                }
            },
            // #5 玩家状态 - 疾跑
            TestCase("玩家状态 - 疾跑") { p ->
                PlayerActions.sprintStart(p).thenCompose {
                    CompositeActions.wait(p, 20)
                }.thenCompose {
                    PlayerActions.sprintStop(p)
                }
            },
            // #6 聊天消息
            TestCase("聊天消息 - ChatMessage") { p ->
                ChatActions.chatMessage(p, "[BlackBoxPro] Integration test message")
            },
            // #7 命令执行
            TestCase("命令执行 - ChatCommand") { p ->
                ChatActions.chatCommand(p, "me BlackBoxPro test")
            },
            // #8 手臂挥动
            TestCase("手臂挥动 - SwingArm") { p ->
                EntityActions.swingArm(p, "main_hand")
            },
            // #9 复合行为 - 等待
            TestCase("复合行为 - Wait") { p ->
                CompositeActions.wait(p, 10)
            },
            // #10 批量指令 - Batch
            TestCase("批量指令 - Batch") { p ->
                CompositeActions.batch(p, listOf(
                    "sneak_start" to JsonObject(),
                    "wait" to JsonObject().apply { addProperty("ticks", 10) },
                    "sneak_stop" to JsonObject()
                ))
            }
        )
    }

    /**
     * 非阻塞延迟，返回一个在指定毫秒后完成的 Future。
     */
    private fun delay(ms: Long): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        CompletableFuture.delayedExecutor(ms, java.util.concurrent.TimeUnit.MILLISECONDS).execute {
            future.complete(Unit)
        }
        return future
    }
}
