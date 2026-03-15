package com.blackboxpro.neoforge.action.player

import net.minecraft.network.protocol.game.ServerboundClientCommandPacket

class OpenHorseInventoryAction : PlayerCommandAction(
    ServerboundClientCommandPacket.Mode.OPEN_HORSE_INVENTORY,
    "Opened horse inventory"
)
