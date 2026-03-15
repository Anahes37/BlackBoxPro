package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class HorseJumpStartAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.START_HORSE_JUMP,
    "Started horse jump"
)
