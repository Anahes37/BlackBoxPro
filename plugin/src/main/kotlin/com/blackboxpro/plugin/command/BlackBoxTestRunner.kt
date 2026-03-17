package com.blackboxpro.plugin.command

import com.blackboxpro.plugin.api.action.*
import com.blackboxpro.plugin.channel.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture

/**
 * 集成测试运行器。
 *
 * 每个测试用例经历三个截图阶段：
 * - before：动作执行前的画面
 * - during：动作执行中的画面（持续性动作在中间态截图，瞬时动作在执行后立即截图）
 * - after / FAILED：动作完成后的画面
 *
 * 截图通过 [screenshot] 方法串入 Future 链，确保每张截图写入完成后再继续。
 */
object BlackBoxTestRunner {

    /**
     * @param start 启动动作，返回 Future
     * @param finish 收尾动作（持续性动作），为 null 表示瞬时动作
     */
    private data class TestCase(
        val name: String,
        val id: String,
        val start: (Player) -> CompletableFuture<ResponseMessage>,
        val finish: ((Player) -> CompletableFuture<ResponseMessage>)? = null
    )

    fun runAll(player: Player, sender: CommandSender) {
        val cases = buildCases(player)
        val testId = "integration_${System.currentTimeMillis() / 1000}"

        sender.sendMessage("§6[BlackBoxPro Test] §f开始执行 ${cases.size} 个测试用例...")
        sender.sendMessage("§7  截图会话: $testId")
        sender.sendMessage("")

        val results = mutableListOf<Triple<String, Boolean, String?>>()
        val startTime = System.currentTimeMillis()

        // 测试开始截图
        var chain = screenshot(player, testId, "00_test_start")

        cases.forEachIndexed { index, case ->
            chain = chain.thenCompose { delay(800L) }.thenCompose {
                val num = (index + 1).toString().padStart(2, '0')
                val tag = "${num}_${case.id}"
                val t0 = System.currentTimeMillis()

                // 阶段 1: before
                screenshot(player, testId, "${tag}_1_before").thenCompose {
                    // 阶段 2: 执行 start
                    case.start(player)
                }.thenCompose { startResponse ->
                    if (!startResponse.isSuccess) {
                        // 失败：截 during + FAILED
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            screenshot(player, testId, "${tag}_3_FAILED")
                        }.thenApply { startResponse }
                    } else if (case.finish != null) {
                        // 持续性动作：截 during → 执行 finish → 截 after
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            case.finish.invoke(player)
                        }.thenCompose { finishResponse ->
                            val suffix = if (finishResponse.isSuccess) "3_after" else "3_FAILED"
                            screenshot(player, testId, "${tag}_${suffix}").thenApply { finishResponse }
                        }
                    } else {
                        // 瞬时动作：截 during + after
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            screenshot(player, testId, "${tag}_3_after")
                        }.thenApply { startResponse }
                    }
                }.thenApply { finalResponse ->
                    val elapsed = System.currentTimeMillis() - t0
                    val passed = finalResponse.isSuccess
                    val reason = if (!passed) (finalResponse.message ?: finalResponse.status) else null
                    results.add(Triple(case.name, passed, reason))
                    if (passed) {
                        sender.sendMessage("§a[BlackBoxPro Test] §a✓ §f#${index + 1} ${case.name} §7(${elapsed}ms)")
                    } else {
                        sender.sendMessage("§c[BlackBoxPro Test] §c✗ §f#${index + 1} ${case.name} §c- $reason §7(${elapsed}ms)")
                    }
                }
            }
        }

        chain.thenCompose {
            delay(300L)
        }.thenCompose {
            screenshot(player, testId, "99_test_end")
        }.thenRun {
            val totalTime = System.currentTimeMillis() - startTime
            val passed = results.count { it.second }
            val failed = results.size - passed
            sender.sendMessage("")
            sender.sendMessage("§6[BlackBoxPro Test] §f完成: §a$passed/${results.size} 通过§f, §c$failed 失败 §7(总耗时 ${totalTime}ms)")
            if (failed > 0) {
                results.filter { !it.second }.forEach { (name, _, reason) ->
                    sender.sendMessage("§c  ✗ $name: $reason")
                }
            }
        }.exceptionally { ex ->
            // 异常时尝试截图（fire-and-forget）
            ScreenshotActions.screenshot(player, testId, "99_test_exception", player.name).exceptionally { null }
            sender.sendMessage("§c[BlackBoxPro Test] 测试异常中断: ${ex.message}")
            null
        }
    }

    private fun buildCases(player: Player): List<TestCase> {
        val loc = player.location
        return listOf(
            // === 通讯基础 ===
            TestCase("通讯基础 - PlayerOnGround", "ping",
                start = { p -> MovementActions.playerOnGround(p, true) }
            ),

            // === 移动控制 ===
            TestCase("移动控制 - PlayerMove", "move",
                start = { p -> MovementActions.playerMove(p, loc.x + 5, loc.y, loc.z, speed = 1.0, timeout = 100) }
            ),
            TestCase("移动控制 - PlayerMoveLook", "move_look",
                start = { p -> MovementActions.playerMoveLook(p, loc.x - 5, loc.y, loc.z, pitch = 0.0f, speed = 1.0, timeout = 100) }
            ),

            // === 视角控制 ===
            TestCase("视角控制 - LookAt", "look_at",
                start = { p -> CompositeActions.lookAt(p, 0.0, 100.0, 0.0) }
            ),
            TestCase("视角控制 - PlayerLook", "look",
                start = { p -> MovementActions.playerLook(p, 90.0f, -30.0f) }
            ),

            // === 玩家状态（持续性） ===
            TestCase("玩家状态 - 潜行", "sneak",
                start = { p ->
                    PlayerActions.sneakStart(p).thenCompose { CompositeActions.wait(p, 30) }
                },
                finish = { p -> PlayerActions.sneakStop(p) }
            ),
            TestCase("玩家状态 - 疾跑", "sprint",
                start = { p ->
                    PlayerActions.sprintStart(p).thenCompose { CompositeActions.wait(p, 30) }
                },
                finish = { p -> PlayerActions.sprintStop(p) }
            ),
            TestCase("玩家状态 - 跳跃", "jump",
                start = { p -> PlayerActions.jump(p) }
            ),
            TestCase("玩家状态 - 交换主副手", "swap_hands",
                start = { p -> PlayerActions.swapHands(p) }
            ),

            // === 实体交互 ===
            TestCase("手臂挥动 - 主手", "swing_main",
                start = { p -> EntityActions.swingArm(p, "main_hand") }
            ),
            TestCase("手臂挥动 - 副手", "swing_off",
                start = { p -> EntityActions.swingArm(p, "off_hand") }
            ),

            // === 聊天与命令 ===
            TestCase("聊天消息 - ChatMessage", "chat",
                start = { p -> ChatActions.chatMessage(p, "[BlackBoxPro] Integration test message") }
            ),
            TestCase("命令执行 - ChatCommand", "command",
                start = { p -> ChatActions.chatCommand(p, "me BlackBoxPro test") }
            ),

            // === 快捷栏（持续性） ===
            TestCase("快捷栏切换 - SetCarriedItem", "hotbar",
                start = { p -> ContainerActions.setCarriedItem(p, 4) },
                finish = { p -> ContainerActions.setCarriedItem(p, 0) }
            ),

            // === 查询 ===
            TestCase("查询 - 玩家状态", "query_state",
                start = { p -> QueryActions.queryPlayerState(p) }
            ),
            TestCase("查询 - 手持物品", "query_held",
                start = { p -> QueryActions.queryHeldItem(p) }
            ),
            TestCase("查询 - 附近实体", "query_entities",
                start = { p -> QueryActions.queryNearbyEntities(p, radius = 16.0) }
            ),
            TestCase("查询 - 聊天历史", "query_chat",
                start = { p -> QueryActions.queryChatHistory(p, count = 5) }
            ),
            TestCase("查询 - 药水效果", "query_effects",
                start = { p -> QueryActions.queryActiveEffects(p) }
            ),

            // === 复合行为 ===
            TestCase("复合行为 - Wait", "wait",
                start = { p -> CompositeActions.wait(p, 10) }
            ),
            TestCase("批量指令 - Batch", "batch",
                start = { p ->
                    CompositeActions.batch(p, listOf(
                        "sneak_start" to JsonObject(),
                        "wait" to JsonObject().apply { addProperty("ticks", 10) },
                        "swing_arm" to JsonObject().apply { addProperty("hand", "main_hand") },
                        "wait" to JsonObject().apply { addProperty("ticks", 10) },
                        "sneak_stop" to JsonObject()
                    ))
                }
            ),

            // === 截图 ===
            TestCase("截图功能 - Screenshot", "screenshot_test",
                start = { p -> ScreenshotActions.screenshot(p, "integration_verify", "final_check", p.name) }
            )
        )
    }

    /**
     * 截图并等待完成。串入 Future 链确保编号不冲突。
     */
    private fun screenshot(player: Player, testId: String, prefix: String): CompletableFuture<Unit> =
        ScreenshotActions.screenshot(player, testId, prefix, player.name)
            .thenApply { }
            .exceptionally { }

    private fun delay(ms: Long): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        // 将毫秒转换为 tick（1 tick = 50ms），最少 1 tick
        val ticks = (ms / 50).coerceAtLeast(1)
        submit(async = true, delay = ticks) { future.complete(Unit) }
        return future
    }
}
