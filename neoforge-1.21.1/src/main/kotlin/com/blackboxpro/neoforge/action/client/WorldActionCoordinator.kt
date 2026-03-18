package com.blackboxpro.neoforge.action.client

import org.slf4j.LoggerFactory

/**
 * 纯客户端世界管理动作的互斥协调器。
 *
 * create_world / join_world / leave_world 都属于世界状态迁移动作，
 * 同一时刻只允许一个处于进行中，避免动作互相踩状态，导致“命令已发出”
 * 但目标状态并未真正达成的半完成状态。
 */
object WorldActionCoordinator {

    private val logger = LoggerFactory.getLogger("BlackBoxPro-WorldActionCoordinator")

    private data class ActiveAction(
        val actionId: String,
        val commandId: String
    )

    @Volatile
    private var activeAction: ActiveAction? = null

    @Synchronized
    fun tryBegin(actionId: String, commandId: String): String? {
        val current = activeAction
        if (current != null) {
            logger.warn(
                "Rejected world action '{}' (id={}), active action is '{}' (id={})",
                actionId,
                commandId,
                current.actionId,
                current.commandId
            )
            return "Another world action is already in progress: ${current.actionId}"
        }

        activeAction = ActiveAction(actionId, commandId)
        logger.info("World action started: {} (id={})", actionId, commandId)
        return null
    }

    @Synchronized
    fun finish(actionId: String, commandId: String) {
        val current = activeAction
        if (current == null) {
            return
        }
        if (current.commandId != commandId) {
            logger.warn(
                "Ignoring finish for world action '{}' (id={}), active action is '{}' (id={})",
                actionId,
                commandId,
                current.actionId,
                current.commandId
            )
            return
        }

        activeAction = null
        logger.info("World action finished: {} (id={})", actionId, commandId)
    }
}
