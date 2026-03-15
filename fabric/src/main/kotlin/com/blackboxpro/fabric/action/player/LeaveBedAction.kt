package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class LeaveBedAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.LEAVE_BED,
    "Left bed"
)
