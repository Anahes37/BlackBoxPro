package com.blackboxpro.fabric.action.composite

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import org.slf4j.LoggerFactory

object TickScheduler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-TickScheduler")

    private data class ScheduledTask(
        var remainingTicks: Int,
        val task: () -> Unit
    )

    // 所有操作限定在主线程，无需 ConcurrentLinkedQueue
    private val tasks = ArrayDeque<ScheduledTask>()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            // 收集到期任务到临时列表，避免迭代中执行 task 导致重入问题
            val ready = mutableListOf<() -> Unit>()
            val iterator = tasks.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                entry.remainingTicks--
                if (entry.remainingTicks <= 0) {
                    iterator.remove()
                    ready.add(entry.task)
                }
            }
            ready.forEach { it() }
        }

        // 断线时清理所有残留任务
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            val count = tasks.size
            tasks.clear()
            if (count > 0) {
                logger.info("Cleared {} scheduled tasks on disconnect", count)
            }
        }
    }

    fun schedule(delayTicks: Int, task: () -> Unit) {
        if (delayTicks <= 0) {
            task()
        } else {
            tasks.addLast(ScheduledTask(delayTicks, task))
        }
    }

    fun clear() {
        tasks.clear()
    }
}
