package com.blackboxpro.forge.action.player

import net.minecraft.network.play.client.CPacketEntityAction

class SprintStartAction : PlayerCommandAction(
    CPacketEntityAction.Action.START_SPRINTING,
    "Started sprinting"
)
