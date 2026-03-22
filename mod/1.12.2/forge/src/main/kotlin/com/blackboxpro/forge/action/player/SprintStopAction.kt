package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class SprintStopAction : PlayerCommandAction(
    CPacketEntityAction.Action.STOP_SPRINTING,
    "Stopped sprinting"
)
