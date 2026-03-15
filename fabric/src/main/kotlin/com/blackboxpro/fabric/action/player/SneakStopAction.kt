package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class SneakStopAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY,
    "Stopped sneaking"
)
