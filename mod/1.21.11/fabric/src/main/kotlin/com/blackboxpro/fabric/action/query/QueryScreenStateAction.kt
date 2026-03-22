package com.blackboxpro.fabric.action.query

import com.blackboxpro.fabric.action.ActionExecutor
import com.blackboxpro.fabric.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.ingame.*

/**
 * 查询当前打开的屏幕/GUI 状态。
 * Action ID: "query_screen_state"
 */
class QueryScreenStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val client = MinecraftClient.getInstance()
        val screen = client.currentScreen

        val data = JsonObject().apply {
            addProperty("open", screen != null)
            addProperty("screenClass", screen?.javaClass?.simpleName ?: "none")
            addProperty("title", screen?.title?.string ?: "")

            if (screen is HandledScreen<*>) {
                addProperty("isContainer", true)
                val handler = client.player?.currentScreenHandler
                if (handler != null) {
                    addProperty("windowId", handler.syncId)
                    addProperty("slotCount", handler.slots.size)
                }
            } else {
                addProperty("isContainer", false)
            }

            addProperty("screenType", classifyScreen(screen))

            // DisconnectedScreen：补充断线原因和详情
            if (screen is DisconnectedScreen) {
                runCatching {
                    val f = DisconnectedScreen::class.java.getDeclaredField("reason")
                    f.isAccessible = true
                    val v = f.get(screen)
                    if (v != null) addProperty("reason", v.toString())
                }
                runCatching {
                    val f = DisconnectedScreen::class.java.getDeclaredField("info")
                    f.isAccessible = true
                    val v = f.get(screen)
                    if (v != null) addProperty("info", v.toString())
                }
            }
        }

        return ActionResult.ok("Screen state queried", data)
    }

    private fun classifyScreen(screen: net.minecraft.client.gui.screen.Screen?): String = when (screen) {
        null -> "none"
        is InventoryScreen -> "player_inventory"
        is CreativeInventoryScreen -> "creative_inventory"
        is GenericContainerScreen -> "generic_container"
        is Generic3x3ContainerScreen -> "generic_3x3"
        is ShulkerBoxScreen -> "shulker_box"
        is CraftingScreen -> "crafting_table"
        is FurnaceScreen -> "furnace"
        is SmokerScreen -> "smoker"
        is BlastFurnaceScreen -> "blast_furnace"
        is BrewingStandScreen -> "brewing_stand"
        is AnvilScreen -> "anvil"
        is EnchantmentScreen -> "enchanting_table"
        is GrindstoneScreen -> "grindstone"
        is LoomScreen -> "loom"
        is CartographyTableScreen -> "cartography_table"
        is StonecutterScreen -> "stonecutter"
        is SmithingScreen -> "smithing_table"
        is MerchantScreen -> "villager_trade"
        is HopperScreen -> "hopper"
        is BeaconScreen -> "beacon"
        is HorseScreen -> "horse"
        is BookScreen -> "book"
        is BookEditScreen -> "book_edit"
        is DisconnectedScreen -> "disconnected"
        is HandledScreen<*> -> "container_unknown"
        else -> "other"
    }
}
