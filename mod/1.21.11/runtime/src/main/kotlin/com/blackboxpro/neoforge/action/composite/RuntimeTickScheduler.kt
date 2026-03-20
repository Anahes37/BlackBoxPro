package com.blackboxpro.neoforge.action.composite

object RuntimeTickScheduler {

    interface Scheduler {
        fun schedule(delayTicks: Int, task: () -> Unit)
        fun clear()
    }

    private object ImmediateScheduler : Scheduler {
        override fun schedule(delayTicks: Int, task: () -> Unit) {
            task()
        }

        override fun clear() = Unit
    }

    @Volatile
    private var scheduler: Scheduler = ImmediateScheduler

    fun bind(scheduler: Scheduler) {
        this.scheduler = scheduler
    }

    fun schedule(delayTicks: Int, task: () -> Unit) {
        scheduler.schedule(delayTicks, task)
    }

    fun clear() {
        scheduler.clear()
    }
}
