package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class ElytraStartAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.START_FLYING_WITH_ELYTRA,
    "Started elytra flight"
)
