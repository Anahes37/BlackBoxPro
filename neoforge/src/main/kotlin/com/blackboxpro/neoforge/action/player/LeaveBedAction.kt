package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class LeaveBedAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.LEAVE_BED,
    "Left bed"
)
