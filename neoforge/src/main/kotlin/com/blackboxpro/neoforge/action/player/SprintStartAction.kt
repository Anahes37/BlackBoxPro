package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class SprintStartAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.START_SPRINTING,
    "Started sprinting"
)
