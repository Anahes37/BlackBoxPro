package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class SneakStartAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.PRESS_SHIFT_KEY,
    "Started sneaking"
)
