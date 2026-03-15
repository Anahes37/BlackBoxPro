package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class HorseJumpStopAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.STOP_HORSE_JUMP,
    "Stopped horse jump"
)
