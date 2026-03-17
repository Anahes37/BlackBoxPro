package com.blackboxpro.forge.action.composite

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import org.apache.logging.log4j.LogManager

object TickScheduler {

    private val logger = LogManager.getLogger("BlackBoxPro-TickScheduler")

    private data class ScheduledTask(
        var remainingTicks: Int,
        val task: () -> Unit
    )

    private val tasks = ArrayDeque<ScheduledTask>()

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
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
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
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
