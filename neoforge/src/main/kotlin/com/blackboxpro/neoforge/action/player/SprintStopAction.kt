package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class SprintStopAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.STOP_SPRINTING,
    "Stopped sprinting"
)
