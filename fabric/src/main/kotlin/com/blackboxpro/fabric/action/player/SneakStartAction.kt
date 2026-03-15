package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class SneakStartAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY,
    "Started sneaking"
)
