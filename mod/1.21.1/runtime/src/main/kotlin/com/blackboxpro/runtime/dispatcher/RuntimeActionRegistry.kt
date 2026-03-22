package com.blackboxpro.runtime.dispatcher

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.action.ActionExecutor

class RuntimeActionRegistry(
    private val logger: LogHandler
) {
    private val mutableExecutors = mutableMapOf<String, ActionExecutor>()
    private var executors: Map<String, ActionExecutor> = emptyMap()
    private var frozen = false

    fun register(actionId: String, executor: ActionExecutor) {
        check(!frozen) { "ActionRegistry is frozen, cannot register new actions" }
        if (mutableExecutors.containsKey(actionId)) {
            logger.warn("Overriding executor for action: {}", actionId)
        }
        mutableExecutors[actionId] = executor
        logger.debug("Registered action: {}", actionId)
    }

    fun find(actionId: String): ActionExecutor? = executors[actionId]

    fun size(): Int = executors.size

    fun ensureNotInitialized() {
        check(!frozen) { "ActionRegistry already initialized" }
    }

    fun freezeAndValidate(expectedActionIds: Set<String>) {
        executors = mutableExecutors.toMap()
        frozen = true

        val actual = executors.keys
        val missing = expectedActionIds - actual
        val extra = actual - expectedActionIds

        if (missing.isNotEmpty()) {
            logger.warn("Action catalog mismatch, missing executors: {}", missing.joinToString(", "))
        }
        if (extra.isNotEmpty()) {
            logger.warn("Action catalog mismatch, untracked executors: {}", extra.joinToString(", "))
        }

        logger.info("All actions registered. Total: {}", executors.size)
    }
}
