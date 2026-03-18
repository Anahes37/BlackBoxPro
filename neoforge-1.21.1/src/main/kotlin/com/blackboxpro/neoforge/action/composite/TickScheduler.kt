package com.blackboxpro.neoforge.action.composite

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.common.NeoForge
import org.slf4j.LoggerFactory

object TickScheduler {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-TickScheduler")

    private data class ScheduledTask(
        var remainingTicks: Int,
        val task: () -> Unit
    )

    private val tasks = ArrayDeque<ScheduledTask>()

    fun init() {
        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
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

    @SubscribeEvent
    fun onDisconnect(event: ClientPlayerNetworkEvent.LoggingOut) {
        val count = tasks.size
        tasks.clear()
        if (count > 0) {
            logger.info("Cleared {} scheduled tasks on disconnect", count)
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
