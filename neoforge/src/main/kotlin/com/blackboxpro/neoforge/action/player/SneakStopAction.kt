package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class SneakStopAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.RELEASE_SHIFT_KEY,
    "Stopped sneaking"
)
