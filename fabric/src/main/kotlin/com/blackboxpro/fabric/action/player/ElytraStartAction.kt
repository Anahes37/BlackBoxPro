package com.blackboxpro.fabric.action.player

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

class ElytraStartAction : PlayerCommandAction(
    ClientCommandC2SPacket.Mode.START_FLYING_WITH_ELYTRA,
    "Started elytra flight"
)
